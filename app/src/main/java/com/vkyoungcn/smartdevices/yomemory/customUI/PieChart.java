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
* 饼图
* 暂时实现为一个只有两项数据(其中一是总数、另一是单项分数)的、参数既定的简单版。
* 当然，数据是动态生成的。
* */
public class PieChart extends View {
    private static final String TAG = "PieChart";
    private Context mContext;

//    private int donePercentage = 0;//按照这个数据绘制位置。
    private int totalAmount = 0;//分母（总数）部分
    private int fractionalAmount = 0;//分子部分

    int minDesiredWidth;//最小需要的尺寸，稍后直接在代码设定（暂定153dps,是原设计306dp的一半））。
    int minDesiredHeight;//(暂定90dp)
    int maxDesiredWidth;//最大尺寸设定为306dp
    int maxDesiredHeight;//(180dp)
    int lineWidth;//折线宽
    int textSize;

    int padding_H;//考虑到水平位置时文字区太宽不美观，加入一个缩进（暂时只在水平方向使用）；大小暂定9dp（是smallSize的一半）

    //不能在onDraw中开辟大型变量
    RectF rectF =  new RectF();
    DecimalFormat dFormat = new DecimalFormat("0.00");

    //避开0除错误，同时还要赋值，于是就全局化。
    float fractionalAngle;//子饼所占的角度，标量，符号在方法调用中加。
    float totalMiddleAngle;//总量饼折线（引出段）的引出角度（饼弧上的中位点）
    float fractionalMiddleAngle;

    String strFractionRatioIn2 ="";
    String strRestPartRatioIn2 ="";//未学习部分（虽然饼子是化成全饼，但毕竟只按其未遮盖部分代表未学）


// 以下，没必要设置该属性。该尺寸只能根据得到的最终画布进行按比例分配。
//    int dimension_large;//设计模型图中，有大小两种尺寸，分别用于按比例组合出全图尺寸。
// 文本区宽度、圆的半径、右侧标识区宽度采用大尺寸（暂定54dp，最小模式减半）
//    int dimension_small;
// 折线A区、上下横置文本条高度采用小尺寸（暂定18dp，最小模式减半）

    private Paint paintBackground;//可能要绘制统一底色，以便遮盖白框的外侧边。
    private Paint paintPieTotal;
    private Paint paintPieFraction;
    private Paint paintEmptyThickLine;//总量为空时，只绘制一条灰色半径线。
    private Paint paintOutLine;//绘制两种扇形交界位置上的白框（是一个无填充的、半径稍大？的、套在小扇形之外的框装扇形）
    private Paint paintLineTotal;
    private Paint paintTextTotal;
    private Paint paintLineFraction;
    private Paint paintTextFraction;
    private Paint paintTextGeneral;


    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int colorTotal;
    private int colorFraction;
    private int colorText;
    private int colorLine;
    private int colorOutLine;

    private int generalStrokeWidth =4;
    private int thinStrokeWidth = 1;//折线用
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

