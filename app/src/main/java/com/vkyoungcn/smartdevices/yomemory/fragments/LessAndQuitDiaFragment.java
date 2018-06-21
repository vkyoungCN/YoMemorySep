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
/*
* 在分组详情页，点击“学习”后，如果判断该分组的容量小于5个——则提示应从分组列表页点击学习
* 以合并方式进行；（本页未持有其他全部分组的信息，因而难以检索其他碎片组的信息，不适宜进行筛选跳转；
* 而若以跳转回列表页方式处理，则又因无Mission信息不满足目标页基本要求；所以暂时只是提示——取消。）
* 【事实上也可让DFG确定后，直接让本页dismiss，同时携带gid返回上一页（列表页）即可实现上述设想（待）】
* */
public class LessAndQuitDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningGelDiaFragment";
    private TextView tvConfirm;

    public LessAndQuitDiaFragment() {
        // Required empty public constructor
    }

    public static LessAndQuitDiaFragment newInstance(int groupId) {
        LessAndQuitDiaFragment fragment = new LessAndQuitDiaFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_less_in_gd, container, false);

        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningGel);
        tvConfirm.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_confirm_learningGel:
                this.dismiss();
                break;
        }
    }

}
