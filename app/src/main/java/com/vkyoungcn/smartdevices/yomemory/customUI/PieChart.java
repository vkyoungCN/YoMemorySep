package com.vkyoungcn.smartdevices.yomemory.customUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.vkyoungcn.smartdevices.yomemory.R;

/*
* 饼图
* 暂时实现为一个只有两项数据(其中一是总数、另一是单项分数)的、参数既定的简单版。
* 当然，数据是动态生成的。
* */
public class PieChart extends View {
    private static final String TAG = "PieChart";
    private Context mContext;

//    private int donePercentage = 0;//按照这个数据绘制位置。
    private int totalAmount;//分母（总数）部分
    private int fractionalAmount;//分子部分

    int minDesiredWidth;//最小需要的尺寸，稍后直接在代码设定（暂定153dps,是原设计306dp的一半））。
    int minDesiredHeight;//(暂定90dp)
    int maxDesiredWidth;//最大尺寸设定为306dp
    int maxDesiredHeight;//(180dp)
    int lineWidth;//折线宽
    int textSize;

    RectF rectF =  new RectF();

// 以下，没必要设置该属性。该尺寸只能根据得到的最终画布进行按比例分配。
//    int dimension_large;//设计模型图中，有大小两种尺寸，分别用于按比例组合出全图尺寸。
// 文本区宽度、圆的半径、右侧标识区宽度采用大尺寸（暂定54dp，最小模式减半）
//    int dimension_small;
// 折线A区、上下横置文本条高度采用小尺寸（暂定18dp，最小模式减半）


    private Paint paintPieTotal;
    private Paint paintPieFraction;
    private Paint paintEmptyOutLine;//绘制两种扇形交界位置上的白框（是一个无填充的、半径稍大？的、套在小扇形之外的框装扇形）
    private Paint paintLine;
    private Paint paintText;

    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int colorTotal;
    private int colorFraction;
    private int colorText;
    private int colorLine;
    private int colorEmptyOutLine;

    private int strokeWidth =4;
    private int thinStrokeWidth = 2;//折线用
    private int thickStrokeWidth = 8;//框扇用


    public PieChart(Context context) {
        super(context);
        this.mContext = context;
        init(context,null,0);
    }

    public PieChart(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mContext = context;
        //【实测系统调用的是这个】
        init(context,attributeset,0);
    }


    public PieChart(Context context, AttributeSet attrs, int defStyledAttrs) {
        super(context, attrs, defStyledAttrs);
        mContext = context;

        init(context,attrs,defStyledAttrs);
    }


    private void init(Context context, AttributeSet attrs, int defStyledAttrs) {
        initSize();
        initColor();
        initPaint();
        initViewOptions();
    }

