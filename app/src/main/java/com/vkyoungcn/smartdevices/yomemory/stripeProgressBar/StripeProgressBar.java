package com.vkyoungcn.smartdevices.yomemory.stripeProgressBar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.vkyoungcn.smartdevices.yomemory.R;

import java.util.ArrayList;

/*
* 条纹状进度条，根据对应卡片的正误情况显示相应颜色。当前页显示白色稍长的（竖）线。
* */
public class StripeProgressBar extends View {
    private static final String TAG = "StripeProgressBar";
    private static final int DEFAULT_LENGTH = 36;
    private Context mContext;
    
    private ArrayList<String> currentCode;
    private ArrayList<String> targetCode;
    private ArrayList<Integer> emptyCards;//从0起,存的是卡片序列（字串列表序列）的索引。
    private ArrayList<Integer> wrongCards;//从0起。

    private SingleStripe[] stripeSections;
    private Paint singleStripeCorrectPaint;
    private Paint singleStripeNonCorrectPaint;
    private Paint singleStripeEmptyPaint;
    private Paint currentLineWhitePaint;
    private Paint somethingWrongPaint;

    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int singleStripeColorCorrect;
    private int singleStripeColorNonCorrect;
    private int singleStripeColorEmpty;
    private int somethingWrongDark;
    private int currentLineWhite;

    private int singleStripeStrokeWidth;


    //这个可能是程序默认需要的
    public StripeProgressBar(Context context) {
        super(context);
        this.mContext = context;
        this.currentCode = new ArrayList<>();
        this.targetCode = new ArrayList<>();

        init(null);
    }

    public StripeProgressBar(Context context,ArrayList<String> currentCode, ArrayList<String> targetCode) {
        super(context);
        this.mContext = context;
        this.currentCode = currentCode;
        this.targetCode = targetCode;

        init(null);
    }

    public StripeProgressBar(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mContext = context;
        this.currentCode = new ArrayList<>();
        this.targetCode = new ArrayList<>();

        init(attributeset);
    }


    public StripeProgressBar(Context context, AttributeSet attributeset, int defStyledAttrs) {
        super(context, attributeset, defStyledAttrs);
        mContext = context;
        this.currentCode = new ArrayList<>();
        this.targetCode = new ArrayList<>();

        init(attributeset);
    }


    private void init(AttributeSet attributeset) {
        initDefaultAttributes();
        initPaint();
        initViewOptions();
    }


    private void initDefaultAttributes() {
        singleStripeColorCorrect = ContextCompat.getColor(mContext,R.color.singleStripe_color_correct);
        singleStripeColorNonCorrect = ContextCompat.getColor(mContext,R.color.singleStripe_color_nonCorrect);
        singleStripeColorEmpty = ContextCompat.getColor(mContext,R.color.singleStripe_color_empty);
        somethingWrongDark = ContextCompat.getColor(mContext,R.color.some_thing_wrong);
        currentLineWhite = ContextCompat.getColor(mContext,R.color.enclose_white);

        singleStripeStrokeWidth = 8;//【？】
    }


    private void initPaint() {
        singleStripeCorrectPaint = new Paint();
        singleStripeCorrectPaint.setColor(singleStripeColorCorrect);
        singleStripeCorrectPaint.setStrokeWidth(singleStripeStrokeWidth);
        singleStripeCorrectPaint.setStyle(Paint.Style.STROKE);

        singleStripeNonCorrectPaint = new Paint();
        singleStripeNonCorrectPaint.setColor(singleStripeColorNonCorrect);
        singleStripeNonCorrectPaint.setStrokeWidth(singleStripeStrokeWidth);
        singleStripeNonCorrectPaint.setStyle(Paint.Style.STROKE);

        singleStripeEmptyPaint = new Paint();
        singleStripeEmptyPaint.setColor(singleStripeColorEmpty);
        singleStripeEmptyPaint.setStrokeWidth(singleStripeStrokeWidth);
        singleStripeEmptyPaint.setStyle(Paint.Style.STROKE);

        currentLineWhitePaint = new Paint();
        currentLineWhitePaint.setStyle(Paint.Style.STROKE);
        currentLineWhitePaint.setStrokeWidth(singleStripeStrokeWidth);
        currentLineWhitePaint.setColor(currentLineWhite);

        somethingWrongPaint = new Paint();
        somethingWrongPaint.setStyle(Paint.Style.FILL);
        somethingWrongPaint.setAntiAlias(true);
        somethingWrongPaint.setColor(somethingWrongDark);

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
        int minDesiredWidth;
        if(targetCode!=null && !targetCode.isEmpty()) {
            minDesiredWidth = targetCode.size() * 8;//wrap_content时按每条宽8像素计算。(单位暂按像素值理解）
        }else {
            minDesiredWidth = 288;//按36*8。
        }
        int minDesiredHeight = 50;//其中currentBar高度50，其余高40。

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//【经查，此值单位是像素pixels】
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int resultWidth;
        int resultHeight;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            //此时代表xml设置为了具体值或match_parent
            resultWidth = widthSize;
        }else {
            //此时xml设置为了wrap_content
            resultWidth = minDesiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            //Must be this size
            resultHeight = heightSize;
        } else {
            //Be whatever you want
            resultHeight = minDesiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(resultWidth, resultHeight);
    }


