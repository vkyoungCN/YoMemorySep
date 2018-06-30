package com.vkyoungcn.smartdevices.yomemory.validatingEditor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.vkyoungcn.smartdevices.yomemory.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Thanks to the original author Adrián García Lomas.
 * This code is only being used under non_commercial situations.
 */
public class ValidatingEditor extends View {
    private static final String TAG = "ValidatingEditor";
    private static final int DEFAULT_LENGTH = 6;
    private static final String KEYCODE = "KEYCODE_";//按键事件返回的一定是KEYCODE_开头（已知字符）或数字1001（未知字符）
    private static final Pattern KEYCODE_PATTERN = Pattern.compile( KEYCODE + "(.)");//用括号来指示Match中的分组
    private Context mContext;
    private FixedStack<Character> characters;
    private String targetText = "";//用于比较的目标字串
    private String initText = "";//用于设置初始显示的内容（可用于外部恢复）

    private BottomLineSection bottomLineSections[];
    private Paint bottomLinePaint;
    private Paint bottomLineNonCorrectPaint;
    private Paint textPaint;
    private Paint textNonCorrectPaint;
    private Paint backgroundPaint;

    private float bottomLineHorizontalMargin;
    private float bottomLineStrokeWidth;
    private float bottomLineHorizontalLength;
    private float padding;
    private float textSize;
    private float textMarginBottom;
    private float viewHeight;
    int lines = 1;//控件需要按几行显示，根据当前屏幕下控件最大允许宽度和控件字符数（需要的宽度）计算得到。
    private int sizeChangedHeight;//是控件onSizeChanged后获得的尺寸之高度，也是传给onDraw进行线段绘制的canvas-Y坐标(单行时)
    private int sizeChangedWidth;

