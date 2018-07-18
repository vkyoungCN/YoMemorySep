package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class LearningCreateRandomDiaFragment extends DialogFragment implements View.OnClickListener {
//* 发起创建式学习（顺序）时弹出的确认对话框
    private static final String TAG = "LearningCreateRandomDiaFragment";

    private OnGeneralDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;

    public LearningCreateRandomDiaFragment() {
        // Required empty public constructor
    }


    public static LearningCreateRandomDiaFragment newInstance() {
        LearningCreateRandomDiaFragment fragment = new LearningCreateRandomDiaFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_add_random, container, false);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningAddRandom);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningAddRandom);

        //部分需要添加事件监听
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);

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
            case R.id.btn_confirm_learningAddRandom://创建新分组
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_AND_CREATE_RANDOM,null);
                break;
            case R.id.btn_cancel_learningAddRandom:

                this.dismiss();
                break;
        }

    }
}
