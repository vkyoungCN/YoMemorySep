package com.vkyoungcn.smartdevices.yomemory;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapter.LearningViewPrAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningTimeUpDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
* 本页用于“边学边建”模式下的学习环节。
* 进入本Activity后，在新线程中从DB取数据，
* 数据取好后，附加一条“ending伪数据”，作为准结束页。
*
*（边学边建：取36个词，保持一个item列表。不需要新建虚拟，因为group类不持有相关字段。）
*
* （真结束页是跳转的某专用Activity，跳转后将无法返回；而伪结束作为一个CardView，是可以向前滑动的）；
*
* 计时终止时，若已完成（全对），不论当前处于哪一页，都是直接自动跳转，生成新组。
* 计时终止时，若已全部完成但并非全对，则①建组，②错误的item直接记ERR+1。
* 计时终止时，若未完成，则对已完成部分建组（参照上一条规则）；未完成的（因并未做chose标记）无处理。
* 时间到后，没有DFG弹出，直接跳转终结页面。
*
* 需要持有一个boolean列表，用于标识所有items-VE的填写正误情况。
*
* 可自由向前、向后滑动（不再进行限制）。
* 前滑时VE数据保持不变（保持一个VE填写列表，回滚时从列表读取数据；刚加载时列表显然是空的。）
* VE中的正确填写监听器，仍然保留，用于计录本次学习的正误记录
* */
public class LearningCreateActivity extends AppCompatActivity implements OnGeneralDfgInteraction, ValidatingEditor.OnValidatingEditorInputListener {
    private static final String TAG = "LearningActivity";

    public static final int CREATE_IN_ORDER = 2041;
    public static final int CREATE_RANDOM = 2042;

    private int groupId;//最后的结果页面需要获取分组信息
//    private DBGroup dbGroup;//根据传来的gid从DB获取，或从DB获取36个词临时生成（暂不存入DB,也不记Item chose）
//    private RVGroup rvGroup;//需要使用MS等来计算（单次允许的复习）时间

    private int learningType;//区分普通学习、边学边建（顺序）、边学边建（随机）

    private String tableNameSuffix;//用来从DB获取本组所属的ITEMS
//    private String groupSubItemIdsStr;//用来从DB获取本组所属的ITEMS
    private ArrayList<SingleItem> items;//数据源（未初始化）
//    private String newLogs="";//用于回传到调用act的字串，新log
    private ArrayList<Boolean> itemsVeRightOrWrong;//记录每条的学习正误情况，在预取数据时一并初始化。

    private boolean autoSliding = true;//（在VE填写正确时）自动向后滑动的设置开关。

    private Thread timingThread;//采用全局变量便于到时间后终结之。【如果已滑动到ending页则直接停止计时，避免最后timeUp消息的产生】
    private Boolean prolonged = false;//计时完成如果还没有完成复习，可以延长时间一次（暂定15分钟，暂定不影响log计时）

    private int timeCount = 60;//【调试期间临时设置为1分钟】默认执行60次for循环，即1h。（for循环包含60次1秒间隔的执行，一次完整的for要1min）
    private long startingTimeMillis;//学习开始的时间。（需要在……时间内完成，否则拆分）【最后的时间区间计算可令开始时间大于下限，结束小于上限】
    private int timePastInMinute = 0;//流逝分钟数
    private int timeInSecond = 0;

//    private Boolean prepared = false;
    private boolean learningFinishedCorrectly = false;//本组学习完成。完成后置true，计时线程要检测之，避免完成后重新计时（因为代码顺序靠后BUG)
    private boolean learningFinishedWithWrong = false;//用于“在尾卡上等待直到时间耗光了的情况”。（如果是全对到达尾卡，计时就自动停止了。）
    private Handler handler = new LearningActivityHandler(this);

//    private int scrollablePage = 1;//目前可自由滑动的页数范围。只增不减。【现在无限制了】

    private YoMemoryDbHelper memoryDbHelper;

    private FrameLayout fltMask;
    private TextView tv_mask;
    private ViewPager viewPager;//原来是自定义的HalfScrollableVP
    private TextView timePastMin;
    private TextView timePastScd;
    private TextView totalMinutes;//应在xx分钟内完成，的数字部分。

    private TextView tv_currentPageNum;
    private int maxLearnedAmount;//用于判断已滑动过了多少词。当向回滑动时，此数字不减小。
    private TextView tv_totalPageNum;
    private TextView confirmAndFinish;//额外复习完成后的返回按钮，初始隐藏。

    public static final int RESULT_LEARNING_SUCCEEDED = 3020;
//    public static final int RESULT_EXTRA_LEARNING_SUCCEEDED = 3021;
//    public static final int RESULT_EXTRA_LEARNING_SUCCEEDED_UNDER24H = 3022;
    public static final int RESULT_LEARNING_FAILED = 3030;

