package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMerge2DiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMergeDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.RvMergeGroup;

import java.util.ArrayList;
import java.util.List;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class Ckbs2ChoseGroupsRvAdapter extends RecyclerView.Adapter<Ckbs2ChoseGroupsRvAdapter.ViewHolder> {
//* 用于在发起合并学习前弹出的确认对话框中选定若干个源分组
//* 每条项目带有一个复选框（CheckBox），实现了全选和取消全选逻辑
//

    private static final String TAG = "Ckbs2ChoseGroupsRvAdapter";
    private List<RvMergeGroup> groups;//数据源
    private LearningMerge2DiaFragment dfg;//保持一个到对话框的引用，以便改变对话框的UI（由DFG提供改变UI的实际调用方法）
    private int keepCheckedPosition = -1;//发起组固定的场景（如从分组详情页发起）需要对发起组保持强制选中
    private int howManyGroupsChecked = 0;//（除已移除的触发组外，已选中了多少个分组）用于判断是否是全选（从而影响对话框中的全选ckb）
    private ArrayList<Integer> idsList = new ArrayList<>();//最终外传/回传的数据
    private int choseAmount = 0;

    public class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        private final TextView gIndex;
        private final TextView gDescription;
        private final TextView size;
        private final CheckBox checkBox;

        private ViewHolder(View itemView) {
            super(itemView);
            gIndex = itemView.findViewById(R.id.index_rvMergeChoose);
            gDescription = itemView.findViewById(R.id.description_rvMergeChoose);
            size = itemView.findViewById(R.id.groupNum_rvMergeChoose);
            checkBox = itemView.findViewById(R.id.ckb_isChose_rvMergeChoose);

            checkBox.setOnCheckedChangeListener(this);

        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public TextView getgIndex() {
            return gIndex;
        }

        public TextView getgDescription() {
            return gDescription;
        }

        public TextView getSize() {
            return size;
        }



        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!buttonView.isPressed()){
                //未被按下，是程序触发，不能有常规动作（避免连续改写文本）【未测试，不知是否存在此问题】
                // （总量文本的改变等任务交由DFG处理。）但是，回传的idList仍要正确改写。
                if(isChecked) {
                    /*idsList.clear();//先清除再全加入
                    for (RvMergeGroup f : groups) {
                        idsList.add(f.getId());
                    }*/
                    int currentId = groups.get(getAdapterPosition()).getId();
                    int currentSize = groups.get(getAdapterPosition()).getSize();
                    if(idsList.indexOf(currentId) == -1){
                        //目前在结果列表内不存在本id，则加入
                        idsList.add(currentId);
                        choseAmount+=currentSize;
                    }
                    /* 【疑问】程序触发的全选，对于已经处于选中状态者是否会重复触发？*/
                    Log.i(TAG, "onCheckedChanged: adapter position="+getAdapterPosition());

                    //如果存在重复触发，则组的计数总量保持就比较困难
                    //其触发场景无非就是全选、全取消，因而直接设置下列数据
                    if(howManyGroupsChecked != groups.size()){
                        howManyGroupsChecked = groups.size();
                        //全选的外部dfg文本改变由dfg负责（需要遍历，耗时）
                    }


                }else {
                    int currentId = groups.get(getAdapterPosition()).getId();
                    int currentSize = groups.get(getAdapterPosition()).getSize();
                    if(idsList.indexOf(currentId) != -1){
                        //目前在结果列表内存在本id，则移除
                        idsList.remove((Integer) currentId);
                        choseAmount-=currentSize;
                    }

                    if(howManyGroupsChecked != 0){
                        howManyGroupsChecked = 0;
                    }

                }
                return;//修改结果列表即可，后续不执行，尤其是改写文本的操作。
                //【自】虽是程序触发的改变，但是估计各条项会逐一执行监听，因而仍然要逐条各自处理；
                // 不能“一次性”处理（那样可能会产生多次全局操作）
            }
            if(isChecked) {
                //将选中项目（的id）加入List
                int currentId = groups.get(getAdapterPosition()).getId();
                int currentSize = groups.get(getAdapterPosition()).getSize();
                if(idsList.indexOf(currentId) == -1){
                    //目前在结果列表内不存在本id，则加入
                    idsList.add(currentId);
                    choseAmount+=currentSize;
                }

                //判断选中后是否全选
                howManyGroupsChecked++;
                if(howManyGroupsChecked ==groups.size()){
                    //全选了，需要触发外部Dfg的allCkb框（的选中）
                    dfg.switchAllCheckBox(true);
                    // 程序触发的全选也由本Rv负责改写文本（见下方）。
                }

                //改变外部dfg的文本
                dfg.changeTotalChoseNumTvStr(choseAmount);

            }else {
                //取消某项时，将其从List移除
                //如果是需保持的项，直接退出。
                if(getAdapterPosition() == keepCheckedPosition){
                    return;
                }
                int currentId = groups.get(getAdapterPosition()).getId();
                int currentSize = groups.get(getAdapterPosition()).getSize();

                if(idsList.indexOf(currentId) != -1){
                    //目前在结果列表内存在本id，则移除
                    idsList.remove((Integer) currentId);
                    choseAmount-=currentSize;
                }

                howManyGroupsChecked--;
                if(howManyGroupsChecked == groups.size()-1){
                    //说明刚刚从全选状态解除（并且是手动点击），需要触发外部DFG的allCkb框（解除选中）
                    //如果原本已经是0项，则根本不能触发，此逻辑没问题。
                    dfg.switchAllCheckBox(false);
                }

                //改变外部dfg的文本
                dfg.changeTotalChoseNumTvStr(choseAmount);

            }
        }
    }

    public ArrayList<Integer> getIdsList() {
        return idsList;
    }

    public Ckbs2ChoseGroupsRvAdapter(List<RvMergeGroup> groups, LearningMerge2DiaFragment dfg,int keepCheckedPosition) {
        this.groups = groups;
        this.dfg = dfg;
        this.keepCheckedPosition = keepCheckedPosition;
    }

    @Override
    public Ckbs2ChoseGroupsRvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_row_merge_groups_choose, parent, false);
        //可以和另一个相似的RV采用同一布局。

        return new Ckbs2ChoseGroupsRvAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RvMergeGroup group = groups.get(position);

        if(position == keepCheckedPosition){
            holder.getCheckBox().setChecked(true);
            holder.getCheckBox().setClickable(false);//禁止点击
        }else {
            holder.getCheckBox().setChecked(group.isChecked());
        }
        holder.getgIndex().setText(String.valueOf(group.getId()));
        holder.getSize().setText(String.valueOf(group.getSize()));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }


    /*
    * 提供一个清空数据源的方法
    * 当外部dfg改变了rv的数据源时应当调用，以保持变量数据一致
    * 清空已选分组数，已选总词量，待传出的idList
    * */
    public void initRecords(){
        this.choseAmount =0;
        this.howManyGroupsChecked =0;
        this.idsList.clear();
    }

    //版本2：当减小amount限制导致数据源集合减小，根据保留的数据源重新计算记录数据
    public void resetRecords(){
        //先清空
        this.choseAmount =0;
        this.howManyGroupsChecked =0;
        this.idsList.clear();

        //再重算
        for (RvMergeGroup rm :groups) {
            if (rm.isChecked()) {
                choseAmount+=rm.getSize();
                howManyGroupsChecked++;
                idsList.add(rm.getId());
            }
        }
    }

    //保持发起组选定，不可取消
    /*public void keepPositionChecked(int position){
        groups.
    }*/
}
