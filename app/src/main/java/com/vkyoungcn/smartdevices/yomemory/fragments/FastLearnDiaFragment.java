package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class FastLearnDiaFragment extends DialogFragment
        implements View.OnClickListener,CheckBox.OnCheckedChangeListener,Constants {
//* 涉及到的Sp操作返回Activity后处理，发送数据过去
//* 发回：①学习的类型（随机、顺序）；②是否设置了默认值、默认值如何；③是否取消提示框。
//*
//* 如果之前已选中过不再提示则根部不会进入到本dfg。如果选中过某默认值则应显示相应默认值。
    private static final String TAG = "LearningCreateOrderDiaFragment";
    public static final int DEFAULT_MANNER_ORDER = 1201;
    public static final int DEFAULT_MANNER_RANDOM = 1202;
    public static final int DEFAULT_MANNER_UNDEFINED_L = 1203;

    private OnGeneralDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;
    private CheckBox ckb_setAsDefault;
    private CheckBox ckb_NoMoreTip;
    private RadioGroup rgp_orderOrRandom;
    private RadioButton rbn_random;
    private int defaultManner = DEFAULT_MANNER_UNDEFINED_L;//用于装载顺序或随机的默认设置
    private boolean isNoMoreBox =false;

    public FastLearnDiaFragment() {
        // Required empty public constructor
    }


    public static FastLearnDiaFragment newInstance(int defaultManner) {
        FastLearnDiaFragment fragment = new FastLearnDiaFragment();
        Bundle data = new Bundle();
        data.putInt(STR_DEFAULT_MANNER,defaultManner);
        fragment.setArguments(data);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.defaultManner = getArguments().getInt(STR_DEFAULT_MANNER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_fast_learn, container, false);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_dfgFL);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_ok_dfgFL);
        rgp_orderOrRandom = (RadioGroup) rootView.findViewById(R.id.rg_manner_dfgFL);
        rbn_random = (RadioButton)rootView.findViewById(R.id.rb_random_dfgFl);
        if(defaultManner == DEFAULT_MANNER_RANDOM){
            //其余两种都采用布局默认的顺序已选中即可,如果代码默认是随机则需要修改如下
            rbn_random.setChecked(true);
        }
        ckb_setAsDefault = (CheckBox) rootView.findViewById(R.id.ckb_setAsDefault_dfgFL);
        ckb_NoMoreTip = (CheckBox) rootView.findViewById(R.id.ckb_noMoreTipBox_dfgFL);

        //部分需要添加事件监听
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
        ckb_setAsDefault.setOnCheckedChangeListener(this);
        ckb_NoMoreTip.setOnCheckedChangeListener(this);

        return rootView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGeneralDfgInteraction) {
            mListener = (OnGeneralDfgInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGeneralDfgInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_ok_dfgFL:
                //要根据选定的不同模式向调用方activity发回不同消息
                int rBtnId = rgp_orderOrRandom.getCheckedRadioButtonId();
                Bundle data = new Bundle();
                data.putInt(STR_DEFAULT_MANNER_SETTINGS,defaultManner);
                data.putBoolean(STR_NO_MORE_BOX,isNoMoreBox);
                if(rBtnId == R.id.rb_order_dfgFl) {
                    //选择了顺序
                    data.putBoolean(STR_IS_ORDER,true);
                }else{
                    //选择了随机
                    data.putBoolean(STR_IS_ORDER,false);
                }
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FAST_LEARN, data);
                this.dismiss();//如果没有dismiss则从目标Activity返回后该dfg会还在。
                break;

            case R.id.btn_cancel_dfgFL:
                this.dismiss();
                break;
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int rBtnId = rgp_orderOrRandom.getCheckedRadioButtonId();

        switch (buttonView.getId()){
            case R.id.ckb_setAsDefault_dfgFL:
                if(isChecked){
                    if(rBtnId == R.id.rb_order_dfgFl) {
                        //选择了顺序
                        defaultManner = DEFAULT_MANNER_ORDER;
                    }else{
                        //选择了随机
                        defaultManner = DEFAULT_MANNER_RANDOM;
                    }
                }else {
                    defaultManner = DEFAULT_MANNER_UNDEFINED_L;
                }
                break;
            case R.id.ckb_noMoreTipBox_dfgFL:
                if(isChecked){
                    isNoMoreBox = true;
                }else {
                    isNoMoreBox =false;
                }
                break;
        }
    }
}
