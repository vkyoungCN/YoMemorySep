package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.MergeResultRvAdapter;

import java.util.ArrayList;

@SuppressWarnings("all")
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class ReportGroupLM_Fragment extends Fragment implements Constants {
//* 汇报本次学习的情况（作为Fg嵌入在学习结束页面中部）
//* 本FG用于LM模式。

    private static final String TAG = "ReportGroupLM_Fragment";

    /* 控件 */
    private TextView tv_gInfo;
    private TextView tv_gMergeCreated;
    private TextView tv_wrongInfo;
    private TextView tvBtn_wrongWords;

    private TextView tv_rmaOld;
    private TextView tv_rmaNew;
    private TextView tv_msOld;
    private TextView tv_msNew;

    private TextView tv_msResult;
    private TextView tv_msReason;

    private TextView tv_expWrong;
    private RecyclerView recycler;


    /* 业务变量*/
    private boolean isWtvExpanded =false;
    private MergeResultRvAdapter rvAdapter;

    /* Intent收发 */
    private int totalNum=0;
    private int doneNum=0;
    private int emptyNum=0;
    private int correctNum=0;
    private int wrongNum=0;

    private String newGroupStr="";
    private String wrongNamesStr="";

    private ArrayList<Integer> oldFragsSizes;
    private ArrayList<Integer> newFragsSizes;//用于LM下合并提交到DB后各来源组的新容量。
    ArrayList<String> gpDescriptions;


    public ReportGroupLM_Fragment() {
        // Required empty public constructor
    }

    public static ReportGroupLM_Fragment newInstance(Bundle data) {
        ReportGroupLM_Fragment fragment = new ReportGroupLM_Fragment();
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

            gpDescriptions = getArguments().getStringArrayList(STR_GP_DESCRIPTIONS);
            oldFragsSizes = getArguments().getIntegerArrayList(STR_OLD_SIZES);
            newFragsSizes = getArguments().getIntegerArrayList(STR_NEW_SIZES);

           //lm模式不需要接收新旧MS/RMA数据。LM设计为不增MS的机制。
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_group_report_lm, container, false);

        tv_gInfo = rootView.findViewById(R.id.tv_groupInfo_fm_AC);
        tv_gInfo.setText(String.format(getResources().getString(R.string.group_info_ac),totalNum,doneNum,emptyNum));

        tv_gMergeCreated = rootView.findViewById(R.id.tv_groupMC_fm_AC);
        /*if(finisAmount==0) {【注意学习并不一定是按序进行的，因而这种复杂的报告没有意义。另外，完成数量为0时fg根本不加载。】
            //一个都没完成
            tv_gMergeCreated.setText(getResources().getString(R.string.done_nothing));
        }else if(finisAmount<firstGsize){
            //合并后的容量小于合并前
            tv_gMergeCreated.setText(getResources().getString(R.string.result_less_than_origin));

        }else if(finisAmount==firstGsize){
            //完成一组
        }else {
            //完成超过一组
            tv_gMergeCreated.setText(String.format(getResources().getString(R.string.hs_merge_part),newGroupStr));
        }*/

        //所以直接报告合并生成的新分组就好了（只要完成数量>0，这个组必然会生成）
        tv_gMergeCreated.setText(String.format(getResources().getString(R.string.hs_merge_part),newGroupStr));


        tv_wrongInfo = rootView.findViewById(R.id.tv_wrongInfo_fmLM_AC);
        tv_wrongInfo.setText(String.format(getResources().getString(R.string.wrong_info_ac),correctNum,wrongNum));

        tv_expWrong = rootView.findViewById(R.id.tv_expandedWrongInfo_fm_AC);
        tv_expWrong.setText(String.format(getResources().getString(R.string.hs_wrong_items_names),wrongNamesStr));

        tvBtn_wrongWords = rootView.findViewById(R.id.tvBtn_wrongWordsLm);
        tvBtn_wrongWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isWtvExpanded){
                    //尚未展开，可以展开
                    isWtvExpanded =true;
                    tv_expWrong.setVisibility(View.VISIBLE);
                    //点击后符号改变
                    tvBtn_wrongWords.setText(R.string.horizontal_bar);
                }else {
                    //可以收回
                    isWtvExpanded =false;
                    tv_expWrong.setVisibility(View.GONE);
                    //收回后符号改变回原始
                    tvBtn_wrongWords.setText(R.string.down_ward);
                }
            }
        });

        recycler = rootView.findViewById(R.id.rv_mergeResult_LMRP);
        rvAdapter = new MergeResultRvAdapter(gpDescriptions,oldFragsSizes,newFragsSizes,getContext());
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(rvAdapter);

        return rootView;
    }



}
