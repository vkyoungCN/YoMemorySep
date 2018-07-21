package com.vkyoungcn.smartdevices.yomemory;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.ItemsOfMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class ItemsOfMissionActivity extends AppCompatActivity
        implements View.OnClickListener, Constants,CompoundButton.OnCheckedChangeListener {
// Mission所属Items的列表页；
// 页面上部是Mission情况简报；
// 页面下部是所属Items资源的列表展示（Rv）；
// 页面底部提供对所属Items列表中项目进行增删改查操作的按钮。
// 对列表的CURD操作都会更新RV列表的显示。
// 列表加载期间展示遮罩层，加载完毕后取消遮罩。

//* 新增按词的优先级筛选单词进行额外复习
//* 于是要改造数据源的机制，持有一个整体的主数据源items，另外持有一个selectingItems分支数据源，
//* 根据上方筛选条件区的设定，sI加载不同的具体集合予以显示。退出该筛选功能时复原为主数据集。



    private static final String TAG = "ItemsOfMissionActivity";

    public static final int MESSAGE_ITEMS_DB_PRE_FETCHED =5011;
    public static final int REQUEST_CODE_EXTRA_LEARNING = 6011;

    /* 从Intent获取的数据*/
    private RvMission missionFromIntent;//从前一页面获取。后续需要mission的id，suffix字段。
    private String tableItemSuffix;//由于各任务所属的Item表不同，后面所有涉及Item的操作都需要通过后缀才能构建出完整表名。

    /*数据库*/
    private YoMemoryDbHelper memoryDbHelper;
    ArrayList<SingleItem> items = new ArrayList<>();//数据源（整体、主数据集）
    ArrayList<SingleItem> rvShowingItems = new ArrayList<>();//分支数据源（用于进入筛选功能时显示用）

    /*线程*/
    private Handler handler = new ItemsOfMissionHandler(this);//涉及弱引用，通过其发送消息。
    private Boolean fetched =false;//是否已完成过从DB获取数据的任务；

    /*控件*/
    private FrameLayout maskFrameLayout;
    private RelativeLayout rltFabPanel;
    private LinearLayout lltSelectingPanel;

    private TextView itemsTotalNumber;
    private TextView learnedPercentage;
    private TextView tvSelectingResult;
    private ImageView imvSelectBtnOk;
    private ImageView imvSelectBtnCancel;
    private FloatingActionButton fab_More;
    private ImageView imvBtn_fabExtraLearn;
    private RecyclerView mRv;
    private ItemsOfMissionRvAdapter adapter;//Rv适配器引用
    //另有“任务名称、描述”两个控件（位于页面上部Mission详情区）在onCreate内声明为局部变量。


    private CheckBox ckb_3;
    private CheckBox ckb_4;
    private CheckBox ckb_5;
    private CheckBox ckb_6;
    private CheckBox ckb_7;
    private CheckBox ckb_8;
    private CheckBox ckb_9;


    /* 业务逻辑 */
    private boolean isFabPanelExtracted = false;//FAB功能面板是否展开
    private boolean isSelectingPanelExtracted = false;//筛选功能面板是否展开



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_of_mission);

        TextView missionDetailName = findViewById(R.id.tv_mission_detail_name);
        TextView missionDetailDescription = findViewById(R.id.tv_mission_detail_description);
        itemsTotalNumber = findViewById(R.id.numberOfTotalItemsOfMission);
        learnedPercentage = findViewById(R.id.learnedPercentageOfTotalItemsOfMission);
        tvSelectingResult = findViewById(R.id.tv_selectingInfo_IMA);
        imvBtn_fabExtraLearn = findViewById(R.id.imv_extraRePick_IMA);
        imvBtn_fabExtraLearn.setOnClickListener(this);

        imvSelectBtnOk = findViewById(R.id.imvBtn_selectingOk_IMA);
        imvSelectBtnOk.setOnClickListener(this);
        imvSelectBtnCancel = findViewById(R.id.imvBtn_selectingCancel_IMA);
        imvSelectBtnCancel.setOnClickListener(this);
        lltSelectingPanel = findViewById(R.id.llt_selectPanel_IMA);

        ckb_3 = findViewById(R.id.ckb_3_AIM);
        ckb_4 = findViewById(R.id.ckb_4_AIM);
        ckb_5 = findViewById(R.id.ckb_5_AIM);
        ckb_6 = findViewById(R.id.ckb_6_AIM);
        ckb_7 = findViewById(R.id.ckb_7_AIM);
        ckb_8 = findViewById(R.id.ckb_8_AIM);
        ckb_9 = findViewById(R.id.ckb_9_AIM);

        ckb_3.setOnCheckedChangeListener(this);
        ckb_4.setOnCheckedChangeListener(this);
        ckb_5.setOnCheckedChangeListener(this);
        ckb_6.setOnCheckedChangeListener(this);
        ckb_7.setOnCheckedChangeListener(this);
        ckb_8.setOnCheckedChangeListener(this);
        ckb_9.setOnCheckedChangeListener(this);

        fab_More = findViewById(R.id.fab_more_IMA);
        fab_More.setOnClickListener(this);

        maskFrameLayout = findViewById(R.id.maskOverRv_MissionItemsDetail);

        rltFabPanel = findViewById(R.id.rlt_fabFlat_IMA);
        rltFabPanel.setOnClickListener(this);

        missionFromIntent = getIntent().getParcelableExtra(STR_MISSION);
        if (missionFromIntent == null) {
            Toast.makeText(this, "任务信息传递失败", Toast.LENGTH_SHORT).show();
            return;
        } else {
            //根据Mission数据填充Mission信息两项
            missionDetailName.setText(missionFromIntent.getName());
            missionDetailDescription.setText(missionFromIntent.getSimpleDescription());
            tableItemSuffix = missionFromIntent.getTableItem_suffix();
            //资源总数、已学习百分比、资源列表数据均在新线程获取。
        }

        memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());
        rvShowingItems = new ArrayList<>();

        new Thread(new PrepareForMissionItemsDetailRunnable()).start();         // start thread
    }


    final static class ItemsOfMissionHandler extends Handler{
        private final WeakReference<ItemsOfMissionActivity> activityWeakReference;

        private ItemsOfMissionHandler(ItemsOfMissionActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ItemsOfMissionActivity itemsOfMissionActivity = activityWeakReference.get();
            if(itemsOfMissionActivity !=null){
                itemsOfMissionActivity.handleMessage(msg);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){


            case R.id.fab_more_IMA:
                if (!isFabPanelExtracted) {//未展开，要做展开操作

                    //标志变量取反
                    isFabPanelExtracted = true;
                    //展开（取消隐藏）
                    rltFabPanel.setVisibility(View.VISIBLE);

                } else {
                    isFabPanelExtracted = false;
                    rltFabPanel.setVisibility(View.GONE);
                }

                break;
            case R.id.rlt_fabFlat_IMA:

                //在板子展开后，点击板子需要能缩回（隐藏）
                if(isFabPanelExtracted){
                    rltFabPanel.setVisibility(View.GONE);
                    isFabPanelExtracted = false;
                }//只负责缩回就好了。
                break;

            case R.id.imv_extraRePick_IMA:
                //弹出筛选面板，准备对高优先级的Items进行额外复习（不分组，不产生log）
                //能选择目标优先级（该优先级及以上词汇将被显示）
                // 由于2是初始优先级，代表普通状态，因而“高优先级”最小必须是3。
                // 优先级的规划（似乎？）是0~9，因而可选范围3~9、

                //fab面板回收
                if(isFabPanelExtracted){
                    rltFabPanel.setVisibility(View.GONE);
                    isFabPanelExtracted = false;
                }

                //筛选面板展开
                lltSelectingPanel.setVisibility(View.VISIBLE);
                isSelectingPanelExtracted = true;
                //默认选中3~9所有优先级词汇
                Toast.makeText(this, "努力处理数据……", Toast.LENGTH_SHORT).show();

                if(rvShowingItems == null || rvShowingItems.isEmpty()){
                    return;
                }
                //非空时，对影子数据集进行修剪
                //要使用Iterator自带的遍历和删除方式，才能正常删除（或者倒序遍历也可）
                for(Iterator<SingleItem> itemIterator = rvShowingItems.iterator();itemIterator.hasNext();){
                    if(itemIterator.next().getPriority()<3){
                        itemIterator.remove();//注意要使用Iterator的删除方法。
                    };
                }
                    /*for (int i = 0; i < rvShowingItems.size(); i++) {
                        if (rvShowingItems.get(i).getPriority() < 2) {
                            rvShowingItems.remove(i);
                        }
                    }错误，每删一个容量变小索引实际后移，等于隔行删除*/

                /*for (SingleItem si :rvShowingItems) {
                    if (si.getPriority() <=2) {
                        rvShowingItems.remove(si);
                    }
                }错误*/
//                Log.i(TAG, "onClick: rvShowing.size="+rvShowingItems.size());
                if(rvShowingItems.size()==0){
                    Toast.makeText(this, "没有符合指定优先级条件的单词.", Toast.LENGTH_SHORT).show();
                }
                tvSelectingResult.setText(String.format(getString(R.string.hs_selected_item_amount),rvShowingItems.size()));
                adapter.notifyDataSetChanged();//数据集改变。

                break;

            case R.id.llt_selectPanel_IMA:
                //要消耗掉事件
                return;

            case R.id.imvBtn_selectingOk_IMA:
                //直接跳转LPA，传递showingItems集合
                if(rvShowingItems.size() == 0){
                    //没有需要强化复习的单词
                    Toast.makeText(this, "没有需要额外强化复习的单词。", Toast.LENGTH_SHORT).show();
                }else {
                    //数据选定，跳转
                    Intent intentToLPA = new Intent(this, PrepareForLearningActivity.class);
                    intentToLPA.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intentToLPA.putExtra(STR_LEARNING_TYPE, OnGeneralDfgInteraction.LEARNING_EXTRA_NO_RECORDS);
                    intentToLPA.putExtra(STR_TABLE_NAME_SUFFIX,tableItemSuffix);
                    intentToLPA.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING, rvShowingItems);
                    this.startActivityForResult(intentToLPA,REQUEST_CODE_EXTRA_LEARNING);
                    //返回时需要收起面板，替换完整数据源
                }

                break;

            case R.id.imvBtn_selectingCancel_IMA:
                //板子收回，数据源替换会主数据
                lltSelectingPanel.setVisibility(View.GONE);
                isSelectingPanelExtracted = false;

                rvShowingItems.clear();
                rvShowingItems.addAll(items);
                adapter.notifyDataSetChanged();

                break;

        }


    }
    /*
     * 获取各资源原始数据，返回给UI。
     */
    public class PrepareForMissionItemsDetailRunnable implements Runnable{
        @Override
        public void run() {
            //获取原始数据
            items = (ArrayList<SingleItem>) memoryDbHelper.getAllItemsOfMission(tableItemSuffix);
            int learnedNumOfItems = memoryDbHelper.getLearnedNumOfItemsOfMission(tableItemSuffix);
            float percentage = (float) learnedNumOfItems/(float)items.size();

            //因为后期要根据筛选来变更数据显示，所以使用一个指针（影子）数据集
            //注意影子集不能直接“指向”主集，那样对影子作的修改都会直接作用到主集上
            rvShowingItems.addAll(items);//这样影子集是持有了另一套对全部对象的引用，修改将是安全的

            Message message =new Message();
            message.what = MESSAGE_ITEMS_DB_PRE_FETCHED;

            //百分比限定两位小数
            DecimalFormat decimalFormat = new DecimalFormat("###.#");
            Bundle bundleForFloat = new Bundle();
            bundleForFloat.putString("STR_PERCENTAGE",decimalFormat.format(percentage)+"%");
            message.setData(bundleForFloat);

            handler.sendMessage(message);
        }
    }



    void handleMessage(Message message){
        switch (message.what){
            case MESSAGE_ITEMS_DB_PRE_FETCHED://此时是已从DB获取数据
                fetched = true;//用于onResume中的判断。
                //取消Rv区域的遮罩，从消息提取数据。
                maskFrameLayout.setVisibility(View.GONE);

                //初始化Rv构造器，令UI加载Rv控件……
                adapter = new ItemsOfMissionRvAdapter(rvShowingItems, this);
                mRv = findViewById(R.id.items_of_mission_rv);
                mRv.setLayoutManager(new LinearLayoutManager(this));
                mRv.setAdapter(adapter);

                //修改上方详情区的两个字段
                itemsTotalNumber.setText(String.valueOf(items.size()));
                learnedPercentage.setText((String)message.getData().get(STR_STR_PERCENTAGE));
        }



    }

    /*
    * 新增分组操作的后续函数
    *
    @Override
    public void onFragmentInteraction(long lines) {
        Log.i(TAG, "onFragmentInteraction: +1");
        //如果新增操作成功，通知adp变更。
        if (lines != -1) {
            //新增操作只影响一行
            SingleItem singleItem = memoryDbHelper.getSingleItemById((int) lines);

            items.add(0,singleItem);//新增分组放在最前【逻辑便于处理】
            adapter.notifyItemInserted(0);//（仍是0起算，但是加到最后时似乎比较奇怪）
            mRv.scrollToPosition(0);//设置增加后滚动到新增位置。【这个则是从0起算】
        }
    }*/


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int leveledAddingCount = 0;
        switch (buttonView.getId()){
            case R.id.ckb_3_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(3);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(3);
                    if(leveledAddingCount!=0) {
                        //如果有更新，才会通知UI，否则不必。
                        adapter.notifyDataSetChanged();
                    }
                }

                break;
            case R.id.ckb_4_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(4);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(4);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
            case R.id.ckb_5_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(5);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(5);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
            case R.id.ckb_6_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(6);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(6);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
            case R.id.ckb_7_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(7);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(7);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
            case R.id.ckb_8_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(8);
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(8);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
            case R.id.ckb_9_AIM:
                if(!isChecked){
                    //从影子数据集中移除本优先级数据
                    removeLeveledItemsFromShowingList(9);
                    //如果移除后数据集空了，给出“无数据”提示
                    toastIfShowingListNowEmpty();
                    //通知RV-UI
                    adapter.notifyDataSetChanged();
                }else {
                    //从主数据集中查找本优先级数据并加入影子数据集
                    leveledAddingCount = toastAndAddLeveledItemsToShowingList(9);
                    if(leveledAddingCount!=0) {
                        adapter.notifyDataSetChanged();
                    }
                }
                break;

        }
