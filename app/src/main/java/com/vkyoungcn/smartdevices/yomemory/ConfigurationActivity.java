package com.vkyoungcn.smartdevices.yomemory;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import static com.vkyoungcn.smartdevices.yomemory.LogoPageActivity.YO_MEMORY_SP;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.FastLearnDiaFragment.DEFAULT_MANNER_UNDEFINED_L;

public class ConfigurationActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "ConfigurationActivity";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private CheckBox ckb_showTipBox;
    private RadioGroup rgp_orderOrRandom;
    private RadioButton rbn_random;
    private RadioButton rbn_order;
    private int defaultManner = DEFAULT_MANNER_UNDEFINED_L;//用于装载顺序或随机的默认设置
    private boolean showMoreBox = false;
    private boolean isFastLearnUseOrder = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        sharedPreferences = getSharedPreferences(YO_MEMORY_SP, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        boolean noMoreTipBox = sharedPreferences.getBoolean("NO_MORE_TIPS", false);
        showMoreBox = !noMoreTipBox;
        isFastLearnUseOrder = sharedPreferences.getBoolean("IS_ORDER",true);

        rgp_orderOrRandom = (RadioGroup) findViewById(R.id.rg_fastLearnManner_C);
        rbn_random = (RadioButton) findViewById(R.id.rb_random_C);
        rbn_order = (RadioButton) findViewById(R.id.rb_order_C);
        ckb_showTipBox = (CheckBox) findViewById(R.id.ckb_showTipBox_C);

        if(!noMoreTipBox){
            ckb_showTipBox.setChecked(true);//根据现有设置，配置对应的正确UI显示。
        }
        if(isFastLearnUseOrder){
            rbn_order.setChecked(true);
        }else{
            rbn_random.setChecked(true);
        }

        //在UI设置后，添加监听
        ckb_showTipBox.setOnCheckedChangeListener(this);
        rgp_orderOrRandom.setOnCheckedChangeListener(this);

    }

    //ckb的监听和rbg的监听不同。另一个在下方
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.ckb_showTipBox_C:
//                Log.i(TAG, "onCheckedChanged: ckb");
                if(isChecked){
                    showMoreBox = true;
                }else {
                    showMoreBox =false;
                }
                editor.putBoolean("NO_MORE_TIPS",!showMoreBox);
                editor.apply();

                break;

        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getCheckedRadioButtonId()) {
            case R.id.rb_random_C:
                //选择了随机
                defaultManner = DEFAULT_MANNER_RANDOM;
                editor.putBoolean("IS_ORDER", false);
//                Log.i(TAG, "onCheckedChanged: btn_Random");

                break;
            case R.id.rb_order_C:
                    //选择了顺序
//                    Log.i(TAG, "onCheckedChanged: btn_Order");
                    defaultManner = DEFAULT_MANNER_ORDER;
                    editor.putBoolean("IS_ORDER", true);
                    break;
        }
        editor.apply();
    }

}
