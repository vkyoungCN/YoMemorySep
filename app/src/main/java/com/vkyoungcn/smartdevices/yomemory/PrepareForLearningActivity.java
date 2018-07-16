package com.vkyoungcn.smartdevices.yomemory;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_MS;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_RMA;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_TT;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_MERGE;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL_NO_GID;

/*
* 作者1：杨胜@中国海洋大学
* 作者2：杨镇时@中国海洋大学
* author：Victor Young @Ocean University of China
* email: yangsheng@ouc.edu.cn
* */
public class PrepareForLearningActivity extends AppCompatActivity {
    private static final String TAG = "PrepareForLearningActiv";
    private int learningType;//Intent传来
    private String tableNameSuffix;
    private int groupId;//Intent传来，边建边学没有此数据。
    private ArrayList<Integer> gIdsForMerge;//Intent传来，仅在合并学习时有此数据。
    private int missionId;//仅在LC两种模式下使用。传递到最后页，生成新组时使用。//LGN模式下也使用，需要用它获取全部分组。
    private int prioritySetting = DEFAULT_MANNER_TT;//仅LGN模式使用

    private Handler handler = new PrepareLearningDataHandler(this);
    private YoMemoryDbHelper memoryDbHelper;

    private ArrayList<SingleItem> items = new ArrayList<>();//用于装载实际拉取到的数据并向后传递。


    public static final int MESSAGE_LG_DB_DATA_FETCHED = 5071;
    public static final int MESSAGE_LGN_DB_DATA_FETCHED = 5077;
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
            case LEARNING_GENERAL_NO_GID:
                Log.i(TAG, "onCreate: LGN");
                if(getIntent()!=null){
                    prioritySetting = getIntent().getIntExtra("PRIORITY_SETTING",DEFAULT_MANNER_TT);
                    missionId = getIntent().getIntExtra("MISSION_ID",0);

                    //下面为通用模式准备数据List<Item>。
                    //①开启新线程：拉取数据；完成后发送消息
                    //②接收到消息后，跳转，将List装入Intent传递。（同时还要传递learningType和gid以备结束Activity使用）
                    new Thread(new PrepareDataForNoIdGeneralLearningRunnable()).start();         // start thread

                }else {
                    prioritySetting = DEFAULT_MANNER_TT;
                    Toast.makeText(this, "优先条件和任务id未能传递过来，无法处理", Toast.LENGTH_SHORT).show();
                }


                break;
            case LEARNING_AND_CREATE_ORDER:
                //边学边建模式下传入的额外数据
                missionId = getIntent().getIntExtra("MISSION_ID",0);
                //下面为创建模式准备数据List<Item>。
                new Thread(new PrepareDataForLcOrderRunnable()).start();         // start thread

                break;
            case LEARNING_AND_CREATE_RANDOM:
                //边学边建模式下传入的额外数据
                missionId = getIntent().getIntExtra("MISSION_ID",0);
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
                new Thread(new PrepareDataForLMergeRunnable()).start();         // start thread


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
            case MESSAGE_LGN_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                RVGroup rvGroup = (RVGroup) message.obj;

                Intent intentToLearningActivity_LGN = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LGN.putExtra("LEARNING_TYPE",LEARNING_GENERAL);//可以使用LG。（N无所谓了）
                intentToLearningActivity_LGN.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LGN.putExtra("GROUP_ID",rvGroup.getId());
                intentToLearningActivity_LGN.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);
                //Items采用统一的同一个关键字传递即可。
