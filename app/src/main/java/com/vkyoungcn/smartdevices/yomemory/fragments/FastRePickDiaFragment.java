package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;

/*
* 涉及到的Sp操作返回Activity后处理，发送数据过去
* 发回：①对SP的操作：MS/RMA/TT哪项优先（已超时组的顺序），是否取消显示框。
* （如果是tThold优先，默认会将已超时的置于最前，则同时给出Ckb：已超时分组后置）
* ②默认不使用LM（LM原则上作为一种特别复习手段只在group列表页呈现），
* 由于不是新学，所以也不使用LCO/LCR，因而学习方式是LG，不需发回学习类型。
* ③分组筛选方式（同SP），由Activity根据此标准挑选适合的分组（或者将请求进一步发送到PrepareActivity由
* PA负责计算所有各组的tT时限。）
*
* 给出选择：①低MS优先、②低RMA优先、③timeThreshold优先（计算负荷大，发送到Pa处理）
* */
@SuppressWarnings("all")
public class FastRePickDiaFragment extends DialogFragment implements View.OnClickListener,CheckBox.OnCheckedChangeListener {
    private static final String TAG = "FastRePickDiaFragment";
    public static final int DEFAULT_MANNER_MS = 1251;
    public static final int DEFAULT_MANNER_RMA = 1252;
    public static final int DEFAULT_MANNER_TT = 1253;//tt的判断最费时吧，需要把所有分组计算一番。
    public static final int DEFAULT_MANNER_UNDEFINED = 1254;//当ckb“设为默认”未选中时，发送这条值替代各rbn选择。

    private OnGeneralDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;
    private CheckBox ckb_setAsDefault;
    private CheckBox ckb_NoMoreTip;
    private RadioGroup rgp_Manners;
    private RadioButton rbn_ms;
    private RadioButton rbn_rma;
    private RadioButton rbn_tt;
    private int defaultManner = DEFAULT_MANNER_TT;
    private boolean isNoMoreBox =false;

    public FastRePickDiaFragment() {
        // Required empty public constructor
    }


    public static FastRePickDiaFragment newInstance(int defaultManner) {
        FastRePickDiaFragment fragment = new FastRePickDiaFragment();
        Bundle data = new Bundle();
        data.putInt("DEFAULT_MANNER",defaultManner);
        fragment.setArguments(data);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {//【易误点：不能判断savedInstanceState，数据不在sis内】
            this.defaultManner = getArguments().getInt("DEFAULT_MANNER");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_fast_re_pick, container, false);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_dfgFRP);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_ok_dfgFRP);
        rgp_Manners = (RadioGroup) rootView.findViewById(R.id.rg_manner_dfgFRP);
        rbn_ms = (RadioButton)rootView.findViewById(R.id.rb_ms_first_dfgFRP);
        rbn_rma = (RadioButton)rootView.findViewById(R.id.rb_rma_first_dfgFRP);
        rbn_tt = (RadioButton)rootView.findViewById(R.id.rb_tt_first_dfgFRP);
        if(defaultManner == DEFAULT_MANNER_MS){
            //其余两种都采用布局默认的顺序已选中即可,如果代码默认是随机则需要修改如下
            rbn_ms.setChecked(true);
        }else if(defaultManner == DEFAULT_MANNER_RMA){
            rbn_rma.setChecked(true);
        }else {
            rbn_tt.setChecked(true);
        }
        ckb_setAsDefault = (CheckBox) rootView.findViewById(R.id.ckb_setAsDefault_dfgFRP);
        ckb_NoMoreTip = (CheckBox) rootView.findViewById(R.id.ckb_noMoreTipBox_dfgFRP);

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
            case R.id.btn_ok_dfgFRP:
                //要根据选定的不同模式向调用方activity发回不同消息
                int rBtnId = rgp_Manners.getCheckedRadioButtonId();
                Bundle data = new Bundle();
                data.putInt("DEFAULT_MANNER_R_SETTINGS",defaultManner);
                data.putBoolean("NO_MORE_R_BOX",isNoMoreBox);
                //快速复习情景下，只需LG一种模式。区别只在目标分组的选择优先条件。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FAST_RE_PICK, data);
                this.dismiss();//如果没有dismiss则从目标Activity返回后该dfg会还在。
                break;

            case R.id.btn_cancel_dfgFRP:
                this.dismiss();
                break;
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int rBtnId = rgp_Manners.getCheckedRadioButtonId();

        switch (buttonView.getId()){
            case R.id.ckb_setAsDefault_dfgFRP:
                if(isChecked){
                    if(rBtnId == R.id.rb_ms_first_dfgFRP) {
                        defaultManner = DEFAULT_MANNER_MS;
                    }else if(rBtnId == R.id.rb_rma_first_dfgFRP){
                        defaultManner = DEFAULT_MANNER_RMA;
                    }else {
                        defaultManner = DEFAULT_MANNER_TT;
                    }
                }else {
                    //不设为默认，填充以下值，ACTIVITY如果接到该值则不需向SP提交
                    defaultManner = DEFAULT_MANNER_UNDEFINED;
                }
                break;
            case R.id.ckb_noMoreTipBox_dfgFRP:
                if(isChecked){
                    isNoMoreBox = true;
                }else {
                    isNoMoreBox =false;
                }
                break;
        }
    }
}
