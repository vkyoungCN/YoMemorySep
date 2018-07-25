package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
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
public class QuitQueryDiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
    private static final String TAG = "QuitQueryDiaFragment";


    public QuitQueryDiaFragment() {
        // Required empty public constructor
    }


    public static QuitQueryDiaFragment newInstance() {
        QuitQueryDiaFragment fragment = new QuitQueryDiaFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_quit_query, container, false);

        TextView cancel = (TextView) rootView.findViewById(R.id.btn_cancel_quit_QueryDfg);
        TextView confirm = (TextView) rootView.findViewById(R.id.btn_ok_quit_QueryDfg);

        //部分需要添加事件监听
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);

        return rootView;
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_ok_quit_QueryDfg:

                this.dismiss();
                getActivity().finish();//不用传递LA，这样通用！
                break;


            case R.id.btn_cancel_quit_QueryDfg:
                this.dismiss();
                break;

        }

    }
}
