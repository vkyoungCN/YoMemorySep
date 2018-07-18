package com.vkyoungcn.smartdevices.yomemory.customUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.vkyoungcn.smartdevices.yomemory.R;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
/*

* */
public class HorizontalProgressBar extends View {
//* 双色进度条，根据即时传入的百分比（采用int数代表百分比的分子部分）即时刷新UI显示；
//* 已完成和未完成采取不同颜色。
//* 原则上并不需要太精确；
//* 不需要实时持续更新。

//    private static final String TAG = "horizontalProgressBar";
    private int donePercentage = 0;//按照这个数据绘制位置。

    int minDesiredWidth;
    int minDesiredHeight;
    int startPadding;
    int endPadding;
    int bottomPadding;
    int barHeight;
    int lineWidth;

    int numberSize;

    private Paint paintDone;
    private Paint paintNotYet;
    private Paint paintSingleLine;
    private Paint paintNumber;

    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int colorDone;
    private int colorNotYet;
    private int colorNumbers;
    private int colorSingleLine;

    private int strokeWidth =4; //这里设置的单位是像素
    private int thinStrokeWidth = 2;


    //这个可能是程序默认需要的
    public HorizontalProgressBar(Context context) {
        super(context);
        init(context,null,0);
    }

    public HorizontalProgressBar(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        //【实测系统调用的是这个】
        init(context,attributeset,0);
    }


    public HorizontalProgressBar(Context context, AttributeSet attrs, int defStyledAttrs) {
        super(context, attrs, defStyledAttrs);

        init(context,attrs,defStyledAttrs);
    }


    private void init(Context context, AttributeSet attrs, int defStyledAttrs) {
        initDefaultAttributes(context,attrs,defStyledAttrs);
        initColor();
        initPaint();
        initViewOptions();
    }

    private void initDefaultAttributes(Context context, AttributeSet attrs, int defStyledAttrs) {
        /*
         * 获取自定义属性
         *
         * 【注：在attr文件里声明了自定义属性后，还需要在布局文件中为各属性赋值。(如果不声明，则传入的数组内
         * 不包含该项的值，相应变量的值的初始也就无法完成。)
         * 而在布局文件中，只需要根控件中有apk-auto那句xmlns，在设置属性时只要填入app系统就能自动给出所有的选项】
         */
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.HorizontalProgressBar, defStyledAttrs, 0);
        int n = typedArray.getIndexCount();

        for (int i = 0; i < n; i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {
                case R.styleable.HorizontalProgressBar_desiredTotalWidth://【竟然是用下划线连接的】
                    minDesiredWidth = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_desiredTotalHeight:
                    minDesiredHeight = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_startPadding:
                    startPadding = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_endPadding:
                    endPadding = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_bottomPadding:
                    bottomPadding = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_barHeight:
                    barHeight = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_lineWidth:
                    lineWidth = typedArray.getDimensionPixelOffset(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.HorizontalProgressBar_numberSize:
                    numberSize = typedArray.getDimensionPixelOffset(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                /*
                【如果想手动设置颜色，则可在attr文件中设置相应自定义color属性，然后布局中设置值，在此获取。
                注意，本方法中传递的后一个参数（颜色int）只是用做默认值】
                【本程序中直接在代码里设置了（initColor()）。】
                case R.styleable.HorizontalProgressBar_numberSize:
                    textSize = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                    break;
                    */

            }
        }
        typedArray.recycle();
    }

    private void initColor(){
        colorDone = ContextCompat.getColor(getContext(),R.color.horizontalPB_color_done);
        colorNotYet = ContextCompat.getColor(getContext(),R.color.horizontalPB_color_notYet);
        colorSingleLine = ContextCompat.getColor(getContext(),R.color.horizontalPB_color_char);
        colorNumbers = ContextCompat.getColor(getContext(),R.color.horizontalPB_color_char);
    }

    private void initPaint() {
        paintDone = new Paint();
        paintDone.setColor(colorDone);
        paintDone.setStrokeWidth(strokeWidth);
        paintDone.setStyle(Paint.Style.FILL_AND_STROKE);

        paintNotYet = new Paint();
        paintNotYet.setColor(colorNotYet);
        paintNotYet.setStrokeWidth(strokeWidth);
        paintNotYet.setStyle(Paint.Style.STROKE);

        paintNumber = new Paint();
        paintNumber.setColor(colorNumbers);
        paintNumber.setStrokeWidth(strokeWidth);
        paintNumber.setStyle(Paint.Style.STROKE);
        paintNumber.setTextSize(numberSize);

        paintSingleLine = new Paint();
        paintSingleLine.setColor(colorSingleLine);
        paintSingleLine.setStrokeWidth(thinStrokeWidth);
        paintSingleLine.setStyle(Paint.Style.STROKE);

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


        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//【经查，此值单位是像素pixels】
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int resultWidth;
        int resultHeight;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            //此时代表xml设置为了具体值或可按最大进行【实际包括match_parent和wrap_content两种模式】
            resultWidth = widthSize;
        }else {
            //此时对应的是MeasureSpec.UNSPECIFIED（未指定）。【一共EXACTLY/AT_MOST/UNSPECIFIED三种】
            resultWidth = minDesiredWidth;
        }

        //Measure Height
        //测试表明，即使xml设置为wrap_content仍然会进入AT_MOST分支。
        if (heightMode == MeasureSpec.EXACTLY  || heightMode == MeasureSpec.AT_MOST) {
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

//        Log.i(TAG, "onSizeChanged: scH="+h+",scW="+w);
        super.onSizeChanged(w, h, old_w, old_h);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        float doneWidth;

//        Log.i(TAG, "onDraw: startPd="+ startPadding+", endPd="+endPadding+"。Percentage="+donePercentage);
        doneWidth = ((float)(sizeChangedWidth-startPadding-endPadding)/100)*donePercentage;
        //分段绘制
        int fromX = startPadding;
        int fromY = sizeChangedHeight-bottomPadding-barHeight;
        int toX = sizeChangedWidth-endPadding;
        int toY = sizeChangedHeight-bottomPadding;
//        Log.i(TAG, "onDraw: toX="+toX+"fromX+doneW="+(fromX+doneWidth));
        canvas.drawRect(fromX,fromY, fromX+doneWidth, toY,paintDone);

        //绘制标识线
        float startX = fromX+doneWidth;
        canvas.drawLine(startX,fromY-bottomPadding,startX,toY,paintSingleLine);

        //绘制后半段
        canvas.drawRect(startX+lineWidth,fromY, toX, toY,paintNotYet);

        //绘制数字
        canvas.drawText(String.valueOf(donePercentage)+"%", startX, bottomPadding, paintNumber);

    }

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。


    //当进度值改变从而需要更新UI时
    public void setNewPercentage(int currentPercentage ){
        donePercentage = currentPercentage;
        invalidate();
    }

}