    private void initSize() {
        /*
         * 在此，暂不采用xml设置的方式，各预置参数简单地在本代码内设定完成。
         */

                    minDesiredWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 153, getResources().getDisplayMetrics());
                    minDesiredHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
                    maxDesiredWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 306, getResources().getDisplayMetrics());
                    maxDesiredHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics());

                    lineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
                    textSize =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());

           // rectF = new RectF();

    }

    private void initColor(){
        colorTotal = ContextCompat.getColor(getContext(), R.color.pieChart_total);//由于总数对应“未完成”部分，因而设置为灰色
        colorFraction = ContextCompat.getColor(getContext(),R.color.pieChart_fraction);//其实对应已完成部分。
        colorEmptyOutLine = ContextCompat.getColor(getContext(),R.color.pieChart_fraction);
        colorLine = ContextCompat.getColor(getContext(),R.color.picChart_line);
        colorText = ContextCompat.getColor(getContext(),R.color.picChart_text);
    }

    private void initPaint() {
        paintPieTotal = new Paint();
        paintPieTotal.setColor(colorTotal);
        paintPieTotal.setStrokeWidth(strokeWidth);
        paintPieTotal.setStyle(Paint.Style.FILL);

        paintPieFraction = new Paint();
        paintPieFraction.setColor(colorFraction);
        paintPieFraction.setStrokeWidth(strokeWidth);
        paintPieFraction.setStyle(Paint.Style.FILL);

        paintEmptyOutLine = new Paint();
        paintEmptyOutLine.setColor(colorFraction);
        paintEmptyOutLine.setStrokeWidth(thickStrokeWidth);
        paintEmptyOutLine.setStyle(Paint.Style.STROKE);

        paintLine = new Paint();
        paintLine.setColor(colorLine);
        paintLine.setStrokeWidth(thinStrokeWidth);
        paintLine.setStyle(Paint.Style.STROKE);

        paintText = new Paint();
        paintText.setColor(colorText);
        paintText.setStrokeWidth(strokeWidth);
        paintText.setStyle(Paint.Style.STROKE);
        paintText.setTextSize(textSize);
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

        //设置宽度
        if(widthSize<minDesiredWidth){
            resultWidth = minDesiredWidth;
        }else if(widthSize>maxDesiredWidth){
            resultWidth = maxDesiredWidth;
        }else {
            resultWidth = widthSize;
        }

        //设置高度
        if(heightSize<minDesiredHeight){
            resultHeight = minDesiredHeight;
        }else if(heightSize>maxDesiredHeight){
            resultHeight = maxDesiredHeight;
        }else {
            resultHeight = heightSize;
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
//        Log.i(TAG, "onSizeChanged: scH="+h+",scW="+w);
        super.onSizeChanged(w, h, old_w, old_h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float sizeLarge;
        float sizeSmall;
        sizeSmall = sizeChangedWidth/12f;
        sizeLarge = sizeSmall*2;

        rectF.left = sizeLarge+sizeSmall;
        rectF.top = 2*sizeSmall;
        rectF.right = 3*sizeLarge+sizeSmall;
        rectF.bottom = 2*sizeLarge+2*sizeSmall;


        //先绘制底色（为了使白框看上去像透明）
        //【待】


        //再绘制最右侧图例区
        float legendLeft = 3*sizeLarge+2*sizeSmall+sizeSmall/3;
        float legendTop = 2*sizeSmall+sizeLarge+(sizeSmall/3)*2;
        float legendB_1 = 2*sizeSmall+sizeLarge+sizeSmall;

        //图例区·条彩1·总量（计划为18*6（w*6））
        canvas.drawRect(legendLeft,legendTop,legendLeft+sizeSmall,legendB_1,paintPieTotal);
        //图例区·文字1·总资源量
        canvas.drawText("总资源量",legendLeft,legendB_1+sizeSmall,paintText);
        //图例区·彩杠2·已完成量
        canvas.drawRect(legendLeft,legendB_1+5*(sizeSmall/3),legendLeft+sizeSmall,legendB_1+2*sizeSmall,paintPieFraction);
        //图例区·文字2·已完成量
        canvas.drawText("已完成",legendLeft,legendB_1+3*sizeSmall,paintText);


        //绘制饼区（两饼+框饼）
        if(totalAmount == 0){
            //此时不绘制饼，取代为绘制一条灰线【待补paint】

            return;
        }
        //其他情况下，不论任务资源多少，都须绘制完整灰饼（在底层）。
        canvas.drawArc(rectF,0,360,true,paintPieTotal);


        //根据分子数量绘制已完成的扇形和其白色外框。
        if(fractionalAmount == 0 ){
            //不绘制
            return;
        }

        //绘制饼外的文字



        //绘制折线



//        Log.i(TAG, "onDraw: startPd="+ startPadding+", endPd="+endPadding+"。Percentage="+donePercentage);
        doneWidth = ((float)(sizeChangedWidth-startPadding-endPadding)/100)*donePercentage;
        //分段绘制
        int fromX = startPadding;
        int fromY = sizeChangedHeight-bottomPadding-barHeight;
        int toX = sizeChangedWidth-endPadding;
        int toY = sizeChangedHeight-bottomPadding;
        Log.i(TAG, "onDraw: toX="+toX+"fromX+doneW="+(fromX+doneWidth));
        canvas.drawRect(fromX,fromY, fromX+doneWidth, toY,paintPieTotal);

        //绘制标识线
        float startX = fromX+doneWidth;
        canvas.drawLine(startX,fromY-bottomPadding,startX,toY, paintLine);

        //绘制后半段
        canvas.drawRect(startX+lineWidth,fromY, toX, toY, paintPieFraction);

        //绘制数字
        canvas.drawText(String.valueOf(donePercentage)+"%", startX, bottomPadding, paintText);

    }

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。


    //当进度值改变从而需要更新UI时
    public void setNewPercentage(int currentPercentage ){
        donePercentage = currentPercentage;
        invalidate();
    }

}
