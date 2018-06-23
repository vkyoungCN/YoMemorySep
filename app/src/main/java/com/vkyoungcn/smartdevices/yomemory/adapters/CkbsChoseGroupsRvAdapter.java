package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.app.DialogFragment;
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
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;

import java.util.ArrayList;
import java.util.List;

/*
* 用于在分组列表中，“点击学习后，若判断该组容量小于5，则触发合并式学习”时弹出的同级碎片分组选择DFG下的Rv。
* 与另一个用于碎片选择的dfg相比，本dfg的初始分组是既定的（但是该分组直接在dfg中显示，不传入本rv）；
* 区别在于总量计算等……【待】。
* */
public class CkbsChoseGroupsRvAdapter extends RecyclerView.Adapter<CkbsChoseGroupsRvAdapter.ViewHolder> {

    //    private static final String TAG = "CkbsChoseGroupsRvAdapter";
    private List<FragGroupForMerge> groups;
    private Context context;//是所属DFG所属的Activity
    private LearningLessDiaFragment dfg;//保持一个引用，以便改变dfg的UI（由DFG提供改变UI的公共方法）
    private int howManyGroupsChecke = 0;//（除已移除的触发组外，已选中了多少个分组）用于判断是否是全选
    private ArrayList<Integer> idsList = new ArrayList<>();

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
            //本方法负责的任务有：1、实现全选、取消全选的逻辑：①全部选中时，dfg中的allCheck框选中，
            // ②在全选状态下，有一项取消则全选框也取消选中；③全选状态下，点击allCheck框，则全部取消选中
            //——只要点击allCk，就对其他所有项目进行操作；
            // ——其他项目达到全选条件时，allCK框选中；其他项目不是全选条件时，allCk不选中。
            // 逻辑问题在于：其他项目从全选到取消全选，会触发allCk的取消全选；此时不应进一步触发对其他项目的全部取消。

            //本Rv中的所有CKB都是“其他ckb”，而allChk框位于dfg，需要跨class交流。

            //只要有ckb被选中，就检测整体的状态，达到条件时，对allCkb做出操作【但是在VH中无法如此操作】
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
                    // 程序触发的全选仍由Rv负责改写
                }

                //改变外部dfg的文本
                dfg.changeTotalChoseNumTvStr(true,groups.get(getAdapterPosition()).getTotalItemsNum());

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

    public CkbsChoseGroupsRvAdapter(List<FragGroupForMerge> groups, Context context, LearningLessDiaFragment dfg) {
        this.groups = groups;
        this.context = context;
        this.dfg = dfg;
    }

    @Override
    public CkbsChoseGroupsRvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_row_merge_groups_choose, parent, false);

        return new CkbsChoseGroupsRvAdapter.ViewHolder(view);
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
