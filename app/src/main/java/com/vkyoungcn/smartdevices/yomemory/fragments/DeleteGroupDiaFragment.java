package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.Constants;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.DBGroup;
import com.vkyoungcn.smartdevices.yomemory.sqlite.YoMemoryDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class DeleteGroupDiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
    private static final String TAG = "DeleteGroupDiaFragment";
    private String suffix = "";//Item表的后缀，每个Mission不同，创建分组时需要从对应的Item表中拉取Items数据。
    private int position = 0;

    private OnGeneralDfgInteraction mListener;

    public DeleteGroupDiaFragment() {
        // Required empty public constructor
    }


    public static DeleteGroupDiaFragment newInstance(int position) {
        DeleteGroupDiaFragment fragment = new DeleteGroupDiaFragment();
        Bundle args = new Bundle();
        args.putInt(STR_POSITION,position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(STR_POSITION);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_delete_group, container, false);

        TextView cancel = (TextView) rootView.findViewById(R.id.btn_cancel_deleteGroupDfg);
        TextView confirm = (TextView) rootView.findViewById(R.id.btn_ok_deleteGroupDfg);

        //部分需要添加事件监听
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);

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
            case R.id.btn_ok_deleteGroupDfg://删除分组，将位置发回给activity，由调用方负责去DB实际删除,并更新列表显示。
                Bundle data = new Bundle();
                data.putInt(STR_POSITION,position);
                mListener.onButtonClickingDfgInteraction(OnGeneralDfgInteraction.DELETE_GROUP,data);
                this.dismiss();
                break;


            case R.id.btn_cancel_deleteGroupDfg:
                this.dismiss();
                break;

        }

    }
}
