package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.Ckbs2ChoseGroupsRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.RvMergeGroup;

import java.util.ArrayList;

@SuppressWarnings("all")
/*
* */
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class LearningMerge2DiaFragment extends DialogFragment
        implements View.OnClickListener,CompoundButton.OnCheckedChangeListener,Constants {
//* 合并式学习发起时弹出的确认框，在本框中由用户选取用于合并的来源分组
//* 提供了两个用于筛选的条件：MS、分组容量（容量有一个最高的合理限值，不应超过该值）
//* （暂定）在dfg中设置了条件点击“筛选”后，条件数据（通过dfg通用接口）发回Activity，由activity中
//* 的副线程从DB拉取指定数据；然后对DFG中的Rv区域进行更新（有数据更新、无数据显示tv:无符合条件的分组）
//
//* MS可以手动输入数值，也可以点击+/-调整；Amount只能+/-调整；
//* 传入的List数据源是指定MS下的所有分组信息，然后根据Amount在dfg内对数据源做调整
//* 也即只有MS的变化是需要交给Activity重新获取数据的，Amount的变化则不需要。
//

    private static final String TAG = "LearningMerge2DiaFragment";

    /* 常量 */
    public static final int MAX_TERM_AMOUNT = 12; //暂时设定为12;

    private OnGeneralDfgInteraction mListener;

    private ArrayList<RvMergeGroup> rvMergeGroups;//本dfg中Rv所需的主数据源
    private ArrayList<RvMergeGroup> groupsUnderFixedAmount;//本dfg中Rv所需的主数据源
    //groupsUnderFixedAmount中的数据选择范围和term_amount数据，以及tv中term_amount三者的数据必须一致联动。
    //注意，最终后送的数据其实是ArrayList<Integer>

    /* 控件区*/
    private EditText et_MS;
    private TextView tv_Amount;//不允许手动输入

    private TextView tvMsAdd;
    private TextView tvMsMinus;
    private TextView tvAmountAdd;
    private TextView tvAmountMinus;
//    private TextView tvBtnGoSelect;


    private TextView tvInfos;//底部信息条
    private TextView tvInfos_2;//附属的底部副信息条

    private CheckBox ckbAllCheck;//作为RV中各项的“全选”开关，放置在DFG中。

    private TextView tvCancel;
    private TextView tvConfirm;

    private RecyclerView recyclerView;
    private TextView tv_maskWaiting;
    private TextView tv_maskNodata;

    /* 业务逻辑变量 */
    int term_ms = 3;//为什么默认是3？【随便写的，不适合太小太大就是了】
    int term_amount = 7;//默认小于等于7的分组，最大暂定是12。
    private int minTermAmount = 1; //默认是1，但是当用于固定发起组场景时（如从分组详情页发起）值改为同发起组容量

    private int totalChoseNum = 0;//此外还需要保持原始数量（或者保持触发组整组信息？）
    boolean isCkbChangedByApp = false;//与ckbAll配合使用，平常置否，程序调用的setChecked中先置真，setChecked设置后再置否。也可采用isPressed判断。
    private int fixedGroupPosition = -1;//若从详情页发起合并学习（等类似场景），则发起组是固定的，必须强制选中；而且不可改动ms，容量限值也不能低于本组
//    private boolean noDataBellowThisAmount = false;//如果在发起方检索过发现传入的term_amount容量以内都没有分组数据，则传入标记true
    private boolean isBottomTvShowWords = false;

    private Ckbs2ChoseGroupsRvAdapter adapter;
//    private TextView tvTotalNum;//显示当前已经选中的组共有多少项items。
//    private int select_4or8 =4;//与上两个按钮配合，记录选择的值，以便rv区安排合理数据。默认4。
//    private RecyclerView mRvUpper;//上方的RV显示girdView形式的“类别选择：4/8,MS的组合”
//    private RecyclerView mRvDowner;//根据上方rv的选择，动态加载下方的列表。【发现还是用“上、下”比用类别内容更易区分】
//    FragGroupCategoryRvAdapter adapterUpper;//需要在多个方法中操作，全局化。
//    Ckbs2ChoseGroupsRvAdapter adapterDowner;
//    ArrayList<RvMergeGroup> singleArrayList;//下方RV的数据源，未初始化。





    public LearningMerge2DiaFragment() {
        // Required empty public constructor
    }

    //如果是从具体的组发起的合并学习，则目标Ms是既定的，并且提前准备好数据源一并传入。
    // 否则按默认值，并暂不加载Rv。
    public static LearningMerge2DiaFragment newInstance(Bundle data) {
        //初始时是不传递Rv数据源的
        LearningMerge2DiaFragment fragment = new LearningMerge2DiaFragment();
        /*Bundle bundle = new Bundle();

        bundle.putInt(STR_TERM_MS,term_ms);
        bundle.putInt(STR_TERM_AMOUNT,term_amount);*/

        fragment.setArguments(data);
        return fragment;

       /* //如果要采取bundle传递，似乎只能采用拆开传（如果是基础类型，可以传序列化）
        int n = 0;//计数用，计算一共传递了多少个<4的不同MS的ArrayList；（也用于给不同参数做标记）
        for (ArrayList<RvMergeGroup> arrayList : originGroupsForMerge[0]){
            n++;
            bundle.putParcelableArrayList("KEY_4_"+n,arrayList);
        }

        int m = 0;//同上
        for (ArrayList<RvMergeGroup> arrayList : originGroupsForMerge[1]){
            m++;
            bundle.putParcelableArrayList("KEY_8_"+m,arrayList);
        }*/
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments()!=null){
            this.term_ms = savedInstanceState.getInt(STR_TERM_MS);
            this.term_amount = savedInstanceState.getInt(STR_TERM_AMOUNT);
            this.rvMergeGroups = savedInstanceState.getParcelableArrayList(STR_RV_MERGE_GROUP);
            this.fixedGroupPosition = savedInstanceState.getInt(STR_FIXED_GROUP_POSITION);
//            this.noDataBellowThisAmount = savedInstanceState.getBoolean(STR_NO_DATA_BELLOW_THIS_AMOUNT,false);

        }
        groupsUnderFixedAmount = new ArrayList<>();//初始化
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_merge_2, container, false);

        et_MS = rootView.findViewById(R.id.et_msTermResultWindow_dfgLM);
        et_MS.setText(String.valueOf(term_ms));//如果传入的有数据则设为了传入的，否则设为了默认的。

        tv_Amount = rootView.findViewById(R.id.tv_amountTermResultWindow_dfgLM);
        tv_Amount.setText(String.valueOf(term_amount));//如果传入的有数据则设为了传入的，否则设为了默认的。

        tvMsAdd = rootView.findViewById(R.id.tv_msTermAdd_dfgLM);
        tvMsMinus = rootView.findViewById(R.id.tv_msTermMinus_dfgLM);
        tvAmountAdd = rootView.findViewById(R.id.tv_amountTermAdd_dfgLM);
        tvAmountMinus = rootView.findViewById(R.id.tv_amountTermMinus_dfgLM);

        //    private TextView tvBtnGoSelect;
        tvInfos = rootView.findViewById(R.id.tv_bottomInfo_dfgLM);//底部信息条
        tvInfos_2 = rootView.findViewById(R.id.tv_bottomInfo2_dfgLM);//附属的底部副信息条

        ckbAllCheck = rootView.findViewById(R.id.ckb_allCkeck_dfgLM);//作为RV中各项的“全选”开关，放置在DFG中。
        recyclerView = rootView.findViewById(R.id.rv_groupsForSelect_dfgLM);
        tv_maskWaiting = rootView.findViewById(R.id.tv_prepareData_dfgLM);
        tv_maskNodata = rootView.findViewById(R.id.tv_noGroupMatch_dfgLM);
        ckbAllCheck = (CheckBox) rootView.findViewById(R.id.ckb_all_DfgLearningMerge);

        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_dfgLearningMerge);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_dfgLearningMerge);

        tvMsAdd.setOnClickListener(this);
        tvMsMinus.setOnClickListener(this);
        tvAmountAdd.setOnClickListener(this);
        tvAmountMinus.setOnClickListener(this);
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
        ckbAllCheck.setOnCheckedChangeListener(this);
        et_MS.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //（改变前）无操作
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //改变中（无操作）
            }

            @Override
            public void afterTextChanged(Editable s) {
                //文本发生改变后，获取文本，转换；发送给调用方获取新数据源
                //【存在一点可能的问题：输入两位数的第一位后显然可能就已触发，此时如果丢失焦点则两位数无法输入】
                term_ms = Integer.parseInt(et_MS.getText().toString());//xml已设置了只允许输入数字。
                Bundle data =  new Bundle();
                data.putInt(STR_NEW_MS_FOR_FETCH,term_ms);

                tv_maskNodata.setVisibility(View.GONE);
                tv_maskWaiting.setVisibility(View.VISIBLE);//可以叠加显示。
                //rv无论显隐皆可，不做额外调整

                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FETCH_NEW_GROUPS_INFO_FOR_MERGE,data);
            }
        });


        //传入方已知在此数量限之下都没有数据，则下限上提。
       /* if(noDataBellowThisAmount){
            minTermAmount = term_amount;
        }*/

        if(rvMergeGroups !=null){
            //只有在传入了数据后才会非空，此外都是未传数据，不处理。
            if(rvMergeGroups.isEmpty()){
                //传入的是空数据集，即无符合的项目
                recyclerView.setVisibility(View.GONE);
                tv_maskNodata.setVisibility(View.VISIBLE);
            }else {
                if(fixedGroupPosition == -1) {
                    //非限定模式

                    //初始化适配器
                    //将主数据源中的符合≤term_amount的数据加入到groupsUFA列表中，作为下方适配器的真正数据源
                    getGroupsUnderFixedAmount(term_amount);
                    adapter = new Ckbs2ChoseGroupsRvAdapter(groupsUnderFixedAmount, this,fixedGroupPosition);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    recyclerView.setAdapter(adapter);

                    //根据数据源初始数据（可能有初始选中（目前不可能））
                    adapter.resetRecords();
                }else {
                    //限定：发起组强制选中；不可改动ms，容量限值也不能低于本组
                    rvMergeGroups.get(fixedGroupPosition).setChecked(true);

                    minTermAmount = rvMergeGroups.get(fixedGroupPosition).getSize();
                    tvMsMinus.setClickable(false);
                    tvMsAdd.setClickable(false);
                    et_MS.setClickable(false);
                    //通过这种不让限制改变数据源的方式使发起组保持留在rv中，同时要对adapter进行设置，不允许取消

                    //将主数据源中的符合≤term_amount的数据加入到groupsUFA列表中，作为下方适配器的真正数据源
                    getGroupsUnderFixedAmount(term_amount);
                    adapter = new Ckbs2ChoseGroupsRvAdapter(groupsUnderFixedAmount, this,fixedGroupPosition);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    recyclerView.setAdapter(adapter);

                    //根据数据源初始记录数据
                    adapter.resetRecords();

                }
            }
        }

        return rootView;

        /*//从传来的初始数据中整理本adp所需的数据
        ArrayList<ModelForGRv> tempListForGird_4 = new ArrayList<>();//4、8分开
        ArrayList<ModelForGRv> tempListForGird_8 = new ArrayList<>();//4、8分开

        int index_4=0,index_8 =0;//计数用【仅在按序排列时有效】
        for (ArrayList<RvMergeGroup> alf : groupsInTwoDimensionArray[0]) {//这是4的
            if(alf.size()==0) continue;//为0就不添加了。【在这里处理】
            tempListForGird_4.add(new ModelForGRv(index_4, alf.size()));
        }

        for (ArrayList<RvMergeGroup> alf : groupsInTwoDimensionArray[1]) {//这是8的
            if(alf.size()==0) continue;//为0就不添加了。【在这里处理】
            tempListForGird_8.add(new ModelForGRv(index_8, alf.size()));
        }

        adapterUpper = new FragGroupCategoryRvAdapter(tempListForGird_4,this);
        mRvUpper.setAdapter(adapterUpper);



        mRvDowner.setLayoutManager(new LinearLayoutManager(getActivity()));
        singleArrayList = new ArrayList<>();//完成实例化，但暂无数据。
        adapterDowner= new Ckbs2ChoseGroupsRvAdapter(singleArrayList,this);
        mRvDowner.setAdapter(adapterDowner);//但是这时应是不显示的，没有数据。在点击上方Grv区后下方才有实际数据确定好。*/
    }

    /*
    * 用于上方Grv区的临时数据模型
    * */
