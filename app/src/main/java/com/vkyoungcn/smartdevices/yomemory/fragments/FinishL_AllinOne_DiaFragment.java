package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;

import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_ORDER;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_CREATE_RANDOM;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_AND_MERGE;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_EXTRA_NO_RECORDS;
import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.LEARNING_GENERAL;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class FinishL_AllinOne_DiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
//* 本DFG对应合并学习模式下的手动结束情形：
//* （首先显示还剩余多少时间，可以返回查看）
//* 需要传入：总量、未完成量。（若只传未完成量，则无法分辨是否是“一个也没写”的极端情况）
//* ①提示总量、未完成量，提示会拆分；（or全部完成、or全部未完成（隐藏确认键））
//* ②有未正确的，提示将记录错误一次；
//* ③如果没有上述情况任一，则提示正确（？），也可返回查看。
//*
//* 确然后的跳转操作还是由Activity负责，所以不需要持有gid。
//* 三种不同学习模式的手动结束DFG使用同一布局文件。
    private static final String TAG = "FinishL_AllinOne_DiaFragment";

    private int totalAmount;
    private int emptyAmount;
    private int wrongAmount;
    private int correctAmount;
    private int restSeconds;
    private int restMinutes;

    private int learningType;

    private TextView tvAmountReport;
    private TextView tvEmptyInfo;
    private TextView tvRestTimeInfo;
    private TextView btn_Confirm;
    private TextView tvBottomInfo;

    private OnGeneralDfgInteraction mListener;

    public FinishL_AllinOne_DiaFragment() {
        // Required empty public constructor
    }

    public static FinishL_AllinOne_DiaFragment newInstance(int totalAmount, int emptyAmount, int correctAmount, int wrongAmount, int restMinutes, int restSeconds, int learningType) {
        FinishL_AllinOne_DiaFragment fragment = new FinishL_AllinOne_DiaFragment();
        Bundle args = new Bundle();
        args.putInt(STR_TOTAL_AMOUNT,totalAmount);
        args.putInt(STR_WRONG_AMOUNT,wrongAmount);
        args.putInt(STR_EMPTY_AMOUNT,emptyAmount);
        args.putInt(STR_CORRECT_AMOUNT,correctAmount);
        args.putInt(STR_REST_SECONDS,restSeconds);
        args.putInt(STR_REST_MINUTES,restMinutes);
        args.putInt(STR_LEARNING_TYPE,learningType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.totalAmount = getArguments().getInt(STR_TOTAL_AMOUNT);
            this.wrongAmount = getArguments().getInt(STR_WRONG_AMOUNT);
            this.emptyAmount = getArguments().getInt(STR_EMPTY_AMOUNT);
            this.correctAmount = getArguments().getInt(STR_CORRECT_AMOUNT);
            this.restSeconds = getArguments().getInt(STR_REST_SECONDS);
            this.restMinutes = getArguments().getInt(STR_REST_MINUTES);
            this.learningType = getArguments().getInt(STR_LEARNING_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.dfg_finish_all_in_one, container, false);

        rootView.findViewById(R.id.tvBtn_back_dfgLmFinish).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_giveUp_dfgLmFinish).setOnClickListener(this);
        btn_Confirm = rootView.findViewById(R.id.tvBtn_confirm_dfgLmFinish);
        btn_Confirm.setOnClickListener(this);

        tvAmountReport = rootView.findViewById(R.id.tv_amountInfo_dfgLmFinish);
        tvRestTimeInfo = rootView.findViewById(R.id.tv_restTime_dfgLmFinish);
        tvBottomInfo = rootView.findViewById(R.id.tv_bottomInfo_dfgFinish);

        tvAmountReport.setText(String.format(getResources().getString(R.string.hs_end_report),totalAmount,correctAmount,wrongAmount,emptyAmount));

        if(restMinutes==0&&restSeconds==0){
            //已到时间（可能是timeUp触发的结束）
            tvRestTimeInfo.setText(getResources().getString(R.string.time_up1));
        }else {
            tvRestTimeInfo.setText(String.format(getResources().getString(R.string.rest_min_and_sec), restMinutes, restSeconds));
        }

        switch (learningType){
            case LEARNING_AND_MERGE:
                //底部显示“完成部分将被合并”
                tvBottomInfo.setText(getResources().getString(R.string.finish_will_be_merged));
                break;

            case LEARNING_GENERAL:
                //底部显示“若有未完成词汇，则将拆分分组”
                tvBottomInfo.setText(getResources().getString(R.string.finish_not_will_be_divided));
                break;
            case LEARNING_AND_CREATE_ORDER:
            case LEARNING_AND_CREATE_RANDOM:
                //底部显示“将根据完成部分生成新分组”
                tvBottomInfo.setText(getResources().getString(R.string.finish_will_be_created));
                break;
            case LEARNING_EXTRA_NO_RECORDS:
                //底部显示“额外学习不会产生学习记录”
                tvBottomInfo.setText(getResources().getString(R.string.finish_extra_with_no_records));
                break;
        }

        return rootView;



    }

    @Override
    public void onClick(View v) {
        //不论点击的是确认还是取消或其他按键，直接调用Activity中实现的监听方法，
        // 将view的id传给调用方处理。
        switch (v.getId()){
            case R.id.tvBtn_confirm_dfgLmFinish:
                // 由LearningActivity根据其持有的状态列表具体判断拆分等操作。在此不判断不传递。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_FINISH_DFG_CONFIRM,null);
                dismiss();

                break;
            case R.id.tvBtn_back_dfgLmFinish:
                //返回查看，继续计时
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_FINISH_DFG_BACK,null);
                dismiss();

                break;

            case R.id.tvBtn_giveUp_dfgLmFinish:
                //放弃，就当什么都没发生。通知Act，直接返回分组的Rv列表页。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_FINISH_DFG_GIVE_UP,null);
                dismiss();

                break;

        }
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

}
