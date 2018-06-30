package com.vkyoungcn.smartdevices.yomemory;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.LearningViewPrAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningTimeUpDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
* 进入本Activity之前，经过一个预准备数据用的专用Activity，从该页面传来的数据有：
* ①tableSuffix；②learningType；③ArrayList<SingleItems>主体数据；以及：
* ④（仅LG）gid；⑤(仅LMerge)gids列表List。
* 数据的获取和完结分别在前、后页面完成。本页只负责学习逻辑（和最小限度的确保前后逻辑完整的业务）。
* 【新版取消了伪数据尾页，注意逻辑修改】
*
*  本页要实现的主要逻辑有：
*  ①时间控制——限定在60分钟内完成单次学习任务。
*      学习可以暂停（离开本页）但计时继续。
*      到时间后根据已学/未学进行拆分。
*      全部完成后计时并不停止，也不自动跳转完结（可以手动完结；
*      且会提示请及时保存，以免程序被系统强制退出后丢失学习数据），待计时结束按统一逻辑处理。
*  ②各Item的学习情况控制——
*      a.完成状态：正确（已填完整且正确）/错误（有错或未完成）/未填写
*      b.正面、反面与点击提示（且限次数，超过则本次学习置“记忆失败”）
*      c.允许手动改变单项Item的标记记录
*  ③滑动逻辑——可以自由前后滑动；
*       *最后一页为附加的伪完成页（滑到该页时，下方结束按钮变大且改为鲜艳颜色）
*       点击后如果存在错误VE，会弹出DFG提示并确认；如全对则跳转结束页。
*  ④CardView学习逻辑
*       初始显示状态：CardView正面（名称+音标+释义；附加标记等）
*       点击翻面：（VE+音标+释义；附加标记等）。点击提示按钮会给出名称提示（记录次数），点击VE时消失；
*       *当提示记次>3时，不可再提示，且给该item记“记忆失败”错误+1；（每次学习最多加1）
*       b.允许自由前后滑动（滑出时记录VE状态（含所填的内容），滑回时恢复）
*
* 对于学习是否“能使MS提升”，由学习结束后的专用结束Activity根据学习时间进行判断、处理；
* 而本页本着“不能不让学”的原则，无论时间区间如何，都可以开始学习。
* */
public class LearningActivity extends AppCompatActivity implements OnGeneralDfgInteraction, ValidatingEditor.OnValidatingEditorInputListener {
    private static final String TAG = "LearningActivity";

    private int learningType;//Intent传入。
    private String tableNameSuffix;//Intent传入。
    private ArrayList<SingleItem> items;//Intent传入。数据源

    private int groupId;//Intent传入。（仅LG模式下）
    private ArrayList<Integer> gIdsForMerge;//Intent传来，仅在合并学习时有此数据。

    private ArrayList<String> veFillings;//记录填写在VE内的信息，用于回滚时加载。【如果需要记录所填内容，似乎只能在Act记录。】
    //【原方案还有一个布尔List用于记录各VE正误情况，今认为应少建高开销资源，直接在Activity中利用上一List对比。且实际状态有三，布尔不符】
    private String veCacheString = "";//记录正在输入的VE的内容，在滑动卡片时存入list并置空。
    // (该字串在回调监听中设置，设计为VE每增删一个有效字符本字串被改写一次，但上下限与VE同不会越界修改)

//    private boolean autoSliding = true;//（在VE填写正确时）自动向后滑动的设置开关。

    private Thread timingThread;//采用全局变量便于到时间后终结之。【如果已滑动到ending页则直接停止计时，避免最后timeUp消息的产生】

    private int timeCount = 59;//for循环控制变量。执行60次for循环，即1h。（for循环包含60次1秒间隔的执行，一次完整的for=1min）
    private boolean isTimeUp = false;//计时线程的控制变量【旧版采用timeCount兼任。存在-1:59问题】
    private long startingTimeMillis;//学习开始的时间。（需要在……时间内完成，否则拆分）【最后的时间区间计算可令开始时间大于下限，结束小于上限】
//    private int timePastInMinute = 60;//流逝分钟数【借用timeCount即可】
    private int timeInSecond =59;//以秒计算的总流逝时间
    private boolean shouldChangeMinForFirstSec = true;//UI中开始是60:00，当第一秒走过后，应变为59:59.

//    private boolean isTimeUp = false;//计时只在时间到或手动停止时停止。完成学习后并不停止计时（分属两项任务）。
//    private boolean learningFinishedCorrectly = false;//本组学习完成。完成后置true，计时线程要检测之，避免完成后重新计时（因为代码顺序靠后BUG)
//    private boolean learningFinishedWithWrong = false;
    private Handler handler = new LearningActivityHandler(this);

//    private int scrollablePage = 1;//目前可自由滑动的页数范围。只增不减。【现在无限制了】

