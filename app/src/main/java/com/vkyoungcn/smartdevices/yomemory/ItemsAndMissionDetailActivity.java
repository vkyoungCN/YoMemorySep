package com.vkyoungcn.smartdevices.yomemory;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.ItemsOfMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/*
 * Mission所属Items的列表页及Mission详情信息；
 * 页面上部是Mission情况简报；
 * 页面下部是所属Items资源的列表展示（Rv）；
 * 页面底部提供对所属Items列表中项目进行增删改查操作的按钮。
 *
 * 列表加载期间展示遮罩层，加载完毕后取消遮罩。
 * 对列表的CURD操作都会更新RV列表的显示。
 * */
public class ItemsAndMissionDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItemsAndMissionDetailActivity";

    public static final int MESSAGE_ITEMS_DB_PRE_FETCHED =5011;

    private static final String ITEM_TABLE_SUFFIX = "item_table_suffix";

    private RvMission missionFromIntent;//从前一页面获取。后续需要mission的id，suffix字段。
    List<SingleItem> itemList = new ArrayList<>();//数据源
    private YoMemoryDbHelper memoryDbHelper;
    private String tableItemSuffix;//由于各任务所属的Item表不同，后面所有涉及Item的操作都需要通过后缀才能构建出完整表名。
    private RecyclerView mRv;
    private ItemsOfMissionRvAdapter adapter = null;//Rv适配器引用
    private int clickPosition;//点击发生的位置，需要该数据来更新rv

    private FrameLayout maskFrameLayout;
    private TextView itemsTotalNumber;
    private TextView learnedPercentage;
    //另有“任务名称、描述”两个控件（位于页面上部Mission详情区）在onCreate内声明为局部变量。

    private Handler handler = new ItemsOfMissionHandler(this);//涉及弱引用，通过其发送消息。
    private Boolean fetched =false;//是否已完成过从DB获取数据的任务；

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_and_mission_detail_activiy);

        TextView missionDetailName = (TextView) findViewById(R.id.tv_mission_detail_name);
        TextView missionDetailDescription = (TextView) findViewById(R.id.tv_mission_detail_description);
        itemsTotalNumber = (TextView) findViewById(R.id.numberOfTotalItemsOfMission);
        learnedPercentage = (TextView) findViewById(R.id.learnedPercentageOfTotalItemsOfMission);

        maskFrameLayout = (FrameLayout)findViewById(R.id.maskOverRv_MissionItemsDetail);
        missionFromIntent = getIntent().getParcelableExtra("MISSION");

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
        new Thread(new PrepareForMissionItemsDetailRunnable()).start();         // start thread
    }


    final static class ItemsOfMissionHandler extends Handler{
        private final WeakReference<ItemsAndMissionDetailActivity> activityWeakReference;

        private ItemsOfMissionHandler(ItemsAndMissionDetailActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ItemsAndMissionDetailActivity itemsAndMissionDetailActivity = activityWeakReference.get();
            if(itemsAndMissionDetailActivity!=null){
                itemsAndMissionDetailActivity.handleMessage(msg);
            }
        }
    }

    /*
     * 获取各资源原始数据，返回给UI。
     */
    public class PrepareForMissionItemsDetailRunnable implements Runnable{
        @Override
        public void run() {
            //获取原始数据
            ArrayList<SingleItem> items = (ArrayList<SingleItem>) memoryDbHelper.getAllItemsOfMission(tableItemSuffix);
            int learnedNumOfItems = memoryDbHelper.getLearnedNumOfItemsOfMission(tableItemSuffix);
            float percentage = (float) learnedNumOfItems/(float)items.size();

            Message message =new Message();
            message.what = MESSAGE_ITEMS_DB_PRE_FETCHED;
            message.arg1 = items.size();
            message.obj = items;

            //百分比限定两位小数
            DecimalFormat decimalFormat = new DecimalFormat("###.#");
            Bundle bundleForFloat = new Bundle();
            bundleForFloat.putString("STR_PERCENTAGE",decimalFormat.format(percentage)+"%");
            message.setData(bundleForFloat);

            handler.sendMessage(message);
        }
    }

    @SuppressLint("StringFormatInvalid")
    void handleMessage(Message message){
        switch (message.what){
            case MESSAGE_ITEMS_DB_PRE_FETCHED://此时是已从DB获取数据
                fetched = true;//用于onResume中的判断。
                //取消Rv区域的遮罩，从消息提取数据。
                maskFrameLayout.setVisibility(View.GONE);
                itemList = (ArrayList<SingleItem>)message.obj;

                //初始化Rv构造器，令UI加载Rv控件……
                adapter = new ItemsOfMissionRvAdapter(itemList, this);
                mRv = findViewById(R.id.items_of_mission_rv);
                mRv.setLayoutManager(new LinearLayoutManager(this));
                mRv.setAdapter(adapter);

                //修改上方详情区的两个字段
                itemsTotalNumber.setText(String.valueOf(message.arg1));
                learnedPercentage.setText((String)message.getData().get("STR_PERCENTAGE"));
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

            itemList.add(0,singleItem);//新增分组放在最前【逻辑便于处理】
            adapter.notifyItemInserted(0);//（仍是0起算，但是加到最后时似乎比较奇怪）
            mRv.scrollToPosition(0);//设置增加后滚动到新增位置。【这个则是从0起算】
        }
    }*/



}