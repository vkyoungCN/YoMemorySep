package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.GroupsAndMissionDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.ItemsAndMissionDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.MainActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.Mission;
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



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView title;
        private final ImageView star;

        private final ImageView fgStart;//开始碎片学习（自动新建分组）
        private final ImageView fgRe;//碎片快速复习
        private final ImageView groupsOfThis;//跳到任务详情与所属分组页
        private final ImageView itemsOfThis;//跳到任务详情与所属资源页

        int tempStarType = 1;//用于临时改变（存入DB前的）星标状态记录。

        private ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
//            fgsList = itemView.findViewById(R.id.fragmentGroupsList);
            fgStart = itemView.findViewById(R.id.fragmentGroupStart);
            fgRe = itemView.findViewById(R.id.fragmentGroupRe);
            groupsOfThis = itemView.findViewById(R.id.groupsOfThisMission);
            itemsOfThis = itemView.findViewById(R.id.itemsOfThisMission);
            star = itemView.findViewById(R.id.starAtStart);

            fgStart.setOnClickListener(this);
            fgRe.setOnClickListener(this);
            groupsOfThis.setOnClickListener(this);
            itemsOfThis.setOnClickListener(this);
            star.setOnClickListener(this);
        }

        public TextView getTitle() {
            return title;
        }

        public ImageView getStar() {
            return star;
        }

        @Override
        public void onClick(View view) {
            int position  =getAdapterPosition();
            if (position != RecyclerView.NO_POSITION){ // Check if an item was deleted, but the user clicked it before the UI removed it
                switch (view.getId()){
                    case R.id.fragmentGroupStart:
                        //跳转到学习页，边学边建方式建立碎片分组。
                        Toast.makeText(context, "碎片学习点击", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.fragmentGroupRe:
                        //开始分组复习（按某种顺序自动选取第一组？）；复习页给出类似“播放列表”的复习列表？
                        Toast.makeText(context, "碎片复习点击", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.groupsOfThisMission:
                        //跳转到任务详情页。
                        Intent intentToGroups = new Intent(context, GroupsAndMissionDetailActivity.class);
                        intentToGroups.putExtra("Mission",missions.get(position));
                            context.startActivity(intentToGroups);
                            break;
                    case R.id.itemsOfThisMission:
                        //跳转到任务详情页。
                        Intent intentToItems = new Intent(context, ItemsAndMissionDetailActivity.class);
                        intentToItems.putExtra("Mission",missions.get(position));
                        context.startActivity(intentToItems);
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
                        ((MainActivity)context).changeRvStar(position);
                }

            }
        }

    }

    public interface ChangeStar{
        void changeRvStar(int position);
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

    }

    @Override
    public int getItemCount() {
        return missions.size();
    }
}
