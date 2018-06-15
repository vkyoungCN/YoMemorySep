package com.vkyoungcn.smartdevices.yomemory;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;

/*
* 本页面是程序第一个页面
* 职能：①欢迎页（Logo动画）；②首次运行时向DB填充数据。
* 本页一旦离开不能退回（需在下一页特别实现）
* （*原计划在此页从DB预加载Missions数据，但各页职能应清晰化且Missions数据量较小，取消。）
* */
public class LogoPageActivity extends AppCompatActivity {
    
    private static final String TAG = "LogoPageActivity";
    private android.os.Handler handler = new LogoPageActivity.FirstActivityHandler(this);//通过其发送消息。
    public static final int MESSAGE_DB_POPULATED = 5001;
    public static final int MESSAGE_SLEEP_DONE = 5002;

    private android.widget.TextView logoStrCn;
    private android.widget.TextView reTry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_page);

        logoStrCn = (TextView)findViewById(R.id.logo_cn);

        SharedPreferences sharedPreferences=getSharedPreferences("yoMemorySP", MODE_PRIVATE);
        boolean isFirstLaunch=sharedPreferences.getBoolean("IS_FIRST_LAUNCH", true);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        if(isFirstLaunch){
            //第一次
            //开启新线程执行DB填充操作然，同时提示。
            //完成后跳转下一页
            Toast.makeText(this, "首次运行，正在填充数据，请稍等……", Toast.LENGTH_LONG).show();
            new Thread(new PopTheDatabaseRunnable()).start();

            editor.putBoolean("IS_FIRST_LAUNCH", false);
            editor.apply();
        }else{
            //执行动画效果，在动画执行完后（监听器）跳转下一Activity。
            startAnimator();
        }
    }

    public class PopTheDatabaseRunnable implements Runnable{
        @Override
        public void run() {
            YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(getApplicationContext());//应该是在获取DB-Helper后直接触发数据填充吧。
//            ArrayList<Mission> missions = (ArrayList<Mission>) memoryDbHelper.getAllMissions();不再预获取数据
            //应改造填充函数，使返回可判断执行情况的结果值。

            Message message = new Message();
            message.what = MESSAGE_DB_POPULATED;
//            message.obj = missions;

            handler.sendMessage(message);
        }
    }


    final static class FirstActivityHandler extends Handler {
        private final WeakReference<LogoPageActivity> activityWeakReference;

        private FirstActivityHandler(LogoPageActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            LogoPageActivity LogoPageActivity = activityWeakReference.get();
            if(LogoPageActivity!=null){
                LogoPageActivity.handleMessage(msg);
            }
        }
    }

    void handleMessage(Message message){
        switch (message.what){
            case MESSAGE_DB_POPULATED:
//                missions = (ArrayList<Mission>) message.obj;

                Intent intent= new Intent(this,MainActivity.class);
                startActivity(intent);
                break;

        }
    }


    private void startAnimator(){
         ValueAnimator CenterLogoAnimator = ValueAnimator.ofFloat(80,100,100,0);
                CenterLogoAnimator.setDuration(1200);
                CenterLogoAnimator.addUpdateListener(new LogoDisAnimatorListener());
                CenterLogoAnimator.setInterpolator(new LinearInterpolator());
                CenterLogoAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                            Intent intent= new Intent(LogoPageActivity.this,MainActivity.class);
                            startActivity(intent);

                    }
                });
                CenterLogoAnimator.start();
    }

    private class LogoDisAnimatorListener implements ValueAnimator.AnimatorUpdateListener {

        public void onAnimationUpdate(ValueAnimator valueanimator) {
            float value = (Float)valueanimator.getAnimatedValue();
            logoStrCn.setAlpha(value/100);
        }
    }
}
