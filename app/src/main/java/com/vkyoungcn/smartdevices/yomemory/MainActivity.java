package com.vkyoungcn.smartdevices.yomemory;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.adapters.AllMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.fragments.GiveExplanationDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.Mission;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */

public class MainActivity extends AppCompatActivity
        implements AllMissionRvAdapter.ChangeStar,Constants{
// * 首页。
// * 上部预留横向图片式广告位（间隔滚动式）
// * 下方是卡片式（横向）任务列表；点击查看任务详情，并进入该任务的后续可执行逻辑。
// * 底部是功能性按钮。
//    private static final String TAG = "MainActivity";
    public static final int MESSAGE_DB_MISSION_FETCHED = 5001;
    private YoMemoryDbHelper memoryDbHelper;
    private ArrayList<Integer> starClickedPositions = new ArrayList<>();
    private ArrayList<RvMission> rvMissions;//应为要跨方法使用，最后需要存入DB。所以全局。
    private TextView btn_explain;
    private TextView tv_slideForMore;

    SharedPreferences sharedPreferences;
    boolean isBtnExplainBeenClicked;
    ;
    private Handler handler = new MainActivity.MainActivityHandler(this);//通过其发送消息。

    private RecyclerView allMissionRecyclerView;

    private AllMissionRvAdapter allMissionRvAdapter;//便于更新UI时调用，注意检查非空

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //从数据库获取数据源Missions，本页其实只需要显示名称字段；但是按钮需要其所属碎片信息。
        new Thread(new FetchMissionsFromDBRunnable()).start();

        allMissionRecyclerView = (RecyclerView) findViewById(R.id.all_missions_rv);
        btn_explain = findViewById(R.id.btn_explain_MA);
        tv_slideForMore = findViewById(R.id.tv_slideForMore_MA);

        //没有点击过“程序使用说明”按键时，将在其左上显示小标记。
        sharedPreferences = getSharedPreferences(SP_STR_TITLE_YO_MEMORY, MODE_PRIVATE);
        isBtnExplainBeenClicked = sharedPreferences.getBoolean(SP_STR_BTN_EXPLAIN_CLICKED, false);

        if(!isBtnExplainBeenClicked){
            //未曾点击过
            btn_explain.setBackgroundResource(R.drawable.red_mark_for_btn);
        }
    }

    public class FetchMissionsFromDBRunnable implements Runnable{
        @Override
        public void run() {
            memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());
            ArrayList<Mission> missions = (ArrayList<Mission>) memoryDbHelper.getAllMissions();

            //转换成适合于Rv显示的RvMission类。
            rvMissions = new ArrayList<>();
            for (Mission m :missions) {
                rvMissions.add(new RvMission(getApplicationContext(),m));
            }

            Message message = new Message();
            message.what = MESSAGE_DB_MISSION_FETCHED;

            handler.sendMessage(message);
        }
    }

    /*public class UpdateHpbOnTimeRunnable implements Runnable{
        @Override
        public void run() {
            while(count!=0){//时间未到
                try {
                        Thread.sleep(2000);     // sleep 2 秒；

                        //消息发回UI，改变数值20
                        Message message = new Message();
                        message.what = MESSAGE_CHANGE_20;
                        message.arg1 = 6-count;
                        count--;
                        handler.sendMessage(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/


    final static class MainActivityHandler extends Handler {
        private final WeakReference<MainActivity> activityWeakReference;

        private MainActivityHandler(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity MainActivity = activityWeakReference.get();
            if(MainActivity!=null){
                MainActivity.handleMessage(msg);
            }
        }
    }

    void handleMessage(Message message){
        switch (message.what) {
            case MESSAGE_DB_MISSION_FETCHED:

                //取到数据后，应更新UI的显示
//                allMissionRecyclerView.setHasFixedSize(true);//暂时只有固定数量的任务，可以设fix。
                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.HORIZONTAL);
                llm.setReverseLayout(false);//如果设true则第一个出现在最右边。
//                llm.setStackFromEnd(false);//没有影响。

                allMissionRecyclerView.setLayoutManager(llm);
                allMissionRvAdapter = new AllMissionRvAdapter(rvMissions,this);//只要是从本消息到达，则rvMs一定有数据。
                allMissionRecyclerView.setAdapter(allMissionRvAdapter);

                if(rvMissions.size()>=3) {
                    tv_slideForMore.setVisibility(View.VISIBLE);
                }
//                new Thread(new UpdateHpbOnTimeRunnable()).start();

                break;
//            case MESSAGE_CHANGE_20:
//                hpb_progress.setNewPercentage(message.arg1*20);
//                break;
        }
    }

    //当RV-Adp中点击了星标后，会调用此回调方法，通知新的星标类型。
    //在本页退出时，应当存入DB。
    @Override
    public void changeRvStar(int position) {
            allMissionRvAdapter.notifyItemChanged(position);

            starClickedPositions.remove((Integer)position);//先删再提交，避免重复。
            starClickedPositions.add(position);//这些位置上发生过点击，应该提交到DB更新
        // （其新值应已在adapter中设置好了，毕竟是引用类型）
        //点击最后不一定改变了值，但是判断逻辑估计较复杂，从略、只要点击了就全存。
    }

    @Override
    protected void onStop() {
        super.onStop();
        //如果星标位置上发生过点击，则最终会产生向DB的提交（但不一定真的有改变，毕竟星标是循环设置的）
        if(starClickedPositions!=null && !starClickedPositions.isEmpty()) {//【实测在此若不检测null则崩溃】
            int affectRows = memoryDbHelper.updateMissionStartInBatches(rvMissions, starClickedPositions);
            if (affectRows == 0) {
                Toast.makeText(this, "星标有点击，但DB没改变。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * 似乎只有这种方式才能禁止返回到起始页。
     * 可以直接退出程序。
     */
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;//不执行父类点击事件
        }
        return super.onKeyDown(keyCode, event);//继续执行父类其他点击事件
    }

    //临时点击方法，仅用于增加显示任务
    public void createMission(View view){
        long line;
        Mission mission = new Mission("演示任务"+System.currentTimeMillis()%10000,"简述","详细说明……","",1);

        line = memoryDbHelper.createMission(mission);
        if(line == -1){
            Toast.makeText(this, "something goes wrong!", Toast.LENGTH_SHORT).show();
        }else {
            rvMissions.add(new RvMission(getApplicationContext(),mission));
            allMissionRvAdapter.notifyDataSetChanged();
        }

    }

    public void toExplanation(View view){
        //弹出说明
        //取消按钮上的新按钮标识
        btn_explain.setBackgroundResource(0);
        //改变相应记录
        isBtnExplainBeenClicked = true;
        //改变相应全局记录。
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SP_STR_BTN_EXPLAIN_CLICKED, true);
        editor.apply();

        //弹出DFG
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FG_STR_GIVE_EXPLANATION);

        if (prev != null) {
            Toast.makeText(this, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
            transaction.remove(prev);
        }
        DialogFragment dfg = GiveExplanationDiaFragment.newInstance();
        dfg.show(transaction, FG_STR_GIVE_EXPLANATION);

    }

    public void appConfiguration(View view){
        //各全局设置量
        //①快速学习的设置：对话框是否显示、快速学习选项
        //去往专用设置页
        Intent intent = new Intent(this,ConfigurationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        this.startActivity(intent);

    }

}
