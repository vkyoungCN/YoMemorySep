package com.vkyoungcn.smartdevices.yomemory.stripeProgressBar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.vkyoungcn.smartdevices.yomemory.R;

import java.util.ArrayList;

import static com.vkyoungcn.smartdevices.yomemory.stripeProgressBar.SingleStripe.SINGLE_STRIPE_CORRECT;
import static com.vkyoungcn.smartdevices.yomemory.stripeProgressBar.SingleStripe.SINGLE_STRIPE_CURRENT;
import static com.vkyoungcn.smartdevices.yomemory.stripeProgressBar.SingleStripe.SINGLE_STRIPE_EMPTY;
import static com.vkyoungcn.smartdevices.yomemory.stripeProgressBar.SingleStripe.SINGLE_STRIPE_UN_CORRECT;

/*
 * 作者1：杨胜@中国海洋大学图书馆
 * 作者2：杨镇时@中国海洋大学
 * author：Victor Young @Ocean University of China
 * email: yangsheng@ouc.edu.cn
 *
 * 条纹状进度条，根据对应卡片的正误情况显示相应颜色。当前页显示白色稍长的（竖）线。
 * */
public class StripeProgressBar extends View {
    private static final String TAG = "StripeProgressBar";
    private static final int DEFAULT_LENGTH = 36;
    private Context mContext;

    private ArrayList<String> currentCodes;
    private ArrayList<String> targetCodes;//在本字段获得初始化之后，才能进行真正的绘制。
    //各绘制单位的属性，在本字段设置方法中触发
    int totalSize;//有多少个词。
    int currentPosition=1;//当前位置，默认1起。
    float unitSize;//最终使用大尺寸还是小尺寸，使用一个全局变量保持。
    private boolean unitSizeDone =false;//只计算一次即可。

    private ArrayList<Integer> emptyCards;//从0起,存的是卡片序列（字串列表序列）的索引。
    private ArrayList<Integer> wrongCards;//从0起。

    private SingleStripe[] stripeSections;
    private Paint paintCorrect;
    private Paint paintWrong;
    private Paint paintEmpty;
    private Paint paintCurrentOutLine;
    private Paint somethingWrongPaint;
    private Paint paintNumber;
    private Paint paint;//用于onDraw中逐块绘制时选取不同画笔。

    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private float minDesiredWidth;
    private float desiredHeight;

    private float minBarWidth;
    private float barHeight;
    private float unitLarge;
    private float unitSmall;

    private float startPadding;//前后缩进在onD中通过其他尺寸计算设置
    private float endPadding;
    private float bottomPadding;

    private float strokeWidth;
    private float outLineStrokeWidth;

    private float numberSize;

    private int colorCorrect;
    private int colorWrong;
    private int colorEmpty;
    private int somethingWrongDark;
    private int colorCurrentOutLine;
    private int colorNumbers;




    public StripeProgressBar(Context context) {
        super(context);
        this.mContext = context;
        this.currentCodes = new ArrayList<>();
        this.targetCodes = new ArrayList<>();

        init(null);
    }

    public StripeProgressBar(Context context, AttributeSet attributeset) {
        super(context, attributeset);
//        Log.i(TAG, "StripeProgressBar: CONSTRUCTOR2");
        //【调用的这个构造器】
        mContext = context;
        this.currentCodes = new ArrayList<>();
        this.targetCodes = new ArrayList<>();

        init(attributeset);
    }


    public StripeProgressBar(Context context, AttributeSet attributeset, int defStyledAttrs) {
        super(context, attributeset, defStyledAttrs);
        mContext = context;
        this.currentCodes = new ArrayList<>();
        this.targetCodes = new ArrayList<>();
//        Log.i(TAG, "StripeProgressBar: CONSTRUCTOR3");

        init(attributeset);
    }


    private void init(AttributeSet attributeset) {
        initSize();
        initColor();
        initPaint();
        initViewOptions();
    }

