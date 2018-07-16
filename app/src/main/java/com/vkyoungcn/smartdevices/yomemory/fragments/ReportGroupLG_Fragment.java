package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.LearningActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

@SuppressWarnings("all")
/*
 * 作者1：杨胜 @中国海洋大学
 * 作者2：杨镇时 @中国海洋大学
 * author：Victor Young @Ocean University of China
 * email: yangsheng@ouc.edu.cn
 *
 * 用于单项Item的复习学习
 * 此时，默认显示英文+音标，点击翻面后显示汉译；
 * 需要点击翻面后并输入正确的拼写才能滑动到下一页。
 */
public class ReportGroupLG_Fragment extends Fragment {
    private static final String TAG = "ReportGroupLG_Fragment";

    private TextView tv_gInfo;
    private TextView tv_gDvdInfo;
    private TextView tv_wrongInfo;
    private TextView tvBtn_wrongWords;

    private TextView tv_rmaOld;
    private TextView tv_rmaNew;
    private TextView tv_msOld;
    private TextView tv_msNew;

    private TextView tv_msResult;
    private TextView tv_msReason;

    private TextView tv_expWrong;
    private boolean isWtvExpanded =false;

    private int totalNum=0;
    private int doneNum=0;
    private int emptyNum=0;
    private int correctNum=0;
    private int wrongNum=0;

    private String newGroupStr="";
    private String wrongNamesStr="";

    private float newRma= 0f;
    private float oldRma= 0f;
    private int newMs =0;
    private int oldMs =0;

    private boolean isMsUp =false;
    private boolean isTooLate = false;



    public ReportGroupLG_Fragment() {
        // Required empty public constructor
    }

    public static ReportGroupLG_Fragment newInstance(Bundle data) {
        ReportGroupLG_Fragment fragment = new ReportGroupLG_Fragment();
        fragment.setArguments(data);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            totalNum = getArguments().getInt("TOTAL_NUM");
            doneNum=getArguments().getInt("DONE_NUM");
            emptyNum=getArguments().getInt("EMPTY_NUM");
            correctNum=getArguments().getInt("CORRECT_NUM");
            wrongNum=getArguments().getInt("WRONG_NUM");

            newGroupStr = getArguments().getString("NEW_GROUP");
            wrongNamesStr = getArguments().getString("WRONG_NAMES");

            newRma = getArguments().getInt("NEW_RMA");
            oldRma =getArguments().getInt("OLD_RMA");
            newMs = getArguments().getInt("NEW_MS");
            oldMs = getArguments().getInt("OLD_MS");

            isMsUp = getArguments().getBoolean("IS_MS_UP");
            isTooLate = getArguments().getBoolean("IS_TOO_LATE");

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_group_report_lg, container, false);

        tv_gInfo = rootView.findViewById(R.id.tv_groupInfo_fg_AC);
        tv_gInfo.setText(String.format(getResources().getString(R.string.group_info_ac),totalNum,doneNum,emptyNum));

        tv_gDvdInfo = rootView.findViewById(R.id.tv_groupDvd_fg_AC);
        if(emptyNum!=0){
            //有拆分
            tv_gDvdInfo.setText(String.format(getResources().getString(R.string.group_dvd_ac_1),newGroupStr));
        }

        tv_wrongInfo = rootView.findViewById(R.id.tv_wrongInfo_fgLG_AC);
        tv_wrongInfo.setText(String.format(getResources().getString(R.string.wrong_info_ac),correctNum,wrongNum));

        tv_expWrong = rootView.findViewById(R.id.tv_expandedWrongInfo_fgLG_AC);
        tv_expWrong.setText(String.format(getResources().getString(R.string.hs_wrong_items_names),wrongNamesStr));

        tvBtn_wrongWords = rootView.findViewById(R.id.tvBtn_wrongWordsLG_fg);
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


        tv_msNew = rootView.findViewById(R.id.tv_msChangeNew_fg_AC);
        tv_rmaNew = rootView.findViewById(R.id.tv_RmaChangeNew_fg_AC);
        tv_msOld = rootView.findViewById(R.id.tv_msChangeOld_fg_AC);
        tv_rmaOld = rootView.findViewById(R.id.tv_RmaChangeOld_fg_AC);

        tv_msNew.setText(String.valueOf(newMs));
        tv_rmaNew.setText(String.valueOf(newRma));
        tv_msOld.setText(String.valueOf(oldMs));
        tv_rmaOld.setText(String.valueOf(oldRma));


        tv_msResult = rootView.findViewById(R.id.tv_msResult);
        tv_msReason = rootView.findViewById(R.id.tv_msReason_fg);

        if(!isMsUp){
            //未升级
            tv_msResult.setText(R.string.ms_keep);
            if(!isTooLate){
                //太早
                tv_msReason.setText(R.string.reason_ms_tooEarly);

                //默认tv是太晚，不设置
            }
        }else {
            //升级了，不需再显示原因
            tv_msReason.setVisibility(View.GONE);
        }

        return rootView;
    }



}
