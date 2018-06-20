package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningLessDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMergeDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;

import java.util.ArrayList;
import java.util.List;

public class FragGroupCategoryRvAdapter extends RecyclerView.Adapter<FragGroupCategoryRvAdapter.ViewHolder> {

    //    private static final String TAG = "FragGroupCategoryRvAdapter";
    private ArrayList<LearningMergeDiaFragment.ModelForGRv> categories;
    private Context context;//是所属DFG所属的Activity
    private LearningMergeDiaFragment dfg;//保持一个引用，以便改变dfg的UI（由DFG提供改变UI的公共方法）
    private int howManyGroupsChecke = 0;//（已选中了多少个分组）用于判断是否是全选
    private ArrayList<Integer> idsList = new ArrayList<>();//用于最终（选择好后）的回传

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        private final TextView msNum;
        private final TextView howManyGroups;

        private ViewHolder(View itemView) {
            super(itemView);
            LinearLayout llt = itemView.findViewById(R.id.llt_rvGirdCategoryMergeChoose);
            llt.setOnClickListener(this);
            msNum = itemView.findViewById(R.id.ms_rvGirdCategoryMergeChoose);
            howManyGroups = itemView.findViewById(R.id.howMany_rvGirdCategoryMergeChoose);

        }


        public TextView getMsNum() {
            return msNum;
        }

        public TextView getHowManyGroups() {
            return howManyGroups;
        }


        @Override
        public void onClick(View v) {
            //点击时，即选定了类别，从而要通知dfg改变下方的mrv数据。
            dfg.choseCategory(categories.get(getAdapterPosition()).getMs());
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
                return;//未被按下，是程序触发，没有动作。（总量文本的改变等任务交由DFG处理。）
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

    public FragGroupCategoryRvAdapter(ArrayList<LearningMergeDiaFragment.ModelForGRv> categories, Context context, LearningMergeDiaFragment dfg) {
        this.categories = categories;
        this.context = context;
        this.dfg = dfg;
    }

    @Override
    public FragGroupCategoryRvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_gird_category_choose, parent, false);

        return new FragGroupCategoryRvAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FragGroupForMerge group = groups.get(position);
        holder.getCheckBox().setChecked(true);
        holder.getMsNum().setText(String.valueOf(group.getId()));
        holder.getHowManyGroups().setText(String.valueOf(group.getTotalItemsNum()));

    }

    @Override
    public int getItemCount() {
        return groups.size();
    }


}
