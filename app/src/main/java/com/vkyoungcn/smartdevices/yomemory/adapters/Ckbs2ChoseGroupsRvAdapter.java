package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningLessDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMergeDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;

import java.util.ArrayList;
import java.util.List;

public class Ckbs2ChoseGroupsRvAdapter extends RecyclerView.Adapter<Ckbs2ChoseGroupsRvAdapter.ViewHolder> {

    //    private static final String TAG = "Ckbs2ChoseGroupsRvAdapter";
    private List<FragGroupForMerge> groups;//穿进来的数据
    private LearningMergeDiaFragment dfg;//保持一个引用，以便改变dfg的UI（由DFG提供改变UI的公共方法）
    private int howManyGroupsChecke = 0;//（除已移除的触发组外，已选中了多少个分组）用于判断是否是全选
    private ArrayList<Integer> idsList = new ArrayList<>();//最终外传/回传的数据

    public class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        private final CheckBox checkBox;
        private final TextView id;
        private final TextView subNum;

        private ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.ckb_isChose_rvMergeChoose);
            id = itemView.findViewById(R.id.group_id_rvMergeChoose);
            subNum = itemView.findViewById(R.id.group_num_rvMergeChoose);

            checkBox.setOnCheckedChangeListener(this);

        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public TextView getId() {
            return id;
        }

        public TextView getSubNum() {
            return subNum;
        }



        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!buttonView.isPressed()){
                //未被按下，是程序触发，不能有常规动作（避免连续改写文本）【未测试，不知是否存在此问题】
                // （总量文本的改变等任务交由DFG处理。）但是，回传的idList仍要正确改写。
                if(isChecked) {
                    idsList.clear();//先清除再全加入
                    for (FragGroupForMerge f : groups) {
                        idsList.add(f.getId());
                    }
                }else {
                    idsList.clear();

                }
                return;//后续不执行，尤其是改写文本的操作。
            }
            if(isChecked) {
                //将选中项目（的id）加入List
                idsList.add(groups.get(getAdapterPosition()).getId());

                //判断选中后是否全选
                howManyGroupsChecke++;
                if(howManyGroupsChecke==groups.size()){
                    //全选了，需要触发外部Dfg的allCkb框（的选中）
                    dfg.switchAllCheckBox(true);
                    //原设计中，该方法自带对总量文本框的改写，因而return不再执行下一方法；现已取消该改写功能，
                    // 程序触发的全选仍由本Rv负责改写文本（语句调用就在下方）。
                }

                //改变外部dfg的文本
                dfg.changeTotalChoseNumTvStr(true,groups.get(getAdapterPosition()).getTotalItemsNum());
                //负责：①Rv中的单行手动点击，需触发的文本改变；②因Rv单行点击致 满/满-1 时的……也属于单行改变，只是附带了对外部全选框的操作。

            }else {
                //取消某项时，将其从List移除
                idsList.remove(groups.get(getAdapterPosition()).getId());//由于id值各项必不一致，故可以安全移除。

                howManyGroupsChecke--;
                if(howManyGroupsChecke == groups.size()-1){
                    //说明刚刚从全选状态解除，a需要触发外部DFG的allCkb框（解除选中）
                    //注意，如果原本已经是0项，则根本不能触发，此一点上逻辑没问题。
                    dfg.switchAllCheckBox(false);
                }

                //改变外部dfg的文本
                dfg.changeTotalChoseNumTvStr(false,groups.get(getAdapterPosition()).getTotalItemsNum());

            }

        }


    }

    public ArrayList<Integer> getIdsList() {
        return idsList;
    }

    public Ckbs2ChoseGroupsRvAdapter(List<FragGroupForMerge> groups,LearningMergeDiaFragment dfg) {
        this.groups = groups;
        this.dfg = dfg;
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
        FragGroupForMerge group = groups.get(position);
        holder.getCheckBox().setChecked(true);
        holder.getId().setText(String.valueOf(group.getId()));
        holder.getSubNum().setText(String.valueOf(group.getTotalItemsNum()));

    }

    @Override
    public int getItemCount() {
        return groups.size();
    }


}