    //【学：据说是系统计算好控件的实际尺寸后以本方法通知用户】
    // 【调用顺序：M(多次)-S(单次)-D】。
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        sizeChangedHeight = h;
        sizeChangedWidth = w;

        initStripes();
        super.onSizeChanged(w, h, old_w, old_h);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(targetCode.isEmpty()) {
            //绘制为全灰（深灰）
            canvas.drawRect(0,0,sizeChangedWidth,sizeChangedHeight,somethingWrongPaint);
            //这个坐标系是基于已给定到本控件的位置体现而确定的。

            return;
        }

        //根据两个列表的逐位对比结果，逐位绘制
        int totalSize = targetCode.size();
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
                paint = singleStripeCorrectPaint;
            }else if(singleStripe.getState() == SingleStripe.SINGLE_STRIPE_UN_CORRECT){
                paint = singleStripeNonCorrectPaint;
            }else if(singleStripe.getState() == SingleStripe.SINGLE_STRIPE_EMPTY){
                paint = singleStripeEmptyPaint;
            }else {
                paint = currentLineWhitePaint;
                //此时，是“当前条纹”，覆盖其高度绘制坐标（比其他略长）
                fromY = 0;
                toY = sizeChangedHeight;
            }

            canvas.drawLine(fromX, fromY, toX, toY, paint);

        }

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。
    }




    public ArrayList<Integer> getEmptyPositions() {
        return emptyCards;
    }

    public ArrayList<Integer> getWrongPositions() {
        return wrongCards;
    }



    public void setTargetCodes(ArrayList<String> targetCode) {
        this.targetCode = targetCode;
    }

    public void setCurrentCodes(ArrayList<String> currentCode) {
        this.currentCode = currentCode;
    }



    //仅在初始时调用（另有用于滑动更新的版本）
    private void initStripes() {

        //正误状态列表初始化
        wrongCards= new ArrayList<>();
        emptyCards= new ArrayList<>();

        //获取一共有多少“节”
        int totalSize = targetCode.size();
        stripeSections = new SingleStripe[totalSize];

        //确定各节的状态——正确、错误、未填写、当前。(最后对当前项进行二次赋值以便覆盖)
        //另有reInit方法用于卡片滑动后改写进度条。
        for (int i = 0; i < totalSize; i++) {
            if(currentCode.get(i)!=null && !currentCode.get(i).isEmpty()){
                //该项非NULL、非空，则可进行比较
                if(currentCode.get(i).equals(targetCode.get(i))){
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
    }

    //用于某些无滑动而需要刷新的情形，（其实只刷新状态列表也行）
    // 【调用前需要由外部调用方先改变currentString列表】。
    public void handyCurrentReFresh(int currentPosition ){
        reInitStripes(currentPosition,currentPosition);
        invalidate();
    }

    //用于“因页面滑动”而引发刷新
    public void reFreshStripes(int lastPosition,int currentPosition ){
        reInitStripes(lastPosition,currentPosition);
        invalidate();
    }

    //外界（调用方）在翻页时，可能填入了新的字串，翻页后可根据新的字串列表重新计算本控件（另需一个通知更新的方法）。
    // 本方法中①确定各条纹的颜色状态；②将字串列表的正误状态存储到相应列表。
    //传入改动发生的页面（0起）
    private void reInitStripes(int changingPosition, int currentPosition) {

        //正误状态列表中相关项目清空(如果有的话)
        wrongCards.remove(changingPosition);
        emptyCards.remove(changingPosition);

        //改动的条纹节进行重写
        if(currentCode.get(changingPosition)!=null && !currentCode.get(changingPosition).isEmpty()){
            //该项非NULL、非空，则可进行比较
            if(currentCode.get(changingPosition).equals(targetCode.get(changingPosition))){
                //比较后，值正确
                stripeSections[changingPosition].setState(SingleStripe.SINGLE_STRIPE_CORRECT);
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
    }
}
