package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.GroupsAndMissionDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.ItemsAndMissionDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.MainActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;

import java.util.ArrayList;
import java.util.List;

/*
 *
 * */
public class FragsInMergeRvAdapter extends RecyclerView.Adapter<FragsInMergeRvAdapter.ViewHolder>{
    //    private static final String TAG = "FragsInMergeRvAdapter";
    private ArrayList<Integer> gIdsForMerge;
    private ArrayList<Integer> gSizes;
    private ArrayList<String> gResult;


    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv_gid;
        private final TextView tv_gSize;
        private final TextView tv_gResult;
        private final LinearLayout llt_overall;


        private ViewHolder(View itemView) {
            super(itemView);
            tv_gid = itemView.findViewById(R.id.tvGid_rvMergeFragInfo);
            tv_gSize = itemView.findViewById(R.id.tvGSize_rvMergeFragInfo);
            tv_gResult = itemView.findViewById(R.id.tvGResult_rvMergeFragInfo);
            llt_overall = itemView.findViewById(R.id.llt_rvMergeFragInfo);


        }

        private TextView getTv_gid() {
            return tv_gid;
        }

        private TextView getTv_gSize() {
            return tv_gSize;
        }

        private TextView getTv_gResult() {
            return tv_gResult;
        }

        public LinearLayout getLlt_overall() {
            return llt_overall;
        }
    }

    public FragsInMergeRvAdapter(ArrayList<Integer> gIdsForMerge, ArrayList<Integer> gSizes, ArrayList<String> gResult) {
        this.gIdsForMerge = gIdsForMerge;
        this.gSizes = gSizes;
        this.gResult = gResult;
    }

    @Override
    public FragsInMergeRvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_row_frags_merge_info, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.getTv_gid().setText(String.valueOf(gIdsForMerge.get(position)));
        holder.getTv_gSize().setText(String.valueOf(gSizes.get(position)));
        holder.getTv_gResult().setText(String.valueOf(gResult.get(position)));
        if(position%2==1) {
            holder.getLlt_overall().setBackgroundResource(R.color.lite_gray);
        }
    }

    @Override
    public int getItemCount() {
        return gIdsForMerge.size();
    }
}
