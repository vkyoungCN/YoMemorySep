package com.vkyoungcn.smartdevices.yomemory;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_MERGE;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;

public class PrepareForLearningActivity extends AppCompatActivity {

    private int learningType;//Intent传来
    private String tableNameSuffix;
    private int groupId;//Intent传来，仅边建边学没有此数据。
    private ArrayList<Integer> gIdsForMerge;//Intent传来，仅在合并学习时有此数据。

    private Handler handler = new PrepareLearningDataHandler(this);
    private YoMemoryDbHelper memoryDbHelper;

    private ArrayList<SingleItem> items = new ArrayList<>();//用于装载实际拉取到的数据并向后传递。


    public static final int MESSAGE_LG_DB_DATA_FETCHED = 5071;
    public static final int MESSAGE_LCO_DB_DATA_FETCHED = 5072;
    public static final int MESSAGE_LCR_DB_DATA_FETCHED = 5073;
    public static final int MESSAGE_LM_DB_DATA_FETCHED = 5074;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepare_for_learning);

        memoryDbHelper = YoMemoryDbHelper.getInstance(this);
        learningType = getIntent().getIntExtra("LEARNING_TYPE",0);//所有情形下都会传递本数据
        tableNameSuffix = getIntent().getStringExtra("TABLE_SUFFIX");

        switch (learningType){
            case LEARNING_GENERAL:
                Bundle bundleForGeneral = getIntent().getBundleExtra("BUNDLE_FOR_GENERAL");
                if(bundleForGeneral!=null){
                    groupId = bundleForGeneral.getInt("GROUP_ID_TO_LEARN",0);
                    //通用模式下额外传入的数据只有1个gid。

                    //下面为通用模式准备数据List<Item>。
                    //①开启新线程：拉取数据；完成后发送消息
                    //②接收到消息后，跳转，将List装入Intent传递。（同时还要传递learningType和gid以备结束Activity使用）
                    new Thread(new PrepareDataForGeneralLearningRunnable()).start();         // start thread
                }else {
                    Toast.makeText(this, "groupId未能传递过来", Toast.LENGTH_SHORT).show();
                }
                break;
            case LEARNING_AND_CREATE_ORDER:
                //边学边建模式下不传入额外数据
                //下面为创建模式准备数据List<Item>。
                new Thread(new PrepareDataForLcOrderRunnable()).start();         // start thread

                break;
            case LEARNING_AND_CREATE_RANDOM:
                //边学边建模式下不传入额外数据
                //下面为创建模式准备数据List<Item>。
                new Thread(new PrepareDataForLcRandomRunnable()).start();         // start thread
                break;

            case LEARNING_AND_MERGE:
                //合并模式下额外传入的数据是一个ArrayList。
                Bundle bundleForMerge = getIntent().getBundleExtra("BUNDLE_FOR_MERGE");
                if(bundleForMerge!=null){
                    gIdsForMerge = (ArrayList<Integer>) bundleForMerge.getSerializable("IDS_GROUPS_READY_TO_MERGE");
                    //合并模式下额外传入的数据是一个ArrayList。
                }
                //下面为合并模式准备数据List<Item>。
                new Thread(new PrepareDataForLcMergeRunnable()).start();         // start thread


                break;

        }




    }



    final static class PrepareLearningDataHandler extends Handler{
        private final WeakReference<PrepareForLearningActivity> activityWeakReference;

        private PrepareLearningDataHandler(PrepareForLearningActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PrepareForLearningActivity prepareForLearningActivity = activityWeakReference.get();
            if(prepareForLearningActivity!=null){
                prepareForLearningActivity.handleMessage(msg);
            }
        }
    }

    void handleMessage(Message message) {

        switch (message.what){
            case MESSAGE_LG_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                Intent intentToLearningActivity = new Intent(this,LearningActivity.class);
                intentToLearningActivity.putExtra("LEARNING_TYPE",LEARNING_GENERAL);
                intentToLearningActivity.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity.putExtra("GROUP_ID",groupId);
                intentToLearningActivity.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);
                //Items采用统一的同一个关键字传递即可。

                intentToLearningActivity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity);

                break;

            case MESSAGE_LCO_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                //边学边建模式下没有gid。
                Intent intentToLearningActivity_LCO = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCO.putExtra("LEARNING_TYPE",LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCO.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LCO.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);

                intentToLearningActivity_LCO.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LCO);

                break;

            case MESSAGE_LCR_DB_DATA_FETCHED:
                Intent intentToLearningActivity_LCR = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCR.putExtra("LEARNING_TYPE",LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCR.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LCR.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);

                intentToLearningActivity_LCR.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LCR);

                break;
            case MESSAGE_LM_DB_DATA_FETCHED:
                Intent intentToLearningActivity_LM = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LM.putExtra("LEARNING_TYPE",LEARNING_AND_MERGE);
                intentToLearningActivity_LM.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LM.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);
                intentToLearningActivity_LM.putIntegerArrayListExtra("GIDS_FOR_MERGE",gIdsForMerge);

                intentToLearningActivity_LM.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LM);

                break;

        }

    }


    public class PrepareDataForGeneralLearningRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForGeneralLearningRunnable";

        @Override
        public void run() {
            //从DB准备数据
                items = memoryDbHelper.getItemsByGroupId(groupId, tableNameSuffix);
            //【取消在Items列表数据的最后附加一条“伪数据”，用于伪完成页显示的设置，
            // 目前所有页面功能定位可以一样，不需要尾页来承担特殊功能】

            Message message = new Message();
            message.what = MESSAGE_LG_DB_DATA_FETCHED;

            handler.sendMessage(message);
        }
    }

    /*
    * 顺序方式从DB获取数据（暂定36个）
    * */
    public class PrepareDataForLcOrderRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForGeneralLearningRunnable";

        @Override
        public void run() {
            //从DB准备数据
            items = memoryDbHelper.getCertainAmountItemsOrderly(36,tableNameSuffix);
            //在Items列表数据的最后附加一条“伪数据”，用于伪完成页显示。【仍然附加！】
            SingleItem endingItem = new SingleItem(0,"完成","","",true,0,true,(short) 0,(short) 0);
            items.add(endingItem);

            Message message = new Message();
            message.what = MESSAGE_LCO_DB_DATA_FETCHED;

            handler.sendMessage(message);
        }
    }

    /*
     * 随机方式从DB获取数据（暂定36个）
     * */
    public class PrepareDataForLcRandomRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForLcRandomRunnable";

        @Override
        public void run() {
            //从DB准备数据
            items = memoryDbHelper.getCertainAmountItemsRandomly(36,tableNameSuffix);

            Message message = new Message();
            message.what = MESSAGE_LCR_DB_DATA_FETCHED;

            handler.sendMessage(message);
        }
    }



    public class PrepareDataForLcMergeRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForLcMergeRunnable";

        @Override
        public void run() {
            //从DB准备数据
            items = memoryDbHelper.getItemsWithInGidList(gIdsForMerge,tableNameSuffix);

            //items列表的顺序（按所归属的gid）要和gid列表的顺序一致，这样才能在最终的结束页进行正确处理。
            Collections.sort(gIdsForMerge,new ascOrderById());

            Message message = new Message();
            message.what = MESSAGE_LM_DB_DATA_FETCHED;

            handler.sendMessage(message);
        }
    }

    private class ascOrderById implements Comparator{
        @Override
        public int compare(Object o1, Object o2) {
            return (Integer)o1 - (Integer)o2;
        }
    }

}