    public static final int MESSAGE_DB_DATE_FETCHED =5101;
    public static final int MESSAGE_ONE_MINUTE_CHANGE =5102;
    public static final int MESSAGE_ONE_SECOND_CHANGE =5103;
    public static final int MESSAGE_TIME_UP = 5104;
//    public static final int MESSAGE_LOGS_SAVED = 5105;
//    public static final int MESSAGE_EXTRA_LEARNING_ACCOMPLISHED = 5106;
//    public static final int MESSAGE_EXTRA_LEARNING_UNDER_1H = 5107;
//    public static final int MESSAGE_EXTRA_LEARNING_1H_24H = 5108;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_learning);

        //从Intent获取参数
        groupId = getIntent().getBundleExtra("BUNDLE_GROUP_ID").getInt("GROUP_ID_TO_LEARN");
        learningType = getIntent().getIntExtra("LEARNING_TYPE",0);
        tableNameSuffix = getIntent().getStringExtra("TABLE_SUFFIX");//各种learningType都有TableSuffix



        fltMask = (FrameLayout) findViewById(R.id.flt_mask_learningPage);
        tv_mask = (TextView)findViewById(R.id.tv_onItsWay_learningPage);
        timePastMin = (TextView)findViewById(R.id.tv_time_past_numMinute_Learning);
        timePastScd = (TextView)findViewById(R.id.tv_time_past_numSecond_Learning);
        tv_currentPageNum = (TextView)findViewById(R.id.currentPageNum_learningActivity);
        tv_totalPageNum = (TextView)findViewById(R.id.totalPageNum_learningActivity);//总数字需要在数据加载完成后设置，在handleMessage中处理
        totalMinutes = (TextView) findViewById(R.id.tv_num_itemLearningActivity);
        confirmAndFinish = (TextView)findViewById(R.id.learningFinish) ;

        viewPager = (ViewPager) findViewById(R.id.viewPager_ItemLearning);
       /* if(learningType == R.color.colorGP_Newly) {
            viewPager.setScrollable(true);//初学状态，vp可以直接滑动
        }*/

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                //设置底端页码显示逻辑
                //当页面滑动时为下方的textView设置当前页数，但是只在开始滑动后才有效果，初始进入时需要手动XML设为1
                if(maxLearnedAmount <position+1){
                    maxLearnedAmount = position+1;//只加不减
                }
                tv_currentPageNum.setText(String.valueOf(position+1));//索引从0起需要加1


