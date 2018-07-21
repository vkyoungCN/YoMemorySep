package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;

import java.text.SimpleDateFormat;
import java.util.Date;


/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class LearningGelDiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
//* 发起常规模式学习活动时弹出的确认对话框
//* 目前，从groups列表页和group单组详情页都能发起LG学习，所以如有修改，应对二者都予以改动。
// 、
    private static final String TAG = "LearningGelDiaFragment";
    private OnGeneralDfgInteraction mListener;

    private RVGroup rvGroup;
    private TextView tvCancel;
    private TextView tvConfirm;
    private TextView tvDescription;
    private TextView tvLastLearningTime;
    private CheckBox ckbUseNoOrder;

    private boolean isUsingOrder = true;//记录是否使用顺序方式（默认true，默认使用顺序方式）。可通过ckb更改为乱序。

    public LearningGelDiaFragment() {
        // Required empty public constructor
    }

    public static LearningGelDiaFragment newInstance(RVGroup rvGroup) {
        LearningGelDiaFragment fragment = new LearningGelDiaFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(STR_GROUP,rvGroup);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.rvGroup = getArguments().getParcelable(STR_GROUP);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_gel, container, false);

        tvDescription = (TextView) rootView.findViewById(R.id.tv_group_description_learningGel);
        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningGel);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningGel);
        tvLastLearningTime = (TextView) rootView.findViewById(R.id.tv_group_lastTime_learningGel);
        ckbUseNoOrder = (CheckBox) rootView.findViewById(R.id.ckb_noOrder_dfgLG);

        tvDescription.setText(rvGroup.getDescription());

        if(rvGroup.getLastLearningTime()==0){
            //没有复习记录
            tvLastLearningTime.setText(getResources().getString(R.string.no_lastLearningTime));

        }else {
            Date lastLearningTimeDate = new Date(rvGroup.getLastLearningTime());
            SimpleDateFormat sdFormat = new SimpleDateFormat(STR_DATE_PATTEN_1);
            String lastLearningTimeStr = sdFormat.format(lastLearningTimeDate);
            tvLastLearningTime.setText(lastLearningTimeStr);
        }
        //部分需要添加事件监听
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
        ckbUseNoOrder.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    isUsingOrder = false;
                }else {
                    isUsingOrder = true;
                }
            }
        });

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
            case R.id.btn_confirm_learningGel:
                //从rv中取数据，存入Bundle，最终交到学习页。
                Bundle bundle = new Bundle();
                bundle.putInt(STR_GROUP_ID_TO_LEARN,rvGroup.getId());
                if(!isUsingOrder){
                    //组内乱序模式
                    mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_GENERAL_INNER_RANDOM,bundle);
                }else {
                    //如果使用顺序
                    mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_GENERAL, bundle);
                }
                dismiss();
                break;

            case R.id.btn_cancel_learningGel:

                this.dismiss();
                break;
        }
    }

}
