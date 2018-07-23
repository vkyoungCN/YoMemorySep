package com.vkyoungcn.smartdevices.yomemory;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.fragments.ReportGroupLC_Fragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.ReportGroupLG_Fragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.ReportGroupLM_Fragment;
import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.models.SingleLearningLog;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_MERGE;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class AccomplishActivity extends AppCompatActivity implements Constants {
    private static final String TAG = "AccomplishActivity";
//* 主要处理逻辑（业务）
//
//* 空的：
//* LG下：已学的维持原组，（原组的Logs正常+1处理），未学的要成立新组（items的gid统一变更为新组），
//* 新组的成立时间是current，但旧复习记录保留不删（所以成立时间晚于学习时间，无所谓）
//
//* LC下：空的无所谓
//* LM下：已完成的合并到主组（items改gid，【Nw：原分组/原分组Log记录保留】），部分完成的来源组拆分（同lg）
//* 未进行到的组不处理； 如果完成数量连主组容量都未达到则类同LG,拆分。
//
//* 错误的（首先错误的肯定是在已完成部分）
//* 由于数据源是引用形式，所以估计其错误记录数已随卡片翻阅中的操作一并更改，直接随items新状态存入db
//
//* logs记录：按开始时间处理较为容易（结束时间涉及精准度问题，中间需计算且也不是特别有道理；
//* 既然都没特别充分的道理，就按简单且精确的来吧）

    /* 常量*/
    public static final String UI_STR_LEARNING_TYPE_G = "普通";
    public static final String UI_STR_LEARNING_TYPE_M = "合并";
    public static final String UI_STR_LEARNING_TYPE_C = "创建";

    public static final int DB_DONE_LG_ACA = 2801;
    public static final int DB_DONE_LG_DVD_ACA = 2802;
    public static final int DB_DONE_LM_ACA = 2803;
    public static final int DB_DONE_LC_ACA = 2804;

    public static final String STR_FRAG_MERGE_PART = "部分吞噬";
    public static final String STR_FRAG_MERGED_FULL = "完全吞噬";
    public static final String STR_FRAG_MERGE_UN = "未受影响";


    /* 控件 */
    private FrameLayout flt_fragment;
    private FrameLayout flt_mask;
    private TextView tv_startTime;
    private TextView tv_usedUpTime;
    private TextView tv_learningType;
    private TextView tv_saving;


    //    private TextView tv_finishTime;


    /* Intent收发*/
    /* 传来*/
    private int learningType;
    private String tableSuffix = "";
    private ArrayList<SingleItem> items;//之前几页的主数据源，配合上两个位置列表进行修改。
    private ArrayList<Integer> emptyItemPositions;
    private ArrayList<Integer> wrongItemPositions;

    private int groupId;//仅在LG模式下随intent传来。
    private int missionId;//仅在LCO/LCR模式下随intent传来。
    private ArrayList<Integer> gIdsForMerge;//仅在LM模式下随intent传来。


    private long startTime = 0;
    private int restMinutes = 0;//用于在本页显示已用时间
    private int restSeconds = 0;

    /* 以下数据用于report_fg中的数据显示*/
    private float newRma=0;
    private float oldRma=0;
    private int newMs = 0;
    private int oldMs = 0;

    private int totalNum=0;
    private int doneNum=0;
    private int emptyNum=0;
    private int correctNum=0;
    private int wrongNum=0;



    private boolean isMsUp =false;//report_fg中根据isMsUp+isTooLate的配合来确定是“升级、未升级-太早、未升级-太晚”三者之哪一个。
    private boolean isTooLate = false;

    /* 业务变量*/
    private ArrayList<Integer> oldFragsSizes;
    private ArrayList<Integer> newFragsSizes;//用于LM下合并提交到DB后各来源组的新容量。
//    private ArrayList<String> oldFragsMergedResultStrings;//使用新旧数量对比可以代替结果字段
    ArrayList<String> gpDescriptions;

    private ArrayList<SingleItem> notEmptyItems;//在LC及LM模式下，需要把非空词组成一个临时items数据集。然后不应再操作items主集。

//    private boolean isTooNear = false;//本次复习（的开始时间）距上次复习的时间是否太近（从而MS不提升）
//    private boolean isTooFar = false;
    private boolean isDivided = false;//最终是否拆分（仅LG模式下使用）.[LM模式下，如果主组都未完成，则也要拆分，同样会使用本标记]



//    private RVGroup groupRvNew;//（LG模式下用）DB操作完成后，从DB重新取得分组数据。


    private String newGroupStr="";
    private String wrongNamesStr="";

    private Handler handler = new AccomplishActivityHandler(this);//涉及弱引用，通过其发送消息。

    private YoMemoryDbHelper memoryDbHelper;
    private DBGroup group;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accomplish);

        //从intent取部分数据
        learningType = getIntent().getIntExtra(STR_LEARNING_TYPE,0);
        tableSuffix = getIntent().getStringExtra(STR_TABLE_NAME_SUFFIX);
        items = getIntent().getParcelableArrayListExtra(STR_ITEMS);

        emptyItemPositions = getIntent().getIntegerArrayListExtra(STR_EMPTY_ITEMS_POSITIONS);
        wrongItemPositions = getIntent().getIntegerArrayListExtra(STR_WRONG_ITEMS_POSITIONS);

        startTime = getIntent().getLongExtra(STR_START_TIME,0);
