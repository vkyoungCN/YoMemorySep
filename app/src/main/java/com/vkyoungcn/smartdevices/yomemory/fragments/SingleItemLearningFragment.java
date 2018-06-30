package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;
import com.vkyoungcn.smartdevices.yomemory.validatingEditor.ValidatingEditor;

@SuppressWarnings("all")
/**
 * 用于单项Item的复习学习
 * 此时，默认显示英文+音标，点击翻面后显示汉译；
 * 需要点击翻面后并输入正确的拼写才能滑动到下一页。
 */
public class SingleItemLearningFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "SingleItemRePickingFrag";
    private static final String SINGLE_ITEM = "single_item";

    private SingleItem singleItem;
    private LinearLayout lltPositive;
    private RelativeLayout rltNegative;
    private ValidatingEditor tv_validatingEditor;

    private boolean cardPositive = true;
    private Boolean pageSlidingAvailable = false;//只有在翻面后输入了正确的拼写后才可翻页。（此时可以再翻回正面）
    InputMethodManager manager;
    private boolean softKBActive = false;

    private ValidatingEditor.OnValidatingEditorInputListener mListener;

    public SingleItemLearningFragment() {
        // Required empty public constructor
    }

    public static SingleItemLearningFragment newInstance(SingleItem singleItem) {
        SingleItemLearningFragment fragment = new SingleItemLearningFragment();
        Bundle args = new Bundle();
        args.putParcelable(SINGLE_ITEM, singleItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            singleItem = getArguments().getParcelable(SINGLE_ITEM);
        }
        manager = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_single_item_learning, container, false);
        CardView cardView =(CardView) rootView.findViewById(R.id.card_view);
        cardView.setOnClickListener(this);

        lltPositive = (LinearLayout) rootView.findViewById(R.id.llt_positivePage_singleItemLearning_re);
        rltNegative = (RelativeLayout) rootView.findViewById(R.id.rlt_negativePage_singleItemLearning_re);
        TextView tvName = (TextView)rootView.findViewById(R.id.tv_name_singleItemLearning_re);
        TextView tv_ext1 = (TextView) rootView.findViewById(R.id.tv_ext1_singleItemLearning_re);
        TextView tv_ext2 = (TextView) rootView.findViewById(R.id.tv_ext2_singleItemLearning_re);
        TextView tv_ext2Pos = (TextView) rootView.findViewById(R.id.tv_ext2Pos_singleItemLearning_re);
        tv_validatingEditor = (ValidatingEditor) rootView.findViewById(R.id.validatingEditor_singleItemLearning);
        tv_validatingEditor.requestFocus();//因为发现焦点默认在cardView上。



        tvName.setText(singleItem.getName());
//        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/GentiumPlus_I.ttf");
//        tv_ext1.setTypeface(typeface);
        tv_ext1.setText(singleItem.getExtending_list_1());
        tv_ext2.setText(singleItem.getExtending_list_2());
        tv_ext2Pos.setText(singleItem.getExtending_list_2());

        tv_validatingEditor.setTargetText(singleItem.getName());
        tv_validatingEditor.setCodeReadyListener(mListener);//该监听由Activity实现，这样就将二者关联起来了。
        tv_validatingEditor.setOnClickListener(this);

        EditorInfo veEditorInfo = new EditorInfo();
        veEditorInfo.inputType = InputType.TYPE_NULL;
        tv_validatingEditor.onCreateInputConnection(veEditorInfo);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ValidatingEditor.OnValidatingEditorInputListener) {
            mListener = (ValidatingEditor.OnValidatingEditorInputListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ValidatingEditor.OnValidatingEditorInputListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.card_view:
                    if(cardPositive) {
                        cardPositive = false;
                        //当前是正面，要反转
                        lltPositive.setVisibility(View.GONE);
                        rltNegative.setVisibility(View.VISIBLE);
                    }else {
                        cardPositive = true;
                        //当前是反面，要反转
                        lltPositive.setVisibility(View.VISIBLE);
                        rltNegative.setVisibility(View.GONE);
                    }
                    break;
                case R.id.validatingEditor_singleItemLearning:
                    tv_validatingEditor.requestFocus();
                    manager.showSoftInput(tv_validatingEditor, InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }

        }

}
