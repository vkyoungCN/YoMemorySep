package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;

import java.util.ArrayList;


/*
* 本DFG对应的是手动结束的情形，根据传入的列表产生不同处理逻辑：
* （首先显示还剩余多少时间，可以返回查看）
* ①有未完成的，提示会拆分；
* ②有未正确的，提示将记录错误一次；
* ③如果没有上述情况任一，则提示正确（？），也可返回查看。
*
* 确然后的跳转操作还是由Activity负责，所以不需要持有gid。
* */
public class LearningHandyFinishDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningHandyFinishDiaFragment";


    private ArrayList<Byte> emptyCards;
    private ArrayList<Byte> wrongCards;
    private int restSeconds;

    private TextView tvWrongInfo;
    private TextView tvEmptyInfo;
    private TextView tvRestTimeInfo;

    private OnGeneralDfgInteraction mListener;

    public LearningHandyFinishDiaFragment() {
        // Required empty public constructor
    }

    public static LearningHandyFinishDiaFragment newInstance(ArrayList<Integer> emptyCards,ArrayList<Integer> wrongCards,int restSeconds) {
        LearningHandyFinishDiaFragment fragment = new LearningHandyFinishDiaFragment();
        Bundle args = new Bundle();
        args.putSerializable("WRONG_CARD_INDEXES",wrongCards);
        args.putSerializable("EMPTY_CARD_INDEXES",emptyCards);
        args.putInt("REST_SECONDS",restSeconds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.wrongCards = (ArrayList<Byte>) getArguments().getSerializable("WRONG_CARD_INDEXES");
            this.emptyCards = (ArrayList<Byte>) getArguments().getSerializable("EMPTY_CARD_INDEXES");
            this.restSeconds = getArguments().getInt("REST_SECONDS");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.dfg_learning_handy_finish, container, false);
        rootView.findViewById(R.id.tvBtn_back_dfgLHFinish).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_confirm_dfgLHFinish).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_giveUp_dfgLHFinish).setOnClickListener(this);
        tvWrongInfo = rootView.findViewById(R.id.tv_wrong_dfgLHFinish);
        tvEmptyInfo = rootView.findViewById(R.id.tv_empty_dfgLHFinish);
        tvRestTimeInfo = rootView.findViewById(R.id.tv_restTime_dfgLHFinish);

        int wrongNum = wrongCards.size();
        int emptyNum = emptyCards.size();
        if(wrongNum==0){
            tvWrongInfo.setText(getResources().getString(R.string.wrong_info_all_correct));
        }else {
            tvWrongInfo.setText(String.format(getResources().getString(R.string.wrong_info_un_correct),wrongNum));
        }
        if(emptyNum==0){
            tvEmptyInfo.setText(getResources().getString(R.string.empty_info_all_correct));
        }else {
            tvEmptyInfo.setText(String.format(getResources().getString(R.string.empty_info_un_correct),emptyNum));
        }

        tvRestTimeInfo.setText(String.format(getResources().getString(R.string.rest_min_and_sec),restSeconds/60,restSeconds%60));

        return rootView;



    }

    @Override
    public void onClick(View v) {
        //不论点击的是确认还是取消或其他按键，直接调用Activity中实现的监听方法，
        // 将view的id传给调用方处理。
        switch (v.getId()){
            case R.id.tvBtn_confirm_dfgLHFinish:
                // 由LearningActivity根据其持有的状态列表具体判断拆分等操作。在此不判断不传递。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_FINISH_DFG_CONFIRM,null);
                dismiss();

                break;
            case R.id.tvBtn_back_dfgLHFinish:
                //返回查看，继续计时
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_FINISH_DFG_BACK,null);
                dismiss();

                break;

            case R.id.tvBtn_giveUp_dfgLHFinish:
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
