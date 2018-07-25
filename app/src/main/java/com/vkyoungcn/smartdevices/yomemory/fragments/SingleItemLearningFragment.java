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
public class SingleItemLearningFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "SingleItemRePickingFrag";
    private static final String SINGLE_ITEM = "single_item";

    private SingleItem singleItem;

    private int restTipChances;//默认3次开始（外部有传入）。
    private String initText = "";//默认空
//    private boolean isTipLimitingFree = false;//当VE输入完成且正确（一遍）后，提示次数取消

    private TextView tv_priorityAdd;
    private TextView tv_priorityNum;
    private TextView tv_priorityMinus;
    private TextView tv_errNum;

    private TextView tv_surfaceName;
    private FrameLayout flt_surfaceName;
    private LinearLayout llt_surfaceVe;
    private TextView tv_TipRestTimes;
    private TextView tv_TipRestTimeLabel;
    private TextView tv_unLimitedLabel;
    private TextView tv_tipForCardClick;
    private FrameLayout flt_showTips;
    private ImageView imv_showTips;//图标在可用次数用完时消失（实际上的点击监听是所由在的flt监听的。）
    private ValidatingEditor ve_ValidatingEditor;

    private TextView tv_phonetic;
    private TextView tv_translation;

    private boolean isNameSurfaceOn = true;
    private Boolean pageSlidingAvailable = false;//【旧版待删】只有在翻面后输入了正确的拼写后才可翻页。（此时可以再翻回正面）
    InputMethodManager manager;
    private boolean softKBActive = false;
    private boolean isThisCardVeBeenCorrect;//当页面载入时根据Activity中持有的剩余次数列表（或正误列表）加载的本卡对应的正误情况

    private ValidatingEditor.OnValidatingEditorInputListener mListener;

    public SingleItemLearningFragment() {
        // Required empty public constructor
    }

    public static SingleItemLearningFragment newInstance(SingleItem singleItem,int restTipChances,String initText) {
        SingleItemLearningFragment fragment = new SingleItemLearningFragment();
        Bundle args = new Bundle();
        args.putParcelable(SINGLE_ITEM, singleItem);
        args.putInt("REST_TIP_CHANCES",restTipChances);
        args.putString("INIT_TEXT_VE",initText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            singleItem = getArguments().getParcelable(SINGLE_ITEM);
            restTipChances = getArguments().getInt("REST_TIP_CHANCES",3);
            initText = getArguments().getString("INIT_TEXT_VE");
        }
        manager = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_single_item_learning, container, false);
        CardView cardView =(CardView) rootView.findViewById(R.id.cardView_sIL);
        cardView.setOnClickListener(this);

        tv_priorityAdd = (TextView)rootView.findViewById(R.id.tv_priorityAdd_sIL_CDV);
        tv_priorityNum = (TextView)rootView.findViewById(R.id.tv_priorityNum_sIL_CDV);
        tv_priorityMinus = (TextView)rootView.findViewById(R.id.tv_priorityMinus_sIL_CDV);
        tv_errNum = (TextView)rootView.findViewById(R.id.tv_errNum_sIL_CDV);

        tv_surfaceName = (TextView) rootView.findViewById(R.id.tv_surfaceName_sIL_CDV);
        flt_surfaceName = (FrameLayout) rootView.findViewById(R.id.flt_surfaceName_sIL_CDV);
        llt_surfaceVe = (LinearLayout) rootView.findViewById(R.id.llt_surfaceVe_sIL_CDV);
        flt_showTips = (FrameLayout) rootView.findViewById(R.id.flt_showTips_sIL);
        flt_showTips.setOnClickListener(this);

        imv_showTips = (ImageView)rootView.findViewById(R.id.imv_showTips_sIL);

        tv_TipRestTimes = (TextView)rootView.findViewById(R.id.tv_restTipTimes_sIL_CDV);
        tv_tipForCardClick = rootView.findViewById(R.id.tv_tipForCardClick_sIL);
        tv_TipRestTimeLabel = (TextView)rootView.findViewById(R.id.tv_restTipLable_sIL);
        tv_unLimitedLabel = rootView.findViewById(R.id.tv_labelUnderUnLimited_sIL);
        //fg重新加载（pr滑动导致）时，要考虑卡片状态是否已经正确填写过一次。
        //如果是新开启（尚未成功填写）则tv显示剩余可提示次数
        // 如果已填写成功完全一次，则tv显示“打开输入框练习拼写”。
        if(restTipChances == -2){
            //已填写正确一次，-2代表“次数”无限
            tv_TipRestTimeLabel.setVisibility(View.GONE);
            tv_TipRestTimes.setVisibility(View.GONE);
            tv_unLimitedLabel.setVisibility(View.VISIBLE);

//            tv_TipRestTimeLabel.setText(getResources().getString(R.string.open_VE));
//            tv_TipRestTimes.setVisibility(View.GONE);//次数标签隐藏即可。
            tv_tipForCardClick.setVisibility(View.GONE);//底端提示条在这种模式下应取消。

        }else {
            //新卡片，有限制
            tv_TipRestTimeLabel.setVisibility(View.VISIBLE);
            tv_TipRestTimes.setVisibility(View.VISIBLE);
            tv_unLimitedLabel.setVisibility(View.GONE);

            tv_TipRestTimes.setText(String.valueOf(restTipChances));
            //label此时按默认文本即可
        }
        ve_ValidatingEditor = (ValidatingEditor) rootView.findViewById(R.id.ve_singleItemLearning);