    private YoMemoryDbHelper memoryDbHelper;

    private ViewPager viewPager;//原是自定义的HalfScrollableVP
    private TextView tv_timeRestMin;
    private TextView tv_timeRestScd;
    private TextView totalMinutes;//应在xx分钟内完成，的数字部分。
    private TextView tv_rollLabel;//上方滚动标语栏
    private TextView tv_currentPageNum;
    private TextView tv_totalPageNum;
    private int maxLearnedAmount = 0;//用于判断已滑动过了多少词。当向回滑动时，此数字不减小。

    public static final int RESULT_LEARNING_SUCCEEDED = 3020;
    public static final int RESULT_LEARNING_FAILED = 3030;

    public static final int MESSAGE_ONE_MINUTE_CHANGE =5102;
    public static final int MESSAGE_ONE_SECOND_CHANGE =5103;
    public static final int MESSAGE_TIME_UP = 5104;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_learning);

        //从Intent获取参数
        learningType = getIntent().getIntExtra("LEARNING_TYPE",0);
        tableNameSuffix = getIntent().getStringExtra("TABLE_NAME_SUFFIX");//各种learningType都有TableSuffix
        items = getIntent().getParcelableArrayListExtra("ITEMS_FOR_LEARNING");//都有，主数据。

//        groupId = getIntent().getBundleExtra("BUNDLE_GROUP_ID").getInt("GROUP_ID_TO_LEARN");【似乎用不到】，gids列表也用不到


        //获取各控件
        tv_timeRestMin = (TextView)findViewById(R.id.tv_time_past_numMinute_Learning);
        tv_timeRestScd = (TextView)findViewById(R.id.tv_time_past_numSecond_Learning);
        //给时间的分、秒数设置字体
        Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/digit.ttf");
        tv_timeRestMin.setTypeface(typeFace);
        tv_timeRestScd.setTypeface(typeFace);

        tv_currentPageNum = (TextView)findViewById(R.id.currentPageNum_learningActivity);
        tv_totalPageNum = (TextView)findViewById(R.id.totalPageNum_learningActivity);//总数字需要在数据加载完成后设置，在handleMessage中处理

        tv_rollLabel = (TextView)findViewById(R.id.tv_rollLabel_learningActivity) ;


        viewPager = (ViewPager) findViewById(R.id.viewPager_ItemLearning);
        //给Vpr设置监听
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            int oldPageNum = 0;//用于记录页面滑动操作的“起始页”

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                oldPageNum = position;//获取滑动开始时的页面索引
            }

            /*
            * 本方法中需要完成的任务：
            * ①设置页脚数字
            * ②判断并设置最大已滑动值
            * ③给自定义进度条UI传递当前页面数【待】
            * ④（由于刚进入新页）将VE缓存（对应上一页内容）存入String列表；并检测本页对应位置上是否有值，有则传入。
            * ⑤理论上，每滑动一页，就应将上一页的正误情况存入记录列表。
            *
            * （注意反向滑动下的特殊逻辑）
            * */
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //据文档，本方法调用时，滑动已经完成。

                //任务一：
                //设置底端页码显示逻辑
                //当页面滑动时为下方的textView设置当前页数，但是只在开始滑动后才有效果，初始进入时需要手动XML设为1
                tv_currentPageNum.setText(String.valueOf(position + 1));//索引从0起需要加1
                //【旧版中，因为有伪数据尾页，在滑到最后一页时需要各种特殊的逻辑和判断】

                //任务二：
                //记录已滑动的上限值（注意只增不减）【但上限不包括伪数据尾页，所以位于任务一的if内】
                if(maxLearnedAmount <position+1){
                    maxLearnedAmount = position+1;//只加不减
                }