//                Log.i(TAG, "handleMessage: LGN");

                intentToLearningActivity_LGN.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LGN);

                break;

            case MESSAGE_LCO_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                //边学边建模式下没有gid。
                Intent intentToLearningActivity_LCO = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCO.putExtra("LEARNING_TYPE",LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCO.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LCO.putExtra("MISSION_ID",missionId);
                intentToLearningActivity_LCO.putParcelableArrayListExtra("ITEMS_FOR_LEARNING",items);
//                Log.i(TAG, "handleMessage: items done,size="+items.size());
                intentToLearningActivity_LCO.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LCO);

                break;

            case MESSAGE_LCR_DB_DATA_FETCHED:
                Intent intentToLearningActivity_LCR = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCR.putExtra("LEARNING_TYPE",LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCR.putExtra("TABLE_NAME_SUFFIX",tableNameSuffix);
                intentToLearningActivity_LCR.putExtra("MISSION_ID",missionId);
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


    public class PrepareDataForNoIdGeneralLearningRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForNoIdGeneralLearningRunnable";

        @Override
        public void run() {
            //需要一个新方法，能够按指定条件对所有分组排序，只需返回其第前一（或者多缓存几项）项结果
            Log.i(TAG, "run: LGN");

            //从DB准备数据
            ArrayList<DBGroup> groups = memoryDbHelper.getAllGroupsByMissionId(missionId, tableNameSuffix);
            //【取消在Items列表数据的最后附加一条“伪数据”，用于伪完成页显示的设置，
            // 目前所有页面功能定位可以一样，不需要尾页来承担特殊功能】
            ArrayList<RVGroup> rvGroups = new ArrayList<>();
            for (DBGroup d : groups) {
                rvGroups.add(new RVGroup(d));
            }
            RVGroup targetGroup = getMinRvGroupUseTerm(prioritySetting,rvGroups);

            items = memoryDbHelper.getItemsByGroupId(targetGroup.getId(),tableNameSuffix);

            Message message = new Message();
            message.what = MESSAGE_LGN_DB_DATA_FETCHED;
            message.obj = targetGroup;

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



    public class PrepareDataForLMergeRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForLMergeRunnable";

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

    /*
    * 基于Group列表页的排序方法删减而来，只挑最小项是容易很多的。
    * */
    private RVGroup getMinRvGroupUseTerm(int term,ArrayList<RVGroup> rvGroups){
        RVGroup minRVGroup = rvGroups.get(0);//指针项，先指向第一项

        switch (term) {
            case DEFAULT_MANNER_MS:
                int msPointerPosition = 0;//记录指针指向的索引位置。

                if(minRVGroup.getMemoryStage() == 0){
                    //表明首项是新建组，不应做最小指针
                    //循环查找第一个不是新建组的分组
                    for (int i = 1; i < rvGroups.size(); i++) {
                        if(rvGroups.get(i).getMemoryStage() == 0){
                            minRVGroup = rvGroups.get(i);//首个不为新建组的分组作为指针。
                            msPointerPosition = i;
                            break;//跳出for
                        }
                    }
                    if(msPointerPosition!=0){
                        //最小指针已经不指向第一项，说明存在一个不是新建组的分组
                        //从该分组之后开始查找目标值比该值小且不是0的
                        for (int i = msPointerPosition+1; i < rvGroups.size(); i++) {
                            if (rvGroups.get(i).getMemoryStage()!=0 &&
                                    rvGroups.get(i).getMemoryStage()< minRVGroup.getMemoryStage()) {
                                minRVGroup = rvGroups.get(i);//指针指向较小者
                            }
                        }
                        //如果该分组已经是列表最后一位则上述for不会执行，直接使用现在指向的min返回即可。
                        //以及，如果后续循环中没有符合要求的，则同样直接使用现在指向的min返回。

                        //如果还是第一项的话，代表即所有分组都是新建组，则可直接返回第一项，不做筛选改动。

                    }
                }else {
                    for (int i = 1; i < rvGroups.size(); i++) {
                        //从第二项开始，和指针项比较，如果小于指针项，则令指针指向当前项。（所以循环一次后，是将指针指向了当前列中的最小项）
                        //因为都是引用类型，无法换值，只能移动指针。
                        if (rvGroups.get(i).getMemoryStage() < minRVGroup.getMemoryStage()) {
                            minRVGroup = rvGroups.get(i);//指针指向较小者
                        }
                    }
                }
            break;
            case DEFAULT_MANNER_RMA:
                int rmaPointerPosition = 0;//记录指针指向的索引位置。

                if(minRVGroup.getRM_Amount() == 0){
                    //表明首项是新建组，不应做最小指针
                    //循环查找第一个不是新建组的分组
                    for (int i = 1; i < rvGroups.size(); i++) {
                        if(rvGroups.get(i).getRM_Amount() == 0){
                            minRVGroup = rvGroups.get(i);//首个不为新建组的分组作为指针。
                            rmaPointerPosition = i;
                            break;//跳出for
                        }
                    }
                    if(rmaPointerPosition!=0){
                        //最小指针已经不指向第一项，说明存在一个不是新建组的分组
                        //从该分组之后开始查找目标值比该值小且不是0的
                        for (int i = rmaPointerPosition+1; i < rvGroups.size(); i++) {
                            if (rvGroups.get(i).getRM_Amount()!=0 &&
                                    rvGroups.get(i).getRM_Amount()< minRVGroup.getRM_Amount()) {
                                minRVGroup = rvGroups.get(i);//指针指向较小者
                            }
                        }
                        //如果该分组已经是列表最后一位则上述for不会执行，直接使用现在指向的min返回即可。
                        //以及，如果后续循环中没有符合要求的，则同样直接使用现在指向的min返回。

                        //如果还是第一项的话，代表即所有分组都是新建组，则可直接返回第一项，不做筛选改动。

                    }
                }else {
                    for (int i = 1; i < rvGroups.size(); i++) {
                        //从第二项开始，和指针项比较，如果小于指针项，则令指针指向当前项。（所以循环一次后，是将指针指向了当前列中的最小项）
                        //因为都是引用类型，无法换值，只能移动指针。
                        if (rvGroups.get(i).getRM_Amount() < minRVGroup.getRM_Amount()) {
                            minRVGroup = rvGroups.get(i);//指针指向较小者
                        }
                    }
                }
                break;

                case DEFAULT_MANNER_TT:
                    int pointerPosition = 0;//记录指针指向的索引位置。

                    if(minRVGroup.getMinutesTillFarThreshold() == -1){
                        //表明首项是新建组，不应做最小指针
                        //循环查找第一个不是新建组的分组
                        for (int i = 1; i < rvGroups.size(); i++) {
                            if(rvGroups.get(i).getMinutesTillFarThreshold()!=-1){
                                minRVGroup = rvGroups.get(i);//首个不为新建组的分组作为指针。
                                pointerPosition = i;
                                break;//跳出for
                            }
                        }
                        if(pointerPosition!=0){
                            //说明最小指针已经不指向第一项，说明存在一个不是新建组的分组
                            //从该分组之后开始查找tt值比该值小且不是-1的
                            for (int i = pointerPosition+1; i < rvGroups.size(); i++) {
                                if (rvGroups.get(i).getMinutesTillFarThreshold()!=-1 &&
                                        rvGroups.get(i).getMinutesTillFarThreshold()< minRVGroup.getMinutesTillFarThreshold()) {
                                    minRVGroup = rvGroups.get(i);//指针指向较小者
                                }
                            }
                            //如果该分组已经是列表最后一位则上述for不会执行，直接使用现在指向的min返回即可。
                            //以及，如果后续循环中没有符合要求的，则同样直接使用现在指向的min返回。

                            //如果还是第一项的话，代表即所有分组都是新建组，则可直接返回第一项，不做筛选改动。

                        }
                    }else {
                        //首项不是新建组，可以做最小指针
                        //循环查找之后所有分组，其中不是新建组且条件值比min小的选出（指向）
                        for (int i = pointerPosition+1; i < rvGroups.size(); i++) {
                            if (rvGroups.get(i).getMinutesTillFarThreshold()!=-1 &&
                                    rvGroups.get(i).getMinutesTillFarThreshold()< minRVGroup.getMinutesTillFarThreshold()) {
                                minRVGroup = rvGroups.get(i);//指针指向较小者
                            }
                            //如果列表只有1个分组、如果后续没有符合条件的，都直接返回首项即可。
                        }
                    }

                break;
        }

        return minRVGroup;
    }

}
