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
import com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import static com.vkyoungcn.smartdevices.yomemory.LogoPageActivity.YO_MEMORY_SP;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_UNDEFINED_L;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_MS;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_RMA;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_TT;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastRePickDiaFragment.DEFAULT_MANNER_UNDEFINED;

/*
* 作者1：杨胜 @中国海洋大学
* 作者2：杨镇时 @中国海洋大学
* author：Victor Young @Ocean University of China
* email: yangsheng@ouc.edu.cn
* */
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
    private SharedPreferences.Editor editor;
    private boolean noMoreL_TipBox;
    private boolean noMoreR_TipBox;
    private boolean isFastLearnUseOrder;
    private int fastRePickManner;
    private int totalGroupsNum;

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

        totalGroupsNum = memoryDbHelper.getGroupsNumOfMission(mission.getId());
        tv_AllGroupsNum.setText(String.format(getResources().getString(R.string.holder_total_groupNum),totalGroupsNum));

        sharedPreferences = getSharedPreferences(YO_MEMORY_SP, MODE_PRIVATE);


    }

    public void toAllItemsDetails(View view){
        Intent intentToItems = new Intent(this,ItemsOfMissionActivity.class);
        intentToItems.putExtra("MISSION",mission);
        this.startActivity(intentToItems);
    }

    public void toAllGroupsDetails(View view){
        Intent intentToGroups = new Intent(this,GroupsOfMissionActivity.class);
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
    public void fastLearn(View view){
        //先检测sp，是否设置了“不再提示”
        //检测sp，默认的开始方式是随机还是顺序（如无，则按顺序开始）
        noMoreL_TipBox = sharedPreferences.getBoolean("NO_MORE_TIPS", false);
        isFastLearnUseOrder = sharedPreferences.getBoolean("IS_ORDER",true);

        Toast.makeText(this, "MissionDetail页快速学习按钮", Toast.LENGTH_SHORT).show();
        if(!noMoreL_TipBox) {
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

    public void fastRePick(View view){
        //检测sp中关于目标Dfg的特别设置
        noMoreR_TipBox = sharedPreferences.getBoolean("NO_MORE_R_TIPS", false);
        fastRePickManner = sharedPreferences.getInt("FAST_R_MANNER",DEFAULT_MANNER_TT);
        if(!noMoreR_TipBox) {
            //未设置不显示对话框
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("FAST_RE_PICK");

            if (prev != null) {
                Toast.makeText(this, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
                transaction.remove(prev);
            }
            DialogFragment dfg = FastRePickDiaFragment.newInstance(fastRePickManner);
            dfg.show(transaction, "FAST_RE_PICK");
        }else {
            //直接根据默认设置开始


        }
    }

    @Override
    public void onButtonClickingDfgInteraction(int dfgType, Bundle data) {
        //目前只有两种DFG发回本回调。
        int learningType = LEARNING_AND_CREATE_ORDER;//默认值
        editor = sharedPreferences.edit();
        Intent intentToPFL = new Intent(this, PrepareForLearningActivity.class);
        intentToPFL.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intentToPFL.putExtra("TABLE_SUFFIX", mission.getTableItem_suffix());//通用数据

        switch (dfgType) {
            case FAST_LEARN:
            if (data != null) {
                boolean useOrder = data.getBoolean("IS_ORDER");
                if(!useOrder){
                    learningType = LEARNING_AND_CREATE_RANDOM;
                }
                //对传出的全局设置进行修改。
                //（一）默认方式
                int defaultManner = data.getInt("DEFAULT_MANNER_SETTINGS");
                if (defaultManner == DEFAULT_MANNER_ORDER){
                    editor.putBoolean("IS_ORDER", true);
                }else if(defaultManner == DEFAULT_MANNER_RANDOM){
                    editor.putBoolean("IS_ORDER", false);
                }

                //（二）是否继续显示对话框
                noMoreL_TipBox = data.getBoolean("NO_MORE_BOX", false);
                if (noMoreL_TipBox) {
                    editor.putBoolean("NO_MORE_TIPS", true);
                }/*else {
               editor.putBoolean("NO_MORE_TIPS",false);
               //【从“不开启”到“开启”的改变则应在其他页面处理，如设置界面。】
           }*/
                editor.apply();
            }

            //主要任务：跳转到学习准备页（该页负责根据学习类型进行数据准备）
            intentToPFL.putExtra("LEARNING_TYPE", learningType);
            intentToPFL.putExtra("MISSION_ID",mission.getId());//LC特别数据
            this.startActivity(intentToPFL);

            break;

            case FAST_RE_PICK:
                if(totalGroupsNum ==0){
                    Toast.makeText(this, "尚无分组建立，无法执行复习。", Toast.LENGTH_SHORT).show();
                    return;
                }

                learningType = LEARNING_GENERAL_NO_GID;//快速复习只对应一种模式LG（但是没有GID传递）

                if (data != null) {
                    //对传出的全局设置进行修改。
                    //（一）默认方式（快速复习）
                    int defaultRpManner = data.getInt("DEFAULT_MANNER_R_SETTINGS");
                    if (defaultRpManner != DEFAULT_MANNER_UNDEFINED) {
                        editor.putInt("FAST_R_MANNER", defaultRpManner);
                    } //另一种是未设置，则不做更改即可

                    //（二）是否继续显示对话框（快速复习）
                    noMoreL_TipBox = data.getBoolean("NO_MORE_BOX", false);
                    if (noMoreL_TipBox) {
                        editor.putBoolean("NO_MORE_R_TIPS", true);
                    }/*else {
                        editor.putBoolean("NO_MORE_TIPS",false);//【从“不开启”到“开启”的改变则应在其他页面处理，如设置界面。】
                    }*/
                    editor.apply();

                    intentToPFL.putExtra("PRIORITY_SETTING",defaultRpManner);//LGN的特别数据，要在if内设置

                }

                //主要任务：跳转到学习准备页（该页负责根据学习类型进行数据准备）
                intentToPFL.putExtra("LEARNING_TYPE", learningType);
                intentToPFL.putExtra("MISSION_ID",mission.getId());
                this.startActivity(intentToPFL);

                break;
        }


    }



}
