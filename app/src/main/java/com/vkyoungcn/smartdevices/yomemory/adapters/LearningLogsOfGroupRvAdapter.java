package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.SingleLearningLog;

import java.util.ArrayList;

public class LearningLogsOfGroupRvAdapter extends RecyclerView.Adapter<LearningLogsOfGroupRvAdapter.ViewHolder> {
    private static final String TAG = "LearningLogsOfGroupRvAdapter";
//    private String[] strGroupLogs;//【这里初始化无意义的，如果传入的是null一样挂。已出错记录】
    private ArrayList<SingleLearningLog> learningLogs = new ArrayList<>();

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv_Num;
        private final TextView tv_Time;
        private final TextView tv_isEffect;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_Num = itemView.findViewById(R.id.tv_num_LG);
            tv_Time = itemView.findViewById(R.id.tv_time_LG);
            tv_isEffect = itemView.findViewById(R.id.tv_isEffect_LG);

        }

        public TextView getTv_Num() {
            return tv_Num;
        }

        public TextView getTv_Time() {
            return tv_Time;
        }

        public TextView getTv_isEffect() {
            return tv_isEffect;
        }
    }

    public LearningLogsOfGroupRvAdapter(ArrayList<SingleLearningLog> learningLogs) {
        this.learningLogs = learningLogs;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        SingleLearningLog singleLearningLog = learningLogs.get(position);
        holder.getTv_Num().setText(String.valueOf(position));
        holder.getTv_Time().setText(String.valueOf(singleLearningLog.getTimeInLong()));
        holder.getTv_isEffect().setAlpha(singleLearningLog.isEffective()?1:0);

    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_row_logs_of_group,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return learningLogs.size();
    }
}
