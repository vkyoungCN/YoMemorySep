package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningMergeDiaFragment;

import java.util.ArrayList;

/*
* 用于点击“合并学习”后弹出的dfg中（该dfg有两个RV），其中上方rv用于选择碎片类型；
* 下方rv用于既定类型条件下的碎片分组列表。
* 本rvAdapter是用于上方Rv的。
* */
public class FragGroupCategoryRvAdapter extends RecyclerView.Adapter<FragGroupCategoryRvAdapter.ViewHolder> {

    //    private static final String TAG = "FragGroupCategoryRvAdapter";
    private ArrayList<LearningMergeDiaFragment.ModelForGRv> categories;
    private LearningMergeDiaFragment dfg;//保持一个引用，以便改变dfg的UI（由DFG提供改变UI的公共方法）

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
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
            //向外传递MS值，由MS+4/8确定备选组的范围；4还是8则由外部DFG确定
        }


    }

    public FragGroupCategoryRvAdapter(ArrayList<LearningMergeDiaFragment.ModelForGRv> categories, LearningMergeDiaFragment dfg) {
        this.categories = categories;
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
        LearningMergeDiaFragment.ModelForGRv model = categories.get(position);
        holder.getMsNum().setText(String.valueOf(model.getMs()));
        holder.getHowManyGroups().setText(String.valueOf(model.getHowManyGroups()));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }


}
