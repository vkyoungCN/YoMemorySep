package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.SingleLearningLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class LearningLogsOfGroupRvAdapter extends RecyclerView.Adapter<LearningLogsOfGroupRvAdapter.ViewHolder> implements Constants {
//* 用于展示指定分组的所有复习记录
//* 纵向列表形式
    private static final String TAG = "LearningLogsOfGroupRvAdapter";
//    private String[] strGroupLogs;//【这里初始化无意义的，如果传入的是null一样崩溃。】
    private ArrayList<SingleLearningLog> learningLogs;
    private ArrayList<String> strLearningTimes;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv_Num;
        private final TextView tv_Time;
        private final ImageView imv_isEffect;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_Num = itemView.findViewById(R.id.tv_num_LG);
            tv_Time = itemView.findViewById(R.id.tv_time_LG);
            imv_isEffect = itemView.findViewById(R.id.imv_isEffect_LG);
        }

        public TextView getTv_Num() {
            return tv_Num;
        }

        public TextView getTv_Time() {
            return tv_Time;
        }

        public ImageView getImv_isEffect() {
            return imv_isEffect;
        }
    }

    public LearningLogsOfGroupRvAdapter(ArrayList<SingleLearningLog> learningLogs,ArrayList<String> strLearningTimes) {
        this.learningLogs = learningLogs;
        this.strLearningTimes = strLearningTimes;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        SingleLearningLog singleLearningLog = learningLogs.get(position);
        holder.getTv_Num().setText(String.valueOf(position));
        holder.getTv_Time().setText(strLearningTimes.get(position));
        holder.getImv_isEffect().setVisibility(singleLearningLog.isEffective()?View.VISIBLE:View.GONE);
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
