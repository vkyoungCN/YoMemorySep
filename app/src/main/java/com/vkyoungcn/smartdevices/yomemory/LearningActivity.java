package com.vkyoungcn.smartdevices.yomemory;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.LearningViewPrAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.Finish_LC_DiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.Finish_LG_DiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.Finish_LM_DiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;
import com.vkyoungcn.smartdevices.yomemory.customUI.StripeProgressBar;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
* 作者：杨胜 @中国海洋大学
* 别名：杨镇时
* author：Victor Young@ Ocean University of China
* email: yangsheng@ouc.edu.cn
* 2018.08.01
* */

public class LearningActivity extends AppCompatActivity
        implements OnGeneralDfgInteraction, ValidatingEditor.OnValidatingEditorInputListener,Constants {
    private static final String TAG = "LearningActivity";
    /*
     * 程序的“学习”环节所对应的页面。
     *
     * 进入本页面前，先经过了一个预先准备数据的PrepareForLearningActivity。
     * 从该页面传来的数据有：
     * ①tableSuffix；②learningType；③ArrayList<SingleItems>主体数据；以及：
     * ④（仅LG）gid；⑤(仅LMerge)gids列表List。
     * （数据的获取由前置页面负责，数据对DB的存入实际由结束页负责。本页只负责学习环节的逻辑（和为确保前后完整所需的逻辑）。
     *
     *  本页主要逻辑：
     *  ①时间控制——限定在60分钟内完成单次学习任务。
     *      学习可以暂停（离开本页）但计时继续；
     *      LG模式下学习结束时，未学习的词汇将从原组拆分出，生成一个新组（复制原组之前的学习日志）；
     *      全部完成后计时并不停止，也不自动跳转完结（提示“可以再翻阅一遍，或者点击finish”）可以手动完结；
     *      且会提示“确认完成后，点击finish以便保存本次记录”。
     *
     *  ②各Item的学习情况控制——
     *      a.三种状态：正确（已填完整且正确）/错误（已填写但有错）/未填写
     *      b.正面、反面与点击提示（且限次数，超过则相应词条【翻到正面，且不可再向VE填写（待处理，要对VE取消焦点）】，
     *      且对应词条记为错误状态。）
     *      c.允许手动改变单项Item的优先级。【后期增加跨分组/按优先级选取词的强化记忆】
     *  ③CardView学习逻辑
     *       初始显示状态：CardView正面（名称+音标+释义；附加标记等）
     *       点击翻面：（VE+音标+释义；附加标记等）。点击提示按钮会给出名称提示（记录次数），点击VE时消失；
     *       *当提示记次>3时，不可再提示，且给该item记“记忆失败”错误+1；（每次学习最多加1）
     *       b.允许自由前后滑动（滑出时记录VE状态（含所填的内容），滑回时恢复）
     *  ④滑动逻辑——
     *       可以自由前后滑动；
     *       滑到第二页时，finish按钮出现，可以手动点击完结。
     *  ⑤学习的开始——
     *      “不能不让学”的原则，不论开始时的时间情况如何，都是可以开始学习的，但最后对于学习
     *      是否“能使MS提升”，则由结束Activity根据学习时间进行判断、处理。
     * */


    /* 由Intent传入的数据 */
    private int learningType;//Intent传入。
    private String tableNameSuffix;//Intent传入。
    private ArrayList<SingleItem> items;//Intent传入。数据源
    private int groupId;//Intent传入。（LG模式）
    private int missionId;//Intent传入。（LCO/LCR模式）完成页创建新组时使用
    private ArrayList<Integer> gIdsForMerge;//Intent传来。（LM模式）

    /* 控件变量*/
    private ViewPager viewPager;//原是自定义的HalfScrollableVP
    private TextView tv_timeRestMin;
    private TextView tv_timeRestScd;
    private LinearLayout llt_timeCount;
    private TextView tv_currentPageNum;
    private TextView tv_totalPageNum;
    private TextView tv_finish;
    private StripeProgressBar spb_bar;
    private FloatingActionButton fab_finish;

    /* 控件附属变量*/
    private boolean isFabShowing = false;//fab按钮是否处于显示状态，用于避免多次触发展开条件（滑到指定卡片）时都设一遍显示。
    private int currentPagePosition = 0;//从0起，用于跨方法使用当前卡片索引值。
    private int oldPagePosition = 0;//由于三个监听方法都不能真正直接获取正确旧页码（滑动发起的页码），因而采取了其他方法绕行。
    private int maxLearnedAmount = 0;//用于判断已滑动过了多少词。当向回滑动时，此数字不减小。
    private boolean AutoSliding = true;//在填写了正确VE后，是否运行自动滑动。

    /* 业务逻辑变量 */
    private ArrayList<String> targetCodes = new ArrayList<>();//用于条纹进度条。
    private ArrayList<String> veFillings;//记录填写在VE内的信息，用于回滚时加载；以及条纹进度条当前状态的生成。
    private String veCacheString = "";//记录当前卡片VE所填写的内容（VE每改动一个字符，均会通过监听发送一遍当前ve中的完整字串）。卡片滑动时存入list并置空。
    private ArrayList<Byte> restChances;//剩余提示次数（卡片在输入状态下不显示词条本身，可以点击提示临时显示）


    /* 报告数据。（学习、存储完成后生成，传递给DFG或结束页）*/
    ArrayList<Integer> emptyPositions = new ArrayList<>();//未填写词（空词）在items中的索引值（组成的list）
    ArrayList<Integer> wrongPositions =  new ArrayList<>();//错词在items中的索引值

    //以下4变量，虽可在使用时通过变量计算直接获取，但考虑到多个方法间的统一性，在此声明为全局变量。
    int totalAmount =0;//items总共有多少各词（items.size()）
    int emptyAmount = 0;//emptyPositions.size()
    int wrongAmount =0;//wrongPositions.size()，是已填写词汇中的错误数量，不包括空词数量。
    int finishAmount =0;//totalAmount-emptyAmount，是已填写数量。

    private long startingTimeMillis;//学习开始的时间。

    //（手动）结束时的剩余时间。自动结束时设0即可。
    int restMinutes =0;
    int restSeconds = 0;


    /* 线程变量 */
    private Handler handler = new LearningActivityHandler(this);
    private Thread timingThread;//采用全局变量便于到时间后终结之。【如果已滑动到ending页则直接停止计时，避免最后timeUp消息的产生】

    private int timeCount = 59;//for循环控制变量。执行60次for循环，即1h。（for循环包含60次1秒间隔的执行，一次完整的for=1min）
    private int timeInSecond =59;//以秒计算的总流逝时间
    private boolean isTimeUp = false;//计时线程的控制变量【旧版采用timeCount兼任。存在-1:59问题】
    private boolean isFirstLoop = true;//因为从DFG返回本LA时，继续计时，其秒数不一定是59开始，因而存在逻辑错误，需要单独跑一圈
    public static final int MESSAGE_ONE_MINUTE_CHANGE =5102;
    public static final int MESSAGE_ONE_SECOND_CHANGE =5103;
    public static final int MESSAGE_TIME_UP = 5104;


//    private ArrayList<Integer> itemIds;//需要先把items的id列表传给进度条中的EmptyCard列表，然后随进度再把非空的删除。
//    private boolean autoSliding = true;//（在VE填写正确时）自动向后滑动的设置开关。
//    private boolean finishByHand = false;
//    private long finishTimeMillis;//用于最后传递
//    private int timePastInMinute = 60;//流逝分钟数【借用timeCount即可】
//    private boolean shouldChangeMinForFirstSec = true;//UI中开始是60:00，当第一秒走过后，应变为59:59.
//    private long timeRestInSec = 360;//当手动结束而又返回时使用。
//    跑掉秒数的余数，然后开始正常计时。统一起见，开头第一圈同样视为余数跑完。

//    private boolean isTimeUp = false;//计时只在时间到或手动停止时停止。完成学习后并不停止计时（分属两项任务）。
//    private boolean learningFinishedCorrectly = false;//本组学习完成。完成后置true，计时线程要检测之，避免完成后重新计时（因为代码顺序靠后BUG)
//    private boolean learningFinishedWithWrong = false;

//    private int scrollablePage = 1;//目前可自由滑动的页数范围。只增不减。【现在无限制了】
//    private TextView tv_rollLabel;//上方滚动标语栏
//    private YoMemoryDbHelper memoryDbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);

        //从Intent获取参数
        learningType = getIntent().getIntExtra(STR_LEARNING_TYPE,0);
        tableNameSuffix = getIntent().getStringExtra(STR_TABLE_NAME_SUFFIX);//各种learningType都有TableSuffix
        items = getIntent().getParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING);//都有，主数据。

        //获取两种特别模式下特定的数据项【但是似乎用不到，或可直接传递到后续完成页】
        if(learningType==LEARNING_GENERAL){
            groupId = getIntent().getIntExtra(STR_GROUP_ID,0);
        }else if(learningType == LEARNING_AND_MERGE){
            gIdsForMerge = getIntent().getIntegerArrayListExtra(STR_GROUP_ID_FOR_MERGE);
        }else{
            //在剩余两种模式是LCO/LCR时有效。如果增加了其他新模式，则本逻辑必须修改。
            missionId = getIntent().getIntExtra(STR_MISSION_ID,0);
        }


        //获取各控件
        tv_timeRestMin = (TextView)findViewById(R.id.tv_time_past_numMinute_Learning);
        tv_timeRestScd = (TextView)findViewById(R.id.tv_time_past_numSecond_Learning);
        //给时间的分、秒数设置字体
        Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/digit.ttf");
        tv_timeRestMin.setTypeface(typeFace);
        tv_timeRestScd.setTypeface(typeFace);

        llt_timeCount = (LinearLayout)findViewById(R.id.llt_timeCountGroup);
        tv_currentPageNum = (TextView)findViewById(R.id.currentPageNum_learningActivity);
        tv_totalPageNum = (TextView)findViewById(R.id.totalPageNum_learningActivity);//总数字需要在数据加载完成后设置，在handleMessage中处理
        tv_totalPageNum.setText(String.valueOf(items.size()));
