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
