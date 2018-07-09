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
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.GroupsOfMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.CreateGroupDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningAddInOrderDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningAddRandomDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMergeDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/*
 * 单个Mission及其所属分组的详情页；
 * 页面上部是Mission详情；
 * 页面下部是所属分组的集合展示（Rv）；默认要按某种顺序（待定）
 * 本页面中的其他任务：①新建分组；②开始学习。
 * 当分组删除时，所属词汇回归为未选中的Item。
 * 点击Rv中的条项可以进入学习/复习页面；会有确认框弹出提示。
 * 学习/复习完成或因超时而未能完成的，都会回到本页面；完成则更新RV列表的显示，
 * 失败则产生一条消息【待实现】。
 * */
public class GroupsAndMissionDetailActivity extends AppCompatActivity implements
        CreateGroupDiaFragment.onCreateGroupDFgConfirmListner,OnGeneralDfgInteraction {
    private static final String TAG = "MissionDetailActivity";

    public static final int EFFECTIVE_PICKING = 316;//先判断好是否仍在有效时期内，然后直接传递给后续页面。（学习完成需要将新Log存入DB，Log需要此信息。）
//    public static final int PICKING_TYPE_INIT = 311;//新版的逻辑中，只需要区分是新学还是复习两组情况。不再需要传递具体状态，因再需要据此生成特殊Logs。
//    public static final int PICKING_TYPE_RE_PICK =312;

    public static final int MESSAGE_PRE_DB_FETCHED = 5011;
    public static final int MESSAGE_RE_FETCH_DONE = 5012;
    public static final int MESSAGE_RV_SCHEDULE_REFRESHING = 5013;

    private static final String ITEM_TABLE_SUFFIX = "item_table_suffix";
    private static final String GROUP_SUB_ITEM_ID_STR = "group_sub_item_ids_str";
    public static final int REQUEST_CODE_LEARNING = 2011;//学习完成后，要回送然后更新Rv数据源和显示。

    private boolean isFabPanelExtracted = false;//FAB面板组默认处于回缩状态。

    private RvMission missionFromIntent;//从前一页面获取。后续页面需要mission的id，suffix字段。
    List<RVGroup> rvGroups = new ArrayList<>();//分开设计的目的是避免适配器内部的转换。让转换在外部完成，适配器直接使用数据才能降低卡顿。

    private ArrayList<FragGroupForMerge>[][] groupsInTwoDimensionArray;//用于后续DFG的数据装载，两个维度分别对应MS、同MS下<4,<8。

    private YoMemoryDbHelper memoryDbHelper;
    private String tableItemSuffix;//由于各任务所属的Item表不同，后面所有涉及Item的操作都需要通过后缀才能构建出完整表名。
    private RecyclerView mRv;
    private GroupsOfMissionRvAdapter adapter = null;//Rv适配器引用
    private int clickPosition;//点击（前往学习页面）发生的位置，需要该数据来更新rv位置

    private FrameLayout maskFrameLayout;
    //另外，页面上部的Mission详情区“任务名称、描述”两个控件不声明为全局变量。在onCreate内以局部变量声明。
    private RelativeLayout rltFabPanel;

    private Activity self;//为了后方Timer配合runOnUiThread.
    private Handler handler = new GroupOfMissionHandler(this);//涉及弱引用，通过其发送消息。
    private Boolean fetched = false;//是否已执行完成过从DB获取分组数据的任务；如完成，则onResume中可以重启UI-Timer
    private Boolean needForScheduleRefreshing = true;//分组列表数据定时更新线程的控制变量。当刷新分组列表时暂停该更新；（？退出到Pause状态时，是否需要手动停止？）
    private Boolean isHandyRefreshing = false;//点击刷新列表的按键后，会重新执行加载数据的线程，为与首次的自动运行相区分，此标志变量会设true。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.self = this;
        setContentView(R.layout.activity_groups_and_mission_detail);

        TextView missionDetailName = (TextView) findViewById(R.id.tv_mission_detail_name_GMDA);
        TextView missionDetailDescription = (TextView) findViewById(R.id.tv_mission_detail_description_GMDA);
        maskFrameLayout = (FrameLayout) findViewById(R.id.maskOverRv_MissionDetail_GMDA);
        rltFabPanel = (RelativeLayout) findViewById(R.id.rlt_fabFlat_GMDA);

        missionFromIntent = getIntent().getParcelableExtra("MISSION");
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
            //如果已执行了DB数据的加载（也即本次onResume是onPause后发生的；不是Activity的首次LC环节）
            //（而Activity的首次LC期间，取决于运行的耗时情况，数据线程可能尚未完成，数据可能尚未准备好。）
            needForScheduleRefreshing = true;
            //【实践验证，只设true，线程不会自动重启。】
            new Thread(new RMAReCalculateRunnable()).start();         // 启动更新线程
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        needForScheduleRefreshing = false;
    }


    final static class GroupOfMissionHandler extends Handler {
        private final WeakReference<GroupsAndMissionDetailActivity> activityWeakReference;

        private GroupOfMissionHandler(GroupsAndMissionDetailActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GroupsAndMissionDetailActivity missionDetailActivity = activityWeakReference.get();
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
            //将各分组原始数据转换为UI所需数据，比较耗时。相关数据直接设置给Activity的成员。

            ArrayList<RVGroup> tempRVGroups = new ArrayList<>();
            //暂时按记忆级别排序
            for (DBGroup dbg : dbGroupsOrigin) {
                RVGroup RVGroup = new RVGroup(dbg);
                tempRVGroups.add(RVGroup);
                //尝试过，但似乎无法在此直接排序。无法令最新项同之前所有项目进行比较。
            }

            rvGroups = ascOrderByMemoryStage(tempRVGroups);

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
            try {
                RVGroup gp = (RVGroup) minRVGroup.clone();//克隆方式复制（以免只复制地址）。
                resultRVGroups.add(gp);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            RVGroups.remove(RVGroups.indexOf(minRVGroup));//源列中，最小的项目已删除；则再次的循环筛选将选出次小项。

        }
        return resultRVGroups;
    }

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
                //尝试过，但似乎无法在此直接排序。无法令最新项同之前所有项目进行比较。
            }

            rvGroups = ascOrderByMemoryStage(tempRVGroups);

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
            int n = 0;
            while (needForScheduleRefreshing) {
                n++;//每分加一【如果改到最后++，这样线程第一轮即可更新一次，适用于onResume；】
                //更新频率1分钟一次即可，因为RMA函数的计算单位就是分钟。

                List<Integer> refreshingNeededPositionsList = new ArrayList<>();
                //并不是所有条目都需要更新，有的条目RMA数值许久不变，因而不需更新
                //旧版的做法其实很精妙，是每秒进行一次检测（remainingTimeAmount），每秒向接收方发送
                // 一次“需更新”的列表；但是对剩余时间较久的分组，只在秒计数器n到达15、30、60、
                // 3600时，才加入“需更新”的名单。（也即，有时更新名单可能是空的；负荷主要在每秒的计算上，
                // 而实际的UI更新方面只更新少量的条目）

                try {
                    Thread.sleep(1000 * 60);//休息1分钟【新版1min更新，负荷已大降】
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //【待】总觉得这里存在问题——学习结束后，分组的信息是更新了的、而此Schedule更新也是更新，
                // 可能存在两个操作对一个资源的污染问题。——并不，进入onPause()后会将标记置否吧。
                for (RVGroup singleRVGroup : rvGroups) {
                    if (singleRVGroup.refreshRMA()) {
                        //新旧值不同，需要更新。（对于不需要更新的不再加以处理）
                        refreshingNeededPositionsList.add(rvGroups.indexOf(singleRVGroup));
                    }
                }

                Message message = new Message();
                message.what = MESSAGE_RV_SCHEDULE_REFRESHING;
                message.obj = refreshingNeededPositionsList;

                handler.sendMessage(message);
            }
        }
    }

    void handyRefresh() {
        //手动触发刷新时（是一个底层的彻底的更新，从DB重新获取数据，然后重新计算）
        //①先停止计时更新（控制变量重设false）；
        //②清空旧Adapter数据
        //③设置 手动刷新Rv的识别变量为真（避免重新加载adp）
        //④开启从DB获取数据的线程获取新数据（不同于预获取的线程，是一个新）
        //完成后，计时更新控制变量重设真，再次启动计时更新线程【？】
        needForScheduleRefreshing = false;
        rvGroups.clear();

        isHandyRefreshing = true;
        new Thread(new ReFetchForGroupsAndMissionRunnable()).start();         // start thread
        //控制变量uiRefreshingNeeded重设为true的操作在消息处理方法中进行
    }


    void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_PRE_DB_FETCHED://此时是从DB获取各分组数据并转换成合适的数据源完成
                fetched = true;//用于onResume中的判断（如果在LC的首次流程中，数据线程可能尚未完成，此即为false，不会开启刷新线程）。
                //取消上方遮罩
                maskFrameLayout.setVisibility(View.GONE);

                //初始化Rv构造器，令UI加载Rv控件……
                adapter = new GroupsOfMissionRvAdapter(rvGroups, this, missionFromIntent.getTableItem_suffix());
                mRv = findViewById(R.id.groups_in_single_mission_rv);
                mRv.setLayoutManager(new LinearLayoutManager(this));
                mRv.setAdapter(adapter);

                //Rv加载后，启动更新计时器【此方式不能实现UI的更新，因为UI所需的数据是提前写好了的；
                // 要更新显示，必须提供新的数据；这一任务符合较大，改由新线程执行】【后来的我：？啥意思】
                new Thread(new RMAReCalculateRunnable()).start();         // start thread
                break;

            case MESSAGE_RE_FETCH_DONE:
                //手动触发的数据刷新，更新适配器并设置自动更新变量为真。
                adapter.notifyDataSetChanged();
                needForScheduleRefreshing = true;
                break;

            case MESSAGE_RV_SCHEDULE_REFRESHING:
                ArrayList<Integer> positionsNeedForUpdate = (ArrayList) (message.obj);

                //所有的批量notify方法都只能用于连续项目，所以只能…
                if (positionsNeedForUpdate == null || positionsNeedForUpdate.size() == 0)
                    return;//判空
                for (int i : positionsNeedForUpdate) {
                    adapter.notifyItemChanged(i);//【旧版似乎是传递的id，但是id和位置不对应啊。】
                }

        }
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
        Fragment prev = getFragmentManager().findFragmentByTag("CREATE_GROUP");

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = CreateGroupDiaFragment.newInstance(missionFromIntent.getTableItem_suffix(), missionFromIntent.getId());
        dfg.show(transaction, "CREATE_GROUP");
    }

    public void findGroup(View view) {
        //查询指定分组（查询的方式和条件还没想好）
        Toast.makeText(self, "施工中，查询分组的方法。", Toast.LENGTH_SHORT).show();
    }

    public void learnAndAddInOrder(View view) {
        //启动DFG，在dfg中点击了确认后，再交互到本Activity下的onLDfgInteraction方法，然后再生成Intent跳转。
        // 启动“边学边建”，按顺序建组
        // 传递“顺序/随机”二选一；其余不传递
        // 由LearningActivity负责拉取36个资源，记录学习位置，最后在完成Activity对已学部分生成新组。
        // 学习中的暂停、非正常终止、正常终止逻辑都由LA负责。
        // （关于组内乱序：新建时还是按顺序学习比较好，不提供此选项）
        Toast.makeText(self, "按学习数量建立分组（顺序）", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("LEARNING_ADD_IN_ORDER");

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningAddInOrderDiaFragment.newInstance();
        dfg.show(transaction, "LEARNING_ADD_IN_ORDER");

    }

    public void learnAndAddRandom(View view) {
        //启动“边学边建”，随机顺序。
        Toast.makeText(self, "施工中，边学边建。", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("LEARNING_ADD_RANDOM");

        if (prev != null) {
            Toast.makeText(self, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = LearningAddRandomDiaFragment.newInstance();
        dfg.show(transaction, "LEARNING_ADD_RANDOM");
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
        Toast.makeText(self, "合并学习，正在准备数据，马上就好。", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("READY_TO_LEARN_MERGE");

        if (prev != null) {
            transaction.remove(prev);
        }

        //本页有全部分组的信息，应在本页准备好数据（容量<4、<8）
        // 后续页面只能直接从DB取数据，而SQL复杂不宜用。
        for (RVGroup rvg : rvGroups) {
            if (rvg.getTotalItemsNum() < 4) {
                //一维上[0][x]是小于4的，x对应其不同MS；各元素本身就是一个ArrayList哦。
                groupsInTwoDimensionArray[0][rvg.getMemoryStage()].add(new FragGroupForMerge(rvg));
                //替代了switch，简洁。
            } else if (rvg.getTotalItemsNum() < 8) {
                groupsInTwoDimensionArray[1][rvg.getMemoryStage()].add(new FragGroupForMerge(rvg));
            }
        }

        //数据已组织好，接下来是传递，以及DFG中的接收显示。

        DialogFragment dfg = LearningMergeDiaFragment.newInstance(groupsInTwoDimensionArray);
        dfg.show(transaction, "READY_TO_LEARN_MERGE");
    }


    /*
     * 各DialogFragment交互方法
     * */
    @Override
    public void onCreateGroupDFgConfirm(long lines) {
        Log.i(TAG, "onCreateGroupDFgConfirm: +1");
        //如果新增操作成功，通知adp变更。
        if (lines != -1) {
            //新增操作只影响一行
            DBGroup dGroup = memoryDbHelper.getGroupByLine(lines, tableItemSuffix);
            RVGroup newRVGroup = new RVGroup(dGroup);

            rvGroups.add(0, newRVGroup);//新增分组放在最前【逻辑便于处理】
            adapter.notifyItemInserted(0);//（仍是0起算，但是加到最后时似乎比较奇怪）
            mRv.scrollToPosition(0);//设置增加后滚动到新增位置。【已查，从0起算】
        } else {
            Toast.makeText(self, "操作未能成功，DB：-1.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {
        Intent intentToLPA = new Intent(this, PrepareForLearningActivity.class);
        intentToLPA.putExtra("TABLE_SUFFIX", tableItemSuffix);
        intentToLPA.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        switch (dfgType) {
            case LEARNING_GENERAL:
                intentToLPA.putExtra("LEARNING_TYPE", LEARNING_GENERAL);
                intentToLPA.putExtra("BUNDLE_FOR_GENERAL", data);
                this.startActivity(intentToLPA);
                break;

            case LEARNING_AND_CREATE_ORDER:
                //需要传递标记
                intentToLPA.putExtra("LEARNING_TYPE", LEARNING_AND_CREATE_ORDER);
                intentToLPA.putExtra("MISSION_ID", missionFromIntent.getId());//在最后完成页生成新组时需要本字段信息。
                this.startActivity(intentToLPA);
                break;

            case LEARNING_AND_CREATE_RANDOM:
                intentToLPA.putExtra("LEARNING_TYPE", LEARNING_AND_CREATE_RANDOM);
                intentToLPA.putExtra("MISSION_ID", missionFromIntent.getId());//在最后完成页生成新组时需要本字段信息。
                this.startActivity(intentToLPA);
                break;

            case LEARNING_AND_MERGE:
                intentToLPA.putExtra("LEARNING_TYPE", LEARNING_AND_MERGE);
                intentToLPA.putExtra("BUNDLE_FOR_MERGE", data);//这里传递的是从DFG（及其选择Rv）传来的，
                // 作为合并学习的来源碎片分组的分组id（构成的bundle，内部的key：IDS_GROUPS_READY_TO_MERGE）
                this.startActivity(intentToLPA);
                break;


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