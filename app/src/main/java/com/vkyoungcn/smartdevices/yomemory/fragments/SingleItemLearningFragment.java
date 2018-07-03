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
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private TextView tv_restChances;
    private int restTipChances = 3;//默认3次开始。

    private TextView tv_priorityAdd;
    private TextView tv_priorityNum;
    private TextView tv_priorityMinus;
    private TextView tv_errNum;

    private TextView tv_surfaceName;
    private LinearLayout llt_surfaceVe;
    private ImageView imv_surfaceVe;
    private ValidatingEditor ve_ValidatingEditor;

    private TextView tv_phonetic;
    private TextView tv_translation;

    private boolean isNameSurfaceOn = true;
    private Boolean pageSlidingAvailable = false;//【旧版待删】只有在翻面后输入了正确的拼写后才可翻页。（此时可以再翻回正面）
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

        tv_restChances = (TextView)rootView.findViewById(R.id.tv_restChances_sIL_CDV);
        tv_priorityAdd = (TextView)rootView.findViewById(R.id.tv_priorityAdd_sIL_CDV);
        tv_priorityNum = (TextView)rootView.findViewById(R.id.tv_priorityNum_sIL_CDV);
        tv_priorityMinus = (TextView)rootView.findViewById(R.id.tv_priorityMinus_sIL_CDV);
        tv_errNum = (TextView)rootView.findViewById(R.id.tv_errNum_sIL_CDV);

        tv_surfaceName = (TextView) rootView.findViewById(R.id.tv_surfaceName_sIL_CDV);
        llt_surfaceVe = (LinearLayout) rootView.findViewById(R.id.llt_surfaceVe_sIL_CDV);
        imv_surfaceVe = (ImageView)rootView.findViewById(R.id.imv_showTip_sIL_CDV);

        ve_ValidatingEditor = (ValidatingEditor) rootView.findViewById(R.id.ve_singleItemLearning);
//        ve_ValidatingEditor.requestFocus();//因为发现焦点默认在cardView上。[改为点击后申请获取]

        tv_phonetic = (TextView) rootView.findViewById(R.id.tv_phonetic_singleItemLearning);
        tv_translation = (TextView) rootView.findViewById(R.id.tv_translation_singleItemLearning);


        tv_restChances.setText(String.format(getContext().getResources().getString(R.string.rest_chances),restTipChances));

        tv_priorityAdd.setOnClickListener(this);
        tv_priorityNum.setText(String.valueOf(singleItem.getPriority()));
        tv_priorityMinus.setOnClickListener(this);

        tv_errNum.setText(String.valueOf(singleItem.getFailedSpelling_times()));

        tv_surfaceName.setText(singleItem.getName());
//        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/GentiumPlus_I.ttf");
//        tv_ext1.setTypeface(typeface);
        tv_phonetic.setText(singleItem.getPhonetic());
        tv_translation.setText(singleItem.getTranslations());


        ve_ValidatingEditor.setTargetText(singleItem.getName());
        ve_ValidatingEditor.setCodeReadyListener(mListener);//该监听由Activity实现，这样就将二者关联起来了。
        ve_ValidatingEditor.setOnClickListener(this);//【新版中，VE点击只获取焦点】

        EditorInfo veEditorInfo = new EditorInfo();
        veEditorInfo.inputType = InputType.TYPE_NULL;
        ve_ValidatingEditor.onCreateInputConnection(veEditorInfo);

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
                case R.id.tv_priorityAdd_sIL_CDV:
                    //优先级+1。
                    short priorityForAdd = singleItem.getPriority();
                    if(priorityForAdd == 9)return;//达到最高级9级时，无效。
                    singleItem.setPriority((short)(priorityForAdd+ 1));
                    tv_priorityNum.setText(String.valueOf(priorityForAdd+1));
                    break;
                case R.id.tv_priorityMinus_sIL_CDV:
                    //优先级-1。
                    short priorityForMinus = singleItem.getPriority();
                    if(priorityForMinus == 1) return;//已是最低级，无效
                    singleItem.setPriority((short)(priorityForMinus- 1));
                    tv_priorityNum.setText(String.valueOf(priorityForMinus-1));

                    break;
                    //【考虑到都是引用参数，故而最后应该不需特别处理就能存入DB】

                case R.id.tv_surfaceName_sIL_CDV:
                    //点击后翻转到VE层面。
                    isNameSurfaceOn = false;
                    llt_surfaceVe.setVisibility(View.VISIBLE);
                    tv_surfaceName.setVisibility(View.GONE);

                    //其中，如果已是多次提示状态，则翻面后不再显示提示按钮。
                    if(restTipChances==0){
                        imv_surfaceVe.setVisibility(View.INVISIBLE);
                        imv_surfaceVe.setClickable(false);
                    }

                    break;
                case R.id.imv_showTip_sIL_CDV:
                    //点击后（限次3次）翻转到Name层面
                    //3次后设为invisible（不能设gone）
                    if(restTipChances>0) {
                        //仍有可用的提示次数，则
                        isNameSurfaceOn = true;
                        restTipChances--;
                        llt_surfaceVe.setVisibility(View.GONE);
                        tv_surfaceName.setVisibility(View.VISIBLE);

                    }

                    break;
                case R.id.ve_singleItemLearning:
                    //点击后申请焦点
                    ve_ValidatingEditor.requestFocus();
                    break;


                case R.id.card_view:
                    if(isNameSurfaceOn) {
                        isNameSurfaceOn = false;
                        //当前是正面，要反转
                        tv_surfaceName.setVisibility(View.GONE);
                        llt_surfaceVe.setVisibility(View.VISIBLE);
                    }else {
                        isNameSurfaceOn = true;
                        //当前是反面，要反转
                        tv_surfaceName.setVisibility(View.VISIBLE);
                        llt_surfaceVe.setVisibility(View.GONE);
                    }
                    break;
                case R.id.validatingEditor_singleItemLearning:
                    ve_ValidatingEditor.requestFocus();
                    manager.showSoftInput(ve_ValidatingEditor, InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }

        }

}
