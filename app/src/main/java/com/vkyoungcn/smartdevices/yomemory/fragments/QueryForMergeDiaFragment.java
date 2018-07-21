package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class QueryForMergeDiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
//* 用于当发起学习的分组容量较小时，询问是否按合并模式进行，如点击同意，则发送信息到Activity，
//* 由Activity负责调用后续dfg

//* 本dfg发起Activity有：单项Group详情页、Group列表的Rv适配器。

    private static final String TAG = "DeleteGroupDiaFragment";
//    private String suffix = "";//Item表的后缀，每个Mission不同，创建分组时需要从对应的Item表中拉取Items数据。
    private int position = -1;//如果是从rv适配器发起，则需要传递位置索引。

    private OnGeneralDfgInteraction mListener;

    public QueryForMergeDiaFragment() {
        // Required empty public constructor
    }


    public static QueryForMergeDiaFragment newInstance(Bundle data) {
        QueryForMergeDiaFragment fragment = new QueryForMergeDiaFragment();
        /*Bundle args = new Bundle();
        args.putInt(STR_POSITION,position);*/
        if(data!=null) {
            fragment.setArguments(data);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(STR_POSITION,-1);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_query_for_merge, container, false);

        TextView cancel = (TextView) rootView.findViewById(R.id.btn_cancel_queryDfg);
        TextView ok = (TextView) rootView.findViewById(R.id.btn_ok_queryDfg);
        TextView no = (TextView) rootView.findViewById(R.id.btn_lg_queryDfg);

        //部分需要添加事件监听
        no.setOnClickListener(this);
        cancel.setOnClickListener(this);
        ok.setOnClickListener(this);

        return rootView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGeneralDfgInteraction) {
            mListener = (OnGeneralDfgInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGeneralDfgInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_ok_queryDfg:
                //发回Activity，同意按lm模式启动
                Bundle data = new Bundle();
                data.putInt(STR_POSITION,position);//如果初始化时没有传递该值，则为-1，无所谓的。

                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.OK_THEN_USE_LM,data);
                this.dismiss();
                break;

            case R.id.btn_lg_queryDfg:
                //发回Actitivy，强行按LG模式启动
                Bundle data2 = new Bundle();
                data2.putInt(STR_POSITION,position);
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.FORCE_USE_LG,data2);
                this.dismiss();
                break;


            case R.id.btn_cancel_queryDfg:
                this.dismiss();
                break;

        }

    }
}
