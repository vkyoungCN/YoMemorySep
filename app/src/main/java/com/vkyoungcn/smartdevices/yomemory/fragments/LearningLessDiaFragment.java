package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.CkbsChoseGroupsRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;

import java.util.ArrayList;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
【两个合并式弹出的对话框合并为一个，功能改造，通用化】
public class LearningLessDiaFragment extends DialogFragment
        implements View.OnClickListener,CompoundButton.OnCheckedChangeListener,Constants {
//* 发起合并式学习时弹出的确认对话框
//* 该对话框兼具待合并组的筛选功能（①选定目标MS、限制容量；②根据①得出的待选组，选出要参与合并的待合并组）
//* 选定的组发送回Activity并进一步后送发起学习
    private static final String TAG = "LearningLessDiaFragment";

    private OnGeneralDfgInteraction mListener;

    private ArrayList<FragGroupForMerge> groups = new ArrayList<>();//所有符合条件的待选组（从上一页传来）

    private TextView tvCancel;
    private TextView tvConfirm;

    private TextView tvTotalNum;//显示当前已经选中的组共有多少项items。
    private int triggerGroupNum = 0;//此外还需要保持原始数量（或者保持触发组整组信息？）
    private int totalChoseNum = 0;//此外还需要保持原始数量（或者保持触发组整组信息？）

    private TextView tvTriggerGroupid;//首行不放入Rv，因为其ckb不需监听器，rv中逻辑处理比较麻烦。
    private TextView tvTriggerGroupNum;//首行

    private CheckBox ckbAllCheck;//作为RV中各项的“全选”开关，放置在DFG中。
    boolean isCkbChangedByApp = false;//与ckbAll配合使用，平常置否，程序调用的setChecked中先置真，setChecked设置后再置否。
        //在rv中采用了isPressed判断。

    private RecyclerView mRv;
    RecyclerView.Adapter adapter;//需要在多个方法中操作，全局化

    public LearningLessDiaFragment() {
        // Required empty public constructor
    }

    public static LearningLessDiaFragment newInstance(ArrayList<FragGroupForMerge> groupsForMerge) {
        LearningLessDiaFragment fragment = new LearningLessDiaFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(STR_GROUPS,groupsForMerge);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.groups = (ArrayList<FragGroupForMerge>) savedInstanceState.getSerializable(STR_GROUPS);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_less, container, false);

        mRv = (RecyclerView) rootView.findViewById(R.id.rv_groups_chose_DfgLearningLess);
        tvTotalNum = (TextView) rootView.findViewById(R.id.tv_total_items_DfgLearningLess);
        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningLess);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningLess);
        ckbAllCheck = (CheckBox) rootView.findViewById(R.id.allCkb_dfgLess);

        ckbAllCheck.setOnCheckedChangeListener(this);

        tvTriggerGroupid = (TextView) rootView.findViewById(R.id.group_id_triggerGroup);
        tvTriggerGroupNum = (TextView) rootView.findViewById(R.id.group_num_triggerGroup);

        tvTriggerGroupid.setText(String.valueOf(groups.get(0).getId()));
        tvTriggerGroupNum.setText(String.valueOf(groups.get(0).getTotalItemsNum()));

        triggerGroupNum = groups.get(0).getTotalItemsNum();
        totalChoseNum = triggerGroupNum;
        tvTotalNum.setText(String.valueOf(triggerGroupNum));//暂时只有触发组的容量。

        groups.remove(0);//首个项目移除

        //Rv配置：LM、适配器
        mRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter= new CkbsChoseGroupsRvAdapter(groups,getActivity(),this);
        mRv.setAdapter(adapter);

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

    //由所含的mRv调用，改变下方Tv控件的值
    //传递加或减；以及改变量。
    public void changeTotalChoseNumTvStr(boolean isAdd,int deltaNum){
        if(isAdd){
            String num = String.valueOf(totalChoseNum+deltaNum);
            tvTotalNum.setText("已选总量："+ num);
        }else {
            String num = String.valueOf(totalChoseNum-deltaNum);
            tvTotalNum.setText("已选总量："+ num);
        }
    }

    //由所含的mRv调用，改变上方全选框的状态
    public void switchAllCheckBox(boolean isChecked){
        isCkbChangedByApp =true;
        ckbAllCheck.setChecked(isChecked);
        isCkbChangedByApp = false;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_confirm_learningLess://创建新分组
                //从rv中取数据，存入Bundle，最终交到学习页。
                Bundle bundle = new Bundle();
                bundle.putSerializable("IDS_GROUPS_READY_TO_MERGE",((CkbsChoseGroupsRvAdapter)mRv.getAdapter()).getIdsList());

                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_AND_MERGE,bundle);
                break;

            case R.id.btn_cancel_learningLess:

                this.dismiss();
                break;
        }

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //先检测是不是被程序触发
        if(isCkbChangedByApp){
            //程序触发的，即因Rv中各项CKB均置真或刚从全真中取消某项；此时没有操作
            //【计数改变的操作暂定由rv负责】

        }else {
            //用户手动触发
            if(isChecked){
                //通知Rv，将所有项目的ckb置真

                int totalNum = triggerGroupNum;
                for (FragGroupForMerge f :groups) {
                    f.setChecked(true);//其中各项设为选中（此时该数据集已经没有首项了，都是Rv的数据）
                     totalNum = totalNum + f.getTotalItemsNum();//计算数量
                }
                totalChoseNum = totalNum;//要保持数据和选择一致。
                adapter.notifyDataSetChanged();//通过改变数据集的方式使Rv中的显示改变（可能是最省力的方式）
                //下方文本设为新的总量值
                tvTotalNum.setText("已选总量："+String.valueOf(totalNum));

            }else {
                //手动取消所有ckbs的选中。通知Rv,将所有项目的ckb置否
                for (FragGroupForMerge f :groups) {
                    f.setChecked(false);
                }
                totalChoseNum = triggerGroupNum;//要保持数据和选择一致。

                adapter.notifyDataSetChanged();
                tvTotalNum.setText("已选总量："+String.valueOf(triggerGroupNum));

            }
        }
    }
}
