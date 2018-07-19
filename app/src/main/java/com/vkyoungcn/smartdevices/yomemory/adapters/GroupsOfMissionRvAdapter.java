package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.GroupDetailActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.fragments.DeleteGroupDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.LearningGelDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.fragments.QueryForMergeDiaFragment;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;

import java.util.List;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class GroupsOfMissionRvAdapter extends RecyclerView.Adapter<GroupsOfMissionRvAdapter.ViewHolder>
        implements Constants {
//* 是展示任务所属分组的RecyclerView所使用的适配器
// 采用纵向列表形式。
    private static final String TAG = "GroupsOfMissionRvAdapter";

    private List<RVGroup> groups;//数据源
    private Context context;
    private String tableSuffix;

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener{
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
            llt_overall.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.rv_goBtn_groupOfMission:
                    //
                    // 先检测容量，<4个触发合并复习，弹出LESS版DFG（带选择功能）。（检索容量<8的同MS分组，以供选择；
                    // 触发4、筛选8；后期可增加调节条件的字段）。如没有可选分组，可以开始学习【毕竟不能不让学】；
                    // 如果是合并式学习——先在adp中准备好数据，因为按SubNum检索无法从DB直接进行（不是DB直接字段）因而DFG无法高效完成检索；
                    //
                    //从本地List取数据后，传入Dfg以供选择，DFG中选好后只传gid到学习页。
                    //
                    // 正常分组正常模式学习，弹出普通DFG。两种DFG回传给本ACT的消息变量不同，将触发不同的学习页组织形式。

                    Toast.makeText(context, "准备弹确认对话框", Toast.LENGTH_SHORT).show();

                    FragmentTransaction transaction = ((Activity)context).getFragmentManager().beginTransaction();
                    Fragment prev_1 = ((Activity)context).getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_LESS);
                    Fragment prev_2 = ((Activity)context).getFragmentManager().findFragmentByTag(FG_STR_READY_TO_LEARN_GEL);

                    if (prev_1 != null) {
                        transaction.remove(prev_1);
                    }
                    if (prev_2 != null) {
                        transaction.remove(prev_2);
                    }

                    RVGroup triggerGroup = groups.get(getAdapterPosition());

                    if(triggerGroup.getTotalItemsNum()<5){
                        //4个（含）以内的，触发合并式学习的询问对话框
                        Bundle data_2 = new Bundle();
                        data_2.putInt(STR_POSITION,getAdapterPosition());
                        DialogFragment dfg = QueryForMergeDiaFragment.newInstance(data_2);
                        dfg.show(transaction, FG_STR_QUERY_FOR_MERGE);
                    }else {
                        //正常容量正常学习。此时只需传递正常的分组信息即可
                        DialogFragment dfg = LearningGelDiaFragment.newInstance(triggerGroup);
                        dfg.show(transaction, FG_STR_READY_TO_LEARN_GEL);

                    }
                 /*   if(subNum<5){
                        //4个（含）以内的，触发合并式学习
                        //准备数据
                        for (RVGroup rgp :groups) {
                            if(rgp.getTotalItemsNum()<9 && rgp.getMemoryStage()==msNum){
                                //8个以下（含），且MS同级的分组，可选
                                groupsListForChose.add(new RvMergeGroup(rgp));
                            }
                        }

                        //到合并学习的确认DFG。
                        DialogFragment dfg = LearningLessDiaFragment.newInstance(groupsListForChose);
                        dfg.show(transaction, FG_STR_READY_TO_LEARN_LESS);
                    }else {
                        //正常容量正常学习。此时只需传递正常的分组id即可
                        DialogFragment dfg = LearningGelDiaFragment.newInstance(groups.get(getAdapterPosition()));
                        dfg.show(transaction, FG_STR_READY_TO_LEARN_GEL);

                    }*/

                    break;

                case R.id.rvLlt_overall_groupOfMission:
                    //转到详情页（详情页上可以进行编辑、也有学习按钮）
                    Toast.makeText(context, "转到详情页", Toast.LENGTH_SHORT).show();

                    //后页需要group_id、tableSuffix
                    Intent intentToGD = new Intent(context, GroupDetailActivity.class);
                    intentToGD.putExtra(STR_GROUP,groups.get(getAdapterPosition()));
                    Log.i(TAG, "onClick: clickd group.ms:"+groups.get(getAdapterPosition()).getMemoryStage());
                    intentToGD.putExtra(STR_TABLE_SUFFIX,tableSuffix);

                    context.startActivity(intentToGD);

                    break;

            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.rvLlt_overall_groupOfMission:
                    //弹出dfg确认删除
                    FragmentTransaction transaction = ((Activity)context).getFragmentManager().beginTransaction();
                    Fragment prev =  ((Activity)context).getFragmentManager().findFragmentByTag(FG_STR_DELETE_GROUP);

                    if (prev != null) {
                        Toast.makeText(context, "Old DialogFg still there, removing first...", Toast.LENGTH_SHORT).show();
                        transaction.remove(prev);
                    }
                    DialogFragment dfg = DeleteGroupDiaFragment.newInstance(getAdapterPosition());
                    dfg.show(transaction, FG_STR_DELETE_GROUP);

                    break;
            }

            return false;


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
