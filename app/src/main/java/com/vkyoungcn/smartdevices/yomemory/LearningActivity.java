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
import com.vkyoungcn.smartdevices.yomemory.fragments.OnSimpleDFgButtonClickListener;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.spiralCore.LogList;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YouMemoryDbHelper;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

import java.lang.ref.WeakReference;
import java.util.List;

/*
* 进入本Activity后，在启动的新线程中处理从DB取数据的业务，
* 数据取好后，附加一条“ending伪数据”，作为结束页面；
* 在结束页面还有其他特殊逻辑：①取消页脚的页数显示；②到达最后一页（伪数据页）后计时停止，生成要存入DB的Log数据；
* ③增加完成学习的确认按钮，以及DB数据生成、存入逻辑；④允许向前滑动查看，但数据不再改变（以TextView说明这个情况）；
* （同时，取消初始设计中以Dfg收尾的计划）
* */
public class LearningActivity extends AppCompatActivity implements OnSimpleDFgButtonClickListener, ValidatingEditor.codeCorrectAndReadyListener {
    private static final String TAG = "ItemLearningActivity";

    public static final int CREATE_IN_ORDER = 2041;
    public static final int CREATE_RANDOM = 2042;


    private int groupId;//最后的结果页面需要获取分组信息
    private int learningType;//在最后的操作中，蓝色、橙色的日志生成方式不同，无法统一做“复习类型”传递。
    private String tableNameSuffix;//用来从DB获取本组所属的ITEMS
    private String groupSubItemIdsStr;//用来从DB获取本组所属的ITEMS
    private List<SingleItem> items;//数据源（未初始化）
    private String newLogs="";//用于回传到调用act的字串，新log

    private Thread timingThread;//采用全局变量便于到时间后终结之。【如果已滑动到ending页则直接停止计时，避免最后timeUp消息的产生】
    private Boolean prolonged = false;//计时完成如果还没有完成复习，可以延长时间一次（暂定15分钟，暂定不影响log计时）

    private int timeCount = 60;//【调试期间临时设置为1分钟】默认执行60次for循环（for循环包含60次1秒间隔的执行，完整执行一次for需要60秒）
    private long startingTimeMillis;//用于非正常结束时回传调用方的信息项。
    private int timePastInMinute = 0;//流逝分钟数
    private int timeInSecond = 0;

//    private Boolean prepared = false;
    private Boolean learningFinished = false;//本组学习完成。完成后置true，计时线程要检测之，避免完成后重新计时（因为代码顺序靠后BUG)
    private Handler handler = new LearningActivityHandler(this);

    private int scrollablePage = 1;//目前可自由滑动的页数范围。只增不减。

    private YouMemoryDbHelper memoryDbHelper;

    private FrameLayout fltMask;
    private TextView tv_mask;
    private HalfScrollableViewPager viewPager;
    private TextView timePastMin;
    private TextView timePastScd;
    private TextView totalMinutes;//应在xx分钟内完成，的数字部分。

    private TextView tv_currentPageNum;
    private int currentLearnedAmount;//用于最后判断是否完成，是只增不减的量。
    private TextView tv_totalPageNum;
    private TextView confirmAndFinish;//额外复习完成后的返回按钮，初始隐藏。

    public static final int RESULT_LEARNING_SUCCEEDED = 3020;
    public static final int RESULT_EXTRA_LEARNING_SUCCEEDED = 3021;
    public static final int RESULT_EXTRA_LEARNING_SUCCEEDED_UNDER24H = 3022;
    public static final int RESULT_LEARNING_FAILED = 3030;

