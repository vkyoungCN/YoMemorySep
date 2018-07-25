package com.vkyoungcn.smartdevices.yomemory.validatingEditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.vkyoungcn.smartdevices.yomemory.R;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class ValidatingEditor extends View {
//* 字体可以根据词的长度动态调整（有一个最大值，当空间不足以容纳时会缩小）
//* 当前的设计逻辑中单词将在一行内显示完毕。
//* 如果输入的正确，则背景、下划线呈现绿色，有错的字母则背景呈红色，错字下方的下划线呈现红色；
//* 每输入一个有效字符可以向监听（自定义）发送消息（携带当前的完整字串），全部输入且正确时发送另一种监听消息；
//* 正确填写一次后，允许进行无记录加练。

    private static final String TAG = "ValidatingEditor";
    private Context mContext;

//    private static final int DEFAULT_LENGTH = 6;//在未接到设置的字串时，按6个的长度绘制。
    private static final String KEYCODE = "KEYCODE_";//按键事件返回的一定是KEYCODE_开头（已知字符）或数字1001（未知字符）
    private static final Pattern KEYCODE_PATTERN = Pattern.compile( KEYCODE + "(.)");//用括号来指示Match中的分组

    private String targetText = "";//用于比较的目标字串
    private String initText = "";//用于设置初始显示的内容（可用于外部恢复）

    private LimitedStack<Character> characters;
    private BottomLine bottomLines[];
    private int leastWrongPosition = 0;//从1起，0预置。（记录输入的字符之出错的各字符中索引数最小的一个，用于删除改正时的改色逻辑）
    private int currentPosition = 0;//第一个字母的位置是1。
    private boolean hasCorrectOnce = false;//当有过一次填写正确时，逻辑有大改变：①仍然可以继续输入，但是最终不能再记录入Activity
    // 的输入记录列表；（考虑取消onCode单个改变的监听）②

    private boolean isDataInitBeInterruptedBecauseOfNoSize = false;

    /* 画笔组*/
    private Paint bottomLinePaint;
    private Paint bottomLineErrPaint;
    private Paint textPaint;
    private Paint textErrPaint;
    private Paint backgroundPaint;
    private Paint backgroundErrPaint;
    private Paint textWaitingPaint;

    /* 尺寸组 */
    private float padding;
    private float sectionGapLarge;//6dp【根据模拟器表现调整】
    private float sectionGapSmall;//4dp

    private float bottomLineHeightLarge;//4dp
    private float bottomLineHeightSmall;//2dp

    private float maxSectionWidth;//给定一个宽度的最大值；当字符过多总长超出屏幕时，缩小这一宽度（相应的字体也要缩小）
    private float finalLineWidth;//最终确定的每节宽度（由计算获得，而不是初始化时设定）

    private float textSize;//【考虑让文字尺寸后期改用和section宽度一致或稍小的直接数据】
    private float textBaseLineBottomGap;

    int lines = 1;//控件需要按几行显示，根据当前屏幕下控件最大允许宽度和控件字符数（需要的宽度）计算得到。

    private int sizeChangedHeight = 0;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth = 0;//未获取数据前设置为0

//    private int bottomLineSectionAmount = DEFAULT_LENGTH;

    /* 色彩组 */
    private int bottomLineColor;
    private int bottomErrColor;
    private int textColor;
    private int textErrColor;
    private int backgroundColor;
    private int backgroundErrColor;
//    private int mInputType;

//    private boolean stopDrawing = false;

    //用于描述各字符对应的下划线的一个内部类
    public class BottomLine {

        private float fromX;
        private float fromY;
        private float toX;
        private float toY;

        public BottomLine() {
        }

        public BottomLine(float fromX, float fromY, float toX, float toY) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        public float getFromX() {
            return fromX;
        }

        public void setFromX(float fromX) {
            this.fromX = fromX;
        }

        public float getFromY() {
            return fromY;
        }

        public void setFromY(float fromY) {
            this.fromY = fromY;
        }

        public float getToX() {
            return toX;
        }

        public void setToX(float toX) {
            this.toX = toX;
        }

        public float getToY() {
            return toY;
        }

        public void setToY(float toY) {
            this.toY = toY;
        }
    }

    //用于装载字符数据的栈
    public class LimitedStack<T> extends Stack<T> {

        private int topLimitSize = 0;

        @Override
        public T push(T object) {
            if (topLimitSize > size()) {
                return super.push(object);
            }

            return object;
        }

        public int getTopLimitSize() {
            return topLimitSize;
        }

        public void setTopLimitSize(int topLimitSize) {
            this.topLimitSize = topLimitSize;
        }
    }

    private OnValidatingEditorInputListener listener;

    public ValidatingEditor(Context context) {
        super(context);
        mContext = context;
        init(null);
        this.listener = null;
    }

    public ValidatingEditor(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        mContext = context;
        init(attributeset);
        this.listener = null;
    }


    public ValidatingEditor(Context context, AttributeSet attributeset, int defStyledAttrs) {
        super(context, attributeset, defStyledAttrs);
        mContext = context;
        init(attributeset);
        this.listener = null;
    }

    public void setCodeReadyListener(OnValidatingEditorInputListener listener) {
        this.listener = listener;
    }

    private void init(AttributeSet attributeset) {
        initSize();
        initColor();
        initPaint();
        initViewOptions();
    }

    //调用方（activity）实现本接口。VE中获取对调用方Activity的引用，然后调用这两个方法进行通信
    public interface OnValidatingEditorInputListener {
        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered

        /* 所有字符输入完毕且正确时触发 */
        void onCodeCorrectAndReady();

        /* 当输入一个（有效）字符，使VE的显示发生变化时触发 */
        void onCodeChanged(String newStr);

    }

    private void initSize() {
        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        maxSectionWidth =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
//        maxSectionWidth = getContext().getResources().getDimension(R.dimen.bottomLine_stroke_width);//【旧方法？】查API知此方法自动处理单位转换。
//        sectionGapLarge = getContext().getResources().getDimension(R.dimen.bottomLine_horizontal_margin);
        sectionGapLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        sectionGapSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        bottomLineHeightLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        bottomLineHeightSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        textBaseLineBottomGap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        textSize =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 34, getResources().getDisplayMetrics());
//        viewHeight = getContext().getResources().getDimension(R.dimen.view_height);
    }

    private void initColor(){
        bottomLineColor = ContextCompat.getColor(mContext,R.color.ve_bottomLine_default_color);
        bottomErrColor = ContextCompat.getColor(mContext,R.color.ve_bottomLine_nonCorrect_color);
        textColor = ContextCompat.getColor(mContext,R.color.ve_textColor);
        textErrColor = ContextCompat.getColor(mContext,R.color.ve_text_err_color);
        backgroundColor = ContextCompat.getColor(mContext,R.color.ve_background);
        backgroundErrColor = ContextCompat.getColor(mContext,R.color.ve_background_not_correct);
    }



    private void initPaint() {
        bottomLinePaint = new Paint();
        bottomLinePaint.setColor(bottomLineColor);
        bottomLinePaint.setStrokeWidth(bottomLineHeightLarge);
        bottomLinePaint.setStyle(android.graphics.Paint.Style.STROKE);

        bottomLineErrPaint = new Paint();
        bottomLineErrPaint.setColor(bottomErrColor);
        bottomLineErrPaint.setStrokeWidth(bottomLineHeightLarge);
        bottomLineErrPaint.setStyle(android.graphics.Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setTextSize(textSize);
//        textPaint.setStrokeWidth(4);
        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
//        textPaint.setTextAlign(Paint.Align.CENTER);

        textErrPaint = new Paint();
        textErrPaint.setTextSize(textSize);
        textErrPaint.setStrokeWidth(4);
        textErrPaint.setColor(textErrColor);
        textErrPaint.setAntiAlias(true);
//        textErrPaint.setTextAlign(Paint.Align.CENTER);//如果开启了这个，x坐标就不再是左端起点而是横向上的中点。

        textWaitingPaint = new Paint();
        textWaitingPaint.setTextSize(textSize);
        textWaitingPaint.setStrokeWidth(4);
        textWaitingPaint.setColor(textErrColor);
        textWaitingPaint.setAntiAlias(true);
        textWaitingPaint.setTextAlign(Paint.Align.CENTER);//如果开启了这个，x坐标就不再是左端起点而是横向上的中点。


        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setStrokeWidth(4);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

        backgroundErrPaint = new Paint();
        backgroundErrPaint.setStyle(Paint.Style.FILL);
        backgroundErrPaint.setStrokeWidth(4);
        backgroundErrPaint.setAntiAlias(true);
        backgroundErrPaint.setColor(backgroundErrColor);

    }


    private void initViewOptions() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    //【学：据说是系统计算好控件的实际尺寸后以本方法通知用户】
    // 【调用顺序：M(多次)-S(单次)-D】。
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        sizeChangedHeight = h;
        sizeChangedWidth = w;
//        initBottomLines(w);//这里暂时还没有容量数据，因而暂时无法初始化下划线数据。

        //万一程序对targetCode的设置（数据初始化）早于控件尺寸的确定（则是无法初始化下划线数据的），
        // 则需要在此重新对下划线数据进行设置
        if(isDataInitBeInterruptedBecauseOfNoSize){
            isDataInitBeInterruptedBecauseOfNoSize =false;//表示事件/状况已被消耗掉
//            Log.i(TAG, "onSizeChanged: initBL");
            initBottomLines(w,targetText.length());
        }

        super.onSizeChanged(w, h, old_w, old_h);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //合理的尺寸由外部代码及布局文件实现，这里不设计复杂的尺寸交互申请逻辑，而是直接使用结果。

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.getSize(heightMeasureSpec));

    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {

        return new VeInputConnection(this,false);
    }

