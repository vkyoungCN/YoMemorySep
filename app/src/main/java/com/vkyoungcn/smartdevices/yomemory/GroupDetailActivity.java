package com.vkyoungcn.smartdevices.yomemory;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.ItemsOfMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningGelDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LessAndQuitDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LogsOfGroupDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnLearningConfirmDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.models.SingleLearningLog;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class GroupDetailActivity extends Activity implements OnLearningConfirmDfgInteraction {
//    private static final String TAG = "GroupDetailActivity";
    private RVGroup rvGroup;
    private YoMemoryDbHelper memoryDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        TextView groupId = (TextView) findViewById(R.id.tv_id_GD);
        TextView groupDescription = (TextView) findViewById(R.id.tv_description_GD);
        TextView groupSubNum = (TextView) findViewById(R.id.tv_subNum_GD);
        TextView settingUpTime = (TextView) findViewById(R.id.tv_setupTime_GD);
        TextView lastLearnTime = (TextView) findViewById(R.id.tv_lastLearningTime_GD);
        TextView group_MS = (TextView) findViewById(R.id.tv_ms_GD);
        TextView group_RMA = (TextView) findViewById(R.id.tv_rma_GD);
        TextView remainTime = (TextView) findViewById(R.id.tv_remainTime_GD);

        ImageView moreLogsBtn = findViewById(R.id.ivBtn_allLogs_GD);

        RecyclerView rv_itemsOfGroup = (RecyclerView)findViewById(R.id.rv_itemsOfGroup_GD);

        int groupIdFromIntent = getIntent().getIntExtra("GroupId",0);
        String tableSuffix = getIntent().getStringExtra("TableSuffix");

        memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());

        //从DB获取本group，如果在Intent传的话需要序列化，也是较大开销。
        DBGroup dbGroup = memoryDbHelper.getGroupById(groupIdFromIntent,tableSuffix);
        rvGroup = new RVGroup(dbGroup);

        //从DB获取本组所属的items
        ArrayList<SingleItem> items = memoryDbHelper.getItemsByGroupId(groupIdFromIntent,tableSuffix);

        if (rvGroup !=null) {
            //将group的信息填充到UI
            groupId.setText(String.valueOf(rvGroup.getId()));
            groupDescription.setText(rvGroup.getDescription());
            groupSubNum.setText(String.valueOf(items.size()));

            Date settingUpTimeDate = new Date(rvGroup.getSettingUptimeInLong());
            Date lastLearningTimeDate = new Date(rvGroup.getLastLearningTime());
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String settingUpTimeStr = sdFormat.format(settingUpTimeDate);
            String lastLearningTimeStr = sdFormat.format(lastLearningTimeDate);

            settingUpTime.setText(settingUpTimeStr);

            group_MS.setText(String.valueOf(rvGroup.getMemoryStage()));
            group_RMA.setText(String.valueOf(rvGroup.getRM_Amount()));

            lastLearnTime.setText(lastLearningTimeStr);

            int remainingMinutes = RVGroup.minutesRemainTillThreshold(rvGroup.getMemoryStage());
            remainTime.setText(formatFromMinutes(remainingMinutes));

            moreLogsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //展开显示全部的日志记录，初定DFG模式
                    showLogs(rvGroup.getId());
                }
            });

        }


        //Rv配置：LM、适配器
        rv_itemsOfGroup.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.Adapter adapter = new ItemsOfMissionRvAdapter(items,this);
        rv_itemsOfGroup.setAdapter(adapter);

    }

    private String formatFromMinutes(int minutes){
        int days = minutes/(60*24);
        int hours = (minutes%(60*24))/60;
        int min = (minutes%60);
        if(days!=0) {
            return "请在" + days + "天" + hours + "小时" + min + "分钟内完成复习";
        }else if(hours!=0){
            return "请在"+ hours + "小时" + min + "分钟内完成复习";
        }else {
            return "请在"+ min + "分钟内完成复习";
        }
    }

    public void showLogs(int groupId){
        ArrayList<SingleLearningLog> learningLogs = memoryDbHelper.getAllLogsOfGroup(groupId);
        if(learningLogs==null|| learningLogs.isEmpty()){
            Toast.makeText(this, "没有复习日志", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(learningLogs,new SortByTime());

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("SHOW_LOGS");

        if(prev != null){
            transaction.remove(prev);
        }
        DialogFragment dialogFragment = LogsOfGroupDiaFragment.newInstance(learningLogs);
        dialogFragment.show(transaction,"SHOW_LOGS");
    }


    /*
    * 某控件点击事件对应方法。用于弹出DFG，并通过确认后跳转至学习页，按照普通方式学习（GEL）
    * 根据本组的容量会有不同的学习方式。（但是，本页没有其他分组的信息（没有整个ArrayList只有单个RVG）
    * 因而无法轻易地获取额外数据，因而对碎片分组暂时简单的按照“只提示，不启动”处理）
    * */
    public void learnThisGroupGel(View view){
        Toast.makeText(this, "准备弹确认对话框", Toast.LENGTH_SHORT).show();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev_2 = getFragmentManager().findFragmentByTag("READY_TO_LEARN_GEL");

        if (prev_2 != null) {
            transaction.remove(prev_2);
        }

        if(rvGroup.getTotalItemsNum()<5){
            //4个（含）以内的，触发合并式学习；但无法轻易获取其他碎片组信息，决定只进行提示，不予开始。
            //可以弹出DFG，告知请从分组列表页点击本组，执行合并学习，询问是否跳转到分组列表页
            DialogFragment dfg = LessAndQuitDiaFragment.newInstance(rvGroup.getId());
            dfg.show(transaction, "LESS_IN_GD_DIA");
        }else {
            //正常容量正常学习。此时只需传递正常的分组id即可
            //【但是本activity接下来需要实现与该dfg的交互接口】
            DialogFragment dfg = LearningGelDiaFragment.newInstance(rvGroup);
            dfg.show(transaction, "READY_TO_LEARN_GEL");

        }
    }

    @Override
    public void onLearningConfirmDfgInteraction(int dfgType, Bundle data) {
        if(dfgType == JUMP_TO_GROUP_LIST_THIS_FRAG) {
            Intent intentToGroupListActivity = new Intent(this, GroupsAndMissionDetailActivity.class);

            intentToGroupListActivity.putExtra("GROUP_ID_TO_JUMP",data.getInt("GROUP_ID_TO_JUMP") );
            this.startActivity(intentToGroupListActivity);
        }
    }

    private class SortByTime implements Comparator {
        public int compare(Object o1, Object o2) {
            SingleLearningLog s1 = (SingleLearningLog) o1;
            SingleLearningLog s2 = (SingleLearningLog) o2;
            return (int)(s1.getTimeInLong() - s2.getTimeInLong());
        }
    }



}