/*
* 任务三之UI的设计：考虑接收两个List参数（一个是正确的String列表，一个是实际VE字串列表）；通过对此两个
* 列表的对比，得出各位置上的状态（正确、错误、未填写——三种之一），其中尺寸大小从正确列表的尺寸获取。
* 当页面滑动时，对该UI进行通知/设置：改变其“当前位置”，并对上一个当前位置的状态进行重新计算。
* 【只在页面滑动后才更新某位置上的显示；未滑动时对该页所做的修改不反应到该UI上】
* 可以由该控件设计一个最终状态是否全对的回调，在学习结束时判断。
* */


                //任务四：
                //将上一页的（用于记录输入信息的）临时字串存入List，临时字串清空备用
                veFillings.set(oldPageNum, veCacheString);
                veCacheString = "";
                //本页VE缓存（如果有）设置给VE
                if(veFillings.get(position)!=null){//若为空串""则是可以设置的。
                    ValidatingEditor vdEt = ((LearningViewPrAdapter)viewPager.getAdapter()).currentFragment.getView().findViewById(R.id.validatingEditor_singleItemLearning);
                    vdEt.setInitText(veFillings.get(position));
                }


                if(position==items.size()-1){
                    //最后一张
                    //将结束按钮扩大。

                }
            }
        });

        /*
        * 【在点击了结束按钮（手动强行结束）或时间到而结束后，判断是否全部完成。
        * 1、如果全部完成且全部正确，直接进入结束页（且直接执行DB更新任务）
        * 2、如果全部完成，但并非全对，弹出DFG，询问：A确认（则错误的记错误1次）or B.放弃本次复习。
        * 3、未全部完成（可能是时间到所致），弹出DFG询问：A确认（则未完成的拆分，且错误的记错1）or B.放弃
        */

        //后期增加：①items可选顺序随机；


        startingTimeMillis = System.currentTimeMillis();//记录学习开始的时间

        //启动计时器（每分钟更改一次数字）
        timingThread = new Thread(new TimingRunnable());
        timingThread.start();

    }

    final static class LearningActivityHandler extends Handler {
        private final WeakReference<LearningActivity> activity;

        private LearningActivityHandler(LearningActivity activity) {
            this.activity = new WeakReference<LearningActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LearningActivity itemLearningActivity = activity.get();
            if(itemLearningActivity != null){
                itemLearningActivity.handleMessage(msg);
            }

    }};

    public class PreparingRunnable implements Runnable {
//        private static final String TAG = "PreparingRunnable";

        @Override
        public void run() {
            //从DB准备数据

            //将学习记录初始化（全否）；VE记录列表初始化。
            itemsVeRightOrWrong = new ArrayList<>();
            veFillings = new ArrayList<>();
            for(int i =0;i<items.size();i++) {
                itemsVeRightOrWrong.add(false);
                veFillings.add("");
            }



            Message message = new Message();

            handler.sendMessage(message);
        }
    }

    public class learningFinishedRunnable implements Runnable {
//        private static final String TAG = "learningFinishedRunnabl";

        @Override
        public void run() {

        }
    }

    public class TimingRunnable implements Runnable {
//        private static final String TAG = "TimingRunnable";

        @Override
        public void run() {
            while(!isTimeUp){//时间未到
                try {
//                    for (int i = 0; i < 60&&!learningFinishedCorrectly; i++) {//这样才能在学习完成而分钟数未到的情况下终止计时。
                    for (int i = 0; i < 60; i++) {
                        Thread.sleep(1000);     // sleep 1 秒；

                        //消息发回UI，改变秒数1
                        Message message = new Message();
                        message.what = MESSAGE_ONE_SECOND_CHANGE;
                        handler.sendMessage(message);

                        //这个似乎只能放在for内部【待】
                        if(timeCount == 0 && i==0 ){
                            //当减到-1时，其实秒数已经改为59了，所以需要在减到-1前就停止。
                            //【旧版使用timeCount代替专用控制变量似乎难以避免-1:59的问题（其实也行）】
                            Message messageTimeUp = new Message();
                            messageTimeUp.what = MESSAGE_TIME_UP;
                            handler.sendMessage(message);
                            isTimeUp = true;
                        }
                    }
                    timeCount--;//所以一个count是1分钟

                    //消息发回UI，改变分钟数1（只在未完成状态下才改变数字）
                    if(timeCount!=-1) {
                        //当最后减到-1时，就不再发送分钟改变的消息。
                        Message message = new Message();
                        message.what = MESSAGE_ONE_MINUTE_CHANGE;
                        handler.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(timeCount==-1) {
                //时间耗尽，消息发回UI。
                Message message = new Message();
                message.what = MESSAGE_TIME_UP;
                handler.sendMessage(message);
                timeCount--;
            }
        }
    }

    void handleMessage(Message msg) {
        switch (msg.what) {

                LearningViewPrAdapter learningVpAdapter = new LearningViewPrAdapter(getSupportFragmentManager(), items);
                viewPager.setAdapter(learningVpAdapter);


                break;


            case MESSAGE_ONE_SECOND_CHANGE:
                if(timeInSecond == 0){
                    timeInSecond =60;//先判断，如果该值降低到0，则重置为60；
                }
                timeInSecond--;//【逻辑经推演基本是正确的】
                tv_timeRestScd.setText(String.format("%02d", timeInSecond));
                break;

            case MESSAGE_ONE_MINUTE_CHANGE:
                tv_timeRestMin.setText(String.format("%02d", timeCount));
                break;

            case MESSAGE_TIME_UP:
                timingThread.interrupt();//显式结束计时线程。
                //如果此时已经完成学习（滑动到过最后一页，且VE全部正确），则自动跳转到结束页，并保存记录。
                if((maxLearnedAmount == items.size()-1) &&(itemsVeRightOrWrong.indexOf(false)==-1)){
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

    @Override
    public void onCodeChanged(String str) {
        //传出来的是当前完整的String
        veCacheString = str;
    }
}
