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
public class LearningLessFourDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningLessFourDiaFragment";

    private OnLearningConfirmDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;

    public LearningLessFourDiaFragment() {
        // Required empty public constructor
    }

【增加功能，】
    【在本dfg中，显示可选的同级碎片分组（的id--数量）列表，提供选择框，提示已选总数量（提示未完成
    的分组保持不变、部分完成的分组会被拆分）。】（因而需要传递进来所有合适的分组id列表【或者不传递，由DFG负责，则要传context】）
    public static LearningLessFourDiaFragment newInstance(int groupId) {
        LearningLessFourDiaFragment fragment = new LearningLessFourDiaFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_less, container, false);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningLess);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningLess);

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
                Bundle bundle = new Bundle();
                bundle.putInt();
                mListener.onLearningConfirmDfgInteraction(OnLearningConfirmDfgInteraction.LEARNING_AND_MERGE,null);
                break;
            case R.id.btn_cancel_learningAddRandom:

                this.dismiss();
                break;
        }

    }
}
