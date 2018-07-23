package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;

@SuppressWarnings("all")
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class ReportGroupLC_Fragment extends Fragment implements Constants {
//* 汇报本次学习的情况（作为Fg嵌入在学习结束页面中部）
//* 本FG用于LC模式。

    private static final String TAG = "ReportGroupLC_Fragment";

    /* 控件 */
    private TextView tv_gInfo;
    private TextView tv_gDvdInfo;
    private TextView tv_wrongInfo;
    private TextView tvBtn_wrongWords;

    private TextView tv_rmaNew;
    private TextView tv_msNew;

    private TextView tv_WrongInfo;
    private TextView tv_expanedWrongInfo;

    private TextView tv_newGroup;

    /* 逻辑变量 */
    private boolean isWtvExpanded =false;

    private int totalNum=0;
    private int doneNum=0;
    private int emptyNum=0;
    private int correctNum=0;
    private int wrongNum=0;

    private String newGroupStr="";
    private String wrongNamesStr="";

    private float newRma= 0f;
    private int newMs =0;


    public ReportGroupLC_Fragment() {
        // Required empty public constructor
    }

    public static ReportGroupLC_Fragment newInstance(Bundle data) {
        ReportGroupLC_Fragment fragment = new ReportGroupLC_Fragment();
        fragment.setArguments(data);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            totalNum = getArguments().getInt(STR_TOTAL_NUM);
            doneNum=getArguments().getInt(STR_DONE_NUM);
            emptyNum=getArguments().getInt(STR_EMPTY_NUM);
            correctNum=getArguments().getInt(STR_CORRECT_NUM);
            wrongNum=getArguments().getInt(STR_WRONG_NUM);

            newGroupStr = getArguments().getString(STR_NEW_GROUP);
            wrongNamesStr = getArguments().getString(STR_WRONG_NAMES);

            newRma = getArguments().getFloat(STR_NEW_RMA);
            newMs = getArguments().getInt(STR_NEW_MS);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_group_report_lc, container, false);

        tv_gInfo = rootView.findViewById(R.id.tv_groupInfo_lc_AC);
        tv_gInfo.setText(String.format(getResources().getString(R.string.hs_lc_info_ac),doneNum));

        tv_wrongInfo = rootView.findViewById(R.id.tv_wrongInfo_lc_AC);
        tv_wrongInfo.setText(String.format(getResources().getString(R.string.wrong_info_ac),correctNum,wrongNum));

        tv_expanedWrongInfo = rootView.findViewById(R.id.tv_expandedWrongInfo_lc_AC);
        tv_expanedWrongInfo.setText(String.format(getResources().getString(R.string.hs_wrong_items_names),wrongNamesStr));

        tvBtn_wrongWords = rootView.findViewById(R.id.tvBtn_wrongWordslc_lc);
        tvBtn_wrongWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isWtvExpanded){
                    //尚未展开，可以展开
                    isWtvExpanded =true;
                    tv_WrongInfo.setVisibility(View.VISIBLE);
                    //点击后符号改变
                    tvBtn_wrongWords.setText(R.string.horizontal_bar);
                }else {
                    //可以收回
                    isWtvExpanded =false;
                    tv_WrongInfo.setVisibility(View.GONE);
                    //收回后符号改变回原始
                    tvBtn_wrongWords.setText(R.string.down_ward);
                }
            }
        });

        tv_newGroup = rootView.findViewById(R.id.tv_newGroup_lc);
        tv_newGroup.setText(String.format(getResources().getString(R.string.status_changes),newGroupStr));

        tv_msNew = rootView.findViewById(R.id.tv_msChangeNew_fc_AC);
        tv_rmaNew = rootView.findViewById(R.id.tv_rmaChangeNew_fc_AC);

        tv_msNew.setText(String.valueOf(newMs));
        tv_rmaNew.setText(String.valueOf(newRma));

        return rootView;
    }



}
