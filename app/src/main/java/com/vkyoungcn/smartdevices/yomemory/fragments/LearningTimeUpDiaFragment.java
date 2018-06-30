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


    private ArrayList<Boolean> itemsWrightOrWrong;
    private TextView tvLearningStatus;

    private OnGeneralDfgInteraction mListener;

    public LearningTimeUpDiaFragment() {
        // Required empty public constructor
    }

    public static LearningTimeUpDiaFragment newInstance(ArrayList<Boolean> itemsWrightOrWrong) {
        LearningTimeUpDiaFragment fragment = new LearningTimeUpDiaFragment();
        Bundle args = new Bundle();
        args.putSerializable("ITEMS_CORRECT_OR_NOT",itemsWrightOrWrong);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.itemsWrightOrWrong = (ArrayList<Boolean>) getArguments().getSerializable("ITEMS_CORRECT_OR_NOT");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.dfg_learning_time_up, container, false);
        rootView.findViewById(R.id.tvBtn_confirm_div_timeEndingDfg).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_confirm_err_timeEndingDfg).setOnClickListener(this);
        rootView.findViewById(R.id.tvBtn_anotherChoice_timeEndingDfg).setOnClickListener(this);
        tvLearningStatus = rootView.findViewById(R.id.tv_status_timeEndingDfg);

        int rightNum = 0;
        for (boolean b :
                itemsWrightOrWrong) {
            if (b) {
                rightNum++;
            }
        }
        tvLearningStatus.setText(String.format(getResources().getString(R.string.status_timeUpDfg),itemsWrightOrWrong.size(),rightNum));

        return rootView;

    }

    @Override
    public void onClick(View v) {
        //不论点击的是确认还是取消或其他按键，直接调用Activity中实现的监听方法，
        // 将view的id传给调用方处理。
        switch (v.getId()){
            case R.id.tvBtn_confirm_div_timeEndingDfg:
                //通知Activity，采取拆分处理
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.TIME_UP_CONFIRM_DIVIDE,null);
                dismiss();

                break;

            case R.id.tvBtn_confirm_err_timeEndingDfg:
                //通知Activity，采取相应item增记一次错误的方式处理
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.TIME_UP_CONFIRM_ADD_ERR,null);
                dismiss();

                break;

            case R.id.tvBtn_anotherChoice_timeEndingDfg:
                //放弃，就当什么都没发生。通知Act，直接返回分组的Rv列表页。
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.TIME_UP_DISCARD,null);
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
