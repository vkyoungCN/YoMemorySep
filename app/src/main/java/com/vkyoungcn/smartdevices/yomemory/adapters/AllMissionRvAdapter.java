package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.ItemsAndMissionDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.MainActivity;
import com.vkyoungcn.smartdevices.yomemory.MissionDetailsActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.customUI.LiteProgress;
import com.vkyoungcn.smartdevices.yomemory.models.RvMission;

import java.util.List;

/*
 * 以列表（Rv）形式展示所有任务的集合信息（行UI分两行，上层是名称，下层是一些按钮）
 * 条目有点击事件，点击后跳转到相应任务的详情页MissionDetailActivity。
 * */
public class AllMissionRvAdapter extends RecyclerView.Adapter<AllMissionRvAdapter.ViewHolder>{
    //    private static final String TAG = "AllMissionRvAdapter";
    private List<RvMission> missions;//本页暂时只显示titles，但后续页面需要suffix，点击事件需要相应id
    private Context context;//用于点击事件的启动新Activity
    private boolean is4BtnsShowing = false;



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView title;
        private final ImageView star;
        private final TextView simpleDetail;
        private final LiteProgress litePB;

//        private final TextView groupsOfThis;//跳到任务详情与所属分组页
        private final TextView tv_toMissionDetails;//跳到任务详情与所属资源页



        int tempStarType = 1;//用于临时改变（存入DB前的）星标状态记录。

        private ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_rvAllMission);
//            fgsList = itemView.findViewById(R.id.fragmentGroupsList);
//            groupsOfThis = itemView.findViewById(R.id.groupsOfThisMission);
            tv_toMissionDetails = itemView.findViewById(R.id.tv_toMissionDetails_rvAllMissions);
            simpleDetail = itemView.findViewById(R.id.tv_sDetail_rvAllMission);
            star = itemView.findViewById(R.id.starAtStart);
            litePB = itemView.findViewById(R.id.litePB_RvAM);

            title.setOnClickListener(this);//点击名称区域后，下方llt展开显示
//            groupsOfThis.setOnClickListener(this);
            tv_toMissionDetails.setOnClickListener(this);
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

        @Override
        public void onClick(View view) {
            int position  =getAdapterPosition();
            if (position != RecyclerView.NO_POSITION){ // Check if an item was deleted, but the user clicked it before the UI removed it
                switch (view.getId()){

                   /* case R.id.groupsOfThisMission:
                        //跳转到任务详情页。
                        if(position!=0){return;}//测试期间由于只有第一项任务是有效数据，临时禁止其他项目的跳转
                        Intent intentToGroups = new Intent(context, GroupsAndMissionDetailActivity.class);
                        intentToGroups.putExtra("Mission",missions.get(position));
                        context.startActivity(intentToGroups);
                        break;*/

                    case R.id.tv_toMissionDetails_rvAllMissions:
                        //跳转到任务详情页。
                        Intent intentToMDA = new Intent(context, MissionDetailsActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("MISSION",missions.get(position));
                        intentToMDA.putExtra("BUNDLE_FOR_MISSION",bundle);
//                        Toast.makeText(context, "mission's detail Description:"+missions.get(position).getDetailDescription(), Toast.LENGTH_SHORT).show();
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
                                break;
                            case 1:
                                missions.get(position).setStarType(1);
                                missions.get(position).setStartResourceId(R.drawable.star_blue);
                                break;
                            case 2:
                                missions.get(position).setStarType(2);
                                missions.get(position).setStartResourceId(R.drawable.star_red);
                                break;
                        }
                        ((MainActivity)context).changeRvStar(position,tempStarType%3);


                }

            }
        }

    }

    public interface ChangeStar{
        void changeRvStar(int position,int newStartType);
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
        holder.getSimpleDetail().setText(mission.getSimpleDescription());
        holder.getLitePB().setPercentage(mission.getDonePercentage());
//        holder.getLitePB().setPercentage(0.9f);

    }

    @Override
    public int getItemCount() {
        return missions.size();
    }
}