//        tv_rollLabel = (TextView)findViewById(R.id.tv_rollLabel_learningActivity) ;

        tv_finish = (TextView)findViewById(R.id.finish_tv_learningActivity);
        fab_finish = (FloatingActionButton)findViewById(R.id.finish_fab_learningActivity);

        veFillings = new ArrayList<>();//只这样初始化是不够的，后面按索引位置设置值时会提示越界错误
        restChances = new ArrayList<>();

        spb_bar = (StripeProgressBar) findViewById(R.id.stripeProgressBar_LearningActivity);
        for (SingleItem si : items) {
            //需要这样彻底初始化
            targetCodes.add(si.getName());
            veFillings.add("");
            restChances.add((byte)3);//默认可提示次数，3。【后期可开放修改】
//            itemIds.add(si.getgIndex());//【其实这样一来可能就不必传递Merge时的itemsId了。待】
        }
        spb_bar.initNecessaryData(targetCodes);

        spb_bar.setCurrentCodes(veFillings);

        viewPager = (ViewPager) findViewById(R.id.viewPager_ItemLearning);

        LearningViewPrAdapter learningVpAdapter = new LearningViewPrAdapter(getSupportFragmentManager(), items,restChances);
        viewPager.setAdapter(learningVpAdapter);

        //给Vpr设置监听
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){

            /*@Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //【本方法有问题，log.i测试表明会持续输出多次旧索引但是最后会输出一个新索引！简直有病】
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                oldPagePosition = position;//获取滑动开始时的页面索引
                Log.i(TAG, "onPageScrolled: oldPageIndex="+oldPagePosition);
            }*/


            /*
            * 本方法中需要完成的任务：
            * 0,获取当前卡片索引位置
            * ①设置页脚数字
            * ②判断并设置最大已滑动值
            * ③（由于刚进入新页）将VE缓存（对应上一页内容）存入String列表；并检测本页对应位置上是否有值，有则传入。
            * ④给自定义进度条UI传递当前页面数【待】
            * ⑤理论上，每滑动一页，就应将上一页的正误情况存入记录列表。
            *
            * （注意反向滑动下的特殊逻辑）
            * */
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                oldPagePosition = currentPagePosition;
                //据文档，本方法调用时，滑动已经完成。
                currentPagePosition = position;//其他方法需使用
//                Log.i(TAG, "onPageSelected: currentPageIndex = "+currentPagePosition);

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

                //任务三：
                //将上一页的（用于记录输入信息的）临时字串存入List，临时字串清空备用。
                veFillings.set(oldPagePosition, veCacheString);
                //本页VE缓存（如果有）设置给VE
                if(veFillings.get(position)!=null){//若为空串""则是可以设置的。
                    try {
                        ValidatingEditor vdEt = ((LearningViewPrAdapter)viewPager.getAdapter()).currentFragment.getView().findViewById(R.id.ve_singleItemLearning);
                        vdEt.setInitText(veFillings.get(position));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    veCacheString = veFillings.get(position);//缓存要保持一致，否则（比如非空VE而cache置空的话）第三次滑到时内容就删除了。

                }else {
                    veCacheString = "";//列表内无缓存时置空
                }

                //任务四，重设UI。
                // 本任务位于任务三修改字串列表之后。因为需要基于字串列表来修改状态。
                spb_bar.resetStripeAt(oldPagePosition,position);


                if(!isFabShowing){
                    /*if(learningType!=LEARNING_GENERAL||position==items.size()-1) {
                        // 普通模式下滑到最后一张时将结束按钮显示出来。 【回滑不应隐藏】
                        //其他模式下，第二页开始显示结束按钮；
                        tv_finish.setVisibility(View.VISIBLE);
                        fab_finish.setVisibility(View.VISIBLE);
                        isFabShowing = true;
                    }*/

                    //所有模式下，第二页开始显示结束按钮；
                    tv_finish.setVisibility(View.VISIBLE);
                    fab_finish.setVisibility(View.VISIBLE);
                    isFabShowing = true;
                }
            }
        });


        //后期增加：①items可选顺序随机；


        startingTimeMillis = System.currentTimeMillis();//记录学习开始的时间

        //启动计时器（每秒、分更改一次数字）
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



    public class TimingRunnable implements Runnable {
//        private static final String TAG = "TimingRunnable";

        @Override
        public void run() {
            while(!isTimeUp){//时间未到(&&!finishByHand取消，手动改不停表)
                try {
//                    for (int i = 0; i < 60&&!learningFinishedCorrectly; i++) {//这样才能在学习完成而分钟数未到的情况下终止计时。
                    //先把秒数减完一圈（为了使“从dfg返回恢复”时的计时逻辑正确）
                    if(isFirstLoop) {
                        isFirstLoop = false;
                     //首圈先跑掉秒数的余数。
                        for (int i = 0; i <= timeInSecond; i++) {
                            Thread.sleep(1000);     // sleep 1 秒；

                            //消息发回UI，改变秒数1
                            Message message = new Message();
                            message.what = MESSAGE_ONE_SECOND_CHANGE;
                            handler.sendMessage(message);
                        }
                    //当然分钟数最后也要-1
                        timeCount--;
                    }
                    //接下来是正常跑（如果还有整分钟的话）
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
        }
    }

    void handleMessage(Message msg) {
        switch (msg.what) {
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


            /*
             * 时间到造成的完结和手动点击完成造成的完结弹出的DFG略有不同：手动（时间未到）可返回继续；TimeUp则不可。
             * 完结时的DFG根据VE序列的情况有不同设计：①尚有未填者（提示，提示拆分，可以返回继续（有剩余时间时））；
             * ②全填，有可改正的错者（即提示次数未到）（提示，不涉及拆分，但错者记次，可返回继续（有时间时））；
             * ③未全填、有可改者（前二提示皆有）；④全填，无可改者（或全对）（概括说明性提示，无可挽回的计次，可返回但只是看看没啥意义）
             * 以上四者，在TimeUp情形下取消返回选项。
             * */
                case MESSAGE_TIME_UP:
                    timeUpFinish();//具体逻辑皆在该方法内
                break;

        }
    }


    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {

        switch (dfgType){
            case LEARNING_FINISH_DFG_CONFIRM:
                //确认。
                // 要跳转到结束页，根据完成情况（并结合learningType）进行DB处理
                //
                // 空的：
                // LG下：已学的维持原组，（原组的Logs正常+1处理），未学的要成立新组（items的gid统一变更为新组），
                // 新组的成立时间是current，但旧复习记录保留不删（所以成立时间晚于学习时间，无所谓）
                //
                // LC下：空的无所谓
                // LM下：已完成的合并到主组（items改gid，【Nw：原分组/原分组Log记录保留】），部分完成的来源组拆分（同lg）
                // 未进行到的组不处理； 如果完成数量连主组容量都未达到则类同LG,拆分。
                //
                // 错误的（首先错误的肯定是在已完成部分）
                // 由于数据源是引用形式，所以估计其错误记录数已随卡片翻阅中的操作一并更改，直接随items新状态存入db
                //
                // logs记录：按开始时间处理较为容易（结束时间涉及精准度问题，中间需计算且也不是特别有道理；
                // 既然都没特别充分的道理，就按简单且精确的来吧）

                //准备Intent
                Intent intentToAccomplishActivity = new Intent(this,AccomplishActivity.class);
                intentToAccomplishActivity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                //存入通用数据
                intentToAccomplishActivity.putExtra(STR_LEARNING_TYPE,learningType);
                intentToAccomplishActivity.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToAccomplishActivity.putParcelableArrayListExtra(STR_ITEMS,items);

                intentToAccomplishActivity.putIntegerArrayListExtra(STR_EMPTY_ITEMS_POSITIONS,emptyPositions);
                intentToAccomplishActivity.putIntegerArrayListExtra(STR_WRONG_ITEMS_POSITIONS, wrongPositions);

                intentToAccomplishActivity.putExtra(STR_START_TIME,startingTimeMillis);
//                intentToAccomplishActivity.putExtra("FINISH_TIME",finishTimeMillis);
                intentToAccomplishActivity.putExtra(STR_REST_MINUTES,restMinutes);
                intentToAccomplishActivity.putExtra(STR_REST_SECONDS,restSeconds);

                //各状态下的不同数据
                if(learningType == LEARNING_GENERAL){
                    intentToAccomplishActivity.putExtra(STR_GROUP_ID,groupId);
                }else if(learningType == LEARNING_AND_MERGE){
                    intentToAccomplishActivity.putExtra(STR_GROUP_ID_FOR_MERGE,gIdsForMerge);
                }else if(learningType == LEARNING_AND_CREATE_ORDER || learningType == LEARNING_AND_CREATE_RANDOM) {
                    intentToAccomplishActivity.putExtra(STR_MISSION_ID,missionId);
                }


                this.startActivity(intentToAccomplishActivity);
                this.finish();
                break;

            case LEARNING_FINISH_DFG_BACK:
                //恢复计时，设置时间tv的正确值
                llt_timeCount.setVisibility(View.VISIBLE);

                tv_timeRestMin.setText(String.valueOf(restMinutes));
                tv_timeRestScd.setText(String.valueOf(restSeconds));
                timeCount = restMinutes;
                timeInSecond = restSeconds;
                isFirstLoop = true;

                break;

            case LEARNING_FINISH_DFG_GIVE_UP:
                //直接放弃。
                this.finish();

                break;
        }
    }




    @Override
    public void onCodeCorrectAndReady() {

        //此时已填入正确单词，（如果允许自动滑动则）自动向下一页滑动。
        if(AutoSliding) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            //
        }
    }

    @Override
    public void onCodeChanged(String str) {
        //传出来的是当前完整的String
        veCacheString = str;
    }


    /*
     * 【在点击了结束按钮（手动强行结束）或时间到而结束后，判断是否全部完成。
     * 1、如果全部完成且全部正确，直接进入结束页（且直接执行DB更新任务）
     * 2、如果全部完成，但并非全对，弹出DFG，询问：A确认（则错误的记错误1次）or B.放弃本次复习。
     * 3、未全部完成（可能是时间到所致），弹出DFG询问：A确认（则未完成的拆分，且错误的记错1）or B.放弃
     */

    /*
     * 时间到造成的完结和手动点击完成造成的完结弹出的DFG略有不同：手动（时间未到）可返回继续；TimeUp则不可。
     * 完结时的DFG根据VE序列的情况有不同设计：①尚有未填者（提示，提示拆分，可以返回继续（有剩余时间时））；
     * ②全填，有可改正的错者（即提示次数未到）（提示，不涉及拆分，但错者记次，可返回继续（有时间时））；
     * ③未全填、有可改者（前二提示皆有）；④全填，无可改者（或全对）（概括说明性提示，无可挽回的计次，可返回但只是看看没啥意义）
     * 以上四者，在TimeUp情形下取消返回选项。
     * */
    public void handyFinish(View view){
        //点击“结束”键（fab）后，的方法
        // ①判断当前正误、空的情况，
        // ②弹出DFG；
        // ③根据DFG中的选择向通用DFG接口（本Act实现）发回信息。

        //任务0，停表【但是interrupt()和标记变量置否都无效，计时仍然再跑（而后又在某一时刻停止了）】所以决定不停表
        // 不停表也能避免无限暂停bug


        llt_timeCount.setVisibility(View.GONE);//计时控件隐藏

        //记录“剩余”时间。倒计时算法的精度不足，和System.currentTimeMillis有偏差，为统一，折中用替代方案
        restMinutes = Integer.valueOf(tv_timeRestMin.getText().toString());
        restSeconds = Integer.valueOf(tv_timeRestScd.getText().toString());
        //timeRestInSec = 3600-(System.currentTimeMillis()-startingTimeMillis)/1000;//但是这样得到的数据和倒计时略有差别

        //先将当前页的缓存字串存入缓存字串列表
        veFillings.set(currentPagePosition, veCacheString);

        prepareLearningRecords();


        //根据情况配置fg
        FragmentTransaction transaction = (getFragmentManager().beginTransaction());
        Fragment prev = (getFragmentManager().findFragmentByTag(FG_STR_HANDY_FINISH));

        if (prev != null) {
            Toast.makeText(this, "Old Dfg still there, removing...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }

        switch (learningType){
            case LEARNING_AND_CREATE_ORDER:
            case LEARNING_AND_CREATE_RANDOM:
                DialogFragment dfgLC = Finish_LC_DiaFragment.newInstance(finishAmount,wrongAmount,restMinutes,restSeconds);
                dfgLC.show(transaction,FG_STR_HANDY_FINISH);
                break;
            case LEARNING_GENERAL:
                DialogFragment dfgLG = Finish_LG_DiaFragment.newInstance(totalAmount,emptyAmount,wrongAmount,restMinutes,restSeconds);
                dfgLG.show(transaction,FG_STR_HANDY_FINISH);
                break;
            case LEARNING_AND_MERGE:
                DialogFragment dfg = Finish_LM_DiaFragment.newInstance(totalAmount,finishAmount,wrongAmount,restMinutes,restSeconds);
                dfg.show(transaction,FG_STR_HANDY_FINISH);
                break;
        }

    }

    public void timeUpFinish(){
        //点击“结束”键（fab）后，的方法
        // ①判断当前正误、空的情况，
        // ②弹出DFG；
        // ③根据DFG中的选择向通用DFG接口（本Act实现）发回信息。

        //任务0，停表(虽然似乎无效，至少不能立即停表，但仍要停止。)
        isTimeUp = true;
        timingThread.interrupt();
        //计时控件隐藏
        llt_timeCount.setVisibility(View.GONE);

        //已到时，无需记录“剩余”时间。
        restMinutes =0;
        restSeconds = 0;

        //当前页的缓存字串也要存入缓存字串列表
        veFillings.set(currentPagePosition, veCacheString);

        prepareLearningRecords();

        //根据情况弹出dfg
        FragmentTransaction transaction = (getFragmentManager().beginTransaction());
        Fragment prev = (getFragmentManager().findFragmentByTag(FG_STR_TIME_UP_FINISH));

        if (prev != null) {
            Toast.makeText(this, "Old Dfg still there, removing...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }

        switch (learningType){
            case LEARNING_AND_CREATE_ORDER:
            case LEARNING_AND_CREATE_RANDOM:
                DialogFragment dfgLC = Finish_LC_DiaFragment.newInstance(finishAmount,wrongAmount,restMinutes,restSeconds);
                dfgLC.show(transaction,FG_STR_TIME_UP_FINISH);
                break;
            case LEARNING_GENERAL:
                DialogFragment dfgLG = Finish_LG_DiaFragment.newInstance(totalAmount,emptyAmount,wrongAmount,restMinutes,restSeconds);
                dfgLG.show(transaction,FG_STR_TIME_UP_FINISH);
                break;
            case LEARNING_AND_MERGE:
                DialogFragment dfg = Finish_LM_DiaFragment.newInstance(totalAmount,finishAmount,wrongAmount,restMinutes,restSeconds);
                dfg.show(transaction,FG_STR_TIME_UP_FINISH);
                break;
        }

    }


    private void prepareLearningRecords(){
        //准备统计信息：总数、空、完成、错误的数量
        totalAmount =items.size();
        emptyAmount = 0;
        wrongAmount =0;
        finishAmount = totalAmount-emptyAmount;

        //对“填写记录”列表逐项判断,计数并记录索引列表。
        for (int i =0; i<totalAmount;i++){
            if(veFillings.get(i).isEmpty()){
                emptyAmount++;
                emptyPositions.add(i);//注意，添加的是索引位置不是id。
            }else if(!veFillings.get(i).equals(items.get(i).getName())){
                wrongAmount++;//注意，这个“错误数量”不包括空数量。是已填部分的错误数量。
                wrongPositions.add(i);
            }
        }//旧版的统计职能由spb兼顾，现改由本Activity负责（职能划分更清晰，且不必强制spb的最后更新了）
    }


    /*
    * 用于：在所含有的SingleLearningFragment（所对应卡片）的提示次数变动时，
    * 通知本Activity修改所持有的相对应的全局状态列表
    * */
    public void modifyCardsRestTipChances(int newNumber){
        restChances.set(currentPagePosition,(byte)newNumber);

    }


}
