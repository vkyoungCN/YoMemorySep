package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.MainActivity;
import com.vkyoungcn.smartdevices.yomemory.MissionDetailsActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.customUI.LiteProgress;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;

import java.util.List;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class AllMissionRvAdapter extends RecyclerView.Adapter<AllMissionRvAdapter.ViewHolder>
        implements Constants{
// * 以RecyclerView形式展示所有任务
// * 各条项采用CardView的形式
// *
// * 卡片上的星标可以点击切换；卡片上的详情按钮点击后可跳转到详情页获取更多功能。
// *

//    private static final String TAG = "AllMissionRvAdapter";
    private List<RvMission> missions;//数据源
    private Context context;//用于点击事件的启动新Activity


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView title;//任务标题（名称）
        private final TextView simpleDetail;//（任务的简略说明）

        private final LiteProgress litePB;//卡片下方的“本任务进度条”（轻量版）
        private final ImageView star;//任务左上角的星标，可点击切换。【功能暂未明确，或是用于任务的种类定位】
        private final LinearLayout llt_header;//点击星标后，卡片页头会随之改变底色
        private final TextView tvBtn_toMissionDetails;//跳到任务详情与所属资源页

        int tempStarType = 1;//用于（存入DB前）临时改变星标状态记录。

        private ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_rvAllMission);
            tvBtn_toMissionDetails = itemView.findViewById(R.id.tv_toMissionDetails_rvAMs);
            simpleDetail = itemView.findViewById(R.id.tv_sDetail_rvAllMission);
            star = itemView.findViewById(R.id.starAtStart);
            litePB = itemView.findViewById(R.id.litePB_RvAM);
            llt_header = itemView.findViewById(R.id.llt_header_rvAM);

            title.setOnClickListener(this);
            tvBtn_toMissionDetails.setOnClickListener(this);
            star.setOnClickListener(this);
        }

        public TextView getTitle() {
            return title;
        }

        public ImageView getStar() {
            return star;
        }

        public TextView getSimpleDetail() {
            return simpleDetail;
        }

        public LiteProgress getLitePB() {
            return litePB;
        }

        public LinearLayout getLlt_header() {
            return llt_header;
        }

        @Override
        public void onClick(View view) {
            int position  =getAdapterPosition();
            if (position != RecyclerView.NO_POSITION){ // Check if an item was deleted, but the user clicked it before the UI removed it
                switch (view.getId()){


                    case R.id.tv_toMissionDetails_rvAMs:
                        //跳转到任务详情页。
                        Intent intentToMDA = new Intent(context, MissionDetailsActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(STR_MISSION,missions.get(position));
                        intentToMDA.putExtra(STR_BUNDLE_FOR_MISSION,bundle);
                        context.startActivity(intentToMDA);
                        break;

                    case R.id.starAtStart:
                        //切换星标图片（并且在退出时保存到DB）
                        tempStarType = missions.get(getAdapterPosition()).getStarType();//数据源中的类型值
                        tempStarType++;

                        //两个任务：①改数据；②改UI
                        switch (tempStarType%3){
                            case 0:
                                missions.get(position).setStarType(0);
                                missions.get(position).setStartResourceId(R.drawable.star_gray);
                                missions.get(position).setHeaderColorId(R.color.mission_card_gray);
                                break;
                            case 1:
                                missions.get(position).setStarType(1);
                                missions.get(position).setStartResourceId(R.drawable.star_blue);
                                missions.get(position).setHeaderColorId(R.color.mission_card_blue);
                                break;
                            case 2:
                                missions.get(position).setStarType(2);
                                missions.get(position).setStartResourceId(R.drawable.star_red);
                                missions.get(position).setHeaderColorId(R.color.mission_card_red);
                                break;
                        }
                        ((MainActivity)context).changeRvStarAndSave(position);


                }

            }
        }

    }

    public interface ChangeStar{
        void changeRvStarAndSave(int position);
    }


    public AllMissionRvAdapter(List<RvMission> missions, Context context) {
        this.missions = missions;
        this.context = context;
    }

    @Override
    public AllMissionRvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_row_all_missions, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RvMission mission = missions.get(position);
        holder.getTitle().setText(mission.getName());
        holder.getStar().setImageDrawable(context.getDrawable(mission.getStartResourceId()));
        holder.getLlt_header().setBackgroundColor(ContextCompat.getColor(context,mission.getHeaderColorId()));
        holder.getSimpleDetail().setText(mission.getSimpleDescription());
        holder.getLitePB().setPercentage(mission.getDonePercentage());
//        holder.getLitePB().setPercentage(0.9f);

    }

    @Override
    public int getItemCount() {
        return missions.size();
    }
}