//    此方法是参照网帖学习而来，暂时不太懂其设置的必要性。
    private class VeInputConnection extends BaseInputConnection {
        public VeInputConnection(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            CharSequence firstCharText = String.valueOf(text.charAt(0));
            //只传出其第一个字符
            return super.commitText(firstCharText, newCursorPosition);
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }



    /**
     * Detects the del key and delete characters
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyevent) {
        if (keyCode == KeyEvent.KEYCODE_DEL && characters.size() != 0) {
            characters.pop();
            currentPosition--;
            if(currentPosition<leastWrongPosition){
                leastWrongPosition = 0;
            }
//            Log.i(TAG, "onKeyDown: currentPos="+currentPosition);
            invalidate();//字符改变，重绘

            if(!hasCorrectOnce) {
                //尚未完整正确输入过一次，改变字符的监听仍然在
                listener.onCodeChanged(getCurrentString());
            }
        }
        return super.onKeyDown(keyCode, keyevent);
    }

    /**
     * Capture the keyboard events, for inputs
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyevent) {

        String text = KeyEvent.keyCodeToString(keyCode);//返回的一定是KEYCODE_开头（已知字符）或数字1001（未知字符）

        if(!keyevent.isCapsLockOn()) {
            return inputText(text,false);
        }
        return inputText(text,true);
    }

    /**
     * String text
     * Pass empty string to remove text
     */
    private boolean inputText(String text, boolean capsOn) {
        Matcher matcher = KEYCODE_PATTERN.matcher(text);
        if (matcher.matches()) {
            String matched = matcher.group(1);
            char character;
            if(!capsOn){
                character = matched.toLowerCase().charAt(0);
            }else {
                character = matched.charAt(0);
            }
            characters.push(character);

            if (characters.size() >= targetText.length() ) {//满了【重绘必须在回调之前，且两分支都要有！（排错小结）】
                currentPosition = targetText.length();//【这里既不能继续++，也不能保持数字不变，所以Z直接设置为最大值】
                invalidate();//字符改变，重绘
//                Log.i(TAG, "inputText: currentPos inside VE ="+currentPosition);

                if(getCurrentString().compareTo(targetText) == 0 && !hasCorrectOnce) {
                    //必须是尚未正确输入过的状态才能触发两个监听方法
                    hasCorrectOnce = true;//修改标记为已经（有过一次）完整正确输入。

                    if(listener != null) {
                        listener.onCodeChanged(getCurrentString());//需要在onCCA方法前调用，
                        // 实测如果放在下一方法后，则最后一个字符无法传出。可能原因如下，
                        listener.onCodeCorrectAndReady();//【本方法之后卡片自动滑动，相应组件可能已销毁，后续方法无效？】
                        //【目前的设计逻辑下，满了以后，再继续键入，VE显示上仍然是原词，characters也不变，
                        // 但由于本块代码仍然满足触发条件，因而可能会产生“滑动到某已满卡片后，任意敲入一字符则卡片向后滑动”效果】
                    }
                }

            }else {//还没满（但显然也必须是输入开始后，每次输入（且已成功输入到了characters中后）才会触发）
                currentPosition++;
//                Log.i(TAG, "inputText: currentPos inside VE ="+currentPosition);
                //记录输入的字符之出错的各字符中，索引值最小的一个。
                if(Character.compare(character,targetText.charAt(currentPosition-1))!=0){//此位置上字符输入不正确

                    if(leastWrongPosition==0){
                        leastWrongPosition = currentPosition;//只需记录一次（最小索引位置）即可
                    }

                }
                invalidate();//字符改变，重绘
                if(!hasCorrectOnce) {
                    //有这个监听的地方必须先判断当前的状态是“初次（正确之前）的填写”还是“已正确后的无记录加练”
                    listener.onCodeChanged(getCurrentString());
                }
            }

            return true;
        } else {
            return false;
        }
    }



    @Override
    protected void onDraw(Canvas canvas) {
//        Log.i(TAG, "onDraw: characters="+characters.toString());
        if(targetText.isEmpty()) {
            //此时目标字符还未设置，可以只绘制一个背景
            float fromX = padding;
            float fromY = padding;
            float toX = sizeChangedWidth - padding;
            float toY = sizeChangedHeight - padding;

            canvas.drawRect(fromX,fromY,toX,toY,backgroundPaint);
            return;
        }

        if(leastWrongPosition!=0) {
            //存在错误的字符
            backgroundPaint.setColor(backgroundErrColor);
        }else {
            backgroundPaint.setColor(backgroundColor);
        }

        //绘制背景
        float fromX = padding-8;
        float fromY = padding;
        float toX = sizeChangedWidth - padding+8;
        float toY = sizeChangedHeight - padding;
//        canvas.drawRect(0,0,600,600,backgroundPaint);
        canvas.drawRect(fromX,fromY,toX,toY,backgroundPaint);
//        Log.i(TAG, "onDraw: background done");
        //绘制下划线和文字
        for (int i = 0; i < bottomLines.length; i++) {
            BottomLine line = bottomLines[i];
            //下划线的绘制放在字符绘制后，因为需要根据正误调整配色。

            if (characters.toArray().length-1 >= i ) {
                boolean isCharacterCorrect = true;
                //如果该位置上有字符(已经输入到了此位置或之后)，则需要按文字正误对本位置进行不同的绘制

                //判断此位置字符是否正确，以便选用不同画笔
                isCharacterCorrect = characters.get(i).compareTo(targetText.charAt(i))==0;

                if(isCharacterCorrect) {
                    //字符正确，按正确色的画笔绘制
                    //下划线绘制
                    canvas.drawLine(line.getFromX(), line.getFromY(), line.getToX(), line.getToY(), bottomLinePaint);
                    //字符
                    canvas.drawText(characters.get(i).toString(),line.getFromX(),line.getToY()-textBaseLineBottomGap,textPaint);
                }else {
                    //此位置上字符错误，下划线、字符均按错误绘制
                    //下划线绘制
                    canvas.drawLine(line.getFromX(), line.getFromY(), line.getToX(), line.getToY(), bottomLineErrPaint);
                    //字符
                    canvas.drawText(characters.get(i).toString(),line.getFromX(),line.getToY()-textBaseLineBottomGap,textErrPaint);
                }
            }else {
                //本位置上尚无字符，按正确的下划线绘制，同时不必绘制文字
                canvas.drawLine(line.getFromX(), line.getFromY(), line.getToX(), line.getToY(), bottomLinePaint);
            }
        }
//            invalidate();

    }


    public String getCurrentString() {
        StringBuilder sbd = new StringBuilder();
        for (Character c :
                characters) {
            sbd.append(c);
        }

        return sbd.toString();
    }




    /*
    * 方法由程序调用，动态设置目标字串
    * */
    public void setTargetAndInitText(String targetText,String initText){
        this.targetText = targetText;
        this.initText = initText;

        //由于要根据目标字串的字符数量来绘制控件，所以所有需要用到该数量的初始化动作都只能在此后进行
        initData();
    }

    private void initData() {
        if(!targetText.isEmpty()) {
            bottomLines = new BottomLine[targetText.length()];
            characters = new LimitedStack();
            characters.setTopLimitSize(targetText.length());
        }
        if(initText!=null){
            //如果初始字串不空，则将其存入数据组；
            char[] chars = initText.toCharArray();
            characters.clear();//先清空
            for (char c: chars){
                characters.push(c);
            }
            invalidate();
//            Log.i(TAG, "initData: characters="+characters.toString());
        }

        //根据初始化了的目标字串来初始化下划线数据
        //这个时候还没有获取到尺寸信息则终止操作
        // 在onSizeChanged()中会对isDataInitBeInterruptedBecauseOfNoSize变量进行判断、
        if(sizeChangedWidth == 0){
            isDataInitBeInterruptedBecauseOfNoSize = true;
            return;
        }
        initBottomLines(sizeChangedWidth,targetText.length());

    }

    private void initBottomLines(int viewMaxWidth,int bottomLinesAmount) {
        bottomLines = new BottomLine[bottomLinesAmount];
        for(int i=0;i<targetText.length();i++){//必须得这样彻底初始化，如果只有上一句而不进行for循环初始则崩溃。
            bottomLines[i] = new BottomLine();
        }
//        Log.i(TAG, "initBottomLines: targetText.length="+targetText.length());
//        Log.i(TAG, "initBottomLines: bottomLines="+bottomLines);
        characters = new LimitedStack();
        characters.setTopLimitSize(targetText.length());
        //决定只绘制一行
        //可用总长（控件宽扣除两侧缩进）
        float availableTotalWidth = viewMaxWidth - padding*2;

        //计算能否按较大尺寸容纳所有内容
        if((bottomLinesAmount*maxSectionWidth+(bottomLinesAmount-1)*sectionGapLarge<=availableTotalWidth)){
            //可以容纳，使用大尺寸间隔和既定的大尺寸每节长度、大尺寸字符
            for (int i = 0; i < bottomLinesAmount; i++) {
                bottomLines[i].fromX =padding +(maxSectionWidth+sectionGapLarge)*i;
                //注意，先确定下方位置再确定上方位置。说明控件是靠下的gravity。
                bottomLines[i].fromY = sizeChangedHeight-padding;
                bottomLines[i].toY = sizeChangedHeight-padding;
                bottomLines[i].toX = bottomLines[i].fromX+maxSectionWidth;
//                Log.i(TAG, "initBottomLines: fx="+bottomLines[i].fromX);
//                Log.i(TAG, "initBottomLines: fy="+bottomLines[i].fromY);

            }
        }else {
            //使用小尺寸间隔以及动态确定的每节长度【字符大小还需另行确定】
            float totalWidthPureForLines =  availableTotalWidth-((bottomLinesAmount-1)*sectionGapSmall);
            finalLineWidth = totalWidthPureForLines/bottomLinesAmount;

            for (int i = 0; i < bottomLinesAmount; i++) {
                bottomLines[i].fromX =padding +(finalLineWidth+sectionGapSmall)*i;
                //注意，先确定下方位置再确定上方位置。说明控件是靠下的gravity。
                bottomLines[i].toY = sizeChangedHeight-padding;
                bottomLines[i].fromY =sizeChangedHeight-padding;
                bottomLines[i].toX = bottomLines[i].fromX+finalLineWidth;
            }

            //改文字画笔的字号大小
            float smallerTextSize = (textSize/maxSectionWidth)*finalLineWidth;
            textPaint.setTextSize(smallerTextSize);
            textWaitingPaint.setTextSize(smallerTextSize);
            textErrPaint.setTextSize(smallerTextSize);

        }
//        Log.i(TAG, "initBottomLines: invalidate, init="+initText);

        invalidate();//完成了目标数据、下划线的初始化后刷新控件。

/*
        if(lines ==1) {
            for (int i = 0; i < bottomLinesAmount; i++) {
                bottomLines[i] = createPath(i, 1,1, finalSectionWidth);
            }
        }else {
            int sectionsMaxAmountPerLine = (int)(viewMaxWidth/ finalSectionWidth);
            for (int i = 0; i < bottomLinesAmount; i++) {
                int currentLine = (i/sectionsMaxAmountPerLine)+1;
                int positionInLine = i%sectionsMaxAmountPerLine;
                bottomLines[i] = createPath(positionInLine, lines,currentLine, finalSectionWidth);
            }
        }
*/
    }

    /*
    * 有时候VE带有初始数据（比如卡片再次滑回时）
    * */
    public void setInitText(String initText) {
        this.initText = initText;
//        Log.i(TAG, "setInitText: String Received in Ve = "+initText);

        //存入数据数组
        if(initText!=null){
            //先对旧数据清空
            characters.clear();

            //存入数据数组
            char[] chars = initText.toCharArray();
            for (char c: chars){
                characters.push(c);
            }
        }

        invalidate();//刷新显示。
    }

}