    private void initSize() {

        //保证在拥有最大容量36时，每个单位都能占据4dp。如果容量不足，则考虑向中缩进。
        minBarWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources().getDisplayMetrics());
        //本控件只在水平方向拉伸，高度不变。
        minDesiredWidth = minBarWidth+ 2*unitSmall;//控件宽+左右最小缩进
        desiredHeight =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
        //进度条本身高度
        unitLarge =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        unitSmall =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        bottomPadding=  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        outLineStrokeWidth =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        numberSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());


    }

    private void initColor() {
        colorCorrect = ContextCompat.getColor(mContext,R.color.singleStripe_color_correct);
        colorWrong = ContextCompat.getColor(mContext,R.color.singleStripe_color_nonCorrect);
        colorEmpty = ContextCompat.getColor(mContext,R.color.singleStripe_color_empty);
        somethingWrongDark = ContextCompat.getColor(mContext,R.color.some_thing_wrong);
        colorCurrentOutLine = ContextCompat.getColor(mContext,R.color.singleStripe_color_outLine);
        colorNumbers = ContextCompat.getColor(getContext(),R.color.litePB_color_char);
    }


    private void initPaint() {
        paintCorrect = new Paint();
        paintCorrect.setColor(colorCorrect);
        paintCorrect.setStrokeWidth(strokeWidth);
        paintCorrect.setStyle(Paint.Style.STROKE);

        paintWrong = new Paint();
        paintWrong.setColor(colorWrong);
        paintWrong.setStrokeWidth(strokeWidth);
        paintWrong.setStyle(Paint.Style.STROKE);

        paintEmpty = new Paint();
        paintEmpty.setColor(colorEmpty);
        paintEmpty.setStrokeWidth(strokeWidth);
        paintEmpty.setStyle(Paint.Style.STROKE);

        paintCurrentOutLine = new Paint();
        paintCurrentOutLine.setStyle(Paint.Style.STROKE);
        paintCurrentOutLine.setStrokeWidth(strokeWidth);
        paintCurrentOutLine.setColor(colorCurrentOutLine);

        somethingWrongPaint = new Paint();
        somethingWrongPaint.setStyle(Paint.Style.FILL);
        somethingWrongPaint.setColor(somethingWrongDark);

        paintNumber = new Paint();
        paintNumber.setColor(colorNumbers);
        paintNumber.setStrokeWidth(3);//笔画太粗不好看，2~3px可以，2dp就不好看了。
        paintNumber.setStyle(Paint.Style.STROKE);
        paintNumber.setTextSize(numberSize);
    }


    private void initViewOptions() {
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return false;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//【经查，此值单位是像素pixels】
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int resultWidth;
        int resultHeight;
        boolean wIsMin = false;//为了让H/W保持共同最小或共同最大。
        boolean wIsMax = false;
        boolean hIsMin = false;
        boolean hIsMax = false;

        //设置宽度
        if(widthSize<minDesiredWidth){
            resultWidth = (int)minDesiredWidth;//可用空间过小时，强制绘制为足够宽（可能会产生遮盖）
            wIsMin = true;
        }else {
            resultWidth = widthSize;
        }

        //设置高度
        resultHeight = (int)desiredHeight;//始终强制绘制

        //MUST CALL THIS
        setMeasuredDimension(resultWidth, resultHeight);

    }


    //【学：据说是系统计算好控件的实际尺寸后以本方法通知用户】
    // 【调用顺序：M(多次)-S(单次)-D】。
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        sizeChangedHeight = h;
        sizeChangedWidth = w;