    private int bottomLineSectionAmount = DEFAULT_LENGTH;
    private int bottomLineColor;
    private int bottomLineNonCorrectColor;
    private int textColor;
    private int textNonCorrectColor;
    private int backgroundColor;
    private int backgroundNotCorrectColor;
    private int mInputType;
    private int leastWrongPosition = 0;//从1起，0预置。（记录输入的字符之出错的各字符中索引数最小的一个，用于删除改正时的改色逻辑）
    private int currentPosition = 0;//从1起算。

//    private boolean stopDrawing = false;

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
        initDefaultAttributes();
        initCustomAttributes(attributeset);
        initPaint();
        initViewOptions();
    }

    public interface OnValidatingEditorInputListener {
        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        void onCodeCorrectAndReady();
        void onCodeChanged(String newStr);

    }

    private void initDefaultAttributes() {
        padding = getContext().getResources().getDimension(R.dimen.view_padding);

        bottomLineStrokeWidth = getContext().getResources().getDimension(R.dimen.bottomLine_stroke_width);//查API知此方法自动处理单位转换。
        bottomLineHorizontalLength = getContext().getResources().getDimension(R.dimen.bottomLine_horizontal_length);
        bottomLineHorizontalMargin = getContext().getResources().getDimension(R.dimen.bottomLine_horizontal_margin);
        bottomLineColor = ContextCompat.getColor(mContext,R.color.bottomLine_default_color);
        bottomLineNonCorrectColor = ContextCompat.getColor(mContext,R.color.bottomLine_nonCorrect_color);

        textSize = getContext().getResources().getDimension(R.dimen.text_size);
        textMarginBottom = getContext().getResources().getDimension(R.dimen.text_margin_bottom);
        textColor = ContextCompat.getColor(mContext,R.color.textColor);
        textNonCorrectColor = ContextCompat.getColor(mContext,R.color.text_nonCorrect_Color);
        backgroundColor = ContextCompat.getColor(mContext,R.color.ve_background);
        backgroundNotCorrectColor = ContextCompat.getColor(mContext,R.color.ve_background_not_correct);

        viewHeight = getContext().getResources().getDimension(R.dimen.view_height);
    }

    private void initCustomAttributes(AttributeSet attributeset) {
        TypedArray attributes =
                getContext().obtainStyledAttributes(attributeset, R.styleable.ValidatingEditor);

        bottomLineColor = attributes.getColor(R.styleable.ValidatingEditor_bottomLine_color, bottomLineColor);
        bottomLineNonCorrectColor =
                attributes.getColor(R.styleable.ValidatingEditor_bottomLine_nonCorrect_color, bottomLineNonCorrectColor);
        textColor = attributes.getInt(R.styleable.ValidatingEditor_text_color, textColor);
        textNonCorrectColor = attributes.getInt(R.styleable.ValidatingEditor_text_nonCorrect_color, textNonCorrectColor);

        attributes.recycle();
    }



    private void initPaint() {
        bottomLinePaint = new Paint();
        bottomLinePaint.setColor(bottomLineColor);
        bottomLinePaint.setStrokeWidth(bottomLineStrokeWidth);
        bottomLinePaint.setStyle(android.graphics.Paint.Style.STROKE);

        bottomLineNonCorrectPaint = new Paint();
        bottomLineNonCorrectPaint.setColor(bottomLineNonCorrectColor);
        bottomLineNonCorrectPaint.setStrokeWidth(bottomLineStrokeWidth);
        bottomLineNonCorrectPaint.setStyle(android.graphics.Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        textNonCorrectPaint = new Paint();
        textNonCorrectPaint.setTextSize(textSize);
        textNonCorrectPaint.setColor(textNonCorrectColor);
        textNonCorrectPaint.setAntiAlias(true);
        textNonCorrectPaint.setTextAlign(Paint.Align.CENTER);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

    }


    private void initViewOptions() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    //【学：据说是系统计算好控件的实际尺寸后以本方法通知用户】
    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        sizeChangedHeight = h;
        sizeChangedWidth = w;
        initUnderline(w);

        super.onSizeChanged(w, h, old_w, old_h);

    }

    //目前，宽度固定设置为最大值；高度视控件文本字符数量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);//这样设置的前提是XML中明确给控件设置为match_..
        float requireHeight = viewHeight+padding*2;
        if (targetText.isEmpty()) {
            setMeasuredDimension(maxWidth, (int) requireHeight);
            return;
        }

        float requiredTotalWidth = bottomLineSectionAmount*bottomLineHorizontalLength+padding*2;
        if(requiredTotalWidth>maxWidth){
            lines = (int)(requiredTotalWidth/maxWidth)+1;
            requireHeight = viewHeight*lines+padding*2;
        }

        setMeasuredDimension(maxWidth,(int)requireHeight);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {

        return new VeInputConnection(this,false);
    }

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
            listener.onCodeChanged(getCurrentString());
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

            if (characters.size() >= bottomLineSectionAmount ) {//满了
                if(getCurrentString().compareTo(targetText) == 0) {
                    if(listener != null) {
                        listener.onCodeCorrectAndReady();
                        listener.onCodeChanged(getCurrentString());
                    }
                }
            }else {//还没满（但显然也必须是输入开始后，每次输入（且已成功输入到了characters中后）才会触发）
                currentPosition++;

                //记录输入的字符之出错的各字符中，索引值最小的一个。
                if(Character.compare(character,targetText.charAt(currentPosition-1))!=0){//此位置上字符输入不正确
                    if(leastWrongPosition==0){
                        leastWrongPosition = currentPosition;//只需记录一次（最小索引位置）即可
                    }
                }
            }

            //既然是输入动作（且已存入字符栈；也即会显示出来），就应通知Activity改变缓存字串。
            //【此外，回删、存满都需要通知改变】
            listener.onCodeChanged(getCurrentString());
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(targetText.isEmpty()) {
            return;
        }

            if(leastWrongPosition!=0) {
                backgroundPaint.setColor(backgroundNotCorrectColor);
            }else {
                backgroundPaint.setColor(backgroundColor);
            }
            canvas.drawRect(0,0,sizeChangedWidth,sizeChangedHeight,backgroundPaint);

            for (int i = 0; i < bottomLineSections.length; i++) {
                BottomLineSection sectionPath = bottomLineSections[i];
                float fromX = sectionPath.getFromX() + bottomLineHorizontalMargin;
                float fromY = sectionPath.getFromY();
                float toX = sectionPath.getToX() - bottomLineHorizontalMargin;
                float toY = sectionPath.getToY();

                drawSection(fromX, fromY, toX, toY, canvas);
                if (characters.toArray().length > i && characters.size() != 0) {
                    Boolean characterCorrect = isCharacterCorrectAtPosition(i);
                    drawCharacter(characterCorrect, fromX, toX, fromY, characters.get(i), canvas);
                }
            }
            invalidate();

    }

    private boolean isCharacterCorrectAtPosition(int position){
        return characters.get(position).compareTo(targetText.charAt(position))==0;//相等，字符正确。

    }

    private void drawSection(float fromX, float fromY, float toX, float toY,
                             Canvas canvas) {
        Paint paint = bottomLinePaint;
        /*if (!correct) {
            paint = bottomLineNonCorrectPaint;
        }*/
        canvas.drawLine(fromX, fromY, toX, toY, paint);
    }

    private void drawCharacter(boolean correct, float fromX, float toX, float fromY, Character character, Canvas canvas) {
        Paint paint = textPaint;
        if(!correct){
            paint = textNonCorrectPaint;
        }
        float actualWidth = toX - fromX;
        float centerWidth = actualWidth / 2;
        float centerX = fromX + centerWidth;//似乎跟API文档对应不起来啊？不是从左下角为原点绘制？
        canvas.drawText(character.toString(), centerX, fromY - bottomLineStrokeWidth/2 - textMarginBottom, paint);
    }


    public String getCurrentString() {
        StringBuilder sbd = new StringBuilder();
        for (Character c :
                characters) {
            sbd.append(c);
        }

        return sbd.toString();
    }


    public void setInitText(String initText) {
        this.initText = initText;
    }

    /*
    * 方法由程序调用，动态设置目标字串
    * */
    public void setTargetText(String targetText){
        this.targetText = targetText;

        bottomLineSectionAmount = targetText.length();
        //由于要根据目标字串的字符数量来绘制控件，所以所有需要用到该数量的初始化动作都只能在此后进行
        initDataStructures();

    }



    private void initDataStructures() {
        bottomLineSections = new BottomLineSection[bottomLineSectionAmount];
        characters = new FixedStack();
        characters.setMaxSize(bottomLineSectionAmount);
        if(initText!=null){
            //如果初始字串不空，则将其存入数据组；
            // 但是不知调用次序如何，能否及时生效？
            char[] chars = initText.toCharArray();
            for (char c: chars){
                characters.push(c);
            }
        }

    }

    private void initUnderline(int viewMaxWidth) {
        if(lines ==1) {

            for (int i = 0; i < bottomLineSectionAmount; i++) {
                bottomLineSections[i] = createPath(i, 1,1, bottomLineHorizontalLength);
            }
        }else {
            int sectionsMaxAmountPerLine = (int)(viewMaxWidth/bottomLineHorizontalLength);
            for (int i = 0; i < bottomLineSectionAmount; i++) {
                int currentLine = (i/sectionsMaxAmountPerLine)+1;
                int positionInLine = i%sectionsMaxAmountPerLine;
                bottomLineSections[i] = createPath(positionInLine, lines,currentLine, bottomLineHorizontalLength);
            }
        }
    }

    private BottomLineSection createPath(int positionInLine, int totalLines, int theLine, float sectionLength) {
        float fromX = sectionLength * (float) positionInLine + padding;
        float heightPerLine = (sizeChangedHeight-2*padding)/totalLines;
        float fromY = heightPerLine*theLine+padding;
        return new BottomLineSection(fromX, fromY, fromX + sectionLength, fromY);
    }
}
