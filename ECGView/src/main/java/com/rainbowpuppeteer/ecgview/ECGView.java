package com.rainbowpuppeteer.ecgview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 *
 * @author RainbowPuppeteer
 */
public class ECGView extends View {

    private final static String TAG = "ECGView";

    /**
     *
     */
    private static final class Styles {
        int titleSize;
        int titleColor;
        int titlePadding;
        TitleAlignment titleAlignment;
        boolean showTitle;
    }

    public enum TitleAlignment {
        LEFT(0),
        CENTER(1),
        RIGHT(2);

        final int value;

        TitleAlignment(int i) { value = i; }

        public static TitleAlignment fromId(int i) {
            switch (i) {
                case 0:
                    return TitleAlignment.LEFT;
                case 2:
                    return TitleAlignment.RIGHT;
            }
            return TitleAlignment.CENTER;
        }
    }

    Styles mStyles;

    DataSeries mDataSeries;

    Graph mGraph;

    Paint mTitlePaint;

    String mTitle;

    int measureMode;

    public int KEEP_GRID_SIZE=0;
    public int KEEP_PARENT_SIZE=1;

    public ECGView(Context context) {
        this(context, null);
    }

    public ECGView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public ECGView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        loadXmlStyles(context, attrs, defStyleAttr);
        mGraph.loadXmlStyles(context, attrs, defStyleAttr);
        mDataSeries.loadXmlStyles(context, attrs, defStyleAttr);
    }

    private void init() {
        mStyles = new Styles();
        mDataSeries = new DataSeries(this);
        mGraph = new Graph(this);
        Log.d(TAG, "Init()");
    }

    /**
     *  load styles and settings from xml
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void loadXmlStyles(Context context, AttributeSet attrs, int defStyleAttr) {
        // load styles and settings from xml
        final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ECGView, defStyleAttr, 0);
        mTitle = array.getString(R.styleable.ECGView_title);
        mStyles.titleColor = array.getInt(R.styleable.ECGView_titleColor, Color.BLACK);
        mStyles.titleSize = array.getDimensionPixelSize(R.styleable.ECGView_titleSize, 40);
        mStyles.titlePadding = array.getDimensionPixelOffset(R.styleable.ECGView_titlePadding, 0);
        mStyles.titleAlignment = TitleAlignment.fromId(array.getInt(R.styleable.ECGView_titleAlignment, TitleAlignment.CENTER.value));
        mStyles.showTitle = array.getBoolean(R.styleable.ECGView_showTitle, true);

        measureMode = array.getInt(R.styleable.ECGView_measureMode, KEEP_GRID_SIZE);

        array.recycle();

        reloadStyles();
    }

    private void reloadStyles() {
        mTitlePaint = new Paint();
        mTitlePaint.setAntiAlias(true);
        mTitlePaint.setColor(getTitleColor());
        mTitlePaint.setTextSize(getTitleSize());
        mTitlePaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Measure the view. Called by Android system
     * If width and height of the graph are not defined,
     * The graph will be automatically assigned the number of grids
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (measureMode == KEEP_PARENT_SIZE)
            return;

        int desiredHeight;
        if (isTitleVisible())
            desiredHeight = getTitleHeight() + getTitlePadding() + mGraph.getHeight() + getPaddingHeight();
        else
            desiredHeight = mGraph.getHeight() + getPaddingHeight();
        int heightSpecSize = measureDimension(desiredHeight, MeasureSpec.getSize(heightMeasureSpec));

        int desiredWidth = mGraph.getWidth() + getPaddingWidth();
        int widthSpecSize = measureDimension(desiredWidth, MeasureSpec.getSize(widthMeasureSpec));

        setMeasuredDimension(widthSpecSize, heightSpecSize);
        Log.d(TAG, "OnMeasure(): width: " + widthSpecSize + " height: " + heightSpecSize);
    }

    /**
     * Helps to determine the final size
     *
     * @param desiredSize
     * @param measureSpec
     * @return
     */
    private int measureDimension(int desiredSize, int measureSpec) {
        int res;
        int SpecSize = MeasureSpec.getSize(measureSpec);
        int SpecMode = MeasureSpec.getMode(measureSpec);
        Log.d(TAG, "Spec Size: " + SpecSize + " Spec Mode: " + SpecMode + " DesiredSize: " + desiredSize );
        if (SpecMode == MeasureSpec.EXACTLY) {
            res = SpecSize;
        } else {
            if (desiredSize != 0 && desiredSize != getTitleHeight() + getTitlePadding()) {
                res = desiredSize;
                if (SpecMode == MeasureSpec.AT_MOST) {
                    res = Math.min(res, SpecSize);
                }
            } else
                res = SpecSize;
        }

        if (res < desiredSize) {
            Log.e(TAG, "The view is too small, the content might get cut");
        }

        return res;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isTitleVisible())
            w = w - getTitleHeight() - getTitlePadding();
        mGraph.resizeGraph(w - getPaddingWidth(), h - getPaddingHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "OnLayout(): left: " + left + " right: " + right + " top: " + top + " bottom: " + bottom);
        if (isTitleVisible())
            top = top + getTitleHeight() + getTitlePadding();
        mGraph.layout(changed, left + getPaddingLeft(), top +getPaddingTop(), right - getPaddingRight(), bottom - getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= 11 && !canvas.isHardwareAccelerated()) {
            Log.w(TAG, "This view should be used in hardware accelerated mode. Read this for more info:" +
                    "https://developer.android.com/guide/topics/graphics/hardware-accel.html");
        }
        int top;
        if (isTitleVisible()) {
            reloadStyles();
            float xPos;
            switch (mStyles.titleAlignment) {
                case LEFT:
                    mTitlePaint.setTextAlign(Paint.Align.LEFT);
                    xPos = 0;
                    break;
                case RIGHT:
                    mTitlePaint.setTextAlign(Paint.Align.RIGHT);
                    xPos = getWidth();
                    break;
                default:
                    mTitlePaint.setTextAlign(Paint.Align.CENTER);
                    xPos = getWidth() / 2f;
                    break;
            }
            float yPos = getTitleHeight() + getPaddingTop();
            canvas.drawText(mTitle, xPos, yPos, mTitlePaint);
            top = getTitleHeight() + getTitlePadding() + getPaddingTop();
        } else {
            top = getPaddingTop();
        }
        mGraph.draw(canvas, getPaddingLeft(), top, getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mDataSeries.draw(canvas,
                getPaddingLeft() + mGraph.getGraphLeft(),
                top + mGraph.getGraphTop(),
                getPaddingLeft() + mGraph.getGraphLeft() + mGraph.getGraphWidth(),
                top + mGraph.getGraphTop() + mGraph.getGraphHeight(),
                mGraph.getXMin(),
                mGraph.getYMin(),
                mGraph.getXMax(),
                mGraph.getYMax());
    }

    public double getDataXMax() {
        return mDataSeries.getXMax();
    }
    public double getDataXMin() {
        return mDataSeries.getXMin();
    }
    public double getDataYMax() {
        return mDataSeries.getYMax();
    }
    public double getDataYMin() {
        return mDataSeries.getYMin();
    }

    public DataSeries getDateSeries() {
        return mDataSeries;
    }

    public Graph getGraph() {
        return mGraph;
    }

    public String getTitle() { return mTitle; }
    public int getTitleSize() { return mStyles.titleSize; }
    public int getTitleColor() { return mStyles.titleColor; }
    public int getTitlePadding() { return mStyles.titlePadding; }
    public boolean isTitleVisible() {
        // Not consider title is visible if title text is null
        return mStyles.showTitle && mTitle != null && !mTitle.equals("");
    }

    private int getPaddingWidth() {
        return getPaddingLeft() + getPaddingRight();
    }

    private int getPaddingHeight() {
        return getPaddingTop() + getPaddingBottom();
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    private int getTitleHeight() {
        String title = getTitle();
        if (getTitle() == null)
            title = "";

        Rect textBounds = new Rect();
        mTitlePaint.getTextBounds(title, 0, title.length(), textBounds);
        return textBounds.height();
    }

    public void setMeasureMode(int measureMode) {
        if (measureMode == KEEP_GRID_SIZE || measureMode == KEEP_PARENT_SIZE)
            this.measureMode = measureMode;
    }

    public void setTitle(String title) {
        mTitle= title;
        postInvalidate();
    }
    public void setTitleSize(int size) {
        mStyles.titleSize = size;
        postInvalidate();
    }
    public void setTitleColor(int color) {
        mStyles.titleColor = color;
        postInvalidate();
    }
    public void setTitlePadding(int padding) {
        mStyles.titlePadding = padding;
        postInvalidate();
    }
    public void showTitle(boolean b) {
        mStyles.showTitle = b;
        postInvalidate();
    }
}
