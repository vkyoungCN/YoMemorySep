package com.vkyoungcn.smartdevices.yomemory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.04
 * */
public class BannerActivity extends AppCompatActivity {
//* 程序第一个页面，过渡性页面（结束后不可返回）
//* 功能：欢迎（Logo动画）；

    private static final String TAG = "BannerActivity";

    /* 控件*/
    private ImageView imv_logoBanner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner);

        imv_logoBanner = findViewById(R.id.imv_logoBanner);

        //执行动画
        startAnimator();

    }


    /*
    * 动画，banner渐渐显示；持续2秒。
    * */
    private void startAnimator(){

        ValueAnimator CenterBannerAnimator = ValueAnimator.ofFloat(10,70,90);

        CenterBannerAnimator.addUpdateListener(new BannerAlphaChangeListener());
        CenterBannerAnimator.setInterpolator(new LinearInterpolator());
        CenterBannerAnimator.setDuration(2100);
        CenterBannerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                Intent intent= new Intent(BannerActivity.this,MainActivity.class);
                startActivity(intent);

            }
        });

        CenterBannerAnimator.start();

    }

    private class BannerAlphaChangeListener implements ValueAnimator.AnimatorUpdateListener {

        public void onAnimationUpdate(ValueAnimator valueanimator) {
            float value = (Float)valueanimator.getAnimatedValue();
            imv_logoBanner.setAlpha(value/100);
        }
    }

}
