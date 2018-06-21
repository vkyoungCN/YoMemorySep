package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.adapters.CkbsChoseGroupsRvAdapter;
import com.vkyoungcn.smartdevices.yomemory.models.FragGroupForMerge;
import com.vkyoungcn.smartdevices.yomemory.models.RVGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("all")
public class LearningGelDiaFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "LearningGelDiaFragment";
    private OnLearningConfirmDfgInteraction mListener;

    private RVGroup rvGroup;
    private TextView tvCancel;
    private TextView tvConfirm;
    private TextView tvDescription;
    private TextView tvLastLearningTime;

    public LearningGelDiaFragment() {
        // Required empty public constructor
    }

    public static LearningGelDiaFragment newInstance(RVGroup rvGroup) {
        LearningGelDiaFragment fragment = new LearningGelDiaFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("GROUP",rvGroup);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.rvGroup = savedInstanceState.getParcelable("GROUPS");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_learning_gel, container, false);

        tvDescription = (TextView) rootView.findViewById(R.id.tv_group_description_learningGel);
        tvCancel = (TextView) rootView.findViewById(R.id.btn_cancel_learningGel);
        tvConfirm = (TextView) rootView.findViewById(R.id.btn_confirm_learningGel);
        tvLastLearningTime = (TextView) rootView.findViewById(R.id.tv_group_lastTime_learningGel);

        tvDescription.setText(rvGroup.getDescription());

        Date lastLearningTimeDate = new Date(rvGroup.getLastLearningTime());
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String lastLearningTimeStr = sdFormat.format(lastLearningTimeDate);
        tvLastLearningTime.setText(lastLearningTimeStr);

        //部分需要添加事件监听
        tvConfirm.setOnClickListener(this);
        tvCancel.setOnClickListener(this);

        return rootView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLearningConfirmDfgInteraction) {
            mListener = (OnLearningConfirmDfgInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLearningConfirmDfgInteraction");
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
            case R.id.btn_confirm_learningGel:
                //从rv中取数据，存入Bundle，最终交到学习页。
                Bundle bundle = new Bundle();
                bundle.putInt("GROUP_ID_TO_LEARN",rvGroup.getId());

                mListener.onLearningConfirmDfgInteraction(OnLearningConfirmDfgInteraction.LEARNING_GENERAL,bundle);
                break;

            case R.id.btn_cancel_learningAddRandom:

                this.dismiss();
                break;
        }

    }


}
