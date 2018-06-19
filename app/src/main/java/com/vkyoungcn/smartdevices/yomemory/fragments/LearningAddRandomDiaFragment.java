package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;

@SuppressWarnings("all")
public class LearningAddRandomDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningAddRandomDiaFragment";

    private OnLearningConfirmDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;

    public LearningAddRandomDiaFragment() {
        // Required empty public constructor
    }


    public static LearningAddRandomDiaFragment newInstance() {
        LearningAddRandomDiaFragment fragment = new LearningAddRandomDiaFragment();

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
        if (context instanceof OnLearningConfirmDfgInteraction) {
            mListener = (OnLearningConfirmDfgInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLearningConfirmDfgInteraction");
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
                mListener.onLearningConfirmDfgInteraction(OnLearningConfirmDfgInteraction.LEARNING_AND_CREATE_RANDOM,null);
                break;
            case R.id.btn_cancel_learningAddRandom:

                this.dismiss();
                break;
        }

    }
}
