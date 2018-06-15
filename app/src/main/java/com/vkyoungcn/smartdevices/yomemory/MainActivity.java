package com.vkyoungcn.smartdevices.yomemory;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.adapters.AllMissionRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.Mission;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
 * 首页。
 * 上部预留横向图片式广告位（间隔滚动式）
 * 下方是任务列表；点击可进入新Activity查看任务情况。
 * */
public class MainActivity extends AppCompatActivity implements AllMissionRvAdapter.ChangeStar{
    private static final String TAG = "MainActivity";
    public static final int MESSAGE_DB_MISSION_FETCHED = 5101;
    private android.os.Handler handler = new MainActivity.MainActivityHandler(this);//通过其发送消息。

    private ArrayList<RvMission> missions = new ArrayList<>();
    private RecyclerView allMissionRecyclerView;

    private AllMissionRvAdapter allMissionRvAdapter;//便于更新UI时调用，注意检查非空

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //从数据库获取数据源Missions，本页其实只需要显示名称字段；但是按钮需要其所属碎片信息。
        new Thread(new FetchMissionsFromDBRunnable()).start();
//        YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());

        allMissionRecyclerView = (RecyclerView) findViewById(R.id.all_missions_rv);
//        ArrayList<Mission> allMissions = getIntent().getParcelableArrayListExtra("All_Missions");
        /*if(allMissions == null){
            Toast.makeText(this, "没有任务信息", Toast.LENGTH_SHORT).show();
            allMissions = new ArrayList<>();
        }*/


    }

    public class FetchMissionsFromDBRunnable implements Runnable{
        @Override
        public void run() {
            YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());
            ArrayList<Mission> missions = (ArrayList<Mission>) memoryDbHelper.getAllMissions();
            ArrayList<RvMission> rvMissions = new ArrayList<>();
            for (Mission m :missions) {
                rvMissions.add(new RvMission(m));
            }

            Message message = new Message();
            message.what = MESSAGE_DB_MISSION_FETCHED;
            message.obj = rvMissions;

            handler.sendMessage(message);
        }
    }

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
                missions = (ArrayList<RvMission>) message.obj;

                //取到数据后，应更新UI的显示
                allMissionRecyclerView.setHasFixedSize(true);//暂时只有固定数量的任务，可以设fix。
                allMissionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                allMissionRvAdapter = new AllMissionRvAdapter(missions,this);
                allMissionRecyclerView.setAdapter(allMissionRvAdapter);

                break;
        }
    }

    @Override
    public void changeRvStar(int position) {
        if(allMissionRvAdapter==null){
            return;
        }else {
            allMissionRvAdapter.notifyItemChanged(position);
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

}
