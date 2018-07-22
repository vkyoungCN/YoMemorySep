package com.vkyoungcn.smartdevices.yomemory;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.GroupsOfMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.CreateGroupDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningCreateOrderDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningCreateRandomDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningGelDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMerge2DiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RvMergeGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class GroupsOfMissionActivity extends AppCompatActivity
        implements OnGeneralDfgInteraction,Constants {
//本Activity是单个Mission所属所有分组（列表形式）的展示页；
//    页面上部是简单的Mission信息；页面具备：新建分组、发起创建式学习、发起合并式学习的功能，
// 点击FAB按钮展开相应控制面板。
//    页面下部以RecyclerView（纵向）形式给出所属全部分组的列表；分组列表按某种顺序【待定】排列；
// 单击列表项进入分组详情页，点击列表项目中的学习按钮，可以开始学习；
// 长按列表项可以删除分组。（分组删除后，所属词汇回归为未选中状态）
//     从本页面发起的学习/复习活动，在操作完成后会跳转到本页面（因随后的PFA、LA、AA三页面都不进入历史栈）
   /* 常量声明*/
    private static final String TAG = "GroupsOfMissionActivity";
    /* 消息常量声明*/
    public static final int MESSAGE_PRE_DB_FETCHED = 5011;
    public static final int MESSAGE_RE_FETCH_DONE = 5012;
    public static final int MESSAGE_RV_SCHEDULE_REFRESHING = 5013;


    /* 变量声明区*/
    /* 从Intent获取的数据*/
    private RvMission missionFromIntent;//从前一页面获取。后续页面需要mission的id，suffix字段。
    private String tableItemSuffix;//由于各任务所属的Item表不同，后面所有涉及Item的操作都需要通过后缀才能构建出完整表名。

    /* 数据库操作，从数据库取得的数据*/
    private YoMemoryDbHelper memoryDbHelper;
    ArrayList<RVGroup> rvGroups = new ArrayList<>();//用于RecyclerView的数据源（由ArrayList<DbGroup>转换而来）

    private Handler handler = new GroupOfMissionHandler(this);//涉及弱引用，通过其发送消息。
    private Boolean needForScheduleRefreshing = true;//分组列表数据定时更新线程的控制变量。当刷新分组列表时暂停该更新；（？退出到Pause状态时，是否需要手动停止？）
    private Boolean fetched = false;//是否已执行完成过从DB获取分组数据的任务；如完成，则onResume中可以重启UI-Timer

    private Activity self;//为了后方Timer配合runOnUiThread.
    List<Integer> refreshingNeededPositionsList = new ArrayList<>();//更新线程会把需要更新的项目的索引Id（每分钟变更一次）存在这个表中


    /* 控件及控件相关变量*/
    private RecyclerView mRv;
    private FrameLayout maskFrameLayout;
    private RelativeLayout rltFabPanel;
    TextView tv_groupAmount;

    private boolean isFabPanelExtracted = false;//FAB面板组默认处于回缩状态。
    private GroupsOfMissionRvAdapter adapter = null;//Rv适配器引用


//任务名称、描述两控件在onCreate内以局部变量声明。
//    private int clickPosition;//点击（前往学习页面）发生的位置，需要该数据来更新rv位置
//    private Boolean isHandyRefreshing = false;//点击刷新列表的按键后，会重新执行加载数据的线程，为与首次的自动运行相区分，此标志变量会设true。
//【实际上，目前的经验表明，无论是interrupt方法还是布尔变量置否，似乎都不能使线程立即停止】
//    private ArrayList<RvMergeGroup>[][] groupsInTwoDimensionArray;//用于后续DFG的数据装载，两个维度分别对应MS、同MS下<4,<8。


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.self = this;
        setContentView(R.layout.activity_groups_of_mission);

        TextView missionDetailName = findViewById(R.id.tv_mission_detail_name_GMDA);
        TextView missionDetailDescription = findViewById(R.id.tv_mission_detail_description_GMDA);
        tv_groupAmount = findViewById(R.id.tv_groupAmount_GMD);
        maskFrameLayout = findViewById(R.id.maskOverRv_MissionDetail_GMDA);
        rltFabPanel = findViewById(R.id.rlt_fabFlat_GMDA);

        rltFabPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //在板子展开后，点击板子需要能缩回（隐藏）
                if(isFabPanelExtracted){
                    rltFabPanel.setVisibility(View.GONE);
                    isFabPanelExtracted = false;
                }//只负责缩回就好了。
            }
        });

        missionFromIntent = getIntent().getParcelableExtra(STR_MISSION);
        if (missionFromIntent == null) {
            Toast.makeText(self, "任务信息传递失败", Toast.LENGTH_SHORT).show();
            return;
        } else {
            //根据Mission数据填充Mission信息两项
            missionDetailName.setText(missionFromIntent.getName());
            missionDetailDescription.setText(missionFromIntent.getSimpleDescription());
            tableItemSuffix = missionFromIntent.getTableItem_suffix();
        }

        memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());

        new Thread(new PrepareForGroupsAndMissionRunnable()).start();         // start thread
    }

    //需要采用在pause-resume中重启线程的方案
    // 否则（即使排除了其他BUG，依然）存在“新变更log的条目”不会随线程更新的BUG。
    @Override
    protected void onResume() {
        super.onResume();
        if (fetched) {
            //如果DB数据的加载已完成（为了避免在onCreate中调用的DB获取线程尚未运行完毕时就已到达了
            // onResume方法）（onResume还有可能是onPause后发生的，该情景下倒是应该已有数据了）
            needForScheduleRefreshing = true;
            new Thread(new RMAReCalculateRunnable()).start();// 启动UI数值刷新线程 //【实践：若只设true，线程不会自动重启。】
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        needForScheduleRefreshing = false;
    }


    final static class GroupOfMissionHandler extends Handler {
        private final WeakReference<GroupsOfMissionActivity> activityWeakReference;

        private GroupOfMissionHandler(GroupsOfMissionActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GroupsOfMissionActivity missionDetailActivity = activityWeakReference.get();
            if (missionDetailActivity != null) {
                missionDetailActivity.handleMessage(msg);
            }
        }
    }

    /*
     * 获取各分组原始数据，并进行排序、转换成RVGroup，然后返回给UI。
     * 提供两种排序方式：①衰减率最高的在前；②记忆存量最低的在前；
     */
    public class PrepareForGroupsAndMissionRunnable implements Runnable {
        @Override
        public void run() {
            //获取各分组原始数据
            ArrayList<DBGroup> dbGroupsOrigin = memoryDbHelper.getAllGroupsByMissionId(missionFromIntent.getId(), tableItemSuffix);
            //将各分组原始数据转换为UI所需数据，比较耗时。不适宜在UI线程操作，更不适合在Rv适配器内进行。

            ArrayList<RVGroup> tempRVGroups = new ArrayList<>();
            //排序。暂时按记忆级别排序
            for (DBGroup dbg : dbGroupsOrigin) {
                RVGroup rvGroup = new RVGroup(dbg);
                tempRVGroups.add(rvGroup);
                //尝试过，但似乎无法在此直接排序。无法令最新项同之前所有项目进行比较。

            }

            rvGroups = ascOrderByMemoryStage(tempRVGroups);
//            Log.i(TAG, "run: rvGroup.ms="+rvGroups.get(0).getMemoryStage());

            Message message = new Message();
            message.what = MESSAGE_PRE_DB_FETCHED;
            //数据通过全局变量直接传递。

            handler.sendMessage(message);
        }
    }

    public static ArrayList<RVGroup> ascOrderByMemoryStage(ArrayList<RVGroup> RVGroups) {
        ArrayList<RVGroup> resultRVGroups = new ArrayList<>();

        //此排序属何种算法【？】
        for (int i = 0; i < RVGroups.size(); ) {//不能i++，但size每次减少1。
            RVGroup minRVGroup = RVGroups.get(i);//指针项（代表当前最小？暂时是第一项）。即使用new也是指针形式，最后都是重复数据（且提示new无意义）

            for (int j = 1; j < RVGroups.size(); j++) {
                //从第二项开始，和指针项比较，如果小于指针项，则令指针指向当前项。（所以循环一次后，是将指针指向了当前列中的最小项）
                //因为都是引用类型，无法换值，只能移动指针。

                if (RVGroups.get(j).getMemoryStage() < minRVGroup.getMemoryStage()) {
                    minRVGroup = RVGroups.get(j);//指针指向较小者
                }
            }

            //将选出的最小项目复制到目标列表；然后再次开始循环，选除“次小”的项目。
            RVGroup gp = (RVGroup) minRVGroup.clone();//克隆方式复制（以免只复制地址）。
            resultRVGroups.add(gp);

            RVGroups.remove(RVGroups.indexOf(minRVGroup));//源列中，最小的项目已删除；则再次的循环筛选将选出次小项。

        }
        return resultRVGroups;
    }

    /*
    * 用于手动更新时从新获取数据（注意，Rv列表的刷新展示是基于已有数据进行计算刷新，这里是将底层数据都重新加载）
    * */
    public class ReFetchForGroupsAndMissionRunnable implements Runnable {
        @Override
        public void run() {
            //获取各分组原始数据
            ArrayList<DBGroup> dbGroupsOrigin = memoryDbHelper.getAllGroupsByMissionId(missionFromIntent.getId(), tableItemSuffix);
            //将各分组原始数据转换为UI所需数据，比较耗时。相关数据直接设置给Activity的成员。

            ArrayList<RVGroup> tempRVGroups = new ArrayList<>();
            //暂时按记忆级别排序
            for (DBGroup dbg : dbGroupsOrigin) {
                RVGroup RVGroup = new RVGroup(dbg);
                tempRVGroups.add(RVGroup);
            }

            rvGroups = ascOrderByMemoryStage(tempRVGroups);//数据列表指向了新数据列表。

            Message message = new Message();
            message.what = MESSAGE_RE_FETCH_DONE;
            //数据通过全局变量直接传递。

            handler.sendMessage(message);
        }
    }

    /*
     * 每隔一段时间，更新（重新）RV数据，然后更新RV-UI的显示。
     * 只负责记忆存量字段（RMA）的更新。
     *
     * 【旧：经验证，线程在onPause后（回桌面）不停止计时】
     * 【旧：从后续学习activity返回后，旧数据集仍能计时，但新学习的条目不计时】
     * 早期版本中，计算的字段对有些条目来说是很久不变的，因而只需挑选出部分要更新的条目，计入列表，
     * 按列表所载部分更新即可（可减轻符合）。现版本暂时不予区分，后期可考虑按M_Stage，较高级别的缓速更新。
     *
     * */
    public class RMAReCalculateRunnable implements Runnable {
        @Override
        public void run() {
//            int n = 0;
            while (needForScheduleRefreshing) {
//                n++;//每分加一【如果改到最后++，这样线程第一轮即可更新一次，适用于onResume；】【后来：What】

                /*并不是所有条目都需要更新，有的条目RMA数值许久不变，因而不需更新
                旧版的做法其实很精妙，是每秒进行一次检测（remainingTimeAmount），每秒向接收方发送
                 一次“需更新”的列表；但是对剩余时间较久的分组，只在秒计数器n到达15、30、60、
                 3600时，才加入“需更新”的名单。（也即，有时更新名单可能是空的；负荷主要在每秒的计算上，
                而实际的UI更新方面只更新少量的条目）*/

                try {
                    Thread.sleep(1000 * 60);//休息1分钟（每分钟更新）
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                refreshingNeededPositionsList.clear();//情况列表为新数据做准备。

                for (RVGroup singleRVGroup : rvGroups) {
                    if (singleRVGroup.needRmaRefresh()) {
                        //新旧值不同，需要更新。（对于不需要更新的不再加以处理）
                        refreshingNeededPositionsList.add(rvGroups.indexOf(singleRVGroup));
                    }
                }

                Message message = new Message();
                message.what = MESSAGE_RV_SCHEDULE_REFRESHING;

                handler.sendMessage(message);
            }
        }
    }



    void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_PRE_DB_FETCHED://此时是从DB获取各分组数据并转换成合适的数据源完成
                fetched = true;//用于onResume中的判断（如果在LC的首次流程中，数据线程可能尚未完成，此即为false，不会开启刷新线程）。

                //上方还有一个Tv没有设置数据
                tv_groupAmount.setText(String.valueOf(rvGroups.size()));

                //取消上方遮罩
                maskFrameLayout.setVisibility(View.GONE);

                //初始化Rv构造器，令UI加载Rv控件……

                adapter = new GroupsOfMissionRvAdapter(rvGroups, this, missionFromIntent.getTableItem_suffix());
                mRv = findViewById(R.id.groups_in_single_mission_rv);
                mRv.setLayoutManager(new LinearLayoutManager(this));
                mRv.setAdapter(adapter);

                //Rv加载后，启动更新计时器（要对Rv列表更新，必须必须提供新的数据；
                // 这一任务负荷较大，由新线程执行）
                new Thread(new RMAReCalculateRunnable()).start();         // start thread
                break;

            case MESSAGE_RE_FETCH_DONE:
                //手动触发的数据刷新，更新适配器并设置自动更新变量为真。
                adapter.notifyDataSetChanged();
                needForScheduleRefreshing = true;
                break;

            case MESSAGE_RV_SCHEDULE_REFRESHING:

                //所有的批量notify方法都只能用于连续项目，所以只能…
                if (refreshingNeededPositionsList == null || refreshingNeededPositionsList.size() == 0)
                    return;//判空
                for (int i : refreshingNeededPositionsList) {
                    adapter.notifyItemChanged(i);
                }

        }
    }

    public void handyRefresh(View view) {
        //手动触发刷新时（是一个底层的彻底的更新，从DB重新获取数据，然后重新计算）
        //①先停止计时更新（控制变量重设false）；
        //②清空旧Adapter数据
        //③设置 手动刷新Rv的识别变量为真（避免重新加载adp）
        //④开启从DB获取数据的线程获取新数据（不同于预获取的线程，是一个新）
        //完成后，计时更新控制变量重设真，再次启动计时更新线程【？】
//        Log.i(TAG, "handyRefresh: be");
        needForScheduleRefreshing = false;
//        rvGroups.clear();//不需要，直接指向新列表即可。
//        isHandyRefreshing = true;//此控制变量意义不明，可能思路变了。
        new Thread(new ReFetchForGroupsAndMissionRunnable()).start();         // start thread
        //控制变量uiRefreshingNeeded重设为true的操作在消息处理方法中进行
    }

    public void findGroup(View view) {
        //查询指定分组（查询的方式和条件还没想好）
        Toast.makeText(self, "施工中，查询分组的方法。", Toast.LENGTH_SHORT).show();
    }

    /*
     * 当Fab按键系统的主按钮点击时调用
     *
     * ①根据标志变量判定Fab组是否处于展开状态（再做展开或收缩处理）
     * ②展开：变量置反；组Rlt取消隐藏；（加载动画）
     * ③回缩：变量置反；组Rlt隐藏；（动画）
     * */
    public void fabMainClick(View view) {
        if (!isFabPanelExtracted) {//未展开，要做展开操作

            //标志变量取反
            isFabPanelExtracted = true;
            //展开（取消隐藏）
            rltFabPanel.setVisibility(View.VISIBLE);

        } else {
            isFabPanelExtracted = false;
            rltFabPanel.setVisibility(View.GONE);
        }
    }

    /*
     * 各控件的点击事件方法
     * */
    public void createGroup(View view) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_CREATE_GROUP);

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = CreateGroupDiaFragment.newInstance(missionFromIntent.getTableItem_suffix(), missionFromIntent.getId());
        dfg.show(transaction, FG_STR_CREATE_GROUP);
    }



    public void learnAndAddInOrder(View view) {
        //启动DFG，在dfg中点击了确认后，再交互到本Activity下的onLDfgInteraction方法，然后再生成Intent跳转。
        // 启动“创建式学习”，按顺序建组
        // 传递“顺序/随机”二选一；其余不传递
        // 由LearningActivity负责拉取36个资源，记录学习位置，最后在完成Activity对已学部分生成新组。
        // 学习中的暂停、非正常终止、正常终止逻辑都由LA负责。
        // （关于组内乱序：新建时还是按顺序学习比较好，不提供此选项）
        Toast.makeText(self, "启动创建式学习（顺序）", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_LEARNING_ADD_IN_ORDER);

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningCreateOrderDiaFragment.newInstance();
        dfg.show(transaction, FG_STR_LEARNING_ADD_IN_ORDER);

    }

    public void learnAndAddRandom(View view) {
        //启动“创建式学习”，随机顺序。
        Toast.makeText(self, "启动创建式学习（随机）", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_LEARNING_ADD_RANDOM);

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningCreateRandomDiaFragment.newInstance();
        dfg.show(transaction, FG_STR_LEARNING_ADD_RANDOM);
    }

    public void learnAndMerge(View view) {
        //原则上只从数量小于4个的分组中抽取资源，（可以选择，可扩大到8个）
        //①圈定可抽取的范围，②抽取资源（简单传递一批ItemId，以及GroupID以备最后删组），③记录所学的词条范围；
        //④时间到/数量到/手动点击结束（三种结束方式）后，将所学词条记录到一个新分组（按新的MS统一计算）；
        //⑤如果生成的分组仍然是小于4个，则仍然算做小分组（没有特殊处理）。
        //
        // --注意，这种边学（背/考）边建的方式，内在的要求只能“单遍操作”（翻动卡片前考核式记忆已经完成），
        // 否则若第二遍没有时间进行，那么处理逻辑可就很麻烦了。
        //
        // 关于“组内乱序”，合并式操作不宜采用。

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_MERGE);

        if (prev != null) {
            transaction.remove(prev);
        }

        Bundle data = new Bundle();
        //应当保证在自动发起的模式下，必然先展示有数据的条件设置
        //【注意，dfg的设计规则是传入同MS的所有(小于amount最大上限)分组，然后在dfg内按amount限制再二次区分】
        int autoSetMsTerm = 0;
        int maxAmountTerm = 12;
        ArrayList<RvMergeGroup> groupsForLmSelecting = new ArrayList<>();
        for (RVGroup rg :rvGroups) {
            if (rg.getTotalItemsNum()<= maxAmountTerm) {
                if(autoSetMsTerm == 0) {
                    //找到第一个符合“碎片”尺寸的分组，获取其ms
                    autoSetMsTerm = rg.getMemoryStage();
                    groupsForLmSelecting.add(new RvMergeGroup(rg));
                }else {
                    //利用该MS条件继续寻找碎片分组，并加入
                    if(rg.getMemoryStage() == autoSetMsTerm){
                        groupsForLmSelecting.add(new RvMergeGroup(rg));
                    }
                }
            }
        }

        if(groupsForLmSelecting.size() == 0) {
            Toast.makeText(self, "所有分组的容量都大于12，没有合并的必要。", Toast.LENGTH_SHORT).show();
            //不执行实际动作
        }else {
            Toast.makeText(self, "合并式学习，请选择要合并的分组。", Toast.LENGTH_SHORT).show();
            //确定有数据，启动dfg
            data.putInt(STR_TERM_MS, autoSetMsTerm);
            data.putInt(STR_TERM_AMOUNT, maxAmountTerm);
            data.putParcelableArrayList(STR_RV_MERGE_GROUP, groupsForLmSelecting);
            DialogFragment dfg = LearningMerge2DiaFragment.newInstance(data);
            dfg.show(transaction, FG_STR_READY_TO_LEARN_MERGE);
        }

    /* groupsInTwoDimensionArray = new
        //本页有全部分组的信息，应在本页准备好数据（容量<4、<8）
        // 后续页面只能直接从DB取数据，SQL复杂不宜用。
        for (RVGroup rvg : rvGroups) {
            if (rvg.getTotalItemsNum() < 4) {
                //一维上[0][x]是小于4的，x对应其不同MS；各元素本身就是一个ArrayList哦。
                groupsInTwoDimensionArray[0][rvg.getMemoryStage()].add(new RvMergeGroup(rvg));
                //替代了switch，简洁。
            } else if (rvg.getTotalItemsNum() < 8) {
                groupsInTwoDimensionArray[1][rvg.getMemoryStage()].add(new RvMergeGroup(rvg));
            }
        }*/
    }


    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {
        Intent intentToLPA = new Intent(this, PrepareForLearningActivity.class);
        intentToLPA.putExtra(STR_TABLE_SUFFIX, tableItemSuffix);
        intentToLPA.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        switch (dfgType) {
            case LEARNING_GENERAL:
                //已从LG的dfg确认返回，并紧接着要开始LG学习
                intentToLPA.putExtra(STR_LEARNING_TYPE, LEARNING_GENERAL);
                intentToLPA.putExtra(STR_BUNDLE_FOR_GENERAL, data);
                this.startActivity(intentToLPA);
                break;

            case LEARNING_GENERAL_INNER_RANDOM://LG但是开启组内乱序
                //已从LG的dfg确认返回（并且选择了开启组内乱序），紧接着要跳转到LPA并进一步开始LG学习

                //还要把FAB面板关闭（如果DFG中点取消直接返回则不需取消，点确认应当取消）
                if(isFabPanelExtracted){
                    rltFabPanel.setVisibility(View.GONE);
                    isFabPanelExtracted = false;
                }

                intentToLPA.putExtra(STR_LEARNING_TYPE, LEARNING_GENERAL_INNER_RANDOM);
                intentToLPA.putExtra(STR_BUNDLE_FOR_GENERAL, data);
                this.startActivity(intentToLPA);
                break;
            case LEARNING_AND_CREATE_ORDER:
                //已从LCO的dfg确认返回，并紧接着要开始LCO学习
                //需要传递标记

                //还要把FAB面板关闭（如果DFG中点取消直接返回则不需取消，点确认应当取消）
                if(isFabPanelExtracted){
                    rltFabPanel.setVisibility(View.GONE);
                    isFabPanelExtracted = false;
                }

                intentToLPA.putExtra(STR_LEARNING_TYPE, LEARNING_AND_CREATE_ORDER);
                intentToLPA.putExtra(STR_MISSION_ID, missionFromIntent.getId());//在最后完成页生成新组时需要本字段信息。
                this.startActivity(intentToLPA);
                break;

            case LEARNING_AND_CREATE_RANDOM:
                //已从LCR的dfg确认返回，并紧接着要开始LCR学习
                intentToLPA.putExtra(STR_LEARNING_TYPE, LEARNING_AND_CREATE_RANDOM);
                intentToLPA.putExtra(STR_MISSION_ID, missionFromIntent.getId());//在最后完成页生成新组时需要本字段信息。
                this.startActivity(intentToLPA);
                break;

            case LEARNING_AND_MERGE:
                //已从LM的dfg确认返回，并紧接着要开始LM学习
                intentToLPA.putExtra(STR_LEARNING_TYPE, LEARNING_AND_MERGE);
                intentToLPA.putExtra(STR_BUNDLE_FOR_MERGE, data);//这里传递的是从DFG（及其选择Rv）传来的，
                // 作为合并学习的来源碎片分组的分组id（构成的bundle，内部的key：IDS_GROUPS_READY_TO_MERGE）
                this.startActivity(intentToLPA);
                break;

            case DELETE_GROUP:
                //从DB删除该组，删除后更新UI显示
                int position = data.getInt(STR_POSITION);
                memoryDbHelper.deleteGroupById(rvGroups.get(position).getId(),tableItemSuffix);
                rvGroups.remove(position);
                adapter.notifyItemRemoved(position);
                tv_groupAmount.setText(String.valueOf(rvGroups.size()));
                break;


            case CREATE_GROUP:
                //将执行操作DB建组逻辑，建好后更新UI显示
                //Item抽取方式，随机或顺序
                ArrayList<Integer> itemIds = new ArrayList<>();

                boolean isOrder = data.getBoolean(STR_IS_ORDER);
                int groupSize = data.getInt(STR_GROUP_SIZE);

                //为描述字段获取首词name
                String description = data.getString(STR_DESCRIPTION);
                StringBuilder descriptionSB = new StringBuilder();
                String groupDescriptionStr = null;

                if(isOrder){
                   //按顺序，获取指定数量的Items
                   itemIds = memoryDbHelper.getCertainAmountItemIdsOrderly(groupSize,tableItemSuffix);

                   if(itemIds.size()==0){
                       Toast.makeText(this, "抽取items数量0，顺序抽取失败", Toast.LENGTH_SHORT).show();
                       return;
                   }else {
                       Toast.makeText(this, "顺序抽取成功，数量："+itemIds.size(), Toast.LENGTH_SHORT).show();
                   }

                    String firstItemName = memoryDbHelper.getSingleItemNameById((long)itemIds.get(0),tableItemSuffix);

                   //如果描述字段留空，构建默认描述字段
                   if(description == null ||description.isEmpty()){
                       descriptionSB.append("顺序-");
                       descriptionSB.append(firstItemName);
                       descriptionSB.append("开始");
                       //不附加数量，因为分组拆分后会改变。

                       groupDescriptionStr = descriptionSB.toString();
                   }
               }else {
                   //随机
                   itemIds = memoryDbHelper.getCertainAmountItemIdsRandomly(groupSize,tableItemSuffix);

                   if(itemIds.size()==0){
                       Toast.makeText(this, "抽取items数量0，随机抽取失败", Toast.LENGTH_SHORT).show();
                       return;
                   }else {
                       Toast.makeText(this, "随机抽取成功，数量："+itemIds.size(), Toast.LENGTH_SHORT).show();
                   }
                    String firstItemName = memoryDbHelper.getSingleItemNameById((long)itemIds.get(0),tableItemSuffix);

                   //如果描述字段留空，构建默认描述字段“随机分组-时间”
                   if(description == null ||description.isEmpty()){
                       descriptionSB.append("随机分组-");
                       SimpleDateFormat sdf = new SimpleDateFormat(STR_DATE_PATTEN_1);
                       descriptionSB.append(sdf.format(System.currentTimeMillis()));
                       groupDescriptionStr = descriptionSB.toString();
                   }
               }

               //构造数据类
                DBGroup dbRwaGroup = new DBGroup();
                dbRwaGroup.setDescription(groupDescriptionStr);
                dbRwaGroup.setMission_id(missionFromIntent.getId());
                dbRwaGroup.setSettingUptimeInLong(System.currentTimeMillis());
//                Log.i(TAG, "onButtonClickingDfgInteraction: DBGroup created:"+dbRwaGroup.toString());
                //操作DB，生成
                int gidCreated = memoryDbHelper.createGroup(dbRwaGroup,itemIds,tableItemSuffix);
//                Log.i(TAG, "onButtonClickingDfgInteraction: group Db inserted, gid="+gidCreated);
                //如果新增操作成功，通知adp变更。
                if (gidCreated != 0) {
                    DBGroup dGroup = memoryDbHelper.getGroupById(gidCreated, tableItemSuffix);
                    RVGroup newRVGroup = new RVGroup(dGroup);

                    rvGroups.add(0, newRVGroup);//新增分组放在最前【逻辑便于处理】
                    adapter.notifyItemInserted(0);//（仍是0起算，但是加到最后时似乎比较奇怪）
                    mRv.scrollToPosition(0);//设置增加后滚动到新增位置。【已查，从0起算】
                } else {
                    Toast.makeText(self, "操作未能成功，returned gid="+gidCreated, Toast.LENGTH_SHORT).show();
                }
                fabPanelCollapse();
                tv_groupAmount.setText(String.valueOf(rvGroups.size()));
                break;

            case FETCH_NEW_GROUPS_INFO_FOR_MERGE:
                //发起了合并学习的请求且正在DFG中筛选分组；当需要根据指定的新MS值获取一组新的分组数据再传入时发送来此消息
                int msForFetch = data.getInt(STR_NEW_MS_FOR_FETCH,0);
                ArrayList<RvMergeGroup> newList = new ArrayList<>();//用于传给dfg的新数据源
                if(msForFetch!=0) {
                    for (RVGroup rg :rvGroups) {
                        if(rg.getMemoryStage() == msForFetch){
                            //符合条件的组，转换后加入
                            newList.add(new RvMergeGroup(rg));
                        }//如无符合者，则结果是空组
                    }
                }   //如果要求MS=0时，传递空组即可。

                //数据准备好【？】，通知传入及后续操作
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_MERGE);
                //数据传入，并触发dfg中的后续改变
                ((LearningMerge2DiaFragment)prev).changetListAsMsChanged(newList);

                break;

            case OK_THEN_USE_LM:
                //从询问dfg发过来（发起组的容量过小，询问是否以LM方式开始，选择了是）
                FragmentTransaction transaction_2 = getFragmentManager().beginTransaction();
                Fragment prev_2 = getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_MERGE);
                if (prev_2 != null) {
                    transaction_2.remove(prev_2);
                }
                int position_2 = data.getInt(STR_POSITION,-1);
                if(position_2 == -1){
                    return;
                }
                Bundle data_2 = new Bundle();

                data_2.putInt(STR_TERM_MS,rvGroups.get(position_2).getMemoryStage());
                data_2.putInt(STR_TERM_AMOUNT,8);

                //接下来的操作可能耗时，所以提示一下
                Toast.makeText(this, "正在努力准备数据……", Toast.LENGTH_SHORT).show();

                ArrayList<DBGroup> dbGroups = memoryDbHelper.getAllGroupsByMissionId(rvGroups.get(position_2).getMission_id(),tableItemSuffix);
                ArrayList<RvMergeGroup> rvMergeGroups = new ArrayList<>();
                int positionKeep = 0;
                boolean couldSkip = false;//找到就能跳过后续

                for (DBGroup dbg :dbGroups) {
                    if(dbg.getEffectiveRePickingTimes() == rvGroups.get(position_2).getMemoryStage()){
                        rvMergeGroups.add(new RvMergeGroup(dbg));
                        if(dbg.getId() == rvGroups.get(position_2).getId() && !couldSkip){
                            positionKeep = rvMergeGroups.size()-1;
                            couldSkip = true;
                        }
                    }
                }
                data_2.putInt(STR_FIXED_GROUP_POSITION,positionKeep);
                data_2.putParcelableArrayList(STR_RV_MERGE_GROUP,rvMergeGroups);

                DialogFragment dfg_LM = LearningMerge2DiaFragment.newInstance(data_2);
                dfg_LM.show(transaction_2, FG_STR_READY_TO_LEARN_MERGE);

                break;
            case FORCE_USE_LG:
                //从询问dfg发过来（发起组的容量过小，询问是否以LM方式开始，选择了否）
                FragmentTransaction transaction_3 = getFragmentManager().beginTransaction();
                Fragment prev_3 = getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_GEL);

                if (prev_3 != null) {
                    transaction_3.remove(prev_3);
                }
                int position_3 = data.getInt(STR_POSITION,-1);
                if(position_3 == -1){
                    return;
                }
                DialogFragment dfg_LG = LearningGelDiaFragment.newInstance(rvGroups.get(position_3));
                dfg_LG.show(transaction_3, FG_STR_READY_TO_LEARN_GEL);
                break;
        }
    }


    private void fabPanelCollapse(){
        if(isFabPanelExtracted){
            rltFabPanel.setVisibility(View.GONE);
            isFabPanelExtracted = false;
        }
    }
}





/*旧片段
* groupsStateTimer = new Timer(); 25 28 30
            groupsStateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    self.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });

                }
            },60*1000,60*1000);
* */