/*
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
*/


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
    //传递总量。
    public void changeTotalChoseNumTvStr(int choseAmount){
        totalChoseNum = choseAmount;
        tvInfos.setText(String.format(getResources().getString(R.string.hs_totalNum),totalChoseNum));

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
            case R.id.tv_amountTermAdd_dfgLM:
                //要对当前已指定MS的数据集进行再操作，需要先判断原数据集非空
                if(rvMergeGroups==null){
                    //说明传进来的就是空，其他逻辑已经负责将rv区显示改为无数据，在此不做处理。
                    Toast.makeText(getContext(), "数据集为空，请先改动MS条件", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    if(term_amount < MAX_TERM_AMOUNT){
                        //未超上限，可以调整
                        term_amount++;

                        //tv显示同步改变
                        tv_Amount.setText(String.valueOf(term_amount));
                        //上限增加的话，不需要清空旧数据
                        int count = 0;//记录新增了多少项目
                        for (RvMergeGroup g :rvMergeGroups) {
                            if (g.getSize() == term_amount) {
                                //上限（即允许小于+等于该值的所有项目）+1，只需将等于该值的项目加入即可
                                groupsUnderFixedAmount.add(g);
                                count++;
                            }
                        }
                        //通知数据集改变
                        adapter.notifyItemRangeInserted((groupsUnderFixedAmount.size()-1-count),count);
                        //增加记录时，adpter的原有记录数据可以不清空

                    }else {
                        //不建议对更大容量的分组进行合并
                        Toast.makeText(getContext(), "不建议对更大容量的分组进行合并", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.tv_amountTermMinus_dfgLM:
                if(rvMergeGroups==null){
                    //说明传进来的就是空，其他逻辑已经负责将rv区显示改为无数据，在此不做处理。
                    Toast.makeText(getContext(), "数据集为空，请先改动MS条件", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    if(term_amount > minTermAmount){
                        //最小可设为1。
                        int count = 0;//记录移除了多少项目
                        //将旧上限对应数据移除
                        for (RvMergeGroup g :groupsUnderFixedAmount) {
                            if (g.getSize() == term_amount) {
                                //上限（即允许小于+等于该值的所有项目）+1，只需将等于该值的项目加入即可
                                groupsUnderFixedAmount.remove(g);
                                count++;
                            }
                        }

                        //操作完成后，条件数值一致化。
                        term_amount--;
                        //tv显示同步改变
                        tv_Amount.setText(String.valueOf(term_amount));

                        //通知数据集改变
                        adapter.notifyItemRangeRemoved((groupsUnderFixedAmount.size()),count);//移除点之后起算吗？【暂未查API】
                        //重置adapter中的记录数据（版本2：按当前保留列表计算）
                        adapter.resetRecords();

                    }else {
                        //下限
                        Toast.makeText(getContext(), "容量限值最少为1", Toast.LENGTH_SHORT).show();
                    }
                }

                break;

            case R.id.tv_msTermAdd_dfgLM:
                term_ms++;//没有上限
                et_MS.setText(String.valueOf(term_ms));//显示要同步改变哦

                Bundle data =  new Bundle();
                data.putInt(STR_NEW_MS_FOR_FETCH,term_ms);

                tv_maskNodata.setVisibility(View.GONE);
                tv_maskWaiting.setVisibility(View.VISIBLE);//可以叠加显示。
                //rv无论显隐皆可，不做额外调整

                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FETCH_NEW_GROUPS_INFO_FOR_MERGE,data);
                break;

            case R.id.tv_msTermMinus_dfgLM:
                if(term_ms<=1){
                    //要求最小设定为1
                    Toast.makeText(getContext(), "不能再小了哦", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    term_ms--;
                    et_MS.setText(String.valueOf(term_ms));//显示要同步改变哦

                    Bundle data_2 = new Bundle();
                    data_2.putInt(STR_NEW_MS_FOR_FETCH, term_ms);

                    tv_maskNodata.setVisibility(View.GONE);
                    tv_maskWaiting.setVisibility(View.VISIBLE);//可以叠加显示。
                    //rv无论显隐皆可，不做额外调整

                    mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FETCH_NEW_GROUPS_INFO_FOR_MERGE, data_2);
                }
                break;

            case R.id.btn_confirm_dfgLearningMerge:
                Bundle bundle = new Bundle();
                bundle.putSerializable(STR_IDS_GROUPS_READY_TO_MERGE,(adapter.getIdsList()));

                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.LEARNING_AND_MERGE,bundle);
                break;

            case R.id.btn_cancel_dfgLearningMerge:
                this.dismiss();
                break;
        }

    }

    /* 根据指定的amount条件调整实际数据源集合 */
    private void getGroupsUnderFixedAmount(int term_amount){
        groupsUnderFixedAmount.clear();
        for (RvMergeGroup g :rvMergeGroups) {
            if (g.getSize() <= term_amount) {
                groupsUnderFixedAmount.add(g);
            }
        }
        //都是全局变量不需传递。

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //全选框的操作

        //先检测是不是被程序触发
        if(isCkbChangedByApp){
            //程序触发的，即因Rv中各项CKB均置真或刚从全真中取消某项；此时没有操作
            //不改变文本（而由rv负责），也不触发对分项ckb的改动。
        }else {
            //用户手动设置
            if(isChecked){
                //通知Rv，将所有项目的ckb置真

                int totalNum = 0;//准备计数
                for (RvMergeGroup f :groupsUnderFixedAmount) {
                    f.setChecked(true);//其中各项设为选中
                     totalNum = totalNum + f.getSize();//计算数量
                }
                totalChoseNum = totalNum;//要保持数据和选择一致。
                adapter.notifyDataSetChanged();//通过改变数据集的方式使Rv中的显示改变（可能是最省力的方式）

                //下方文本设为新的总量值
                tvInfos.setText(String.format(getResources().getString(R.string.hs_totalNum),totalChoseNum));

            }else {
                //手动取消全选。通知Rv,将所有项目的ckb置否
                if(fixedGroupPosition==-1) {
                    //通常模式
                    for (RvMergeGroup f : groupsUnderFixedAmount) {
                        f.setChecked(false);
                    }
                    totalChoseNum = 0;//要保持数据和选择一致（以备其他方法使用时数值正确）。

                    adapter.notifyDataSetChanged();
                    tvInfos.setText(String.format(getResources().getString(R.string.hs_totalNum), totalChoseNum));
                }else {
                    //有发起组，发起组不能取消
                    for (RvMergeGroup f : groupsUnderFixedAmount) {
                        f.setChecked(false);
                    }
                    groupsUnderFixedAmount.get(fixedGroupPosition).setChecked(true);
                    totalChoseNum = groupsUnderFixedAmount.get(fixedGroupPosition).getSize();

                    adapter.notifyDataSetChanged();
                    tvInfos.setText(String.format(getResources().getString(R.string.hs_totalNum), totalChoseNum));

                }

            }
        }

    }


    /* 外部Activity根据本dfg的请求按新ms组织好新数据源后，通过本方法传入*/
    public void changetListAsMsChanged(ArrayList<RvMergeGroup> newList){
        if(newList.isEmpty()){
            //无符合条件的数据
            rvMergeGroups.clear();//直接清空
            adapter.notifyDataSetChanged();

            //要将适配器中的记录变量清空以保持一致。
            adapter.initRecords();

            tv_maskWaiting.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            tv_maskNodata.setText(View.VISIBLE);

        }else {
            //有数据
            tv_maskWaiting.setVisibility(View.GONE);
            tv_maskNodata.setVisibility(View.GONE);

            rvMergeGroups = newList;
            //根据新数据源，及既定的term_amount设置用于列表显示的实际数据源
            getGroupsUnderFixedAmount(Integer.parseInt(tv_Amount.getText().toString()));

            //由于数据源换新，记录变量要清空。
            adapter.initRecords();

            adapter.notifyDataSetChanged();
            recyclerView.setVisibility(View.VISIBLE);

        }


    }

    //如果只选定了一条，则显示下方信息“将按照普通模式处理”。由rv适配器调用
    public void showBottomTv(boolean show){
        if(show){
            if(isBottomTvShowWords){
                //已经置显示，则退出。（毕竟高开销）
                return;
            }

            tvInfos_2.setText(getResources().getString(R.string.one_group_will_as_lg));
            isBottomTvShowWords = true;
        }else {
            if(!isBottomTvShowWords){
                //已经置空了
                return;
            }
            tvInfos_2.setText("");
            isBottomTvShowWords = false;
        }
    }

    /*
     * 当上方Grid区域单项点击后，下方mRV要改变数据
     * */
    /*public void choseCategory(int memoryStage){
        int firstIndex = select_4or8/4-1;

        //在上方点击后，根据选定的MS、当前4/8值，确定（用于下方mRv的）新数据集。
         singleArrayList = groupsInTwoDimensionArray[firstIndex][memoryStage];
         adapterDowner.notifyDataSetChanged();//通知改变

    }*/

}
