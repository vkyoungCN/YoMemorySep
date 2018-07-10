package com.vkyoungcn.smartdevices.yomemory;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.customUI.HorizontalProgressBar;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.lang.ref.WeakReference;

/*
* 本页面是程序第一个页面
* 职能：①欢迎（Logo动画）；②首次运行时向DB填充数据；③获取Mission信息向后传递（本项取消）
* 本页一旦离开不能退回（在下一页特别实现）
* */
public class LogoPageActivity extends AppCompatActivity {
    
    private static final String TAG = "LogoPageActivity";
    private Handler handler = new LogoPageActivity.FirstActivityHandler(this);//通过其发送消息。
    public static final int MESSAGE_DB_POPULATED = 5001;
    public static final int MESSAGE_NEW_PERCENTAGE_NUMBER = 5002;

    private TextView logoStrCn;
    private HorizontalProgressBar hpb_progress;//需要传入百分比的分子数值。
    private LinearLayout llt_firstRun;
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_page);

        logoStrCn = (TextView)findViewById(R.id.logo_cn);
        hpb_progress = findViewById(R.id.hpb_LPA);
        llt_firstRun = findViewById(R.id.llt_forFirstRun_LPA);

        SharedPreferences sharedPreferences=getSharedPreferences("yoMemorySP", MODE_PRIVATE);
        boolean isFirstLaunch=sharedPreferences.getBoolean("IS_FIRST_LAUNCH", true);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        if(isFirstLaunch){
            //第一次运行：开启新线程执行DB填充操作然，同时提示。
            //完成后跳转下一页
            llt_firstRun.setVisibility(View.VISIBLE);
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
            YoMemoryDbHelper memoryDbHelper = YoMemoryDbHelper.getInstance(context);//应该是在获取DB-Helper后直接触发数据填充吧。

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
            case MESSAGE_NEW_PERCENTAGE_NUMBER:
                hpb_progress.setNewPercentage(message.arg1);
        }
    }


    private void startAnimator(){
         ValueAnimator CenterLogoAnimator = ValueAnimator.ofFloat(0,80,90,100);
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

    /*
    * 此方法在DB的填充逻辑中调用，用于根据填充的进度修改进度条的显示
    * 【但是注意，由于DB的填充逻辑是在新线程中发起的，因而不能直接在此对hpb控件设置，
    * 否则程序崩溃，提示大致为“新线程触碰了UI线程，不应这样做”】
    * */
    public void setNewPercentNum(int percentageNum){
        Log.i(TAG, "setNewPercentNum:"+percentageNum);
        Message msgForNum = new Message();
        msgForNum.what = MESSAGE_NEW_PERCENTAGE_NUMBER;
        msgForNum.arg1 = percentageNum;
        handler.sendMessage(msgForNum);

    }

}