//        ve_ValidatingEditor.requestFocus();//因为发现焦点默认在cardView上。[改为点击后申请获取]

        tv_phonetic = (TextView) rootView.findViewById(R.id.tv_phonetic_singleItemLearning);
        tv_translation = (TextView) rootView.findViewById(R.id.tv_translation_singleItemLearning);

        tv_priorityAdd.setOnClickListener(this);
        tv_priorityNum.setText(String.valueOf(singleItem.getPriority()));
        tv_priorityMinus.setOnClickListener(this);

        tv_errNum.setText(String.valueOf(singleItem.getFailedSpelling_times()));

        tv_surfaceName.setText(singleItem.getName());
//        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/GentiumPlus_I.ttf");
//        tv_ext1.setTypeface(typeface);
        tv_phonetic.setText(singleItem.getPhonetic());
        tv_translation.setText(singleItem.getTranslations());


        ve_ValidatingEditor.setTargetAndInitText(singleItem.getName(),initText);
//        ve_ValidatingEditor.setInitText(initText);
        ve_ValidatingEditor.setCodeReadyListener(mListener);//该监听由Activity实现，这样就将二者关联起来了。
        ve_ValidatingEditor.setOnClickListener(this);//VE点击获取焦点，打开软键盘

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
                    if(priorityForAdd > 7)return;//达到7级以上时，不可再手动增加（8、9由程序负责）
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

               /* case R.id.flt_surfaceName_sIL_CDV:
                    //点击后翻转到VE层面。
                    isNameSurfaceOn = false;
                    llt_surfaceVe.setVisibility(View.VISIBLE);
                    flt_surfaceName.setVisibility(View.GONE);

                    //其中，如果已是多次提示状态，则翻面后不再显示提示按钮。
                    //【这里需要由Activity全局持有，以免翻页翻回后重置】
                    if(restTipChances==0){
                        tv_TipRestTimes.setVisibility(View.INVISIBLE);
                        tv_TipRestTimes.setClickable(false);
                    }

                    break;*/
                case R.id.flt_showTips_sIL:
                    //点击后（限次3次）翻转到Name层面
                    //3次后设为invisible（不能设gone）

                    //如果已经取消次数限制（如VE已正确填写一遍）
                    if(restTipChances == -2){
                        //这是再次加载fg后发现之前已经有过正确的输入记录，因而按照“可无限提示”模式处理
                        if(isNameSurfaceOn){
                            //当前是正面
                            isNameSurfaceOn = false;
                            llt_surfaceVe.setVisibility(View.VISIBLE);
                            flt_surfaceName.setVisibility(View.GONE);

                            //注意提示区域的tvs已经在fg加载时或者初次“全部填写正确”时完成改变。
                            tv_unLimitedLabel.setText(getResources().getString(R.string.close_VE));//点击后是反面，所以要设置为“关闭”。
                        }else {
                            //直接翻到正面，（此时显示的文本已被外部Activity修改为“关闭输入框”）
                            isNameSurfaceOn = true;
                            llt_surfaceVe.setVisibility(View.GONE);
                            flt_surfaceName.setVisibility(View.VISIBLE);

                            tv_unLimitedLabel.setText(getResources().getString(R.string.open_VE));//点击后是正面，所以要设置为“打开”。
                        }
                    }else if(restTipChances>0&&!isNameSurfaceOn) {
                        //仍在“计次模式”
                        //仍有可用的提示次数，且处于翻面（正面就不用触发了，否则只减次数没效果。）
                        isNameSurfaceOn = true;
                        restTipChances--;
                        //可用次数减少则通知Activity，修改其持有的全局可用次数总列表
                        ((LearningActivity)getActivity()).modifyCardsRestTipChances(restTipChances);

                        llt_surfaceVe.setVisibility(View.GONE);
                        flt_surfaceName.setVisibility(View.VISIBLE);
                        tv_TipRestTimes.setText(String.valueOf(restTipChances));
                        /*if(restTipChances == 0){
                            //图标消失
                            imv_showTips.setVisibility(View.GONE);
                        }*/

                        //下方提示语改为“点击卡片打开输入框”
                        tv_tipForCardClick.setText(getResources().getString(R.string.click_card_to_VE_1));
                    }

                    break;

                case R.id.cardView_sIL:
                    if(isNameSurfaceOn) {
                        isNameSurfaceOn = false;
                        //当前是正面，要反转【点击卡片时只允许从正到翻面，以免影响提示次数统计】
                        flt_surfaceName.setVisibility(View.GONE);
                        llt_surfaceVe.setVisibility(View.VISIBLE);

                        //提示语：改为正确填写以完成学习
                        tv_tipForCardClick.setText(getResources().getString(R.string.click_card_to_VE_2));
                        tv_unLimitedLabel.setText(getResources().getString(R.string.close_VE));//点击后是反面，所以要设置为“关闭”。

                    }
                    break;
                case R.id.ve_singleItemLearning:
                    ve_ValidatingEditor.requestFocus();
                    manager.showSoftInput(ve_ValidatingEditor, InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }

        }

   /* public int getRestTipChances() {
        return restTipChances;
    }*/

   /* 当VE正确填写一次后(VE向Activity发送onCCR消息（只发一次），Activity修改持有的restTips列表，
   * Activity调用fg的本方法对当前的fg做出UI的改动)
   * 一、当时就要：①不再显示提示次数、可以无限点击；②提示次数的tv改为显示“打开输入框练习拼写”（正面）
   * 和“关闭输入框”（反面）；
   * 二、当重新加载该卡片所在fg时，要正确判断（所以要设置一个永久记录变量（暂时考虑由剩余次数==-1兼任））
   * */
   public void setTipLimitingFree(boolean isFree){
       if(isFree){
           restTipChances = -2;//单卡持有的剩余次数改变。兼任变量（设为-2）（比独立boolean更便于判断）
           tv_tipForCardClick.setVisibility(View.GONE);//低端提示条在这种模式下应取消。

           //卡片上的UI修改
           tv_TipRestTimes.setVisibility(View.GONE);
           tv_TipRestTimeLabel.setVisibility(View.GONE);
           tv_unLimitedLabel.setVisibility(View.VISIBLE);
           if(isNameSurfaceOn){
               //正面
               tv_unLimitedLabel.setText(getResources().getString(R.string.open_VE));
           }else {
               //现在是反面
               tv_unLimitedLabel.setText(getResources().getString(R.string.close_VE));
           }

       }
   }

}