                /*
                 * 在尾页（卡片）上，
                 * ①取消页脚的页数显示；
                 * ②到达伪数据页后，检测是否已经填写了所有的VE。若未完成，继续计时，提示第xx个未完成；
                 * 若已完成，计时停止，在点击结束按钮后，跳转结束页。（提示请及时保存学习记录，以免程序被系统强制退出后丢失学习数据。）
                 * */
                //滑动到最后一页时
                if(position==items.size()-1){
                    //最后一张【Ending伪数据页】

                    //检测VE正确情况，如果全对则设置结束标记；
                    //如果仍然有错误的，继续计时。可以手动停止，并选择是拆分还是记录一次错误。
                    if(itemsVeRightOrWrong.indexOf(false)==-1){
                        //说明没有错误的
                        learningFinishedCorrectly = true;
//                        timingThread.interrupt();//先结束计时线程。

                        //下方结束按钮改为显示

                    }else {
                        //未能完全正确，选择两种结束方式之一。计时继续
                        //如果在这种状态下计时已到,由用户在DFG中决定是拆、记错还是整体放弃。
                        learningFinishedWithWrong = true;

                        //提示尚有几个错误的
                        int wrongNum = 0;
                        for (Boolean b :
                                itemsVeRightOrWrong) {
                            if (!b){wrongNum++;}
                        }
                        TextView tv_notAllCorrect;
                        tv_notAllCorrect.setText(String.format(getResources().getString(R.string.still_has_wrong_item),wrongNum));

                    }

                    tv_currentPageNum.setText("--");//总不能显示比总数还+1.

                    //


                    /*计划改由真正的专用结束页负责
                    //显示信息：学习记录保存中，请稍等……同时执行向DB写log
                    tv_mask.setText("学习记录保存中，请稍等");
                    fltMask.setVisibility(View.VISIBLE);
                    //在新线程处理log的DB保存操作。
                    new Thread(new learningFinishedRunnable()).start();
*/

                }
            }
        });

            //从db查询List<SingleItem>放在另一线程
            new Thread(new PreparingRunnable()).start();         // start thread

        //后期增加：①items可选顺序随机；
        // ②增加倒计时欢迎页面；
    }

    final static class LearningActivityHandler extends Handler {
        private final WeakReference<LearningCreateActivity> activity;

        private LearningActivityHandler(LearningCreateActivity activity) {
            this.activity = new WeakReference<LearningCreateActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LearningCreateActivity itemLearningActivity = activity.get();
            if(itemLearningActivity != null){
                itemLearningActivity.handleMessage(msg);
            }

    }};

    public class PreparingRunnable implements Runnable {
//        private static final String TAG = "PreparingRunnable";

        @Override
        public void run() {
            //从DB准备数据
            memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());
            if(learningType == LEARNING_GENERAL) {
                items = memoryDbHelper.getItemsByGroupId(groupId, tableNameSuffix);
            }else if(learningType == LEARNING_AND_CREATE_ORDER){
                ArrayList<Integer> idList = memoryDbHelper.getCertainAmountItemIdsOrderly(36,tableNameSuffix);//此方法只能获取id列表。【注意词条暂未标记为chose】
                items = memoryDbHelper.getItemsWithList(idList,tableNameSuffix);
            }else if(learningType == LEARNING_AND_CREATE_RANDOM){
                ArrayList<Integer> idList = memoryDbHelper.getCertainAmountItemIdsRandomly(36,tableNameSuffix);//此方法只能获取id列表。【注意词条暂未标记为chose】
                items = memoryDbHelper.getItemsWithList(idList,tableNameSuffix);
            }else {
                //出错逻辑暂未处理。
            }
            /* dbGroup = memoryDbHelper.getGroupById(groupId,tableNameSuffix);
            rvGroup = new RVGroup(dbGroup);*/

            //在Items列表数据的最后附加一条“伪数据”，用于伪完成页显示。
            SingleItem endingItem = new SingleItem(0,"完成","","",true,0,true,(short) 0,(short) 0);
            items.add(endingItem);

            //将学习记录初始化（全否）。
            itemsVeRightOrWrong = new ArrayList<>();
            for(int i =0;i<items.size();i++) {
                itemsVeRightOrWrong.add(false);
            }

            Message message = new Message();
            message.what = MESSAGE_DB_DATE_FETCHED;

            handler.sendMessage(message);
        }
    }

    public class learningFinishedRunnable implements Runnable {
//        private static final String TAG = "learningFinishedRunnabl";

        @Override
        public void run() {

        }
    }

    //用于计时并发送更新UI上时间值的消息
    public class TimingRunnable implements Runnable {
//        private static final String TAG = "TimingRunnable";

        @Override
        public void run() {
            while(!learningFinishedCorrectly && timeCount > 0){
                try {
                    for (int i = 0; i < 60&&!learningFinishedCorrectly; i++) {//这样才能在学习完成而分钟数未到的情况下终止计时。
                        Thread.sleep(1000);     // sleep 1 秒；本循环执行完60次需60秒

                        //消息发回UI，改变秒数1
                        Message message = new Message();
                        message.what = MESSAGE_ONE_SECOND_CHANGE;
                        handler.sendMessage(message);

                    }

                    timeCount--;
                    if(!learningFinishedCorrectly) {
                        //消息发回UI，改变分钟数1（只在未完成状态下才改变数字）
                        Message message = new Message();
                        message.what = MESSAGE_ONE_MINUTE_CHANGE;
                        handler.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(timeCount==0) {
                //是由于时间耗尽而结束循环时，消息发回UI。
                Message message = new Message();
                message.what = MESSAGE_TIME_UP;
                handler.sendMessage(message);
            }

        }
    }

    void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_DB_DATE_FETCHED:
                startingTimeMillis = System.currentTimeMillis();
                fltMask.setVisibility(View.GONE);
/*
                int vpLearningType = LearningViewPrAdapter.TYPE_RE_PICKING;//默认复习
                if(learningType == R.color.colorGP_Newly){
                    vpLearningType = LearningViewPrAdapter.TYPE_INIT_LEARNING;
                }//【纯复习采用何种标记目前暂未设计】
*/

                //下方构造参数待修改
                LearningViewPrAdapter learningVpAdapter = new LearningViewPrAdapter(getSupportFragmentManager(), items, vpLearningType);
                viewPager.setAdapter(learningVpAdapter);

                tv_totalPageNum.setText(String.valueOf(items.size() - 1));//最后一页ending伪数据不能算页数。
//                prepared = true;

                //然后由启动计时器（每分钟更改一次数字）
                timingThread = new Thread(new TimingRunnable());
                timingThread.start();
                break;

            case MESSAGE_ONE_SECOND_CHANGE:
                timeInSecond++;
                timePastScd.setText(String.format("%02d", timeInSecond % 60));
                break;

            case MESSAGE_ONE_MINUTE_CHANGE:
                timePastInMinute++;
                timePastMin.setText(String.format("%02d", timePastInMinute));
                break;

            case MESSAGE_TIME_UP:
                timingThread.interrupt();//先结束计时线程。
                //如果此时已经完成学习（滑动到过最后一页，且VE全部正确），则自动跳转到结束页，并保存记录。
                if(learningFinishedCorrectly){
                    Intent intentForAccomplishActivity = new Intent (this,AccomplishActivity.class);
                    intentForAccomplishActivity.putExtra("GROUP_ID",groupId);
                    //到达目标页后，需要根据本组的MS，lastTime等，结合本次学习的起止时间判断是否算作有效MS复习（是否令MS++）
                    intentForAccomplishActivity.putExtra("START_TIME",startingTimeMillis);
                    intentForAccomplishActivity.putExtra("FINISH_TIME",System.currentTimeMillis());

                    intentForAccomplishActivity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    startActivity(intentForAccomplishActivity);
                }else {
                    //否则常规模式：Dfg弹窗提示时间到——
                    // 并给出正误情况。
                    //按钮1：确认。拆分。
                    //按钮2：（默认隐藏）“就当刚才没学过”（提示，这样您的实际记忆水平可能会高于本程序记录的水平哦，
                    // 不过只要您开心，当然就可以这样做。）

                    popUpTimeEndingDiaFragment();
                }

/*
                    //为返回调用方Activity准备数据,
                    Intent intentForFailedReturn = new Intent();
                    intentForFailedReturn.putExtra("startingTimeMills",startingTimeMillis);
                    setResult(RESULT_LEARNING_FAILED,intentForFailedReturn);
                    this.finish();
*/
                break;

/*
            case MESSAGE_LOGS_SAVED:
//                timingThread = null;//停止计时
                // 滑动监听的设置代码早于timingThread实例化代码的位置，所以原先的终止方式无效。
                fltMask.setVisibility(View.GONE);//取消遮盖
                //在原始activity上给出结束按钮。显示学习信息。

                //可以为返回调用方activity而设置数据了
                Intent intent = new Intent();
                intent.putExtra("newLogsStr",newLogs);

                setResult(RESULT_LEARNING_SUCCEEDED,intent);
                this.finish();
                break;
*/

        }
    }

    /*
    * 时间到了之后，弹出DFG。
    * ①未完成（未滑动到最后）就到时间了
    * ②滑动到最后，但是没有全部正确。（虽然即使正确也不会停止计时，但在计时结束后，直接跳转了结束页，不会弹出DFG）
    *  滑动到最后，又向前回滑，但并非全对——无影响，按②处理。
    *【现在的是否完成，是以是否填对全部VE为标志的。】
    * 其他情况
    * ①滑动到最后，全部正确，但是又向回滑动——【暂定强行跳转结束页（可能不太友好）】
    * */
    private void popUpTimeEndingDiaFragment(){
        FragmentTransaction transaction = (getFragmentManager().beginTransaction());
        Fragment prev = (getFragmentManager().findFragmentByTag("Time_up"));

        if (prev != null) {
//            Log.i(TAG, "inside Dialog(), inside if prev!=null branch");
            Toast.makeText(this, "Old Dfg still there, removing...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningTimeUpDiaFragment.newInstance(itemsVeRightOrWrong);
        dfg.show(transaction,"Time_up");
    }

   /* private void finishWithUnAcomplishment(){
        //根据完成的项目数量，小于12只有提示未能完成-确认。大于12可以拆分生成新分组，提示用户
        // （没必要询问是否拆分，如果不拆分就只能全部标记未完成，正常人不能这么干）
        // 此时如果遇到丢失焦点应默认将学习状态（时间、数量）保存，onStop直接存入DB（没有询问环节）。
        if(maxLearnedAmount<12){

        }
    }*/

    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {
        switch (dfgType){
            case TIME_UP_CONFIRM_DIVIDE:
                //要拆分

                break;
            case TIME_UP_DISCARD:
                Intent intentForTimeUpAndCancelReturn = new Intent();
                setResult(RESULT_LEARNING_FAILED,intentForTimeUpAndCancelReturn);
                this.finish();

                break;


        }
    }


    public void confirmAndFinish(View view){
        Intent intent2 = new Intent();
        setResult(RESULT_EXTRA_LEARNING_SUCCEEDED,intent2);
        this.finish();
    }

    @Override
    public void onCodeCorrectAndReady() {
        //检测当前位置对应的词是否已经填对过，若未，则设为对。
        int currentIndex = viewPager.getCurrentItem();
        if(!itemsVeRightOrWrong.get(currentIndex)){
            itemsVeRightOrWrong.set(currentIndex,true);//只改为真，不改为假
            //这种首次改对/填对的情况，要给出提示。
        }

        //此时已填入正确单词，（如果允许自动滑动则）自动向下一页滑动。
        if(autoSliding) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        }
    }
}
