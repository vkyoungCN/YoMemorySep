package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.LearningLogsOfGroupRvAdapter;
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
public class LogsOfGroupDiaFragment extends DialogFragment implements Constants {
//* 展示分组的全部Logs的对话框，内含一个RecyclerView。
    private static final String TAG = "LogsOfGroupDiaFragment";
    private static final String STRING_LEARNING_LOGS = "LEARNING_LOGS";
    private ArrayList<SingleLearningLog> learningLogs;
    private ArrayList<String> strLearningTimes = new ArrayList<>();
    private RecyclerView logRv;

    SimpleDateFormat sdFormat = new SimpleDateFormat(STR_DATE_PATTEN_1);

    public LogsOfGroupDiaFragment() {
        // Required empty public constructor
    }


    public static LogsOfGroupDiaFragment newInstance(ArrayList<SingleLearningLog> learningLogs) {
        LogsOfGroupDiaFragment fragment = new LogsOfGroupDiaFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(STRING_LEARNING_LOGS, learningLogs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            learningLogs = getArguments().getParcelableArrayList(STRING_LEARNING_LOGS);
            for (SingleLearningLog sll: learningLogs) {
                Date dateLearningTime = new Date(sll.getTimeInLong());
                String strLearningTime = sdFormat.format(dateLearningTime);
                strLearningTimes.add(strLearningTime);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.dfg_logs_of_group, container, false);

        //通过调整外层VG的大小将dialogFg的宽度设置为75%，高度设为屏幕可用部分的70%。
        LinearLayout llt = rootView.findViewById(R.id.frame_resize_logs_dfg);
        WindowManager appWm = (WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        try {
            appWm.getDefaultDisplay().getSize(point);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams((int)(point.x*0.75),(int)(point.y*0.7));
        llt.setLayoutParams(gLp);


        logRv = rootView.findViewById(R.id.rv_logs_list);
        logRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        logRv.setAdapter(new LearningLogsOfGroupRvAdapter(learningLogs, strLearningTimes));
        logRv.setHasFixedSize(true);

        return rootView;

    }



}
