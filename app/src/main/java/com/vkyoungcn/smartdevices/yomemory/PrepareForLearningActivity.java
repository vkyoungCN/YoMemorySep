package com.vkyoungcn.smartdevices.yomemory;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_EXTRA_NO_RECORDS;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL_INNER_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL_NO_GID;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class PrepareForLearningActivity extends AppCompatActivity implements Constants {
//    本页面在LearningActivity之前，为LA准备数据。
//    接收各页面发起的学习请求，根据不同的学习种类（LearningType）来构造特定的数据集，
//    然后传递给学习页。
//    本页面结束后不能返回，是一过性页面。
    private static final String TAG = "PrepareForLearningActiv";

    /* Intent数据 */
    private int learningType;//Intent传来
    private String tableNameSuffix;
    private int groupId;//Intent传来，边建边学没有此数据。
    private ArrayList<Integer> gIdsForMerge;//Intent传来，仅在合并学习时有此数据。
    private int missionId;//仅在LC两种模式下使用。传递到最后页，生成新组时使用。//LGN模式下也使用，需要用它获取全部分组。
    private int prioritySetting = DEFAULT_MANNER_TT;//仅LGN模式使用


    /* DB数据*/
    private YoMemoryDbHelper memoryDbHelper;
    private ArrayList<SingleItem> items = new ArrayList<>();//用于装载实际拉取到的数据并向后传递。

    /*线程*/
    private Handler handler = new PrepareLearningDataHandler(this);
    /* 预定义的线程消息常量*/
    public static final int MESSAGE_LG_DB_DATA_FETCHED = 5071;
    public static final int MESSAGE_LCO_DB_DATA_FETCHED = 5072;
    public static final int MESSAGE_LCR_DB_DATA_FETCHED = 5073;
    public static final int MESSAGE_LM_DB_DATA_FETCHED = 5074;

    public static final int MESSAGE_LGN_DB_DATA_FETCHED = 5077;
    public static final int MESSAGE_LGR_DB_DATA_FETCHED = 5078;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepare_for_learning);

        memoryDbHelper = YoMemoryDbHelper.getInstance(this);
        learningType = getIntent().getIntExtra(STR_LEARNING_TYPE,0);//所有情形下都会传递本数据
        tableNameSuffix = getIntent().getStringExtra(STR_TABLE_SUFFIX);

        switch (learningType){
            case LEARNING_GENERAL:
                Bundle bundleForGeneral = getIntent().getBundleExtra(STR_BUNDLE_FOR_GENERAL);
                if(bundleForGeneral!=null){
                    groupId = bundleForGeneral.getInt(STR_GROUP_ID_TO_LEARN,0);
                    //通用模式下额外传入的数据只有1个gid。

                    //下面为通用模式准备数据List<Item>。
                    //①开启新线程：拉取数据；完成后发送消息
                    //②接收到消息后，跳转，将List装入Intent传递。（同时还要传递learningType和gid以备结束Activity使用）
                    new Thread(new PrepareDataForGeneralLearningRunnable()).start();         // start thread
                }else {
                    Toast.makeText(this, "groupId未能传递过来", Toast.LENGTH_SHORT).show();
                }
                break;
            case LEARNING_GENERAL_INNER_RANDOM:
                Bundle bundleForGeneral_innerRandom = getIntent().getBundleExtra(STR_BUNDLE_FOR_GENERAL);
                if(bundleForGeneral_innerRandom!=null){
                    groupId = bundleForGeneral_innerRandom.getInt(STR_GROUP_ID_TO_LEARN,0);
                    //通用模式下额外传入的数据只有1个gid。

                    //下面为通用模式(组内乱序)准备数据List<Item>。
                    //①开启新线程：拉取数据；【然后乱序之】。完成后发送消息
                    //②接收到消息后，跳转，将List装入Intent传递。（同时还要传递learningType和gid以备结束Activity使用）
                    new Thread(new PrepareDataForInnerRandomGeneralLearningRunnable()).start();         // start thread
                }else {
                    Toast.makeText(this, "groupId未能传递过来", Toast.LENGTH_SHORT).show();
                }
                break;


            case LEARNING_GENERAL_NO_GID:
//                Log.i(TAG, "onCreate: LGN");
                if(getIntent()!=null){
                    prioritySetting = getIntent().getIntExtra(STR_PRIORITY_SETTING,DEFAULT_MANNER_TT);
                    missionId = getIntent().getIntExtra(STR_MISSION_ID,0);

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
                missionId = getIntent().getIntExtra(STR_MISSION_ID,0);
                //下面为创建模式准备数据List<Item>。
                new Thread(new PrepareDataForLcOrderRunnable()).start();         // start thread

                break;
            case LEARNING_AND_CREATE_RANDOM:
                //边学边建模式下传入的额外数据
                missionId = getIntent().getIntExtra(STR_MISSION_ID,0);
                //下面为创建模式准备数据List<Item>。
                new Thread(new PrepareDataForLcRandomRunnable()).start();         // start thread
                break;

            case LEARNING_AND_MERGE:
                //合并模式下额外传入的数据是一个ArrayList。
                Bundle bundleForMerge = getIntent().getBundleExtra(STR_BUNDLE_FOR_MERGE);
                if(bundleForMerge!=null){
                    gIdsForMerge =  bundleForMerge.getIntegerArrayList(STR_IDS_GROUPS_READY_TO_MERGE);
//                    Log.i(TAG, "onCreate: gidsForMerge.size in LPA ="+gIdsForMerge.size());
                    //合并模式下额外传入的数据是一个ArrayList。
                }
                //下面为合并模式准备数据List<Item>。
                new Thread(new PrepareDataForLMergeRunnable()).start();         // start thread

                break;
            case LEARNING_EXTRA_NO_RECORDS:
                //此中模式下不需开启额外线程
                ArrayList<SingleItem> items = getIntent().getParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING);

                Intent intentToLearningActivity_EXL = new Intent(this,LearningActivity.class);
                intentToLearningActivity_EXL.putExtra(STR_LEARNING_TYPE,LEARNING_EXTRA_NO_RECORDS);
                intentToLearningActivity_EXL.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_EXL.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);

                intentToLearningActivity_EXL.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_EXL);

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
                intentToLearningActivity.putExtra(STR_LEARNING_TYPE,LEARNING_GENERAL);
                intentToLearningActivity.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity.putExtra(STR_GROUP_ID,groupId);
                intentToLearningActivity.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);
                //Items采用统一的同一个关键字传递即可。

                intentToLearningActivity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity);

                break;

            case MESSAGE_LGR_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。（组内乱序）
                Intent intentToLearningActivity_LGR = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LGR.putExtra(STR_LEARNING_TYPE,LEARNING_GENERAL);
                intentToLearningActivity_LGR.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_LGR.putExtra(STR_GROUP_ID,groupId);
                intentToLearningActivity_LGR.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);
                //Items采用统一的同一个关键字传递即可。

                intentToLearningActivity_LGR.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LGR);

                break;
            case MESSAGE_LGN_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                RVGroup rvGroup = (RVGroup) message.obj;

                Intent intentToLearningActivity_LGN = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LGN.putExtra(STR_LEARNING_TYPE,LEARNING_GENERAL);//可以使用LG。（N无所谓了）
                intentToLearningActivity_LGN.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_LGN.putExtra(STR_GROUP_ID,rvGroup.getId());
                intentToLearningActivity_LGN.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);
                //Items采用统一的同一个关键字传递即可。
