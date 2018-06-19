package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.GroupDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningLessFourDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;

import java.util.ArrayList;
import java.util.List;

public class GroupsOfMissionRvAdapter extends RecyclerView.Adapter<GroupsOfMissionRvAdapter.ViewHolder> {
    private static final String TAG = "GroupsOfMissionRvAdapter";

    private List<RVGroup> groups = new ArrayList<>();
    private Context context;
    private String tableSuffix = "";//避免null。

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView tv_groupId;
        private final TextView tv_groupDescription;
        private final TextView tv_NumSub;
        private final TextView tv_Stage;

        private final TextView tv_RMA;
        private final TextView tv_btnGo;

        private final LinearLayout llt_overall;

        private ViewHolder(View itemView) {
            super(itemView);
            tv_groupId = itemView.findViewById(R.id.rv_id_groupOfMission);
            tv_groupDescription = itemView.findViewById(R.id.rv_description_groupOfMission);
            tv_NumSub = itemView.findViewById(R.id.rv_NumSubs_groupOfMission);
            tv_Stage = itemView.findViewById(R.id.rv_memory_stage_groupOfMission);
            tv_RMA = itemView.findViewById(R.id.rv_RMA_groupOfMission);

            tv_btnGo = itemView.findViewById(R.id.rv_goBtn_groupOfMission);
            tv_btnGo.setOnClickListener(this);

            llt_overall = itemView.findViewById(R.id.rvLlt_overall_groupOfMission);
            llt_overall.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.rv_goBtn_groupOfMission:
                    //弹出DFG：
                    //
                    // 对于纯碎片分组的合并化处理思路（数量<4个）：提示“有…个同MS级别的碎片分组，将一同开启学习”
                    //如果没有其它同级碎片分组，则【毕竟不能不让学】开始学习（可能可以有提示，待定）；
                    // 【暂也不建议列入其他大分组中】【也暂不适于将碎组的Go键屏蔽（既费计算资源，又违反“不能不让学”原则）】
                    //
                    //DFG提示确认形式
                    // ①检测分组的容量（大于4个的按正常逻辑处理【RVG类直接有字段】）；
                    // ②拉取分组的id，所含资源项的id（可以由目的Activity负责）
                    Toast.makeText(context, "转到分组学习页", Toast.LENGTH_SHORT).show();

                    groups.get(getAdapterPosition()).getId();
                    if(groups.get(getAdapterPosition()).getTotalItemsNum()<5){
                        //4个（含）以内的，推荐合并式学习
                        FragmentTransaction transaction = ((Activity)context).getFragmentManager().beginTransaction();
                        Fragment prev = ((Activity)context).getFragmentManager().findFragmentByTag("READY_TO_LEARN_LESS");

                        DialogFragment dfg = LearningLessFourDiaFragment.newInstance();
                        dfg.show(transaction, "READY_TO_LEARN_LESS");
                    }

                    break;

                case R.id.rvLlt_overall_groupOfMission:
                    //转到详情页（详情页上可以进行编辑、也有学习按钮）
                    Toast.makeText(context, "转到详情页", Toast.LENGTH_SHORT).show();

                    //后页需要group_id、tableSuffix
                    Intent intentToGD = new Intent(context, GroupDetailActivity.class);
                    intentToGD.putExtra("GroupId",groups.get(getAdapterPosition()).getId());
                    intentToGD.putExtra("TableSuffix",tableSuffix);

                    context.startActivity(intentToGD);

                    break;

            }
        }

        private TextView getTv_groupId() {
            return tv_groupId;
        }

        private TextView getTv_groupDescription() {
            return tv_groupDescription;
        }

        private TextView getTv_NumSub() {
            return tv_NumSub;
        }

        private TextView getTv_Stage() {
            return tv_Stage;
        }

        public TextView getTv_RMA() {
            return tv_RMA;
        }

        public TextView getTv_btnGo() {
            return tv_btnGo;
        }

    }

    public GroupsOfMissionRvAdapter(List<RVGroup> groups, Context context,String tableSuffix) {
        this.groups = groups;
        this.tableSuffix = tableSuffix;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_row_groups_of_mission,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupsOfMissionRvAdapter.ViewHolder holder, int position) {
        RVGroup rvGroup = groups.get(position);

        holder.getTv_groupId().setText(String.valueOf(rvGroup.getId()));
        holder.getTv_groupDescription().setText(rvGroup.getDescription());
        holder.getTv_NumSub().setText(String.valueOf(rvGroup.getTotalItemsNum()));
        holder.getTv_Stage().setText(String.valueOf(rvGroup.getMemoryStage()));
        holder.getTv_RMA().setText(String.valueOf(rvGroup.getRM_Amount()));

    }

    @Override
    public int getItemCount() {
        return groups.size();
    }
}
