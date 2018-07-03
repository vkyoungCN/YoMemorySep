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
* 本DFG对应的是“没有完全正确，但时间到了”的情形，有三种结束方式：
* ①结束，拆分
* ②结束，不正确的记错误一次，但不拆分；
* ③就当我根本没学过；
*
* 确然后的跳转操作还是由Activity负责，所以不需要持有gid。
* */
public class LearningTimeUpDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningTimeUpDiaFragme";


    private ArrayList<Byte> emptyCards;
    private ArrayList<Byte> wrongCards;
    private int restSeconds;

    private TextView tvWrongInfo;
    private TextView tvEmptyInfo;

    private OnGeneralDfgInteraction mListener;

    public LearningTimeUpDiaFragment() {
        // Required empty public constructor
    }

    public static LearningTimeUpDiaFragment newInstance(ArrayList<Integer> emptyCards,ArrayList<Integer> wrongCards) {
        LearningTimeUpDiaFragment fragment = new LearningTimeUpDiaFragment();
        Bundle args = new Bundle();
        args.putSerializable("WRONG_CARD_INDEXES",wrongCards);
        args.putSerializable("EMPTY_CARD_INDEXES",emptyCards);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.wrongCards = (ArrayList<Byte>) getArguments().getSerializable("WRONG_CARD_INDEXES");
            this.emptyCards = (ArrayList<Byte>) getArguments().getSerializable("EMPTY_CARD_INDEXES");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.dfg_learning_time_up, container, false);
        rootView.findViewById(R.id.tvBtn_confirm_dfgTimeUp).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_giveUp_dfgTimeUp).setOnClickListener(this);
        tvWrongInfo = rootView.findViewById(R.id.tv_wrong_dfgTimeUp);
        tvEmptyInfo = rootView.findViewById(R.id.tv_empty_dfgTimeUp);

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

        return rootView;

    }

    @Override
    public void onClick(View v) {
        //不论点击的是确认还是取消或其他按键，直接调用Activity中实现的监听方法，
        // 将view的id传给调用方处理。
        switch (v.getId()){
            case R.id.tvBtn_confirm_dfgTimeUp:
                // 由LearningActivity根据其持有的状态列表具体判断拆分等操作。在此不判断不传递。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_TIME_UP_DFG_CONFIRM,null);
                dismiss();

                break;

            case R.id.tvBtn_giveUp_dfgTimeUp:
                //放弃，就当什么都没发生。通知Act，直接返回分组的Rv列表页。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_TIME_UP_DFG_GIVE_UP,null);
                dismiss();

                break;

        }
    }


    /*public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }*/

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