//        finishTime = getIntent().getLongExtra("FINISH_TIME",0);
        restMinutes = getIntent().getIntExtra(STR_REST_MINUTES,0);
        restSeconds = getIntent().getIntExtra(STR_REST_SECONDS,0);



        flt_mask = findViewById(R.id.flt_mask_ACA);

        flt_fragment = findViewById(R.id.flt_fragment_AC);

        tv_startTime = findViewById(R.id.tv_startTime_ACA);
        tv_usedUpTime = findViewById(R.id.tv_usedUpTime_ACA);

        tv_learningType = findViewById(R.id.tv_learningType_ACA);
        tv_saving = findViewById(R.id.tv_saving_donStop_AC);


        SimpleDateFormat sdf = new SimpleDateFormat(STR_DATE_PATTEN_1);

        tv_startTime.setText(String.format(getResources().getString(R.string.learning_starting_time_str),sdf.format(new Date(startTime))));
        //从剩余时间到已消耗时间的计算
        int usedTimeSeconds =60-restSeconds;
        int usedTimeMinutes = 60-restMinutes;
        //边界调整
        if(usedTimeSeconds!=60){
            usedTimeSeconds = 0;
            usedTimeMinutes+=1;
        }
        tv_usedUpTime.setText(String.format(getResources().getString(R.string.learning_usd_up_time),usedTimeMinutes,usedTimeSeconds));

        if(emptyItemPositions.size()==items.size()){
            //一个都没完成，这时就别忙活了
            tv_saving.setText(R.string.done_nothing);
            return;
        }


        //中部区域根据不同的学习类型加载不同fg(再消息处理方法中处理)


        String tempStr = "";
        if(learningType == LEARNING_GENERAL){
            tempStr=UI_STR_LEARNING_TYPE_G;
            groupId = getIntent().getIntExtra(STR_GROUP_ID,0);

            new Thread(new GeneralAccomplishRunnable()).start();         // 用于LG模式下的DB与计算线程
            //fg的加载到消息处理方法完成。

        }else if(learningType == LEARNING_AND_MERGE){
            tempStr =UI_STR_LEARNING_TYPE_M;
//            gIdsForMerge = new ArrayList<>();。
            gIdsForMerge = getIntent().getIntegerArrayListExtra(STR_GROUP_ID_FOR_MERGE);

//            rv_oldFragsMergeInfo = (RecyclerView)findViewById(R.id.rv_oldFragsChange_ACA);
            new Thread(new MergedAccomplishRunnable()).start();         // 用于LM模式下的DB与计算线程
            //fg的加载到消息处理方法完成。

        }else if(learningType == LEARNING_AND_CREATE_RANDOM ||learningType == LEARNING_AND_CREATE_ORDER) {
            tempStr = UI_STR_LEARNING_TYPE_C;
            missionId = getIntent().getIntExtra(STR_MISSION_ID,0);

            new Thread(new CreatedAccomplishRunnable()).start();         // 用于LC模式下的DB与计算线程
        }
        tv_learningType.setText(String.format(getResources().getString(R.string.hs_learningType),tempStr));

        //各种模式下，都会传递错词记录表；因而可以统一处理【放在分线程后面可能能利用线程空余提升效率（待？）】

        //相应地，底部显示错词信息的tv所需的String，也可统一构造【旧版设计在各Runnable中生成，多次重复】
        //准备错次+1的词的列表
      /*  【待转到fg中】
      if(wrongItemPositions.size() == 0){
            strForTvErrItems = "(无)";
        }else {
            StringBuilder sbdForErrSrtList = new StringBuilder();
            for (int i :
                    wrongItemPositions) {
                sbdForErrSrtList.append(items.get(i).getName());
                sbdForErrSrtList.append(", ");
            }
            sbdForErrSrtList.deleteCharAt(sbdForErrSrtList.lastIndexOf(","));
            strForTvErrItems = sbdForErrSrtList.toString();
        }

        tv_errInfo.setText(String.format(getResources().getString(R.string.strH_these_add_one),strForTvErrItems));
*/


    }


    final static class AccomplishActivityHandler extends Handler {
        private final WeakReference<AccomplishActivity> activity;

        private AccomplishActivityHandler(AccomplishActivity activity) {
            this.activity = new WeakReference<AccomplishActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AccomplishActivity accomplishActivity = activity.get();
            if(accomplishActivity != null){
                accomplishActivity.handleMessage(msg);
            }

        }}

    /*
    * 正常学习模式下对应
    * 任务1、LOG信息的更新
    * ①判断本次学习在时间上是否MS“有效”（需要获取上次的log记录，当前MS值）【设计是过久、过近都不能使MS上升】,
    * ②更新本组的Log表信息——将time、gid、是否有效，写入log表。
    * 任务2、Item表信息的更新
    * ①将上一页的items数据源直接传递过来；
    * ②根据err列表，对相应item的错次+1；
    * ③根据优先级表，对相应item设置优先级【引用类型？似乎不需额外操作】；
    * 任务3、拆分（非必要）
    * 如果empty表非空，则需拆分：先从DB的group、log表获取旧表信息，在DB生成一个新的DBGroup记录，
    * 更新log表：上一步的新分组是只有Group记录的空分组，接下来根据旧表的log信息生成新logs。
    * 更新items表：按empty的list，修改items中相应的gid，存入DB；
    *
    * 其中group类的信息需要做一些修改，log信息基本照搬【而且可能出现“建立时间”晚于早期“学习记录”时间的情况。仍然是合理的】
    */
    public class GeneralAccomplishRunnable implements Runnable{
        @Override
        public void run() {
            memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());
            group = memoryDbHelper.getGroupById(groupId,tableSuffix);//取到的group信息对应于学习活动前的分组信息


            //准备存入DB的系列操作
            //一、Logs。
            // 以下数据计算本次学习的时间有效与否
            oldMs = group.getEffectiveRePickingTimes();
            SingleLearningLog newLearningLog = new SingleLearningLog(startTime, groupId, true);//按开始时间（暂定）

            if(oldMs !=0){
                //不是初次学习时执行
                //如果==0则是初次学习。此时上次学习是“1970-1-1”无法计算门槛时间，因而无计算必要，直接置true（保留不动）即可

                //以下，计算时间门槛，确定本log是否为“有效”
                oldRma = RVGroup.getRMAmount((byte)oldMs, group.getLastLearningTime());
                int minutesFarThreshold = RVGroup.minutesTillFarThreshold((byte)oldMs, oldRma);
                int minutesShortThreshold = minutesFarThreshold / 5;
//                Log.i(TAG, "run: old ms_1=" + oldMs);
//                Log.i(TAG, "run: old rma_1=" + rma);

                //判断时间以及MS是否有效（但不论是否有效都需要将本次log写入DB（区别在于isEffective列的值））
                int minutesSinceLastLog = (int) (startTime - group.getLastLearningTime()) / (1000 * 60);

                //构造本次学习对应的单条新log，并判定是否有效【并做全局记录，稍后要将数据传入fg】
                if (minutesSinceLastLog < minutesShortThreshold) {
                    newLearningLog.setEffective(false);
//                    isTooNear = true;
                    isTooLate = false;
                    isMsUp = false;
                } else if (minutesSinceLastLog > minutesFarThreshold) {
                    newLearningLog.setEffective(false);
//                    isTooFar = true;
                    isMsUp = false;
                    isTooLate = true;
                } else {
                    newLearningLog.setEffective(true);
                    isMsUp = true;
                    isTooLate = false;
                }
            }else {
                //初学
                newLearningLog.setEffective(true);
                isMsUp = true;
                isTooLate = false;
            }

            //【DB操作】：新Log记录提交到DB
            long lineForLogs = memoryDbHelper.createSingleLog(newLearningLog);
            //判断DB返回的值，操作是否正确。
            if(lineForLogs == -1){
                Toast.makeText(AccomplishActivity.this, "DB Error，Log表新增记录异常", Toast.LENGTH_SHORT).show();
            }


            //在判断拆分与否前先生成Message对象，两分支使用同一消息、携带不同数据。
            Message message =new Message();

            //Items的处理（DB处理、向fg发送）
            //将Items中有出错的词，其错误记录数+1
            //修改错词
            //将Items中有出错的词，其错误记录数+1
            //并生成错误字串
            StringBuilder sbdr = new StringBuilder();
            if(!wrongItemPositions.isEmpty()) {
                for (int i : wrongItemPositions) {
                    items.get(i).failSpellingSelfAddOne();
                    sbdr.append(items.get(i).getName());
                    sbdr.append(", ");
                }
                sbdr.deleteCharAt(sbdr.length()-2);
            }
            wrongNamesStr = sbdr.toString();

            //【以上，Log提交和items的错词记录数是统一处理的；其余的逻辑都需要按是否拆分区别处理。
            // item的gid、新组建立、新组日志拷贝

            //判断是否涉及拆分
            if(emptyItemPositions.size()!=0){
                //有未学习的数据，需拆分【此逻辑仅LG适用】
                isDivided=true;
                //以下是拆分逻辑
                DBGroup newGroupForSplitting = new DBGroup();//新组用于未学到的各item

                //生成新组的描述字串【暂时只需三项字段。其余字段在group建立后再生成】
                String strForDescription = "拆分生成-"
                        +items.get(emptyItemPositions.get(0)).getName()+"等"+items.size()+"个";

                newGroupStr=strForDescription;//还要给fg发送一套。

                newGroupForSplitting.setDescription(strForDescription);
                newGroupForSplitting.setMission_id(group.getMission_id());
                newGroupForSplitting.setSettingUptimeInLong(System.currentTimeMillis());//以当前时间为所生成的拆分组设立时间

                //【DB操作，新分组（拆分生成）创建】
                int newGid = memoryDbHelper.createEmptyGroup(newGroupForSplitting);
                //判断DB返回的结果
                if(newGid == 0){
                    Toast.makeText(AccomplishActivity.this, "生成临时拆分组异常", Toast.LENGTH_SHORT).show();
                    return;
                }

                //处理拆分分组的日志
                //获取旧分组的全部日志
                ArrayList<SingleLearningLog> oldLearningLogs = memoryDbHelper.getAllLogsOfGroup(groupId);

                //【DB操作：新生成分组】的日志批量拷贝
                boolean isCorrect = memoryDbHelper.createBatchLogsForGroup(oldLearningLogs, newGid);
                //判断DB返回结果是否正确【本判断方式并不能涵盖所有异常情形，待改进】
                if(!isCorrect){
                    Toast.makeText(AccomplishActivity.this, "拆分组的日志拷贝异常_1", Toast.LENGTH_SHORT).show();
                    return;
                }//至此，新旧分组的日志DB操作均完毕。


                //对未学的词，设置其归属于新分组；已学的词其gid不变。（注意最后提交给DB的是整体items）
                for (int i :emptyItemPositions) {
                    items.get(i).setGroupId(newGid);
                }
                //【DB操作：所有items】提交到DB（DB中的处理逻辑只修改gid、优先级、错次三项）
                int rowsAffected = memoryDbHelper.updateItemsTri(tableSuffix,items);

                if(rowsAffected!=items.size()){
                    Toast.makeText(AccomplishActivity.this, "items的数据库存入异常", Toast.LENGTH_SHORT).show();
                    return;
                }//对已学的词，要更新其两项内容（优先级、错次两项）【但是上一方法中其实已学词的gid未变且一同提交，因而不需单独处理】

            }else {
                //没有未学词，不需拆分。只对所有item的优先级、错次两项（需要者）进行修改
                int rowsAffected =memoryDbHelper.updateItemsPrtAndErr(tableSuffix,items);
                if(rowsAffected!=items.size()){
                    Toast.makeText(AccomplishActivity.this, "items的数据库存入异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            //至此，LG模式下的DB处理已完成。后面是与主线程的通信（包括为fg准备数据）
            message.what = DB_DONE_LG_ACA;

            totalNum=items.size();
            emptyNum = emptyItemPositions.size();
            wrongNum = wrongItemPositions.size();
            doneNum = totalNum -emptyNum;
            correctNum = doneNum - wrongNum;

            //准备需要向fg发送的信息，先通过全局变量传递到UI线程
            DBGroup groupNew = memoryDbHelper.getGroupById(groupId,tableSuffix);//虽是同一个gid，但此时其log、items(拆分时)均已得到更新；
            RVGroup groupRvNew = new RVGroup(groupNew);
            newRma = groupRvNew.getRM_Amount()<100?groupRvNew.getRM_Amount():100;//由于浮点计算，有可能会得到100.8这样的结果
//            Log.i(TAG, "run: newRma="+newRma);
            newMs = groupNew.getEffectiveRePickingTimes();
//            Log.i(TAG, "run: newMS="+newMs);

            //分组的旧RMA/ms数据
//            RVGroup groupRvOld = new RVGroup(group);//相应group类持有的lastLearningTime仍然是复习之前的数据【计算出来和新的一样，不对】
//            oldRma = groupRvOld.getRM_Amount();
//            Log.i(TAG, "run: oldRma="+oldRma);

//            AccomplishActivity.this.oldMs = groupRvOld.getMemoryStage();
//            Log.i(TAG, "run: oldMs="+ AccomplishActivity.this.oldMs);
//            Log.i(TAG, "run: item(0).priority="+items.get(0).getPriority());
            handler.sendMessage(message);
        }
    }

    public class MergedAccomplishRunnable implements Runnable{
        @Override
        public void run() {
            //【注意，由于学习页不限制卡片的滑动，从而空词并非都是连续排列在后边的！！】
            //【逻辑修改，合并的结果不再是合到主组，而是合并到一个新组，类似于LC，但是该组具备和各来源组一致的MS】

            memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());



            //一、生成新组（空组）
            //寻找非空的词。将所有非空词加入一个临时列表，用于稍后修改。
            //对items的所有索引值进行判断，如果该索引不在“空词的索引列表”中，则该索引对应的词非空。
            notEmptyItems = new ArrayList<>();
            for(int i=0; i<items.size();i++){
                if(emptyItemPositions.indexOf(i)==-1){//已确认，在无此元素时返回-1。
                    notEmptyItems.add(items.get(i));//此后，本线程只使用此列表数据集操作即可(实际上指向的元素仍是同一套资源)
                }//由于全空状态下在进新线程前就被if分支截去了，因而必然有一个非空位置
            }
            Log.i(TAG, "run: emptyIPList.size="+emptyItemPositions.size()+"notEmpList.size="+notEmptyItems.size());

            //一个都没学时，在onCreate方法已经截至，所以这里一定是有数据的

            String gDescription = notEmptyItems.get(0).getName()+"等"+notEmptyItems.size()+"个[合并创建]";
            newGroupStr=gDescription;//还要给fg发送一套。

            //下面这个类只用于生成空的分组，不生成ms记录相关内容，gid可以留0。
            DBGroup groupToBeCreate = new DBGroup(0,gDescription,missionId,System.currentTimeMillis(),startTime,(byte) 0,(short) 0);

            //生成新组【DB操作】在group表的记录
            int gid = memoryDbHelper.createEmptyGroup(groupToBeCreate);
//            Log.i(TAG, "run: LM下生成的新空组gid="+groupId);

            //二、准备Logs（复习时间采用开始时间）
            SingleLearningLog singleLearningLog = new SingleLearningLog(startTime,gid,false);
            //【DB操作，生成log】
            long lineForLogs = memoryDbHelper.createSingleLog(singleLearningLog);
            //判断DB返回的值，操作是否正确。
            if(lineForLogs == -1){
                Toast.makeText(AccomplishActivity.this, "DB Error，Log表新增记录异常", Toast.LENGTH_SHORT).show();
            }


            //【DB操作：新生成分组】的日志批量拷贝
            DBGroup groupForMS = memoryDbHelper.getGroupById(gIdsForMerge.get(0),tableSuffix);
            int msAmount = groupForMS.getEffectiveRePickingTimes();//为保证今后该分组的rma正确计算，
            // 需要构造若干条伪“有效”logs记录
            ArrayList<SingleLearningLog> fakeLogs = new ArrayList<>();
            for(int i =0;i<msAmount;i++){
                fakeLogs.add(new SingleLearningLog(0,gid,true));
            }
            //批量提交到DB【DB操作】
            boolean isCorrect = memoryDbHelper.createBatchLogsForGroup(fakeLogs, groupId);
            //判断DB返回结果是否正确【本判断方式并不能涵盖所有异常情形，待改进】
            if(!isCorrect){
                Toast.makeText(AccomplishActivity.this, "拆分组的日志拷贝异常_1", Toast.LENGTH_SHORT).show();
                return;
            }//至此，分组的真假日志DB操作均完毕。


            //三、提前进行items的错词处理【在LA需要保证逻辑上wrong和empty不能有交集】
            //将Items中有出错的词，其错误记录数+1【需要位于各分线程之前（因为需要在提交到DB之前准备好数据）】
            //修改错词
            //并生成错误字串
            StringBuilder sbdr = new StringBuilder();
            if(!wrongItemPositions.isEmpty()) {
                for (int i : wrongItemPositions) {
                    items.get(i).failSpellingSelfAddOne();//由于位置是对应到items集合的，所以需要对items操作。
                    sbdr.append(items.get(i).getName());
                    sbdr.append(", ");
                }//考虑到实际引用到同一元素因而非空列表应该也得到了修改。
                sbdr.deleteCharAt(sbdr.length()-2);
            }
            wrongNamesStr = sbdr.toString();

            //items的gid修改
            //所有非空词的gid修改为
            for (SingleItem s : notEmptyItems) {
                s.setGroupId(gid);
            }
            //先保持一个“合并”发生前各分组的容量列表
            oldFragsSizes = new ArrayList<>();
            for (int i : gIdsForMerge) {
                oldFragsSizes.add(memoryDbHelper.getSubItemsNumOfGroup(i,tableSuffix));
            }
//            oldFragsMergedResultStrings = new ArrayList<>();//用于记录源碎片组是否被删除，用于rv直接显示【1.合并删除、2.合并拆分、3.未合并】

            //目前，非空词已完成状态修改（gid、err、优先级），可以提交。【DB操作，items保存①】
            memoryDbHelper.updateItemsTri(tableSuffix,notEmptyItems);

            //用于合并的各分组的描述
            gpDescriptions = new ArrayList<>();
            for (int i : gIdsForMerge) {
                gpDescriptions.add(memoryDbHelper.getGroupDescriptionById(i));
            }

            //取得合并后的各分组容量列表,并删除已空组。
            newFragsSizes = new ArrayList<>();
            for (int i : gIdsForMerge) {
                int size = -1;
                size = memoryDbHelper.getSubItemsNumOfGroup(i,tableSuffix);
                newFragsSizes.add(size);

                if(size == 0){
                    memoryDbHelper.deleteGroupById(i,tableSuffix);
                }
//                oldFragsMergedResultStrings.add("");//完全初始化
            }

            /*for(int i=0;i<gIdsForMerge.size();i++){
                if(newFragsSizes.get(i)==0){
                    //该组已被完全吞噬
                    oldFragsMergedResultStrings.set(i, STR_FRAG_MERGED_FULL);
                }else if(newFragsSizes.get(i)<oldFragsSizes.get(i)){
                    //该位置上的来源分组被部分吞噬
                    oldFragsMergedResultStrings.set(i, STR_FRAG_MERGE_PART);
                }else if(newFragsSizes.get(i).equals(oldFragsSizes.get(i))){
                    //该位置上的来源分组未被吞噬，保留原样
                    oldFragsMergedResultStrings.set(i, STR_FRAG_MERGE_UN);
                }
            }*/


            //最后需要把oldSizes、newSizes、gpDescriptions三个列表全部传给dfg的rv适配器
            // 这三个列表均是按gidFm的顺序。


            Message messageForMergeDone = new Message();
            messageForMergeDone.what = DB_DONE_LM_ACA;

            totalNum=items.size();
            emptyNum = emptyItemPositions.size();
            wrongNum = wrongItemPositions.size();
            doneNum = totalNum -emptyNum;
            correctNum = doneNum - wrongNum;


            //已经对数据完成了DB提交，现在准备为UI获取新版信息
            //由于LM模式下，开始时不传递group前来，因而不持有旧组logs信息，即使在“主组发生拆分”这种
            // 实际等同于LG的结果下，也无法计算旧RMA/MS数据。因而不对相关信息进行显示。

            handler.sendMessage(messageForMergeDone);

        }
    }

    public class CreatedAccomplishRunnable implements Runnable{
        @Override
        public void run() {

            memoryDbHelper= YoMemoryDbHelper.getInstance(getApplicationContext());
            notEmptyItems = new ArrayList<>();
            //寻找非空的词。将所有非空词加入一个临时列表，用于稍后修改。
            //对items的所有索引值进行判断，如果该索引不在“空词的索引列表”中，则该索引对应的词非空。
            for(int i=0; i<items.size();i++){
                if(emptyItemPositions.indexOf(i)==-1){//已确认，在无此元素时返回-1。
                    notEmptyItems.add(items.get(i));//此后，本线程只使用此列表数据集操作即可
                }//由于全空状态下在进新线程前就被if分支截去了，因而必然有一个非空位置
            }
            String gDescription = notEmptyItems.get(0).getName()+"等"+notEmptyItems.size()+"个[学习创建]";
            newGroupStr=gDescription;//还要给fg发送一套。

            DBGroup groupToBeCreate = new DBGroup(0,gDescription,missionId,System.currentTimeMillis(),startTime,(byte) 0,(short) 0);

            //生成新组【DB操作】（但是只生成了group表的记录）
            int groupId = memoryDbHelper.createEmptyGroup(groupToBeCreate);
//            Log.i(TAG, "run: LC下生成的新空组gid="+groupId);

            //准备Logs（复习时间采用开始时间；置为有效）
            SingleLearningLog singleLearningLog = new SingleLearningLog(startTime,groupId,true);
            //【DB操作，生成log】
            long lineForLogs = memoryDbHelper.createSingleLog(singleLearningLog);
            //判断DB返回的值，操作是否正确。
            if(lineForLogs == -1){
                Toast.makeText(AccomplishActivity.this, "DB Error，Log表新增记录异常", Toast.LENGTH_SHORT).show();
            }

            //准备Items数据
            //修改gid
            for (SingleItem s :notEmptyItems) {
                s.setGroupId(groupId);
            }

            //修改错词
            //将Items中有出错的词，其错误记录数+1
            //并生成错误字串
            StringBuilder sbdr = new StringBuilder();
            if(!wrongItemPositions.isEmpty()) {
                for (int i : wrongItemPositions) {
                    items.get(i).failSpellingSelfAddOne();
                    sbdr.append(items.get(i).getName());
                    sbdr.append(", ");
                }
                sbdr.deleteCharAt(sbdr.length()-2);
            }
            wrongNamesStr = sbdr.toString();
            memoryDbHelper.updateItemsPdgWithDoubleTrue(tableSuffix,notEmptyItems);


            Message messageForLC = new Message();
            messageForLC.what = DB_DONE_LC_ACA;

            totalNum=items.size();
            emptyNum = emptyItemPositions.size();
            wrongNum = wrongItemPositions.size();
            doneNum = totalNum -emptyNum;
            correctNum = doneNum - wrongNum;




            DBGroup groupDbNew = memoryDbHelper.getGroupById(groupId,tableSuffix);
            RVGroup groupRVNew = new RVGroup(groupDbNew);
            newMs = groupRVNew.getMemoryStage();
            newRma = groupRVNew.getRM_Amount();

            handler.sendMessage(messageForLC);

        }
    }



    void handleMessage(Message msg) {
        //“请勿强行退出”的提示取消
        tv_saving.setText("<存储完成>");

        //数据和DB存储已处理完成，可以加载fg部分
        FragmentTransaction transaction = (getFragmentManager().beginTransaction());
        Fragment prev = (getFragmentManager().findFragmentByTag("REPORT_GROUP"));

        if (prev != null) {
            Toast.makeText(this, "Old Dfg still there, removing...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }

        Bundle bundleForFG = new Bundle();
        bundleForFG.putInt("TOTAL_NUM",totalNum);
        bundleForFG.putInt("DONE_NUM",doneNum);
        bundleForFG.putInt("EMPTY_NUM",emptyNum);
        bundleForFG.putInt("CORRECT_NUM",correctNum);
        bundleForFG.putInt("WRONG_NUM",wrongNum);

//        Log.i(TAG, "handleMessage: newRma before put in Bundle = "+newRma);
//        Log.i(TAG, "handleMessage: oldRma before put in Bundle = "+oldRma);
        bundleForFG.putFloat(STR_NEW_RMA,newRma);
        bundleForFG.putFloat(STR_OLD_RMA,oldRma);
        bundleForFG.putInt(STR_NEW_MS,newMs );
        bundleForFG.putInt(STR_OLD_MS,oldMs);

        bundleForFG.putString("NEW_GROUP",newGroupStr);
        bundleForFG.putString("WRONG_NAMES",wrongNamesStr);

        bundleForFG.putBoolean("IS_MS_UP",isMsUp);
        bundleForFG.putBoolean("IS_TOO_LATE",isTooLate);

        switch (msg.what){
            case DB_DONE_LG_ACA:
                Fragment fgLG = ReportGroupLG_Fragment.newInstance(bundleForFG);
                transaction.add(R.id.flt_fragment_AC, fgLG, "REPORT_GROUP").commit();

                break;

            case DB_DONE_LM_ACA:
                bundleForFG.putStringArrayList(STR_GP_DESCRIPTIONS,gpDescriptions);
                bundleForFG.putIntegerArrayList(STR_OLD_SIZES,oldFragsSizes);
                bundleForFG.putIntegerArrayList(STR_NEW_SIZES,newFragsSizes);

                Fragment fgLM = ReportGroupLM_Fragment.newInstance(bundleForFG);
                transaction.add(R.id.flt_fragment_AC, fgLM, "REPORT_GROUP").commit();

                break;
            case DB_DONE_LC_ACA:
                Log.i(TAG, "handleMessage: DB_DONE_LC_ACA");
                Fragment fgLC = ReportGroupLC_Fragment.newInstance(bundleForFG);
                transaction.add(R.id.flt_fragment_AC, fgLC, "REPORT_GROUP").commit();

                break;
        }
    }

    public void allFinishGoBack(View view){
        this.finish();
    }

}