    public static final int MESSAGE_DB_DATE_FETCHED =5101;
    public static final int MESSAGE_ONE_MINUTE_CHANGE =5102;
    public static final int MESSAGE_ONE_SECOND_CHANGE =5103;
    public static final int MESSAGE_TIME_UP = 5104;
    public static final int MESSAGE_LOGS_SAVED = 5105;
    public static final int MESSAGE_EXTRA_LEARNING_ACCOMPLISHED = 5106;
    public static final int MESSAGE_EXTRA_LEARNING_UNDER_1H = 5107;
    public static final int MESSAGE_EXTRA_LEARNING_1H_24H = 5108;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_learning);

        //从Intent获取参数
        groupId = getIntent().getIntExtra("group_id",0);
        tableNameSuffix = getIntent().getStringExtra("item_table_suffix");
        groupSubItemIdsStr = getIntent().getStringExtra("group_sub_item_ids_str");
        learningType = getIntent().getIntExtra("learning_type",0);

        fltMask = (FrameLayout) findViewById(R.id.flt_mask_learningPage);
        tv_mask = (TextView)findViewById(R.id.tv_onItsWay_learningPage);
        timePastMin = (TextView)findViewById(R.id.tv_time_past_numMinute_Learning);
        timePastScd = (TextView)findViewById(R.id.tv_time_past_numSecond_Learning);
        tv_currentPageNum = (TextView)findViewById(R.id.currentPageNum_learningActivity);
        tv_totalPageNum = (TextView)findViewById(R.id.totalPageNum_learningActivity);//总数字需要在数据加载完成后设置，在handleMessage中处理
        totalMinutes = (TextView) findViewById(R.id.tv_num_itemLearningActivity);
        confirmAndFinish = (TextView)findViewById(R.id.learningFinish) ;

        viewPager = (HalfScrollableViewPager) findViewById(R.id.viewPager_ItemLearning);
        if(learningType == R.color.colorGP_Newly) {
            viewPager.setScrollable(true);//初学状态，vp可以直接滑动
        }

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                //如果是复习，需要在滑动一页后重新设置为不可滑动，直至validatingEditor校验正确
                //但是可以向前一页滑动（由Adapter负责实现）
                //且在向前滑动后可以重新自由滑动到已阅读页

                //跳转到下一页后的首要逻辑是将已阅读（可滑动）范围设为当前最大页（向后滑动才设新值）
                if(scrollablePage < position) {
                    scrollablePage = position;
                }//end if, 向前滑动不设新值。

                //非初学状态下，不能超越上限滑动
                if((learningType == R.color.colorGP_AVAILABLE ||
                        learningType == R.color.colorGP_Miss_ONCE ||
                        learningType == R.color.colorGP_STILL_NOT )&& position>=scrollablePage) {
                    viewPager.setScrollable(false);
                }else {//页码未到（正处在向已学部分回览状态，或新学习模式，都可以自由翻页）
                    viewPager.setScrollable(true);
                }
                //设置底端页码显示逻辑
                //当页面滑动时为下方的textView设置当前页数，但是只在开始滑动后才有效果，初始进入时需要手动XML设为1
                if(currentLearnedAmount<position+1){
                    currentLearnedAmount = position+1;//只加不减
                }
                tv_currentPageNum.setText(String.valueOf(position+1));//索引从0起需要加1
                //滑动到最后一页时
                if(position==items.size()-1){
                    //最后一张【Ending伪数据页】
                    learningFinished = true;
                    timingThread.interrupt();//先结束计时线程。
                    // （不需要用户确认完成）向DB写数据；
                    //能到这一页的，属于正常完成
                    tv_currentPageNum.setText("--");//总不能显示比总数还+1.

                    //显示信息：学习记录保存中，请稍等……同时执行向DB写log
                    tv_mask.setText("学习记录保存中，请稍等");
                    fltMask.setVisibility(View.VISIBLE);
                    //在新线程处理log的DB保存操作。
                    new Thread(new learningFinishedRunnable()).start();


                }
            }
        });

            //从db查询List<SingleItem>放在另一线程
            new Thread(new PreparingRunnable()).start();         // start thread

        //后期增加：①items可选顺序随机；
        // ②增加倒计时欢迎页面；
    }

    final static class LearningActivityHandler extends Handler {
        private final WeakReference<ItemLearningActivity> activity;

        private LearningActivityHandler(ItemLearningActivity activity) {
            this.activity = new WeakReference<ItemLearningActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ItemLearningActivity itemLearningActivity = activity.get();
            if(itemLearningActivity != null){
                itemLearningActivity.handleMessage(msg);
            }

    }};

    public class PreparingRunnable implements Runnable {
//        private static final String TAG = "PreparingRunnable";

        @Override
        public void run() {
            //从DB读取数据
            memoryDbHelper = YouMemoryDbHelper.getInstance(getApplicationContext());
            items = memoryDbHelper.getItemsByGroupSubItemIds(groupSubItemIdsStr,tableNameSuffix);

            //在数据原的最后附加一条“伪数据”，用于学习完成后显示学习信息，处理完成业务。
            SingleItem endingItem = new SingleItem(0,"完成","","",true);
            items.add(endingItem);

            Message message = new Message();
            message.what = MESSAGE_DB_DATE_FETCHED;

            handler.sendMessage(message);
        }
    }

    public class learningFinishedRunnable implements Runnable {
//        private static final String TAG = "learningFinishedRunnabl";

        @Override
        public void run() {
            //额外复习
            if(learningType == R.color.colorGP_STILL_NOT){
                //额外复习处理逻辑
                String oldLogs = memoryDbHelper.getGroupById(groupId).getGroupLogs();
                int minutesPast = LogList.getMinutesBetweenInitLearningAndNow(oldLogs);
                if(minutesPast<60){
                    //1小时内的额外复习
                    //DB记录字段置true
                    //返回调用方后更新UI
                    int lines = memoryDbHelper.updateExtraLearningOneAsTrue(groupId);
                    if(lines == -1 || lines ==0){
                        //失败逻辑，待处理
                        return;
                    }
                    Message message = new Message();
                    message.what = MESSAGE_EXTRA_LEARNING_UNDER_1H ;

                    handler.sendMessage(message);
                }else if(minutesPast<24*60){
                    //1~24h
                    //DB相应字段值+1
                    //返回调用方后通知更新UI
                    int lines2 = memoryDbHelper.updateExtraLearning24HourAsAddOne(groupId);
                    if(lines2 == -1 || lines2 ==0){
                        //失败逻辑，待处理
                        return;
                    }
                    Message message = new Message();
                    message.what = MESSAGE_EXTRA_LEARNING_1H_24H ;

                    handler.sendMessage(message);
                }else {
                    //超过24小时，没有特殊处理
                    Message message = new Message();
                    message.what = MESSAGE_EXTRA_LEARNING_ACCOMPLISHED ;

                    handler.sendMessage(message);
                }

                //判断时间，在1h内的；在1h以上，24h内的；在24h以上的。
                //增加DB列，额外学习的记录。也是要操作DB的。

            }else {
                //需要生成新Logs记录存入DB（覆盖旧Logs）
                //注意，仍然需要传递learningType以区分蓝色、橙色生成几条记录。
                newLogs = LogList.getUpdatedGroupLogs(ItemLearningActivity.this, groupId, System.currentTimeMillis(),learningType);
                //向DB更新
                if(newLogs==null||newLogs.isEmpty()){
                    Toast.makeText(ItemLearningActivity.this, "学习记录生成失败，未知错误。", Toast.LENGTH_SHORT).show();
                }
                long lines = memoryDbHelper.updateLogOfGroupById(groupId,newLogs);

                //完成后通知UI
                Message message = new Message();
                message.what = MESSAGE_LOGS_SAVED;
//                message.obj = lines;//暂时用不到该数据

                handler.sendMessage(message);
            }
        }
    }

    //用于计时并发送更新UI上时间值的消息
    public class TimingRunnable implements Runnable {
//        private static final String TAG = "TimingRunnable";

        @Override
        public void run() {
            while(!learningFinished && timeCount > 0){
                try {
                    for (int i = 0; i < 60&&!learningFinished; i++) {//这样才能在学习完成而分钟数未到的情况下终止计时。
                        Thread.sleep(1000);     // sleep 1 秒；本循环执行完60次需60秒

                        //消息发回UI，改变秒数1
                        Message message = new Message();
                        message.what = MESSAGE_ONE_SECOND_CHANGE;
                        handler.sendMessage(message);

                    }

                    timeCount--;
                    if(!learningFinished) {
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

    void handleMessage(Message msg){
        switch (msg.what){
            case MESSAGE_DB_DATE_FETCHED:
                startingTimeMillis = System.currentTimeMillis();
                fltMask.setVisibility(View.GONE);

                int vpLearningType = LearningViewPrAdapter.TYPE_RE_PICKING;//默认复习
                if(learningType == R.color.colorGP_Newly){
                    vpLearningType = LearningViewPrAdapter.TYPE_INIT_LEARNING;
                }//【纯复习采用何种标记目前暂未设计】

                //下方构造参数待修改
                LearningViewPrAdapter lvAdp = new LearningViewPrAdapter(getSupportFragmentManager(),items,vpLearningType);
                viewPager.setAdapter(lvAdp);

                tv_totalPageNum.setText(String.valueOf(items.size()-1));//最后一页ending伪数据不能算页数。
//                prepared = true;

                //然后由启动计时器（每分钟更改一次数字）
                timingThread =  new Thread(new TimingRunnable());
                timingThread.start();
                break;

            case MESSAGE_ONE_SECOND_CHANGE:
                timeInSecond++;
                timePastScd.setText(String.format("%02d",timeInSecond%60));
                break;

            case MESSAGE_ONE_MINUTE_CHANGE:
                timePastInMinute++;
                timePastMin.setText(String.format("%02d",timePastInMinute));
                break;

            case MESSAGE_TIME_UP:
                timingThread.interrupt();//先结束计时线程。
                //安排时间到后的逻辑：②Dfg弹窗询问：
                //a.是否续时；b.强制结束，执行收尾逻辑。【完成数量大于18个可以拆分为两组】或者强制结束不做任何记录。
                if(!prolonged){
                    //尚未延长过时间，此时可以延长一次。
                    // 弹出dfg，其中要置prolonged为true。如果用户要延长时间，则将计时计数器回调到15分，继续执行线程。
                    popUpTimeEndingDiaFragment();
                }else {
                    //【后期可增加“在分组部分完成时，拆分分组”的功能。】
//                    finishWithUnAcomplishment();//无论是已无再次延时的机会，还是直接dismiss，都调用本方法。
                    Toast.makeText(this, "未能完成，不记录本次学习。", Toast.LENGTH_SHORT).show();

                    //为返回调用方Activity准备数据,
                    Intent intentForFailedReturn = new Intent();
                    intentForFailedReturn.putExtra("startingTimeMills",startingTimeMillis);
                    setResult(RESULT_LEARNING_FAILED,intentForFailedReturn);
                    this.finish();
                }
                break;

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

            case MESSAGE_EXTRA_LEARNING_1H_24H:
            case MESSAGE_EXTRA_LEARNING_UNDER_1H:
                fltMask.setVisibility(View.GONE);//取消遮盖
                //在原始activity上给出结束按钮。显示学习信息。

                //可以为返回调用方activity而设置数据了
                Intent intent2 = new Intent();
//                intent2.putExtra("GroupId",groupId);
                setResult(RESULT_EXTRA_LEARNING_SUCCEEDED_UNDER24H,intent2);
                this.finish();
                break;

            case MESSAGE_EXTRA_LEARNING_ACCOMPLISHED:
                fltMask.setVisibility(View.GONE);
                confirmAndFinish.setVisibility(View.VISIBLE);
                //其返回操作在设visible的控件点击事件完成
        }
    }

    private void popUpTimeEndingDiaFragment(){
        //先将延时标记置为true，代表已延时一次。
        prolonged = true;
        FragmentTransaction transaction = (getFragmentManager().beginTransaction());
        Fragment prev = (getFragmentManager().findFragmentByTag("Time_up"));

        if (prev != null) {
//            Log.i(TAG, "inside Dialog(), inside if prev!=null branch");
            Toast.makeText(this, "Old Dfg still there, removing...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningTimeUpDiaFragment.newInstance();
        dfg.show(transaction,"Time_up");
    }

   /* private void finishWithUnAcomplishment(){
        //根据完成的项目数量，小于12只有提示未能完成-确认。大于12可以拆分生成新分组，提示用户
        // （没必要询问是否拆分，如果不拆分就只能全部标记未完成，正常人不能这么干）
        // 此时如果遇到丢失焦点应默认将学习状态（时间、数量）保存，onStop直接存入DB（没有询问环节）。
        if(currentLearnedAmount<12){

        }
    }*/

    @Override
    public void onDfgButtonClick(int viewId) {
        switch (viewId){
            case R.id.confirm_timeEndingDfg:
                //在LearningTimeUpDiaFragment中点击了确认按键
                // 延时15分钟
                timeCount = 15;//【调试期间暂时设1】计时时间补充15分钟
                totalMinutes.setText(getResources().getString(R.string.minutes_75));
                timingThread.start();//再次启动计时
                break;
            case R.id.cancel_timeEndingDfg:
                //在LearningTimeUpDiaFragment中点击了取消按键
                // 学习未能完成，直接给出消息然后退出到分组列表Activity。
                //【计划中】生成一条程序全局可见的消息（未完成……），存入未读消息列表。
                Toast.makeText(this, "未能完成，不记录本次学习。", Toast.LENGTH_SHORT).show();

                //为返回调用方Activity准备数据,
                Intent intentForTimeUpAndCancelReturn = new Intent();
                intentForTimeUpAndCancelReturn.putExtra("startingTimeMills",startingTimeMillis);
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
        //此时已填入正确单词，自动向下一页滑动。
        viewPager.setCurrentItem(viewPager.getCurrentItem()+1,true);
    }
}
