package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.ItemDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.ItemsOfMissionActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.ArrayList;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class MergeResultRvAdapter extends RecyclerView.Adapter<MergeResultRvAdapter.ViewHolder>
        implements Constants {
//* 用于展示任务所属各Items的RecyclerView的适配器
//* 纵向列表形式
    private static final String TAG = "MergeResultRvAdapter";

    /* 数据源*/
    private ArrayList<Integer> oldFragsSizes;
    private ArrayList<Integer> newFragsSizes;//用于LM下合并提交到DB后各来源组的新容量。
    ArrayList<String> gpDescriptions;
    private Context context;

    class ViewHolder extends RecyclerView.ViewHolder implements Constants{
        private final TextView tv_groupDesp;
        private final TextView tv_oldSize;
        private final TextView tv_newSize;


        private ViewHolder(View itemView) {
            super(itemView);
//            tv_groupId = itemView.findViewById(R.id.tv_id_rvLMRP);
            tv_groupDesp = itemView.findViewById(R.id.tv_desp_rvLMRP);
            tv_oldSize = itemView.findViewById(R.id.tv_oldSize_rvLMRP);
            tv_newSize = itemView.findViewById(R.id.tv_newSize_rvLMRP);

        }


//        private TextView getTv_groupId() {
//            return tv_groupId;
//        }

        private TextView getTv_groupDesp() {
            return tv_groupDesp;
        }

        private TextView getTv_oldSize() {
            return tv_oldSize;
        }

        private TextView getTv_newSize() {
            return tv_newSize;
        }


    }

    public MergeResultRvAdapter(ArrayList<String> gpDescriptions,ArrayList<Integer> oldFragsSizes,ArrayList<Integer>newFragsSizes,Context context) {
        this.gpDescriptions = gpDescriptions;
        this.oldFragsSizes = oldFragsSizes;
        this.newFragsSizes = newFragsSizes;

        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_row_merge_result,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MergeResultRvAdapter.ViewHolder holder, int position) {
        holder.getTv_groupDesp().setText(gpDescriptions.get(position));
        holder.getTv_oldSize().setText(String.valueOf(oldFragsSizes.get(position)));
        holder.getTv_newSize().setText(String.valueOf(newFragsSizes.get(position)));

    }

    @Override
    public int getItemCount() {
        return oldFragsSizes.size();
    }
}
