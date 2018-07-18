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

import java.text.DecimalFormat;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class LiteProgress extends View {
//* 双色进度条，根据即时传入的百分比（采用float）确定显示。不刷新；

    //    private static final String TAG = "LiteProgress";
    private float percentage = 0f;//按照这个数据绘制位置。

    //onDraw中使用，但不能在onDraw中开辟大型变量，提前全局开辟。
    RectF totalRectF = new RectF();
    RectF doneRectF = new RectF();
    DecimalFormat dFormat = new DecimalFormat("0.0");

    private float minDesiredWidth;
    private float DesiredHeight;
    private float maxDesiredWidth;
    private float startPadding;
    private float endPadding;
    private float bottomPadding;
    private float barHeight;
//    private float lineWidth;
    private float gapText_Bar;
    private float indexLineExtraSize;
    private float roundX;
    private float roundY;

    private float numberSize;

    private float strokeWidth;
    private float thickStrokeWidth;


    private Paint paintDone;
    private Paint paintOutLine;
    private Paint paintSingleLine;
    private Paint paintNumber;

    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int colorDone;
    private int colorOutLine;
    private int colorNumbers;
    private int colorSingleLine;




    //这个可能是程序默认需要的
    public LiteProgress(Context context) {
        super(context);

        init();
    }

    public LiteProgress(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        //【实测系统调用的是这个】

        init();
    }


    public LiteProgress(Context context, AttributeSet attrs, int defStyledAttrs) {
        super(context, attrs, defStyledAttrs);

        init();
    }


    private void init() {
        initDefaultAttributes();
        initColor();
        initPaint();
        initViewOptions();
    }

    private void initDefaultAttributes() {
        //最小值按各尺寸要求设计。本控件只在水平方向拉伸，高度不变。
        minDesiredWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        DesiredHeight =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        maxDesiredWidth =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());

        startPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        endPadding= (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()));
        bottomPadding=  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

        barHeight =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        gapText_Bar =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        indexLineExtraSize =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        roundX =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        roundY =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

//        lineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        numberSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());

        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        thickStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());


    }

    private void initColor(){
        colorDone = ContextCompat.getColor(getContext(),R.color.litePB_color_done);
        colorOutLine = ContextCompat.getColor(getContext(),R.color.litePB_color_outLine);
        colorSingleLine = ContextCompat.getColor(getContext(),R.color.litePB_color_index);
        colorNumbers = ContextCompat.getColor(getContext(),R.color.litePB_color_char);
    }

    private void initPaint() {
        paintDone = new Paint();
        paintDone.setColor(colorDone);
        paintDone.setStrokeWidth(strokeWidth);
        paintDone.setStyle(Paint.Style.FILL);

        /*
        paintBk = new Paint();
        paintBk.setColor(colorDone);
        paintBk.setStrokeWidth(strokeWidth);
        paintBk.setStyle(Paint.Style.FILL);
        */

        paintOutLine = new Paint();
        paintOutLine.setColor(colorOutLine);
        paintOutLine.setStrokeWidth(2);
        paintOutLine.setStyle(Paint.Style.STROKE);

        paintNumber = new Paint();
        paintNumber.setColor(colorNumbers);
        paintNumber.setStrokeWidth(3);//笔画太粗不好看，2~3px可以，2dp就不好看了。
        paintNumber.setStyle(Paint.Style.STROKE);
        paintNumber.setTextSize(numberSize);

        paintSingleLine = new Paint();
        paintSingleLine.setColor(colorSingleLine);
        paintSingleLine.setStrokeWidth(thickStrokeWidth);
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
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//【单位是像素pixels】
        int resultWidth;
        int resultHeight;

        //设置宽度
        if(widthSize<minDesiredWidth){
            resultWidth = (int)minDesiredWidth;//可用空间过小时，强制绘制为足够宽（可能会产生遮盖）
        }else if(widthSize>maxDesiredWidth){
            resultWidth = (int)maxDesiredWidth;//太大时，没有必要占用过大空间，按所需最大值使用
        }else {
            resultWidth = widthSize;
        }

        //设置高度（无论获得的可用空间多少，强制按所需高度申请。）
            resultHeight = (int)DesiredHeight;

        //MUST CALL THIS
        setMeasuredDimension(resultWidth, resultHeight);
    }


    //【学：据说是系统计算好控件的实际尺寸后以本方法通知用户】
    // 【调用顺序：M(多次)-S(单次)-D】。
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        sizeChangedHeight = h;
        sizeChangedWidth = w;

        super.onSizeChanged(w, h, old_w, old_h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float totalBarWidth = sizeChangedWidth-startPadding-endPadding;
        float doneWidth = totalBarWidth*percentage;

        //调试用：绘制范围展示
//        canvas.drawRect(0,0,sizeChangedWidth,sizeChangedHeight,paintBk);

        //分段绘制
        float fromX = startPadding;
        float fromY = sizeChangedHeight-bottomPadding-barHeight;
        float doneToX = startPadding+doneWidth;
        float totalToX = sizeChangedWidth-endPadding;
        float toY = sizeChangedHeight-bottomPadding;

        //在左右两端各增加一点额外距离以使圆角不被index指示线遮盖
        //外框是圆角矩形
        totalRectF.top =fromY;
        totalRectF.left = fromX-roundX;
        totalRectF.bottom = toY;
        totalRectF.right= totalToX;

        doneRectF.top = fromY+1;//让出一个像素
        doneRectF.left = fromX-roundX+1;//需要覆盖住下方的外框，但是让出一个像素
        doneRectF.bottom = toY-1;
        doneRectF.right = doneToX;

        //先绘制外框
        canvas.drawRoundRect(totalRectF,roundX,roundY,paintOutLine);

        //绘制完成部分
        canvas.drawRoundRect(doneRectF,roundX,roundY,paintDone);


        //绘制位置指示线(依靠指示线自身宽度遮盖完成区右侧的圆角)
        float lineFromY = fromY-indexLineExtraSize;//要求上端画出界一点
        float lineToY = toY;//下方过界不好看。
        canvas.drawLine(doneToX,lineFromY,doneToX,lineToY,paintSingleLine);

        //绘制数字(进度靠后时，起始点应左移以免最后右侧越界)
        if(percentage>0.95) {
            canvas.drawText(dFormat.format(percentage * 100) + "%", doneToX - 3*numberSize, fromY - gapText_Bar, paintNumber);
        }else if(percentage>0.9){
            canvas.drawText(dFormat.format(percentage * 100) + "%", doneToX - 2*numberSize, fromY - gapText_Bar, paintNumber);
        }else if(percentage>0.3){
            canvas.drawText(dFormat.format(percentage * 100) + "%", doneToX-numberSize, fromY - gapText_Bar, paintNumber);
        }else {
            canvas.drawText(dFormat.format(percentage * 100) + "%", doneToX, fromY - gapText_Bar, paintNumber);

        }
    }

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。


    //当进度值改变从而需要更新UI时
    public void setPercentage(float currentPercentage ){
        percentage = currentPercentage;
        invalidate();
    }

}
