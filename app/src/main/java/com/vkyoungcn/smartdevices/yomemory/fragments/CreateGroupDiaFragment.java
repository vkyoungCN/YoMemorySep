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

import static com.vkyoungcn.smartdevices.yomemory.fragments.OnGeneralDfgInteraction.CREATE_GROUP;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
@SuppressWarnings("all")
public class CreateGroupDiaFragment extends DialogFragment
        implements View.OnClickListener,Constants {
    private static final String TAG = "CreateGroupDiaFragment";
//    private String suffix = "";//Item表的后缀，每个Mission不同，创建分组时需要从对应的Item表中拉取Items数据。
//    private int missionId = 0;

    private OnGeneralDfgInteraction mListener;

    private RadioGroup radioGroup_manner;
    private RadioGroup radioGroup_size;
    private RadioButton rb_manner_order;
    private RadioButton rb_manner_random;

    private TextView tv_explanationArea;
    private EditText editText_groupDesc;

    public CreateGroupDiaFragment() {
        // Required empty public constructor
    }


    public static CreateGroupDiaFragment newInstance(String tableSuffix, int missionId) {
        CreateGroupDiaFragment fragment = new CreateGroupDiaFragment();
//        Bundle args = new Bundle();
//        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
        }*/


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.dfg_create_group, container, false);
        //所有需要用到的8个控件，获取引用
        //【虽然IDE说redundant，但是不转型后面setText时亲测出错崩溃】
        radioGroup_manner = (RadioGroup) rootView.findViewById(R.id.rg_manner_groupCreateDfg);
        radioGroup_size = (RadioGroup) rootView.findViewById(R.id.rg_size_groupCreateDfg);
        rb_manner_order = (RadioButton) rootView.findViewById(R.id.rb_order_groupCreateDfg);
        rb_manner_random = (RadioButton) rootView.findViewById(R.id.rb_random_groupCreateDfg);
        tv_explanationArea = (TextView) rootView.findViewById(R.id.tv_createGroupDfg_explaining);
        editText_groupDesc = (EditText) rootView.findViewById(R.id.group_desc_in_create_dfg);
        TextView cancel = (TextView) rootView.findViewById(R.id.btn_cancel_createGroupDfg);
        TextView confirm = (TextView) rootView.findViewById(R.id.btn_ok_createGroupDfg);

        //部分需要添加事件监听
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);

        /*radioGroup_manner.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i==rb_manner_order.getMsNum()){
                    tv_explanationArea.setText("按顺序选取，随机模式下已被抽走的项目不会添加。");
                }else if(i==rb_manner_random.getMsNum()){
                    tv_explanationArea.setText("随机选取");
                }
            }
        });*/
//        Log.i(TAG, "onCreateView: done");
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
            case R.id.btn_ok_createGroupDfg://创建新分组
                Bundle data = new Bundle();

                //获取设置的分组目标尺寸
                int groupSize = 0;
                switch(radioGroup_size.getCheckedRadioButtonId()){
                    case R.id.rb_16_groupCreate_dfg:
                        groupSize = 16;
                        break;
                    case R.id.rb_24_groupCreate_dfg:
                        groupSize = 24;
                        break;
                    case R.id.rb_32_groupCreate_dfg:
                        groupSize = 32;
                        break;
                    case R.id.rb_8_groupCreate_dfg:
                        groupSize = 8;
                        break;
                }

                //获取EditText中填入的字串【是否要判空？】
                String description = editText_groupDesc.getText().toString();

                //获取要顺序还是随机取词
                boolean isOrder = true;
                if(radioGroup_manner.getCheckedRadioButtonId()==R.id.rb_random_groupCreateDfg){
                        isOrder = false;
                }


                //发送数据
                data.putBoolean(STR_IS_ORDER,isOrder);
                data.putInt(STR_GROUP_SIZE,groupSize);
                data.putString(STR_DESCRIPTION,description);
                mListener.onButtonClickingDfgInteraction(CREATE_GROUP,data);

                dismiss();

                break;
            case R.id.btn_cancel_createGroupDfg:

                this.dismiss();
                break;

        }

    }
}