//                Log.i(TAG, "handleMessage: LGN");

                intentToLearningActivity_LGN.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LGN);

                break;

            case MESSAGE_LCO_DB_DATA_FETCHED:
                //加载完数据后，准备向后传递，本Activity结束。
                //边学边建模式下没有gid。
                Intent intentToLearningActivity_LCO = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCO.putExtra(STR_LEARNING_TYPE,LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCO.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_LCO.putExtra(STR_MISSION_ID,missionId);
                intentToLearningActivity_LCO.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);
//                Log.i(TAG, "handleMessage: items done,size="+items.size());
                intentToLearningActivity_LCO.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LCO);

                break;

            case MESSAGE_LCR_DB_DATA_FETCHED:
                Intent intentToLearningActivity_LCR = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LCR.putExtra(STR_LEARNING_TYPE,LEARNING_AND_CREATE_ORDER);
                intentToLearningActivity_LCR.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_LCR.putExtra(STR_MISSION_ID,missionId);
                intentToLearningActivity_LCR.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);

                intentToLearningActivity_LCR.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LCR);

                break;
            case MESSAGE_LM_DB_DATA_FETCHED:
                Intent intentToLearningActivity_LM = new Intent(this,LearningActivity.class);
                intentToLearningActivity_LM.putExtra(STR_LEARNING_TYPE,LEARNING_AND_MERGE);
                intentToLearningActivity_LM.putExtra(STR_TABLE_NAME_SUFFIX,tableNameSuffix);
                intentToLearningActivity_LM.putParcelableArrayListExtra(STR_ITEMS_FOR_LEARNING,items);
                intentToLearningActivity_LM.putIntegerArrayListExtra(STR_GROUP_ID_FOR_MERGE,gIdsForMerge);


                intentToLearningActivity_LM.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//要求不记录
                this.startActivity(intentToLearningActivity_LM);

                break;

        }

    }


    /* 为“普通学习”（LG）模式准备数据*/
    public class PrepareDataForGeneralLearningRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForGeneralLearningRunnable";

        @Override
        public void run() {
            items = memoryDbHelper.getItemsByGroupId(groupId, tableNameSuffix);

            Message message = new Message();
            message.what = MESSAGE_LG_DB_DATA_FETCHED;
            handler.sendMessage(message);
        }
    }

    /* 为“普通学习”（LG）模式准备数据*/
    public class PrepareDataForInnerRandomGeneralLearningRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForGeneralLearningRunnable";

        @Override
        public void run() {
            items = memoryDbHelper.getItemsByGroupId(groupId, tableNameSuffix);
            Collections.shuffle(items);//随机打乱。

            Message message = new Message();
            message.what = MESSAGE_LGR_DB_DATA_FETCHED;
            handler.sendMessage(message);
        }
    }

    /*
    * 为未传递groupId过来的普通学习（LGN）准备数据
    * “快速学习”功能会进入到此模式。
    * （快速学习要求系统智能选定最应复习的分组，因而没有预先确定的ID）
    * */

    public class PrepareDataForNoIdGeneralLearningRunnable implements Runnable {
//        private static final String TAG = "PrepareDataForNoIdGeneralLearningRunnable";

        @Override
        public void run() {
            ArrayList<DBGroup> groups = memoryDbHelper.getAllGroupsByMissionId(missionId, tableNameSuffix);
            //取到数据后，准备按各组的RMA或MS，或者由此二项数据计算的时间值来进行排序筛选（只取最前项即可）
            //转换到RVGroup才能得到相关字段
            ArrayList<RVGroup> rvGroups = new ArrayList<>();
            for (DBGroup d : groups) {
                rvGroups.add(new RVGroup(d));
            }
            //按指定条件prioritySetting对rvGroups集合进行筛选，选择其最小项目
            RVGroup targetGroup = getMinRvGroupUseTerm(prioritySetting,rvGroups);
            //获取选定分组的items
            items = memoryDbHelper.getItemsByGroupId(targetGroup.getId(),tableNameSuffix);

            Message message = new Message();
            message.what = MESSAGE_LGN_DB_DATA_FETCHED;
            message.obj = targetGroup;

            handler.sendMessage(message);
        }
    }


    /*
    * 用于创建式学习（顺序）
    * 从DB预拉取（暂定36个）items数据
    * */
    public class PrepareDataForLcOrderRunnable implements Runnable {

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
     * 用于创建式学习（随机）
     * 从DB预拉取（暂定36个）items数据
     * */
    public class PrepareDataForLcRandomRunnable implements Runnable {
        @Override
        public void run() {
            //从DB准备数据
            items = memoryDbHelper.getCertainAmountItemsRandomly(36,tableNameSuffix);

            Message message = new Message();
            message.what = MESSAGE_LCR_DB_DATA_FETCHED;

            handler.sendMessage(message);
        }
    }


    /* 为合并式学习准备数据
    * 需要将作为合并源的各个分组id一并传给目标页
    * （各个源分组需要先排序）
    * */
    public class PrepareDataForLMergeRunnable implements Runnable {
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
    * 按指定的条件（参数1），对传入的分组集合（参数2）进行筛选，选出最小项目并返回。
    * 基于Group列表页的排序方法删减而来，只挑最小项。
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