                    padding_H = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics());
           // totalRectF = new RectF();

    }

    private void initColor(){
        colorTotal = ContextCompat.getColor(getContext(), R.color.pieChart_total);//由于总数对应“未完成”部分，因而设置为灰色
        colorFraction = ContextCompat.getColor(getContext(),R.color.pieChart_fraction);//其实对应已完成部分。
        colorOutLine = ContextCompat.getColor(getContext(),R.color.pieChart_empty_out);
        colorLine = ContextCompat.getColor(getContext(),R.color.picChart_line);
        colorText = ContextCompat.getColor(getContext(),R.color.picChart_text);
    }

    private void initPaint() {
        paintBackground = new Paint();
        paintBackground.setColor(colorOutLine);//背景与外框扇同色
        paintBackground.setStrokeWidth(generalStrokeWidth);
        paintBackground.setStyle(Paint.Style.FILL);

        paintPieTotal = new Paint();
        paintPieTotal.setColor(colorTotal);
        paintPieTotal.setStrokeWidth(generalStrokeWidth);
        paintPieTotal.setStyle(Paint.Style.FILL);

        paintPieFraction = new Paint();
        paintPieFraction.setColor(colorFraction);
        paintPieFraction.setStrokeWidth(generalStrokeWidth);
        paintPieFraction.setStyle(Paint.Style.FILL);

        paintOutLine = new Paint();
        paintOutLine.setColor(colorOutLine);
        paintOutLine.setStrokeWidth(generalStrokeWidth);
        paintOutLine.setStyle(Paint.Style.STROKE);

        paintEmptyThickLine = new Paint();
        paintEmptyThickLine.setColor(colorTotal);//总量为空时画一条粗线，色同总量
        paintEmptyThickLine.setStrokeWidth(thickStrokeWidth);
        paintEmptyThickLine.setStyle(Paint.Style.STROKE);

        paintLineTotal = new Paint();
        paintLineTotal.setColor(colorTotal);
        paintLineTotal.setStrokeWidth(thinStrokeWidth);
        paintLineTotal.setStyle(Paint.Style.STROKE);

        paintTextTotal = new Paint();
        paintTextTotal.setColor(colorTotal);
        paintTextTotal.setStrokeWidth(generalStrokeWidth);
        paintTextTotal.setStyle(Paint.Style.STROKE);
        paintTextTotal.setTextSize(textSize);

        paintLineFraction = new Paint();
        paintLineFraction.setColor(colorFraction);
        paintLineFraction.setStrokeWidth(thinStrokeWidth);
        paintLineFraction.setStyle(Paint.Style.STROKE);

        paintTextFraction = new Paint();
        paintTextFraction.setColor(colorFraction);
        paintTextFraction.setStrokeWidth(generalStrokeWidth);
        paintTextFraction.setStyle(Paint.Style.STROKE);
        paintTextFraction.setTextSize(textSize);

        paintTextGeneral = new Paint();
        paintTextGeneral.setColor(colorText);
        paintTextGeneral.setStrokeWidth(generalStrokeWidth);
        paintTextGeneral.setStyle(Paint.Style.STROKE);
        paintTextGeneral.setTextSize(textSize);
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
            resultWidth = minDesiredWidth;//可用空间过小时，强制绘制为足够宽（可能会产生遮盖）
            wIsMin = true;
        }else if(widthSize>maxDesiredWidth){
            resultWidth = maxDesiredWidth;//太大时，没有必要占用过大空间，按所需最大值使用
            wIsMax =true;
        }else {
            resultWidth = widthSize;
        }

        //设置高度
        if(heightSize<minDesiredHeight){
            resultHeight = minDesiredHeight;
            hIsMin = true;
        }else if(heightSize>maxDesiredHeight){
            resultHeight = maxDesiredHeight;
            hIsMax = true;
        }else {
            resultHeight = heightSize;
        }

        if(hIsMin||wIsMin){
            //二者有一为最小，则结果申请为最小
            resultWidth = minDesiredWidth;
            resultHeight = minDesiredHeight;
        }else if(!hIsMax||!wIsMax){
            //二者有一项未达最大，则按比例缩放
            if(heightSize/8f<widthSize/12f) {
                //此时高较小，宽要按高度的成比例值申请
                resultHeight = heightSize;
                resultWidth = (int)((heightSize/8f)*12);
            }else {
                //等比例或按宽较小
                resultWidth = widthSize;
                resultHeight = (int)((widthSize/12f)*8);
            }

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


        //先绘制底色（为了使白框看上去像透明）【但是框线很窄，其实无所谓。绘制底色会导致与其他控件底色有差别】
//        canvas.drawRect(0,0,sizeChangedWidth,sizeChangedHeight,paintBackground);


        //再绘制最右侧的图例区
        float legendLeft = 4*sizeLarge+2*sizeSmall+sizeSmall/3;
        float legendTop = 2*sizeSmall+sizeLarge;
        float legendB_1 = 2*sizeSmall+sizeLarge+(sizeSmall/3);

        //图例区·条彩1·为完成（计划为18*6（w*6））
        canvas.drawRect(legendLeft,legendTop,legendLeft+sizeSmall,legendB_1,paintPieTotal);
        //图例区·文字1·总资源量【实际是以其未遮盖部分代表未学习数量】
        canvas.drawText("未完成",legendLeft,legendB_1+2*(sizeSmall/3), paintTextGeneral);

        //图例区·彩杠2·已完成量
        canvas.drawRect(legendLeft,legendB_1+5*(sizeSmall/3),legendLeft+sizeSmall,legendB_1+4*(sizeSmall/3),paintPieFraction);
        //图例区·文字2·已完成量
        canvas.drawText("已完成",legendLeft,legendB_1+7*(sizeSmall/3), paintTextGeneral);


        //绘制饼区（两饼+框饼）
        //总量饼
        if(totalAmount == 0){
            //总量为空时，不绘制饼，取代为绘制一条灰线（水平，圆心指向右侧）
            float startX = 2*sizeLarge+sizeSmall;
            float startY = sizeLarge+2*sizeSmall;
            canvas.drawLine(startX,startY,startX+sizeLarge,startY,paintEmptyThickLine);


        }else {
            //其他情况下，不论任务资源多少，都须先绘制出完整的底层灰饼。
            canvas.drawArc(rectF, 0, 360, true, paintPieTotal);

            //分子不是0，可以初始各数据。
            fractionalAngle = ((float) fractionalAmount/(float) totalAmount)*360f;//子饼所占的角度，标量，符号在方法调用中加。
            totalMiddleAngle = (360-fractionalAngle)/2;//总量饼折线（引出段）的引出角度（饼弧上的中位点）
            fractionalMiddleAngle = fractionalAngle/2;

            strFractionRatioIn2 =dFormat.format(((float)fractionalAmount/(float) totalAmount)*100)+"%";
            strRestPartRatioIn2 =dFormat.format((1.0-(float)fractionalAmount/(float)totalAmount)*100)+"%";//未学习部分（虽然饼子是化成全饼，但毕竟只按其未遮盖部分代表未学）
        }

        //分子饼
        //根据分子数量绘制已完成的扇形和其白色外框。
        if(fractionalAmount == 0){
            //只绘制一条白线
            float startX = 2*sizeLarge+sizeSmall;
            float startY = sizeLarge+2*sizeSmall;
            canvas.drawLine(startX,startY,startX+sizeLarge,startY,paintOutLine);

        }else if(fractionalAmount == totalAmount){
            //完整绘制子饼，完全覆盖
            canvas.drawArc(rectF,0,-360,true,paintPieFraction);

        }else {
            //正常绘制子饼
            canvas.drawArc(rectF,0,-fractionalAngle,true,paintPieFraction);
        }

        //框饼（套在子饼外，看上去像两项饼之间的空隙）
        //也要根据分子数量绘制。
        if(fractionalAmount != 0 && fractionalAmount != totalAmount){
            //正常绘制绘制外框（否则不绘制）
            canvas.drawArc(rectF,0,-fractionalAngle,true,paintOutLine);
        }


        //绘制饼外折线。及折线水平段。以及对应文字【(文本在上下边条时若画斜+竖折线不好看，仍然画斜+水平折线)】
        //各线引出自对应圆弧的中间（弧上）位置
        //【为便于统一处理、统一思路，各次绘制均以靠左、靠上方为start，右下为to方向】【暂未改完】
        if(totalAmount == 0){
            //总数都是0了，则只绘制右侧小短线（总量引线），分量引线不绘制。
            float startX = 3*sizeLarge+sizeSmall;
            float stillY = 2*sizeSmall+sizeLarge;
            float toX = 3*sizeLarge+2*sizeSmall;
            canvas.drawLine(startX,stillY,toX,stillY, paintLineTotal);

            //接水平段引线【不要合并到上一绘制，思路不易理清】
            canvas.drawLine(startX,stillY,toX+sizeLarge,stillY, paintLineTotal);

            //文字绘制
            canvas.drawText("无数据",toX,stillY, paintTextTotal);

        }else {
            //总量饼折线(按逻辑，该线引出点将一直位于三、四象限)，加负号处理。

            if(totalMiddleAngle == 180){
                //（此时子饼分量为空）
                float stillY = 2*sizeSmall+sizeLarge;
                float toX = sizeLarge+sizeSmall;

                //折线引出段
                canvas.drawLine(sizeLarge,stillY,toX,stillY,paintLineTotal);

                //折线水平段(注意左侧起点有缩进)
                canvas.drawLine(padding_H,stillY,sizeLarge,stillY, paintLineTotal);
                //文字绘制
                canvas.drawText("100%",padding_H,stillY, paintTextTotal);

            }else if(totalMiddleAngle>=135){
                //为了保证正值运算（从而简化思路，减少出错），在此按其补角求弧度进行运算。
                double radians = ((180-totalMiddleAngle)/180)*Math.PI;//实际是求了对应补角的大小（弧度制）

                float startY = 2*sizeSmall+sizeLarge+(float) Math.tan(radians)*(sizeLarge+sizeSmall);
                float toX = 2*sizeLarge+sizeSmall-(float) Math.cos(radians)*sizeLarge;
                float toY = 2*sizeSmall+sizeLarge+(float) Math.sin(radians)*sizeLarge;
                canvas.drawLine(sizeLarge,startY,toX,toY, paintLineTotal);

                //绘制折线水平段（此时文本、水平线位于左侧）
                canvas.drawLine(0,startY,sizeLarge,startY, paintLineTotal);

                //文字绘制
                canvas.drawText(strRestPartRatioIn2,0,startY, paintTextTotal);

            }else if(totalMiddleAngle>90){
                double radians = ((180-totalMiddleAngle)/180)*Math.PI;//实际是求了对应补角的大小（弧度制）
                float startX = 2*sizeLarge+sizeSmall-(float) ((sizeLarge+sizeSmall)/Math.tan(radians));
                float toY = 2*sizeSmall+sizeLarge+(float) Math.sin(radians)*(sizeLarge);
                float startY =3*sizeSmall+2*sizeLarge;
                float toX = 2*sizeLarge+sizeSmall-(float) Math.cos(radians)*sizeLarge;
                canvas.drawLine(startX,startY,toX,toY, paintLineTotal);

                //绘制折线水平段
                canvas.drawLine(startX,startY,startX+sizeLarge,startY, paintLineTotal);
                //文字绘制
                canvas.drawText(strRestPartRatioIn2,startX,startY+2*(sizeSmall/3), paintTextTotal);
                //由于文字太小，贴底绘制会导致与折线距离过远。

            }else if(totalMiddleAngle == 90){
                //90度时正切无穷大无法计算
                float stillX = 2*sizeLarge+sizeSmall;
                float startY = 2*sizeLarge+2*sizeSmall;
                float stopY = 2*sizeLarge+3*sizeSmall;
                canvas.drawLine(stillX,startY,stillX,stopY, paintLineTotal);

                //绘制折线水平段
                canvas.drawLine(stillX,stopY,stillX+sizeLarge,stopY, paintLineTotal);
                //文字绘制
                canvas.drawText(strRestPartRatioIn2,stillX,stopY+2*sizeSmall/3, paintTextTotal);
            }else if(totalMiddleAngle>=45){
                double radians = (totalMiddleAngle/180)*Math.PI;//此时不必求补角，该角度已位于全正值范围
                float startX = 2*sizeLarge+sizeSmall+(float) Math.cos(radians)*sizeLarge;
                float startY = 2*sizeSmall+sizeLarge+(float) Math.sin(radians)*sizeLarge;
                float toX = 2*sizeLarge+sizeSmall+(sizeLarge+sizeSmall)/(float) Math.tan(radians);
                float toY =3*sizeSmall+2*sizeLarge;

                canvas.drawLine(startX,startY,toX,toY, paintLineTotal);

                //绘制折线水平段（此时文本位于下方）
                canvas.drawLine(toX,toY,toX+sizeLarge,toY, paintLineTotal);

                //文字绘制
                canvas.drawText(strRestPartRatioIn2,toX,toY+2*sizeSmall/3, paintTextTotal);
            }else if(totalMiddleAngle<45) {
                double radians = (totalMiddleAngle/180)*Math.PI;//此时不必求补角，该角度已位于全正值范围
                float startX = 2*sizeLarge+sizeSmall+(float) Math.cos(radians)*sizeLarge;
                float startY = 2*sizeSmall+sizeLarge+(float) Math.sin(radians)*sizeLarge;
                float toY = 2*sizeSmall+sizeLarge+(float) Math.tan(radians)*(sizeLarge+sizeSmall);
                float toX = 3*sizeLarge+2*sizeSmall;

                canvas.drawLine(startX,startY,toX,toY, paintLineTotal);

                //绘制折线水平段（此时文本、水平线位于右侧）
                canvas.drawLine(toX,toY,toX+sizeLarge,toY, paintLineTotal);

                //文字绘制
                canvas.drawText(strRestPartRatioIn2,toX,toY, paintTextTotal);
            }else {
                //只剩=0了，与180同（画在右侧小短线）然后接水平段.
                canvas.drawLine(3*sizeLarge+sizeSmall,2*sizeSmall+sizeLarge,4*sizeLarge+2*sizeSmall,2*sizeSmall+sizeLarge, paintLineTotal);

                //文字绘制
                canvas.drawText(strRestPartRatioIn2,3*sizeLarge+2*sizeSmall,2*sizeSmall+sizeLarge, paintTextTotal);
            }





            //分量（子饼）折线引出段
            if(fractionalMiddleAngle == 0){
                //绘制右侧小短线（引线），考虑到位于if-else内此时总量确定已不为0.接右侧水平段
                float startX = 3*sizeLarge+sizeSmall;
                float toY = 2*sizeSmall+sizeLarge;
                float toX = 3*sizeLarge+2*sizeSmall;
                canvas.drawLine(startX,toY,toX+sizeLarge,toY, paintLineFraction);

                //文字绘制
                canvas.drawText(strFractionRatioIn2,toX,toY, paintTextFraction);

            }else if(fractionalMiddleAngle<=45){
                double radians = (fractionalMiddleAngle/180)*Math.PI;//此时不必求补角，该角度已位于全正值范围
                float startX = 2*sizeLarge+sizeSmall+(float) Math.cos(radians)*sizeLarge;//ok
                float startY = 2*sizeSmall+sizeLarge-(float) Math.sin(radians)*sizeLarge;//ok
                float toX = 2*sizeSmall+3*sizeLarge;//ok
                float toY = 2*sizeSmall+sizeLarge-(sizeLarge+sizeSmall)*(float) Math.tan(radians);

                canvas.drawLine(startX,startY,toX,toY, paintLineFraction);

                //绘制折线水平段（此时文本、水平线位于右侧）
                canvas.drawLine(toX,toY,toX+sizeLarge,toY, paintLineFraction);
                //文字绘制
                canvas.drawText(strFractionRatioIn2,toX,toY, paintTextFraction);

            }else if(fractionalMiddleAngle<90){
                double radians = (fractionalMiddleAngle/180)*Math.PI;
                float startX = 2*sizeLarge+sizeSmall+(float) Math.cos(radians)*sizeLarge;//ok
                float startY = 2*sizeSmall+sizeLarge-(float) Math.sin(radians)*sizeLarge;//ok
                float toX = sizeSmall+2*sizeLarge+(sizeLarge+sizeSmall)/(float) Math.tan(radians);//ok

                canvas.drawLine(startX,startY,toX,sizeSmall, paintLineFraction);
                //绘制折线水平段（此时文本位于上方）
                canvas.drawLine(toX,sizeSmall,toX+sizeLarge,sizeSmall, paintLineFraction);

                //文字绘制
                canvas.drawText(strFractionRatioIn2,toX,sizeSmall, paintTextFraction);
            }else if(fractionalMiddleAngle == 90){
                float startX = 2*sizeLarge+sizeSmall;
                float startY = 2*sizeSmall;
                float toX = 2*sizeLarge+sizeSmall;

                //上方竖线，
                canvas.drawLine(startX,startY,toX,sizeSmall, paintLineFraction);
                //绘制折线水平段（此时文本位于上方）
                canvas.drawLine(toX,sizeSmall,toX+sizeLarge,sizeSmall, paintLineFraction);
                //文字绘制
                canvas.drawText(strFractionRatioIn2,2*sizeLarge+sizeSmall,sizeSmall, paintTextFraction);
            }else if(fractionalMiddleAngle<=135){
                fractionalMiddleAngle = fractionalMiddleAngle-90;//调整计算角度
                double radians = (fractionalMiddleAngle/180)*Math.PI;//转弧度制

                float toX = 2*sizeLarge+sizeSmall-(float) Math.sin(radians)*sizeLarge;//ok
                float toY = 2*sizeSmall+sizeLarge-(float) Math.cos(radians)*sizeLarge;//ok
                float startX = sizeSmall+2*sizeLarge-(sizeLarge+sizeSmall)*(float) Math.tan(radians);

                canvas.drawLine(startX,sizeSmall,toX,toY, paintLineFraction);
                //绘制折线垂直段（此时文本位于上方，竖线）
                canvas.drawLine(startX,sizeSmall,startX+sizeLarge,sizeSmall, paintLineFraction);

                //文字绘制
                canvas.drawText(strFractionRatioIn2,startX,sizeSmall, paintTextFraction);
            }else if(fractionalMiddleAngle<180){
                fractionalMiddleAngle = 180-fractionalMiddleAngle;
                double radians = (fractionalMiddleAngle/180)*Math.PI;//此时不必求补角，该角度已位于全正值范围
                float startY = 2*sizeSmall+sizeLarge-(sizeLarge+sizeSmall)*(float) Math.tan(radians);//ok
                float toX = sizeSmall+2*sizeLarge-sizeLarge*(float) Math.cos(radians);//ok
                float toY = 2*sizeSmall+sizeLarge-sizeLarge*(float) Math.sin(radians);//ok

                canvas.drawLine(sizeLarge,startY,toX,toY, paintLineFraction);

                //绘制折线水平段（此时文本、水平线位于左侧）
                canvas.drawLine(0,startY,sizeLarge,startY, paintLineFraction);
                //文字绘制
                canvas.drawText(strFractionRatioIn2,0,startY, paintTextFraction);
            }else {
                //=180.直接接续左侧水平段
                canvas.drawLine(0,2*sizeSmall+sizeLarge,sizeLarge+sizeSmall,2*sizeSmall+sizeLarge, paintLineFraction);
                //文字绘制
                canvas.drawText(strFractionRatioIn2,0,2*sizeSmall+sizeLarge, paintTextFraction);
            }
        }
    }

//       invalidate();不再需要持续绘制，每次滑动页面或手动更新时重绘一次即可。



    public void setData(int totalAmount, int fractionalAmount ){
//        Log.i(TAG, "setData: totalAmount="+totalAmount+"f-Amount="+fractionalAmount);
        this.totalAmount = totalAmount;
        this.fractionalAmount = fractionalAmount;
        invalidate();
    }
}