//        initStripes();//【这里不能初始化方法，因为此时还没有初始数据，空指针错误】
        super.onSizeChanged(w, h, old_w, old_h);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(targetCodes == null){
            return;
            //暂未设置数据，无法绘制
        }else if(targetCodes.isEmpty()) {
            //绘制为全灰（深灰）
            canvas.drawRect(0,0,sizeChangedWidth,sizeChangedHeight,somethingWrongPaint);
            //这个坐标系是基于已给定到本控件的位置体现而确定的。
            return;
        }

        if(!unitSizeDone){
            totalSize = targetCodes.size();
            if(sizeChangedWidth>=(totalSize*unitLarge+2*unitLarge)) {
                //如果可用尺寸在按大尺寸单元绘制时够用，则按大尺寸进行，否则一律按小尺寸
//            Log.i(TAG, "onDraw: totalSize="+totalSize);
                unitSize = unitLarge;

//            Log.i(TAG, "onDraw: unitSize= Large");
            }else {
                unitSize = unitSmall;
//            Log.i(TAG, "onDraw: unitSize= Small");
            }
            barHeight = 2*unitSize;// 因实际尺寸过小，高度采用2倍。

            unitSizeDone = true;//只进行一次计算（但可能并不是在onDraw的第一次调用中进行）
        }


        //先求两侧剩余空间，设为最终padding值（单侧值）
        float paddingHorizontalSingle = (sizeChangedWidth-totalSize*unitSize)/2;

        //绘制总条
        //空间总高36dp，进度条绘制区大体在一半高度位置
        float startX = paddingHorizontalSingle;
        float startY = desiredHeight/2-unitSize;//一半在中线以上，一半在中线下
        float toX = sizeChangedWidth-paddingHorizontalSingle;
        float toY = startY+barHeight;

        canvas.drawRect(startX,startY,toX,toY,paintEmpty);

        //根据数据情况绘制总条
        //逐块绘制
        for (int i =0;i<totalSize;i++) {
            SingleStripe stripe = stripeSections[i];
            float startXPerUnit = paddingHorizontalSingle+i*unitSize;
            float toXPerUnit = paddingHorizontalSingle+(i+1)*unitSize;
            float startYPerUnit = startY;//要多出来一圈
            float toYPerUnit = toY;
            switch (stripe.getState()){
                case SINGLE_STRIPE_CORRECT:
                    paint = paintCorrect;
                    canvas.drawRect(startXPerUnit,startYPerUnit,toXPerUnit,toYPerUnit,paintCurrentOutLine);
                    break;
                case SINGLE_STRIPE_UN_CORRECT:
                    paint = paintWrong;
                    canvas.drawRect(startXPerUnit,startYPerUnit,toXPerUnit,toYPerUnit,paintCurrentOutLine);
                    break;
                case SINGLE_STRIPE_EMPTY:
                case SINGLE_STRIPE_CURRENT:
                    //不予绘制
                    break;
            }
        }


        //绘制当前位置
        float indexStartX = paddingHorizontalSingle+(currentPosition-1)*unitSize-strokeWidth;
        float indexToX = paddingHorizontalSingle+currentPosition*unitSize+strokeWidth;
        float indexStartY = startY-strokeWidth;//要多出来一圈
        float indexToY = toY+strokeWidth;

        canvas.drawRect(indexStartX,indexStartY,indexToX,indexToY,paintCurrentOutLine);


    }



        //根据两个列表的逐位对比结果，逐位绘制
        /*int totalSize = targetCodes.size();
        int widthPerStripe = sizeChangedWidth/totalSize;

        for (int i = 0; i < stripeSections.length; i++) {
            SingleStripe singleStripe = stripeSections[i];

            //计算各条坐标
            //按总宽平均计算每条宽度；高度上当前条全高、其他条两端各空出8像素。
            float fromX = i*widthPerStripe;
            float fromY = 5;//
            float toX = (i+1)*widthPerStripe-1;
            float toY = sizeChangedHeight-5;

            Paint paint;
            //确定各条的绘图颜色、笔触等
            if(singleStripe.getState() == SingleStripe.SINGLE_STRIPE_CORRECT){
                paint = paintCorrect;
            }else if(singleStripe.getState() == SingleStripe.SINGLE_STRIPE_UN_CORRECT){
                paint = paintWrong;
            }else if(singleStripe.getState() == SingleStripe.SINGLE_STRIPE_EMPTY){
                paint = paintEmpty;
            }else {
                paint = paintCurrentOutLine;
                //此时，是“当前条纹”，覆盖其高度绘制坐标（比其他略长）
                fromY = 0;
                toY = sizeChangedHeight;
            }

            canvas.drawLine(fromX, fromY, toX, toY, paint);

        }*/

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。




    public ArrayList<Integer> getEmptyPositions() {
        return emptyCards;
    }

    public ArrayList<Integer> getWrongPositions() {
        return wrongCards;
    }



    public void setTargetCodes(ArrayList<String> targetCode) {
        this.targetCodes = targetCode;
//        Log.i(TAG, "setTargetCodes: targetCodes.size="+targetCodes.size());
                initStripes();
    }

    public void setCurrentCodes(ArrayList<String> currentCode) {
        this.currentCodes = currentCode;
    }



    //只是进行初始化，仅在初始时调用（另有用于滑动更新的版本）
    //翻页时的修改另有其他方法。
    private void initStripes() {
        //获取一共有多少“节”
        totalSize = targetCodes.size();

        //正误状态列表初始化
        wrongCards = new ArrayList<>();
        emptyCards = new ArrayList<>();


        stripeSections = new SingleStripe[totalSize];
        //此时各节都是空的才对。只需获取容量尺寸。各节设为空状态。
        for (int i =0;i<totalSize;i++){
            SingleStripe stripe = new SingleStripe();
            stripe.setState(SingleStripe.SINGLE_STRIPE_EMPTY);
            stripeSections[i] = stripe;
        }

        //调用invalidate开始绘制
        invalidate();
    }

 /*
        //确定各节的状态——正确、错误、未填写、当前。(最后对当前项进行二次赋值以便覆盖)
        //另有reInit方法用于卡片滑动后改写进度条。
        for (int i = 0; i < totalSize; i++) {
            if(currentCodes.get(i)!=null && !currentCodes.get(i).isEmpty()){
                //该项非NULL、非空，则可进行比较
                if(currentCodes.get(i).equals(targetCodes.get(i))){
                    //比较后，值正确
                    stripeSections[i].setState(SingleStripe.SINGLE_STRIPE_CORRECT);
                }else {
                    //值错误
                    stripeSections[i].setState(SingleStripe.SINGLE_STRIPE_UN_CORRECT);
                    wrongCards.add(i);
                }
            }else {
                //该项空或NULL，未填
                stripeSections[i].setState(SingleStripe.SINGLE_STRIPE_EMPTY);
                emptyCards.add(i);
            }
        }
        stripeSections[0].setState(SingleStripe.SINGLE_STRIPE_CURRENT);//初始时，开始位显然位于开始【如果有额外逻辑再修改】
    }*/

    //用于某些无滑动而需要刷新的情形，（其实只刷新状态列表也行）
    // 【调用前需要由外部调用方先改变currentString列表】。
    /*public void handyCurrentReFresh(int currentPosition ){
        reInitStripes(currentPosition,currentPosition);
        invalidate();
    }*/

    //用于“因页面滑动”而引发刷新
    /*public void reFreshStripes(int lastPosition,int currentPosition ){
        reInitStripes(lastPosition,currentPosition);
        invalidate();
    }*/

    //外界（调用方）在翻页时，可能填入了新的字串，翻页后可根据新的字串列表重新计算本控件（另需一个通知更新的方法）。
    // 本方法中①确定各条纹的颜色状态；②将字串列表的正误状态存储到相应列表。
    //传入改动发生的页面（0起）

    /*
    * 本方法调用时，将根据当前的已输入字串列表（与目标列表对比），对条纹数组元素的状态进行更新；
    * 本方法只更改指定位置上的元素。
    * 同时需要指定新当前位置（从调用方的逻辑上说，需要跳转到新卡片页后，旧卡片状态才会提交过来）
    * */
    public void resetStripeAt(int changingPosition, int currentPosition) {
        this.currentPosition = currentPosition+1;

        //正误状态列表中相关项目清空(如果有的话)【用于向调用方返回统计结果（*本控件除UI职责外暂时还担负结果统计职责）】
        if(!wrongCards.isEmpty()) {
            wrongCards.remove((Integer) changingPosition);
        }
        if(!emptyCards.isEmpty()){
            emptyCards.remove((Integer) changingPosition);
        }

        //改动的条纹节进行重写
        if(currentCodes.get(changingPosition)!=null && !currentCodes.get(changingPosition).isEmpty()){
            //该项非NULL、非空，则可进行比较
            if(currentCodes.get(changingPosition).equals(targetCodes.get(changingPosition))){
                //比较后，值正确
                stripeSections[changingPosition].setState(SINGLE_STRIPE_CORRECT);
            }else {
                //值错误
                stripeSections[changingPosition].setState(SingleStripe.SINGLE_STRIPE_UN_CORRECT);
                wrongCards.add(changingPosition);
            }
        }else {
            //该项空或NULL，未填
            stripeSections[changingPosition].setState(SingleStripe.SINGLE_STRIPE_EMPTY);
            emptyCards.add(changingPosition);
        }
        stripeSections[currentPosition].setState(SingleStripe.SINGLE_STRIPE_CURRENT);

        invalidate();//数据更新后重绘控件。
    }
}