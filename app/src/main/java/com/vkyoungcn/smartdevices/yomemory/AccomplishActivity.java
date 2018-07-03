package com.vkyoungcn.smartdevices.yomemory;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.models.SingleLearningLog;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_MERGE;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;

public class AccomplishActivity extends AppCompatActivity {

    public static final int DB_DONE_LG_UN_DVD_ACA = 2801;
    public static final int DB_DONE_LG_DVD_ACA = 2802;

    private FrameLayout flt_mask;
    private TextView tv_startTime;
    private TextView tv_usedUpTime;
    private TextView tv_finishTime;
    private TextView tv_learningType;

    private LinearLayout llt_newStatus;
    private LinearLayout llt_oldStatus;
    private LinearLayout llt_oldFrags;

    private TextView tv_newGroupInfo;
    private TextView tv_oldGroupInfo;

    private TextView tv_newRma;
    private TextView tv_newMs;
    private TextView tv_oldRma;
    private TextView tv_oldMs;

    private TextView tv_dvdInfo;
    private TextView tv_errInfo;

    private int learningType;
    private String tableSuffix = "";
    private long startTime;
    private long finishTime;

    private int usedUpTimeMins;
    private int usedUpTimeSeconds;

    private ArrayList<Integer> emptyItemPositions;
    private ArrayList<Integer> errItemPositions;
    private int groupId;//仅在LG模式下随intent传来。
    private ArrayList<Integer> gidsForMerge;//仅在LM模式下随intent传来。
    private ArrayList<SingleItem> items;//之前几页的主数据源，配合上两个位置列表进行修改。

    private Handler handler = new AccomplishActivityHandler(this);//涉及弱引用，通过其发送消息。

