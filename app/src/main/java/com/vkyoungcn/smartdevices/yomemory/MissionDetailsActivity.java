package com.vkyoungcn.smartdevices.yomemory;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.customUI.PieChart;
import com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningAddInOrderDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import static com.vkyoungcn.smartdevices.yomemory.LogoPageActivity.YO_MEMORY_SP;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_RANDOM;

public class MissionDetailsActivity extends AppCompatActivity implements OnGeneralDfgInteraction {
    private static final String TAG = "MissionDetailsActivity";
    private RvMission mission;

    private TextView tv_MissionName;
    private TextView tv_MissionDescription;
    private TextView tv_MissionDetailDescription;

    private TextView tv_AllItemsNum;
    private TextView tv_LearnedItemsNum;
    private TextView tv_AllGroupsNum;

    private PieChart pc_pieChart;

    private YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(this);

    private SharedPreferences sharedPreferences;
    private boolean noMoreTipBox;
    private boolean isFastLearnUseOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_details);

        mission = getIntent().getBundleExtra("BUNDLE_FOR_MISSION").getParcelable("MISSION");

        tv_MissionName = findViewById(R.id.tv_missionName_MDA);
        tv_MissionDescription = findViewById(R.id.tv_missionDescription_MDA);
        tv_MissionDetailDescription = findViewById(R.id.tv_mission_detailDescription_MDA);
        tv_AllItemsNum = findViewById(R.id.tv_totalItemsNum_MDA);
        tv_LearnedItemsNum = findViewById(R.id.tv_totalItemsLearnedNum_MDA);
        tv_AllGroupsNum = findViewById(R.id.tv_totalGroupsNum_MDA);
        pc_pieChart = findViewById(R.id.pieChart_MDA);

        tv_MissionName.setText(mission.getName());
        tv_MissionDescription.setText(mission.getSimpleDescription());
        tv_MissionDetailDescription.setText(mission.getDetailDescription());

        int totalItemsNum = memoryDbHelper.getNumOfItemsOfMission(mission.getTableItem_suffix());
//        int totalItemsNum = 10000;
        tv_AllItemsNum.setText(String.format(getResources().getString(R.string.holder_total_itemNum),totalItemsNum));

        int totalLearnedItemsNum = memoryDbHelper.getLearnedNumOfItemsOfMission(mission.getTableItem_suffix());
//        int totalLearnedItemsNum =8700;
        tv_LearnedItemsNum.setText(String.format(getResources().getString(R.string.holder_learned_itemNum),totalLearnedItemsNum));

        pc_pieChart.setData(totalItemsNum,totalLearnedItemsNum);

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

    //对应快速学习按钮
    /*
    *点击后弹出的（dfg或maskFlt）中选择随机或顺序，提示按完成数量记录分组。
    * 1、选择顺序、随机（默认顺序）。设为默认。
    * 2、提示语：将预先以顺序或随机方式选取一定数量的词汇，根据最终学习完成的数量记录为新的分组。
    * （提示语2：根据少量、分散更利于记忆的原则，分组容量将限制在36个词汇以内。
    * （提示语3：*可以在程序→设置中取消本提示框的显示（将直接按默认选择开始）
    * 按钮：取消、确定
    * 结果——开始“边学边建”
    * */
    public void learnNow(View view){
        //先检测sp，是否设置了“不再提示”
        //检测sp，默认的开始方式是随机还是顺序（如无，则按顺序开始）
        sharedPreferences = getSharedPreferences(YO_MEMORY_SP, MODE_PRIVATE);
        noMoreTipBox = sharedPreferences.getBoolean("NO_MORE_TIPS", false);
        isFastLearnUseOrder = sharedPreferences.getBoolean("IS_ORDER",true);
//        Log.i(TAG, "learnNow: is sp order? ="+isFastLearnUseOrder);
//        Log.i(TAG, "learnNow: is noMoreBox = "+noMoreTipBox);

        Toast.makeText(this, "MissionDetail页快速学习按钮", Toast.LENGTH_SHORT).show();
        if(!noMoreTipBox) {
         //并未设置“不再显示对话框”
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("FAST_LEARN");

            if (prev != null) {
                Toast.makeText(this, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
                transaction.remove(prev);
            }

            int defaultManner = DEFAULT_MANNER_ORDER;
            if(!isFastLearnUseOrder){
                defaultManner = DEFAULT_MANNER_RANDOM;
            }
            DialogFragment dfg = FastLearnDiaFragment.newInstance(defaultManner);
            dfg.show(transaction, "FAST_LEARN");

        }else {
            //此时不再显示，根据默认模式直接开始
            if(isFastLearnUseOrder) {
                onButtonClickingDfgInteraction(LEARNING_AND_CREATE_ORDER,null);
            }else {
                onButtonClickingDfgInteraction(LEARNING_AND_CREATE_RANDOM,null);
            }
        }
    }

    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {
       if(data!=null){
           //对传出的全局设置进行修改。
           SharedPreferences.Editor editor = sharedPreferences.edit();
           //（一）默认方式
           int defaultManner = data.getInt("DEFAULT_MANNER_SETTINGS");
           if(defaultManner == DEFAULT_MANNER_RANDOM){
               editor.putBoolean("IS_ORDER", false);
           }else if(defaultManner == DEFAULT_MANNER_ORDER){
               //还有可能之前已经设随机为默认，然后又改为顺序默认。
               editor.putBoolean("IS_ORDER",true);
           }//另一种是未设置，则不做更改即可

           //（二）是否继续显示对话框
           noMoreTipBox = data.getBoolean("NO_MORE_BOX",false);
           if(noMoreTipBox){
               editor.putBoolean("NO_MORE_TIPS",true);
           }/*else {
               editor.putBoolean("NO_MORE_TIPS",false);
               //【从“不开启”到“开启”的改变则应在其他页面处理，如设置界面。】
           }*/
           editor.apply();
       }

        //主要任务：跳转到学习准备页（该页负责根据学习类型进行数据准备）
        Intent intentToPFL = new Intent(this,PrepareForLearningActivity.class);
        intentToPFL.putExtra("LEARNING_TYPE",dfgType);
        intentToPFL.putExtra("TABLE_SUFFIX",mission.getTableItem_suffix());
        intentToPFL.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        this.startActivity(intentToPFL);



    }
}
