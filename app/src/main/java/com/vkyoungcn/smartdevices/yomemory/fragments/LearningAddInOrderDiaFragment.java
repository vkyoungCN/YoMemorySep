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
public class LearningAddInOrderDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningAddInOrderDiaFragment";

    private OnGeneralDfgInteraction mListener;

    private TextView tvCancel;
    private TextView tvConfirm;

    public LearningAddInOrderDiaFragment() {
        // Required empty public constructor
    }


    public static LearningAddInOrderDiaFragment newInstance() {
        LearningAddInOrderDiaFragment fragment = new LearningAddInOrderDiaFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_create_group, container, false);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningAddInOrder);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningAddInOrder);

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
            case R.id.btn_confirm_learningAddInOrder://创建新分组
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_AND_CREATE_ORDER,null);
                break;
            case R.id.btn_cancel_learningAddInOrder:

                this.dismiss();
                break;
        }

    }
}