    private YoMemoryDbHelper memoryDbHelper;
    private DBGroup group;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accomplish);

        learningType = getIntent().getIntExtra("LEARNING_TYPE",0);
        tableSuffix = getIntent().getStringExtra("TABLE_NAME_SUFFIX");
        startTime = getIntent().getLongExtra("START_TIME",0);
        finishTime = getIntent().getLongExtra("FINISH_TIME",0);

        usedUpTimeMins = (int)(finishTime-startTime)/1000*60;
        usedUpTimeSeconds = (int)((finishTime-startTime)/1000)%60;

        emptyItemPositions = getIntent().getIntegerArrayListExtra("EMPTY_ITEMS_POSITIONS");
        errItemPositions = getIntent().getIntegerArrayListExtra("WRONG_ITEM_POSITIONS");
        items = getIntent().getParcelableArrayListExtra("ITEMS");


        flt_mask = findViewById(R.id.flt_mask_ACA);
        tv_startTime = findViewById(R.id.tv_startTime_ACA);
        tv_usedUpTime = findViewById(R.id.tv_usedUpTime_ACA);
        tv_finishTime = findViewById(R.id.tv_finishTime_ACA);
        tv_learningType = findViewById(R.id.tv_learningtype_ACA);

        llt_newStatus = findViewById(R.id.llt_newStatus_ACA);
        llt_oldStatus = findViewById(R.id.llt_oldStatus_ACA);
        llt_oldFrags = findViewById(R.id.llt_oldFrags_ACA);

        tv_newGroupInfo = findViewById(R.id.tv_groupNewInfo_ACA);
        tv_oldGroupInfo = findViewById(R.id.tv_groupOldInfo_ACA);

        tv_newRma = findViewById(R.id.tv_newRMA_ACA);
        tv_newMs = findViewById(R.id.tv_newMS_ACA);
        tv_oldRma = findViewById(R.id.tv_oldRMA_ACA);
        tv_oldMs = findViewById(R.id.tv_oldMS_ACA);

        tv_dvdInfo = findViewById(R.id.tv_groupDvdInfo_ACA);
        tv_errInfo = findViewById(R.id.tv_itemErrInfo_ACA);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        tv_startTime.setText(String.format(getResources().getString(R.string.learning_starting_time_str),sdf.format(new Date(startTime))));
        tv_usedUpTime.setText(String.format(getResources().getString(R.string.learning_usd_up_time),usedUpTimeMins,usedUpTimeSeconds));
        tv_finishTime.setText(String.format(getResources().getString(R.string.learning_finish_time_str),sdf.format(new Date(finishTime))));

        //中部区域根据不同模式采取不同UI表现
        String tempStr = "";
        if(learningType == LEARNING_GENERAL){
            tempStr="普通";
            llt_oldFrags.setVisibility(View.GONE);
            llt_oldStatus.setVisibility(View.VISIBLE);
            groupId = getIntent().getIntExtra("GROUP_ID",0);

            new Thread(new GeneralAccomplishRunnable()).start();         // start thread
            //【以下对各Tv进行设置，要等待DB处理完成后才能执行】



        }else if(learningType == LEARNING_AND_MERGE){
            tempStr ="合并";
            llt_oldStatus.setVisibility(View.GONE);
            llt_oldFrags.setVisibility(View.VISIBLE);

            gidsForMerge = getIntent().getIntegerArrayListExtra("GROUP_ID_FOR_MERGE");

            tv_newGroupInfo.setText(String.format(getResources().getString(R.string.),,));
            tv_newRma.setText();
            tv_newMs.setText();

            tv_dvdInfo.setText(String.format(getResources().getString(R.string.),);//设置为“合并出一个xx容量的分组”
            tv_errInfo.setText(String.format(getResources().getString(R.string.),);

            //为Rv-adp设置适配器

        }else {
            tempStr = "创建";
            llt_oldStatus.setVisibility(View.GONE);
            llt_oldFrags.setVisibility(View.GONE);

            tv_newGroupInfo.setText(String.format(getResources().getString(R.string.),,));
            tv_newRma.setText();
            tv_newMs.setText();

            tv_dvdInfo.setText(String.format(getResources().getString(R.string.),);//设置为“生成了一个xx容量的分组”
            tv_errInfo.setText(String.format(getResources().getString(R.string.),);


        }
        tv_learningType.setText(String.format(getResources().getString(R.string.learningType),tempStr));




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

        }};
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
            group = memoryDbHelper.getGroupById(groupId,tableSuffix);

            int minutesFarThreshold = RVGroup.minutesTillFarThreshold(group.getEffectiveRePickingTimes());
            int minutesShortThreshold = minutesFarThreshold/5;

            //判断时间以及MS是否有效（但不论是否有效都需要将本次log写入DB（区别在于isEffective列的值））
            //关于MS有效问题，如果按中间值似乎相对公平/合理的，但考虑到将来可能会在Group列表页面上，
            // 基于其当前的时间计算“如果现在开始学习，是否MS有效”该时间只能基于其当时的时间计算，因而
            // 统一起见，此处应暂按开始时间为宜，
            int minutesStartToLast = (int)(startTime-group.getLastLearningTime())/(1000*60);

            SingleLearningLog singleLearningLog = new SingleLearningLog(startTime,groupId,true);//按开始时间（暂定）
            if(minutesStartToLast<minutesShortThreshold||minutesStartToLast>minutesFarThreshold){
                singleLearningLog.setEffective(false);
            }else {
                singleLearningLog.setEffective(true);
            }

            //新Log记录提交到DB
            long lineForLogs = memoryDbHelper.createSingleLog(singleLearningLog);
            //判断DB返回的值，操作是否正确。
            if(lineForLogs == -1){
                Toast.makeText(AccomplishActivity.this, "DB Error，Log表新增记录异常", Toast.LENGTH_SHORT).show();
            }

            //将Items中有出错的词，其错误记录数+1
            for (int i : errItemPositions) {
                items.get(i).failSpellingSelfAddOne();
            }

            //在判断拆分与否前先生成Message对象，两分支使用同一对象、不同常量。
            Message message =new Message();

            //【以上，Log提交和items的错词记录数是统一处理的；以下，按是否拆分处理item的gid、新组建立、新组日志拷贝】
            //判断是否涉及拆分
            if(emptyItemPositions.size()!=0){
                //有未学习的数据，需拆分【此逻辑仅在LG模式下适用】

                //以下是拆分逻辑
                DBGroup newGroup = new DBGroup();//新组用于未学到的各item

                //生成新组的描述字串【暂时只需三项字段。其余字段在group建立后再生成】
                String strForDescription = "拆分产生，"
                        +items.get(emptyItemPositions.get(0)).getName()+"起，数量"+items.size();
                newGroup.setDescription(strForDescription);
                newGroup.setMission_id(group.getMission_id());
                newGroup.setSettingUptimeInLong(finishTime);//以此次学习结束的时间为拆分（产生的）组的设立时间

                //提交到DB
                int newGid = memoryDbHelper.createEmptyGroup(newGroup);
                //判断DB返回的结果
                if(newGid == 0){
                    Toast.makeText(AccomplishActivity.this, "生成临时拆分组异常", Toast.LENGTH_SHORT).show();
                    return;
                }

                //处理拆分分组的日志
                //获取旧分组的全部日志
                ArrayList<SingleLearningLog> oldLearningLogs = memoryDbHelper.getAllLogsOfGroup(groupId);
                //将日志复制给新分组（提交到DB）
                boolean isCorrect = memoryDbHelper.createBatchLogsForGroup(oldLearningLogs, newGid);
                //判断DB返回结果是否正确【本判断方式并不能涵盖所有异常情形，待改进】
                if(!isCorrect){
                    Toast.makeText(AccomplishActivity.this, "拆分组的日志拷贝异常_1", Toast.LENGTH_SHORT).show();
                    return;
                }

                //对未学的词，设置其归属于新分组
                for (int i :
                        emptyItemPositions) {
                    items.get(i).setGroupId(newGid);
                }
                //提交到DB（DB中的处理逻辑只修改gid、优先级、错次三项）
                int rowsAffected = memoryDbHelper.updateItemsTri(tableSuffix,items);
                if(rowsAffected!=emptyItemPositions.size()){
                    Toast.makeText(AccomplishActivity.this, "拆分组的日志拷贝异常_1", Toast.LENGTH_SHORT).show();
                    return;
                }
                //对已学的词，要更新其两项内容（优先级、错次两项）【但是上一方法中其实已学词的gid未变且一同提交，因而不需单独处理】

                message.what = DB_DONE_LG_DVD_ACA;

            }else {
                //没有未学词，不需拆分。只对所有item的优先级、错次两项（需要者）进行修改
                memoryDbHelper.updateItemsDual(tableSuffix,items);

                message.what = DB_DONE_LG_UN_DVD_ACA;
            }



            handler.sendMessage(message);

        }
    }

    void handleMessage(Message msg) {
        switch (msg.what){
            case DB_DONE_LG_UN_DVD_ACA:
                //确定是在LG模式下才会到达此处(且是未拆分)
                tv_newGroupInfo.setText(String.format(getResources().getString(R.string.groupId_Num),groupId,group.getTotalItemNum()));
                tv_oldGroupInfo.setText(String.format(getResources().getString(R.string.groupId_Num),groupId,group.getTotalItemNum()));//此时新旧其实一致
                tv_newRma.setText();
                tv_newMs.setText();
                tv_oldRma.setText();
                tv_oldMs.setText();

                tv_dvdInfo.setText(String.format(getResources().getString(R.string.),);//设置为“xx个未完成，拆分”【其他模式下则是另外的srt】
                tv_errInfo.setText(String.format(getResources().getString(R.string.),);
                break;
            case DB_DONE_LG_DVD_ACA:

                break;

        }
    }
}
