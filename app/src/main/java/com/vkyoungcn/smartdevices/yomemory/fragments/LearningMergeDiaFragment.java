package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.CkbsChoseGroupsRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.adapters.FragGroupCategoryRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;

import java.util.ArrayList;

@SuppressWarnings("all")
public class LearningMergeDiaFragment extends DialogFragment implements View.OnClickListener,CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LearningMergeDiaFragment";

    private OnLearningConfirmDfgInteraction mListener;

    private ArrayList<FragGroupForMerge>[][] groupsInTwoDimensionArray;//在本DFG的设计方案下，需要这样一个结构来装载数据。
//    private ArrayList<FragGroupForMerge> groups = new ArrayList<>();//所有符合条件的待选组（从上一页传来）
    //【最终回传给GofM页面（然后从该页面发起向学习页的跳转）的数据其实是ArrayList<Integer>】

    private TextView tvCancel;
    private TextView tvConfirm;

    private TextView tvTotalNum;//显示当前已经选中的组共有多少项items。
//    private int triggerGroupNum = 0;//此外还需要保持原始数量（或者保持触发组整组信息？）
    private int totalChoseNum = 0;//此外还需要保持原始数量（或者保持触发组整组信息？）

//    private TextView tvTriggerGroupid;//首行不放入Rv，因为其ckb不需监听器，rv中逻辑处理比较麻烦。
//    private TextView tvTriggerGroupNum;//首行

    private CheckBox ckbAllCheck;//作为RV中各项的“全选”开关，放置在DFG中。
    boolean isCkbChangedByApp = false;//与ckbAll配合使用，平常置否，程序调用的setChecked中先置真，setChecked设置后再置否。
        //在rv中采用了isPressed判断。

    private RecyclerView mRvCategory;//上方的RV显示girdView形式的“类别选择：4/8,MS的组合”
    private RecyclerView mRvGroups;//根据上方rv的选择，动态加载下方的列表。
    RecyclerView.Adapter adapterUpper;//需要在多个方法中操作，全局化。
    RecyclerView.Adapter adapterDowner;

    public LearningMergeDiaFragment() {
        // Required empty public constructor
    }


    public static LearningMergeDiaFragment newInstance(ArrayList<FragGroupForMerge>[][] originGroupsForMerge) {
        LearningMergeDiaFragment fragment = new LearningMergeDiaFragment();
        Bundle bundle = new Bundle();

        //如果要采取bundle传递，似乎只能采用拆开传（如果是基础类型，可以传序列化）
        int n = 0;//计数用，计算一共传递了多少个<4的不同MS的ArrayList；（也用于给不同参数做标记）
        for (ArrayList<FragGroupForMerge> arrayList : originGroupsForMerge[0]){
            n++;
            bundle.putParcelableArrayList("KEY_4_"+n,arrayList);
        }

        int m = 0;//同上
        for (ArrayList<FragGroupForMerge> arrayList : originGroupsForMerge[1]){
            m++;
            bundle.putParcelableArrayList("KEY_8_"+m,arrayList);
        }

        bundle.putInt("KEY_N",n);
        bundle.putInt("KEY_M",m);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments()!=null){
            int msNumFour = savedInstanceState.getInt("KEY_N");
            int msNumEight = savedInstanceState.getInt("KEY_M");

            int i,j;
            for(i=0;i<=msNumFour;i++){
                groupsInTwoDimensionArray[0][i] = savedInstanceState.getParcelableArrayList("KEY_4_"+i);
            }
            for(j=0;j<=msNumFour;j++){
                groupsInTwoDimensionArray[1][j] = savedInstanceState.getParcelableArrayList("KEY_8_"+j);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_merge, container, false);

        mRvCategory = (RecyclerView) rootView.findViewById(R.id.rv_category_chose_DfgLearningMerge);
        mRvGroups =  (RecyclerView) rootView.findViewById(R.id.rv_group_chose_DfgLearningMerge);
        tvTotalNum = (TextView) rootView.findViewById(R.id.tv_total_items_DfgLearningMerge);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_dfgLearningMerge);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_dfgLearningMerge);
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);

        ckbAllCheck = (CheckBox) rootView.findViewById(R.id.ckb_all_DfgLearningMerge);
        ckbAllCheck.setOnCheckedChangeListener(this);


        mRvCategory.setLayoutManager(new GridLayoutManager(getActivity(),3));

        //从传来的初始数据中整理本adp所需的数据
        ArrayList<ModelForGRv> tempListForGird_4 = new ArrayList<>();//4、8分开
        ArrayList<ModelForGRv> tempListForGird_8 = new ArrayList<>();//4、8分开

        int index_4=0,index_8 =0;//计数用【仅在按序排列时有效】
        for (ArrayList<FragGroupForMerge> alf : groupsInTwoDimensionArray[0]) {//这是4的
            if(alf.size()==0) continue;//为0就不添加了。【在这里处理】
            tempListForGird_4.add(new ModelForGRv(index_4, alf.size()));
        }

        for (ArrayList<FragGroupForMerge> alf : groupsInTwoDimensionArray[1]) {//这是8的
            if(alf.size()==0) continue;//为0就不添加了。【在这里处理】
            tempListForGird_8.add(new ModelForGRv(index_8, alf.size()));
        }

        FragGroupCategoryRvAdapter categoryAdapter = new FragGroupCategoryRvAdapter(tempListForGird_4,getActivity(),this);
        mRvCategory.setAdapter(categoryAdapter);



        mRvGroups.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter= new CkbsChoseGroupsRvAdapter(groups,getActivity(),this);
        mRv.setAdapter(adapter);


        return rootView;
    }

    public class ModelForGRv{
        int Ms = 0;
        int howManyGroups = 0;

        public ModelForGRv(int ms, int howManyGroups) {
            Ms = ms;
            this.howManyGroups = howManyGroups;
        }

        public int getMs() {
            return Ms;
        }

        public void setMs(int ms) {
            Ms = ms;
        }

        public int getHowManyGroups() {
            return howManyGroups;
        }

        public void setHowManyGroups(int howManyGroups) {
            this.howManyGroups = howManyGroups;
        }
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
            case R.id.btn_confirm_learningAddRandom://创建新分组
                //从rv中取数据，存入Bundle，最终交到学习页。
                Bundle bundle = new Bundle();
                bundle.putSerializable("IDS_GROUPS_READY_TO_MERGE",((CkbsChoseGroupsRvAdapter)mRv.getAdapter()).getIdsList());

                mListener.onLearningConfirmDfgInteraction(OnLearningConfirmDfgInteraction.LEARNING_AND_MERGE,bundle);
                break;

            case R.id.btn_cancel_learningAddRandom:

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

    /*
    * 当上方Grid区域单项点击后，下方mRV要改变数据
    * */
    public void choseCategory(int memoryStage){

    }


}