//        Log.i(TAG, "onCheckedChanged: size = "+rvShowingItems.size());
        tvSelectingResult.setText(String.format(getString(R.string.hs_selected_item_amount),rvShowingItems.size()));
    }

    private void removeLeveledItemsFromShowingList(int priority){
        if(priority>2&&priority<10) {
            //从影子数据集中移除本优先级数据
            for (SingleItem si : rvShowingItems) {
                if (si.getPriority() == priority) {
                    rvShowingItems.remove(si);
                }
            }
            if (rvShowingItems.size() == 0) {
                Toast.makeText(this, "没有符合指定优先级条件的单词", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toastIfShowingListNowEmpty(){
        if(rvShowingItems.size()==0){
            Toast.makeText(this, "没有符合指定优先级条件的单词", Toast.LENGTH_SHORT).show();
        }
    }

    //从主数据集查找指定优先级的词汇，并加入到rvShowingItems显示用数据集。返回新增数量。
    private int toastAndAddLeveledItemsToShowingList(int priority){
        Toast.makeText(this, "努力加载数据……", Toast.LENGTH_SHORT).show();
        int count = 0;
        for (SingleItem si :items) {
            if (si.getPriority() == priority) {
                rvShowingItems.add(si);
                count++;
            }
        }
        if (count == 0){
            //没有新增数据时提示
            Toast.makeText(this, "没有找到优先级="+priority+"的数据。", Toast.LENGTH_SHORT).show();
        }
        return count;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //板子收回，数据源替换会主数据
        if(isSelectingPanelExtracted) {
            lltSelectingPanel.setVisibility(View.GONE);
            isSelectingPanelExtracted = false;
        }

        rvShowingItems.clear();
        rvShowingItems.addAll(items);
        adapter.notifyDataSetChanged();

    }
}