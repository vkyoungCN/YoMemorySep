package com.vkyoungcn.smartdevices.yomemory;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.Mission;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

public class MissionDetailsActivity extends AppCompatActivity {
    private static final String TAG = "MissionDetailsActivity";
    private RvMission mission;

    private TextView tv_MissionName;
    private TextView tv_MissionDescription;

    private TextView tv_AllItemsNum;
    private TextView tv_LearnedItemsNum;
    private TextView tv_AllGroupsNum;

    private YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(this);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_detailsctivity);

        mission = getIntent().getParcelableExtra("MISSION");

        tv_MissionName = findViewById(R.id.tv_missionName_MDA);
        tv_MissionDescription = findViewById(R.id.tv_missionDescription_MDA);
        tv_AllItemsNum = findViewById(R.id.tv_totalItemsNum_MDA);
        tv_LearnedItemsNum = findViewById(R.id.tv_totalItemsLearnedNum_MDA);
        tv_AllGroupsNum = findViewById(R.id.tv_totalGroupsNum_MDA);

        tv_MissionName.setText(mission.getName());
        tv_MissionDescription.setText(String.format(getResources().getString(R.string.holder_mission_description),mission.getDescription()));

        int totalItemsNum = memoryDbHelper.getNumOfItemsOfMission(mission.getTableItem_suffix());
        tv_AllItemsNum.setText(String.format(getResources().getString(R.string.holder_total_itemNum),totalItemsNum));

        int totalLearnedItemsNum = memoryDbHelper.getLearnedNumOfItemsOfMission(mission.getTableItem_suffix());
        tv_LearnedItemsNum.setText(String.format(getResources().getString(R.string.holder_learned_itemNum),totalLearnedItemsNum));

        int totalGroupsNum = memoryDbHelper.getGroupsNumOfMission(mission.getId());
        tv_AllGroupsNum.setText(String.format(getResources().getString(R.string.holder_total_groupNum),totalGroupsNum));


    }

    public void toAllItemsDetails(View view){
        Intent intentToItems = new Intent(this,ItemsAndMissionDetailActivity.class);
        intentToItems.putExtra("MISSION",mission);
        this.startActivity(intentToItems);
    }

    public void toAllGroupsDetails(View view){
        Intent intentToGroups = new Intent(this,GroupsAndMissionDetailActivity.class);
        intentToGroups.putExtra("MISSION",mission);
        this.startActivity(intentToGroups);
    }

}
