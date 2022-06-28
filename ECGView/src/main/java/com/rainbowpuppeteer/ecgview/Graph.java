package com.rainbowpuppeteer.ecgview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import java.text.DecimalFormat;

/**
 * @author RainbowPuppeteer
 */
public class Graph {

    private static final String TAG = "ECGView.Graph";

    private static final int NaN = Integer.MIN_VALUE;

    private final ECGView mECGView;

    private final Styles mStyles;

    private int yGridNumbers;
    private int xGridNumbers;

    private double gridYInterval;
    private double gridXInterval;

    private int gridsPerLargeGrid;

    private double xMax, xMin;
    private double yMax, yMin;

    private int labelWidth;
    private int labelHeight;

    DecimalFormat xFormat;
    DecimalFormat yFormat;

    private boolean autoInvalidate;
    private boolean layoutRequired;
    private boolean invalidateRequired;
    private boolean syncBounds;

    private boolean keepGridHeight;
    private boolean keepGridWidth;

    Paint mLabelPaint;
    Paint mGridPaint;
    Paint mLargeGridPaint;

    Graph(ECGView ecgView) {
        mECGView = ecgView;
        mStyles = new Styles();

        mLabelPaint = new Paint();
        mGridPaint = new Paint();
        mLargeGridPaint = new Paint();

        xFormat = new DecimalFormat("#.##");
        yFormat = new DecimalFormat("#.#");

        xMax = Double.NaN;
        xMin = Double.NaN;
        yMax = Double.NaN;
        yMin = Double.NaN;

        labelWidth = NaN;
        labelHeight = NaN;

        xGridNumbers = NaN;
        yGridNumbers = NaN;

        gridsPerLargeGrid = 5;

        gridYInterval = 1f;
        gridXInterval = 0.04;

        autoInvalidate = false;
        invalidateRequired = true;
        syncBounds = true;
        keepGridHeight = false;
        keepGridWidth = false;

        layoutRequired();
    }

    private void reloadStyles() {
        mLabelPaint = new Paint();
        mLabelPaint.setColor(getLabelColor());
        mLabelPaint.setTextSize(getLabelTextSize());
        mLabelPaint.setAntiAlias(true);

        mLargeGridPaint = new Paint();
        mLargeGridPaint.setColor(getLargeGridColor());
        mLargeGridPaint.setStrokeWidth(getLargeGridBoarderSize());

        mGridPaint = new Paint();
        mGridPaint.setColor(getGridColor());
        mGridPaint.setStrokeWidth(getGridBoarderSize());
    }

    /**
     * Measure the graph + y label width based on
     * number of grids and grid size
     *
     * @return width
     */
    public int getWidth() {
        if (xGridNumbers == NaN) {
            return 0;
        }
        int result = 0;
        if (labelWidth == NaN) calculateLabelWidth();
        if (isYLabelsVisible())
            result += getLabelWidth() + getLabelPadding();
        return result + getGraphWidth() + getGraphPadding() * 2;
    }

    /**
     * Measure the graph + x label height based on
     * number of grids and grid size
     *
     * @return height
     */
    public int getHeight() {
        if (yGridNumbers == NaN) {
            return 0;
        }
        int result = 0;
        if (labelHeight == NaN) calculateLabelHeight();
        if (isXLabelsVisible())
            result += getLabelHeight() + getLabelPadding();
        return result + getGraphHeight() + getGraphPadding() * 2;
    }

    /**
     * Measure only the graph width
     *
     * @return width
     */
    public int getGraphWidth() {
        return xGridNumbers * getGridWidth();
    }

    /**
     * Measure only the graph height
     *
     * @return height
     */
    public int getGraphHeight() {
        return yGridNumbers * getGridHeight();
    }

    /**
     * Get the graph top
     *
     * @return graph top
     */
    public int getGraphTop() {
        if (getXLabelPosition() == XLabelPosition.TOP)
            return getLabelHeight() + mStyles.graphPadding;
        else
            return mStyles.graphPadding;
    }

    /**
     * Get the graph left
     *
     * @return graph left
     */
    public int getGraphLeft() {
        if (getYLabelPosition() == YLabelPosition.LEFT)
            return getLabelWidth() + mStyles.graphPadding;
        else
            return mStyles.graphPadding;
    }

    /**
     * Get label height without padding
     * To get a non-zero value or to update the value,
     * call calculateLabelHeight() first
     *
     * @return label height
     */
    private int getLabelHeight() {
        if (labelHeight == NaN)
            return 0;
        return labelHeight;
    }

    /**
     * Get label width without padding
     * To get a non-zero value or to update the value,
     * call calculateLabelHeight() first
     *
     * @return label width
     */
    private int getLabelWidth() {
        if (labelWidth == NaN)
            return 0;
        return labelWidth;
    }

    /**
     * Calculate the height of current labels
     * according to xMax, xMin and labelTextSize
     */
    private void calculateLabelHeight() {
        final int min = getLabelTextSize();
        int height;
        String text;

        Rect textBounds = new Rect();
        if (Double.isNaN(xMax))
            text = "";
        else
            text = xFormat.format(xMax);
        mLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        height = textBounds.height();
        if (Double.isNaN(xMin))
            text = "";
        else
            text = xFormat.format(xMin);
        mLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        height = Math.max(height, textBounds.height());
        labelHeight = Math.max(min, height);
        Log.d(TAG, "LABELHEIGHT "+labelHeight);
    }

    /**
     * Calculate the height of given value
     */
    private int calculateLabelHeight(String s) {
        Rect textBounds = new Rect();
        mLabelPaint.getTextBounds(s, 0, s.length(), textBounds);
        return textBounds.height();
    }

    /**
     * Calculate the width of current labels
     * according to yMax, yMin and labelTextSize
     */
    private void calculateLabelWidth() {
        final int min = getLabelTextSize() * 2;
        int width;
        String text;

        Rect textBounds = new Rect();
        if (Double.isNaN(yMax))
            text = "";
        else
            text = yFormat.format(yMax);
        mLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        width = textBounds.width();
        if (Double.isNaN(yMin))
            text = "";
        else
            text = yFormat.format(yMin);
        mLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        width = Math.max(width, textBounds.width());
        labelWidth = Math.max(min, width);
    }

    private static final double b = 0.00005;

    /**
     * Round a to the nearest multiple of b
     *
     * @param toRound number to round
     * @param modulo modulo
     */
    private double round(double toRound, double modulo) {
        double remainder = toRound % modulo;
        if (-b < modulo - remainder && modulo - remainder < b)
            return toRound;
        if (remainder >= modulo / 2) return toRound + remainder;
        else return toRound - remainder;
    }

    /**
     * Ceil a to the nearest multiple of b
     *
     * @param toRound number to round
     * @param modulo modulo
     */
    private double ceil(double toRound, double modulo) {
        return toRound + modulo - toRound % modulo;
    }

    /**
     * Floor a to the nearest multiple of b
     *
     * @param toRound number to round
     * @param modulo modulo
     */
    private double floor(double toRound, double modulo) {
        double tmp = toRound % modulo;
        if (-b < modulo - tmp && modulo - tmp < b)
            return toRound;
        return toRound - tmp;
    }

    /**
     * All the bounds are fixed in this function
     *
     * @see #calculateXBounds(double, double)
     * @see #calculateYBounds(double, double)
     */
    private void checkBounds() {

        if (syncBounds) {
            xMin = Double.NaN;
            yMin = Double.NaN;
            xMax = Double.NaN;
            yMax = Double.NaN;
        }

        if (!Double.isNaN(xMin) && !Double.isNaN(xMax)) {
            calculateXBounds(xMin, xMax);
        } else if (Double.isNaN(xMax) && Double.isNaN(xMin)) {
            // Get current data bounds if both bounds are undefined
            xMax = round(mECGView.getDataXMax(), gridXInterval);
            xMin = round(mECGView.getDataXMin(), gridXInterval);
            calculateXBounds(xMin, xMax);
        } else if (Double.isNaN(xMax)) {
            // Calculate the other one
            xMax = xMin + gridXInterval * xGridNumbers;
        } else {
            xMin = xMax - gridXInterval * xGridNumbers;
        }

        if (!Double.isNaN(yMin) && !Double.isNaN(yMax)) {
            calculateYBounds(yMin, yMax);
        } else if (Double.isNaN(yMax) && Double.isNaN(yMin)) {
            yMax = round(mECGView.getDataYMax(), gridYInterval);
            yMin = round(mECGView.getDataYMin(), gridYInterval);
            calculateYBounds(yMin, yMax);
        } else if (Double.isNaN(yMax)) {
            yMax = yMin + gridYInterval * yGridNumbers;
        } else {
            yMin = yMax - gridYInterval * yGridNumbers;
        }
    }

    /**
     * This function should only be called in checkBounds()
     * Fix the y bounds according to line gravity
     *
     * @param min y min
     * @param max y max
     */
    private void calculateYBounds(double min, double max) {
        if (min == max) {
            Log.w(TAG, "calculateYBounds(): Min equals Top");
            yMin = ceil(max, gridYInterval);
            yMax = min + yGridNumbers * gridYInterval;
            return;
        } else if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        if (yMax == yMin + yGridNumbers * gridYInterval) {
            return;
        }

        if (getLineGravity() == LineGravity.TOP) {
            yMax = ceil(max, gridYInterval);
            yMin = yMax - yGridNumbers * gridYInterval;
        } else if (getLineGravity() == LineGravity.BOTTOM) {
            yMin = floor(min, gridYInterval);
            yMax = yMin + yGridNumbers * gridYInterval;
        } else {
            final double half = (double) yGridNumbers / 2.f;
            final double mid = (max + min) / 2;
            yMax = ceil(mid + half * gridYInterval, gridYInterval);
            yMin = ceil(mid - half * gridYInterval, gridYInterval);
        }
    }

    /**
     * This function should only be called in checkBounds()
     * What ever the x max / x min is, scroll to the end
     *
     * @param min x min
     * @param max x max
     */
    private void calculateXBounds(double min, double max) {
        xMax = ceil(max, gridXInterval);
        xMin = xMax - xGridNumbers * gridXInterval;
    }

    /**
     * This function should only be called in layout()
     *
     * @param graphWidth width of the graph
     */
    private void generateXGrids(int graphWidth) {
        if  (!Double.isNaN(xMin) && !Double.isNaN(xMax)) {
            generateXGrids(graphWidth, xMin, xMax);
        } else if (Double.isNaN(xMin) && Double.isNaN(xMax)) {
            xGridNumbers = (int) Math.floor((double) graphWidth / (double) getGridWidth());
            xMax = 0;
            xMin = -1 * xGridNumbers * gridXInterval;
        }
        else if (Double.isNaN(xMax)) {
            xGridNumbers = (int) Math.floor((double) graphWidth / (double) getGridWidth());
            xMax = xMin + xGridNumbers * gridXInterval;
        } else {
            xGridNumbers = (int) Math.floor((double) graphWidth / (double) getGridWidth());
            xMin = xMax - xGridNumbers * gridXInterval;
        }
    }

    /**
     * This function should only be called in layout()
     *
     * @param graphWidth width of the graph
     * @param xMin x min
     * @param xMax x max
     * @see #layout(boolean, int, int, int, int) 
     */
    private void generateXGrids(int graphWidth, double xMin, double xMax) {
        if (xGridNumbers == NaN) {
            Log.w(TAG, "GenerateXGrids(): xGridNumbers is uninitialized!");
            return;
        }
        if (xMax < xMin) {
            this.xMin = floor(xMax, gridXInterval);
            this.xMax = ceil(xMin, gridXInterval);
        } else {
            this.xMax = ceil(xMax, gridXInterval);
            this.xMin = floor(xMin, gridXInterval);
        }
        xGridNumbers = (int) ((double) (this.xMax - this.xMin) / (double) gridXInterval);
        mStyles.gridWidth = (int) Math.floor( (double) graphWidth / (double) xGridNumbers);
    }

    /**
     * This function should only be called in layout()
     *
     * @param graphHeight width of the graph
     * @see #layout(boolean, int, int, int, int) 
     */
    private void generateYGrids(int graphHeight) {
        if  (!Double.isNaN(yMin) && !Double.isNaN(yMax)) {
            generateYGrids(graphHeight, yMin, yMax);
        }else if (Double.isNaN(yMin) && Double.isNaN(yMax)) {
            yGridNumbers = (int) Math.floor((double) graphHeight / (double) getGridHeight());
            yMin = 0;
            yMax = yGridNumbers * gridYInterval;
        } else if (Double.isNaN(yMax)) {
            yGridNumbers = (int) Math.floor((double) graphHeight / (double) getGridHeight());
            yMax = yMin + yGridNumbers * gridYInterval;
        } else {
            yGridNumbers = (int) Math.floor((double) graphHeight / (double) getGridHeight());
            yMin = yMax - yGridNumbers * gridYInterval;
        }
        yGridNumbers = (int) Math.floor((double) graphHeight / (double) getGridHeight());
    }

    private void generateYGrids(int graphHeight, double yMin, double yMax) {
        if (yGridNumbers == NaN) {
            Log.w(TAG, "GenerateYGrids(): yGridNumbers is uninitialized!");
            return;
        }
        if (yMax < yMin) {
            this.yMax = ceil(yMin, gridYInterval);
            this.yMin = floor(yMax, gridYInterval);
        } else {
            this.yMax = ceil(yMax, gridYInterval);
            this.yMin = floor(yMin, gridYInterval);
        }
        yGridNumbers = (int) ((double) (yMax - yMin) / (double) gridYInterval);
        mStyles.gridHeight = (int) Math.floor( (double) graphHeight / (double) yGridNumbers);
    }

    private void resizeGridWidth(int graphWidth, int xGridNumbers) {
        if (xGridNumbers == NaN) {
            Log.w(TAG, "resizeGridWidth(): xGridNumbers is uninitialized!");
            return;
        }
        this.xGridNumbers = xGridNumbers;
        mStyles.gridWidth = (int) Math.floor((double) graphWidth / (double) xGridNumbers);
    }

    private void resizeGridHeight(int graphHeight, int yGridNumbers) {
        if (yGridNumbers == NaN) {
            Log.w(TAG, "resizeGridWidth(): yGridNumbers is uninitialized!");
            return;
        }
        this.yGridNumbers = yGridNumbers;
        mStyles.gridHeight = (int) Math.floor((double) graphHeight / (double) yGridNumbers);
    }

    public void resizeGraph(int width, int height) {
        width -= getGraphPadding() * 2;
        height -= getGraphPadding() * 2;
        calculateLabelWidth();
        calculateLabelHeight();
        if (isYLabelsVisible()) width = width - getLabelWidth() - getLabelPadding();
        if (isXLabelsVisible()) height = height - getLabelHeight() - getLabelPadding();
        resizeGridWidth(width, xGridNumbers);
        resizeGridHeight(height, yGridNumbers);
    }

    public void layout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int graphReservedWidth = width - getGraphPadding() * 2;
        if (isYLabelsVisible()) graphReservedWidth = width - getLabelWidth() - getLabelPadding();
        // Generate Grids
        if (xGridNumbers == NaN) {
           generateXGrids(graphReservedWidth);
        } else if (!changed && !keepGridWidth) {
            resizeGridWidth(graphReservedWidth, xGridNumbers);
        }

        int graphReservedHeight = height - getGraphPadding() * 2 ;
        if (isYLabelsVisible()) graphReservedHeight = height - getLabelHeight() - getLabelPadding();
        if (yGridNumbers == NaN) {
            generateYGrids(graphReservedHeight);
        } else if (!changed && !keepGridHeight) {
            resizeGridHeight(graphReservedHeight, yGridNumbers);
        }

        if (changed) {
            resizeGraph(width, height);
            layoutRequired = false;
        }
    }

    public void draw(Canvas canvas, int left, int top, int right, int bottom) {
        reloadStyles();

        if (xGridNumbers == NaN || yGridNumbers == NaN || layoutRequired) {
            mECGView.requestLayout();
            return;
        }

        checkBounds();

        drawHorizontal(canvas, left, top);
        drawVertical(canvas, left, top);
        invalidateRequired = false;
    }

    private void drawHorizontal(Canvas canvas, int left, int top) {
        float graphTop = top + getGraphTop();
        float graphBottom = graphTop + getGraphHeight();
        float graphLeft = left + getGraphLeft();


        // Label settings
        mLabelPaint.setTextAlign(Paint.Align.CENTER);

        for (int i=0; i <= xGridNumbers; i++) {

            float xPos = graphLeft + i * getGridWidth();

            // Draw grids
            if (isGridsVisible() && isVerticalVisible()) {
                canvas.drawLine(xPos, graphTop, xPos, graphBottom, mGridPaint);
            }

            if (i % gridsPerLargeGrid == 0 || i == xGridNumbers) {
                // Draw large grids
                if (isLargeGridsVisible() && isVerticalVisible()) {
                    canvas.drawLine(xPos, graphTop, xPos, graphBottom, mLargeGridPaint);
                }
                // Draw X labels
                if (isXLabelsVisible()) {
                    String label = xFormat.format(xMin +  i * gridXInterval);
                    if (getXLabelPosition() == XLabelPosition.TOP) {
                        canvas.drawText(label, xPos, graphTop, mLabelPaint);
                    } else if (getXLabelPosition() == XLabelPosition.BOTTOM) {
                        float yPos = graphBottom + getGraphPadding() + getLabelPadding() + getLabelHeight();
                        canvas.drawText(label, xPos, yPos, mLabelPaint);
                    }
                }
            }
        }
    }

    private void drawVertical(Canvas canvas, int left, int top) {
        float graphLeft = left + getGraphLeft();
        float graphRight = graphLeft + getGraphWidth();
        float graphTop = top + getGraphTop();

        for (int i=0; i <= yGridNumbers; i++) {
            float yPos = graphTop + i * getGridHeight();

            // Draw grids
            if (isGridsVisible() && isHorizontalVisible()) {
                canvas.drawLine(graphLeft, yPos, graphRight, yPos, mGridPaint);
            }

            if (i % gridsPerLargeGrid == 0 || i == xGridNumbers) {
                // Draw large grids
                if (isLargeGridsVisible() && isHorizontalVisible()) {
                    canvas.drawLine(graphLeft, yPos, graphRight, yPos, mLargeGridPaint);
                }
                // Draw Y labels
                if (isYLabelsVisible()) {
                    String label = yFormat.format(round(yMax - i * gridYInterval, gridYInterval));
                    float yCenter = yPos + calculateLabelHeight(label) / 2f;
                    if (getYLabelPosition() == YLabelPosition.LEFT) {
                        mLabelPaint.setTextAlign(Paint.Align.RIGHT);
                        float xPos = graphLeft - getGraphPadding() - getLabelPadding();
                        canvas.drawText(label, xPos, yCenter, mLabelPaint);
                    } else if (getYLabelPosition() == YLabelPosition.RIGHT) {
                        mLabelPaint.setTextAlign(Paint.Align.LEFT);
                        float xPos = graphRight + getGraphPadding() + getLabelPadding();
                        canvas.drawText(label, xPos, yCenter, mLabelPaint);
                    }
                }
            }
        }

    }

    public void invalidate() {
        if (!invalidateRequired) {
            invalidateRequired = true;
            mECGView.postInvalidate();
        }
    }

    public void loadXmlStyles(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ECGView, defStyleAttr, 0);
        mStyles.backgroundColor = array.getInt(R.styleable.ECGView_graphBackgroundColor, Color.TRANSPARENT);
        mStyles.gridColor = array.getInt(R.styleable.ECGView_gridColor, context.getResources().getColor(R.color.grid_color));
        mStyles.largeGridColor = array.getInt(R.styleable.ECGView_largeGridColor, context.getResources().getColor(R.color.large_grid_color));
        mStyles.labelColor = array.getInt(R.styleable.ECGView_labelColor, Color.BLACK);
        mStyles.labelTextSize = array.getDimensionPixelSize(R.styleable.ECGView_labelSize, 20);
        mStyles.gridBoarderSize = array.getDimensionPixelSize(R.styleable.ECGView_gridBoarderSize, 1);
        mStyles.largeGridBoarderSize = array.getDimensionPixelSize(R.styleable.ECGView_largeGridBoarderSize, 4);
        mStyles.labelPadding = array.getDimensionPixelOffset(R.styleable.ECGView_labelPadding, 0);
        mStyles.graphPadding = array.getDimensionPixelOffset(R.styleable.ECGView_graphPadding, 0);

        mStyles.showVertical = array.getBoolean(R.styleable.ECGView_showVertical, true);
        mStyles.showHorizontal = array.getBoolean(R.styleable.ECGView_showHorizontal, true);

        final int px_1mm = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, context.getResources().getDisplayMetrics());
        mStyles.gridWidth = array.getDimensionPixelSize(R.styleable.ECGView_gridWidth, px_1mm);
        mStyles.gridHeight = array.getDimensionPixelSize(R.styleable.ECGView_gridHeight, px_1mm);
        if (mStyles.gridWidth == px_1mm)
            mStyles.gridWidth = (int) ((double) array.getDimensionPixelSize(R.styleable.ECGView_largeGridWidth, 5 * px_1mm) / (double) gridsPerLargeGrid);
        if (mStyles.gridHeight == px_1mm)
            mStyles.gridHeight = (int) ((double) array.getDimensionPixelSize(R.styleable.ECGView_largeGridHeight, 5 * px_1mm) / (double) gridsPerLargeGrid);

        yGridNumbers = array.getInt(R.styleable.ECGView_YGridNumbers, NaN);
        xGridNumbers = array.getInt(R.styleable.ECGView_XGridNumbers, NaN);
        int large = array.getInt(R.styleable.ECGView_YLargeGridNumbers, NaN);
        if (large != NaN)
            yGridNumbers = large * gridsPerLargeGrid;
        large = array.getInt(R.styleable.ECGView_XLargeGridNumbers, NaN);
        if (large != NaN)
            xGridNumbers = large * gridsPerLargeGrid;

        mStyles.showLargeGrids = array.getBoolean(R.styleable.ECGView_showLargeGrids, true);
        mStyles.showGrids = array.getBoolean(R.styleable.ECGView_showGrids, true);
        mStyles.yLabelPosition = YLabelPosition.fromId(array.getInt(R.styleable.ECGView_YLabelPosition, XLabelPosition.NONE.value));
        mStyles.xLabelPosition = XLabelPosition.fromId(array.getInt(R.styleable.ECGView_XLabelPosition, YLabelPosition.NONE.value));
        mStyles.lineGravity = LineGravity.fromId(array.getInt(R.styleable.ECGView_lineGravity, LineGravity.CENTER.value));

        gridYInterval = array.getFloat(R.styleable.ECGView_customYIntervalPerGrid, 0.1f);
        gridXInterval = array.getFloat(R.styleable.ECGView_customXIntervalPerGrid, 0.02f);
        gridsPerLargeGrid = array.getInt(R.styleable.ECGView_customGridNumbersPerLargeGrid, 5);

        array.recycle();

        reloadStyles();
    }

    private static final class Styles {
        int backgroundColor;
        int gridColor;
        int largeGridColor;
        int labelColor;
        int gridBoarderSize;
        int largeGridBoarderSize;
        int gridWidth;
        int gridHeight;
        int labelTextSize;
        int labelPadding;
        int graphPadding;

        boolean showLargeGrids;
        boolean showGrids;
        boolean showVertical;
        boolean showHorizontal;

        YLabelPosition yLabelPosition;
        XLabelPosition xLabelPosition;
        LineGravity lineGravity;
    }

    public enum XLabelPosition {
        NONE(0),
        BOTTOM(1),
        TOP(2);

        final int value;

        XLabelPosition(int i) {
            value = i;
        }
        public static XLabelPosition fromId(int i) {
            switch (i) {
                case 1:
                    return XLabelPosition.BOTTOM;
                case 2:
                    return XLabelPosition.TOP;
            }
            return XLabelPosition.NONE;
        }
    }

    public enum YLabelPosition {
        NONE(0),
        LEFT(1),
        RIGHT(2);

        final int value;

        YLabelPosition(int i) {
            value = i;
        }
        public static YLabelPosition fromId(int i) {
            switch (i) {
                case 1:
                    return YLabelPosition.LEFT;
                case 2:
                    return YLabelPosition.RIGHT;
            }
            return YLabelPosition.NONE;
        }
    }

    public enum LineGravity {
        TOP(0),
        CENTER(1),
        BOTTOM(2);

        final int value;

        LineGravity(int i) {
            value = i;
        }
        public static LineGravity fromId(int i) {
            switch (i) {
                case 0:
                    return LineGravity.TOP;
                case 2:
                    return LineGravity.BOTTOM;
            }
            return LineGravity.CENTER;
        }
    }

    public int getBackgroundColor() { return mStyles.backgroundColor; }
    public int getGridColor() { return mStyles.gridColor; }
    public int getLargeGridColor() { return mStyles.largeGridColor; }

    public int getLabelColor() { return mStyles.labelColor; }

    public int getGridWidth() { return mStyles.gridWidth; }
    public int getGridHeight() { return mStyles.gridHeight; }
    public int getLargeGridWidth() { return gridsPerLargeGrid * mStyles.gridWidth; }
    public int getLargeGridHeight() { return gridsPerLargeGrid * mStyles.gridHeight; }
    public int getLabelTextSize() { return mStyles.labelTextSize; }

    public int getLargeGridBoarderSize() { return mStyles.largeGridBoarderSize; }
    public int getGridBoarderSize() { return mStyles.gridBoarderSize; }
    public int getLabelPadding() {return mStyles.labelPadding; }
    public int getGraphPadding() {return mStyles.graphPadding; }

    public int getYGridNumbers() {return yGridNumbers; }
    public int getXGridNumbers() {return xGridNumbers; }
    public int getYLargeGridNumbers() { return (int) Math.ceil((double) yGridNumbers / (double) gridsPerLargeGrid); }
    public int getXLargeGridNumbers() { return (int) Math.ceil((double) xGridNumbers / (double) gridsPerLargeGrid); }

    public boolean isLargeGridsVisible() { return mStyles.showLargeGrids; }
    public boolean isGridsVisible() { return mStyles.showGrids; }
    public boolean isXLabelsVisible() { return getXLabelPosition() != XLabelPosition.NONE; }
    public boolean isYLabelsVisible() { return getYLabelPosition() != YLabelPosition.NONE; }
    public boolean isVerticalVisible() { return mStyles.showVertical; }
    public boolean isHorizontalVisible() { return mStyles.showHorizontal; }

    public YLabelPosition getYLabelPosition() { return mStyles.yLabelPosition; }
    public XLabelPosition getXLabelPosition() { return mStyles.xLabelPosition; }
    public LineGravity getLineGravity() { return mStyles.lineGravity; }

    public double getGridXInterval() { return gridXInterval; }
    public double getGridYInterval() { return gridYInterval; }
    public double getLargeGridXInterval() { return gridsPerLargeGrid * gridXInterval; }
    public double getLargeGridYInterval() { return gridsPerLargeGrid * gridYInterval; }
    public int getGridsPerLargeGrid() { return gridsPerLargeGrid; }
    public double getXMin() { return xMin; }
    public double getXMax() { return xMax; }
    public double getYMin() { return yMin; }
    public double getYMax() { return yMax; }

    public void setBackgroundColor(int color) {
        mStyles.backgroundColor = color;
        if (autoInvalidate)
            invalidate();
    }
    public void setGridColor(int color) {
        mStyles.gridColor = color;
        if (autoInvalidate)
            invalidate();
    }
    public void setLargeGridColor(int color) {
        mStyles.largeGridColor = color;
        if (autoInvalidate)
            invalidate();
    }

    public void setLabelColor(int color) {
        mStyles.labelColor = color;
        if (autoInvalidate)
            invalidate();
    }

    public void setLabelTextSize(int size) {
        mStyles.labelTextSize = size;
        layoutRequired();
        if (autoInvalidate)
            invalidate();
    }
    public void setGridBoarderSize(int size) {
        mStyles.gridBoarderSize = size;
        if (autoInvalidate)
            invalidate();
    }
    public void setLargeGridBoarderSize(int size) {
        mStyles.largeGridBoarderSize = size;
        if (autoInvalidate)
            invalidate();
    }
    public void setLabelPadding(int padding) {
        mStyles.labelPadding = padding;
        if (autoInvalidate)
            invalidate();
    }
    public void setGraphPadding(int padding) {
        mStyles.graphPadding = padding;
        if (autoInvalidate)
            invalidate();
    }

    public void setGridWidth(int width, boolean changeGridNumbers) {
        mStyles.gridWidth = width;
        if (changeGridNumbers) {
            xGridNumbers = NaN;
        }
        if (autoInvalidate)
            invalidate();
    }

    public void setGridHeight(int height, boolean changeGridNumbers) {
        mStyles.gridHeight = height;
        if (changeGridNumbers) {
            yGridNumbers = NaN;
        }
        if (autoInvalidate)
            invalidate();
    }

    public void setLargeGridWidth(int width, boolean changeGridNumbers) {
        setGridWidth((int) ((double) width / (double) gridsPerLargeGrid), changeGridNumbers);
    }

    public void setLargeGridHeight(int height, boolean changeGridNumbers) {
        setGridHeight((int) ((double) height / (double) gridsPerLargeGrid), changeGridNumbers);
    }

    public void setYGridNumbers(int numbers, boolean keepGridHeight) {
        yGridNumbers = numbers;
        setBounds(xMin, yMin, xMax, yMax, true, false);
        this.keepGridHeight = keepGridHeight;
        if (keepGridHeight) {
            mECGView.requestLayout();
        }
        if (autoInvalidate)
            invalidate();
    }
    public void setXGridNumbers(int numbers, boolean keepGridWidth) {
        xGridNumbers = numbers;
        setBounds(xMin, yMin, xMax, yMax, true, false);
        this.keepGridWidth = keepGridWidth;
        if (keepGridWidth) {
            mECGView.requestLayout();
        }
        if (autoInvalidate)
            invalidate();
    }
    public void setYLargeGridNumbers(int numbers, boolean keepGridHeight) {
        setYGridNumbers(gridsPerLargeGrid * numbers, keepGridHeight);
    }
    public void setXLargeGridNumbers(int numbers, boolean keepGridWidth) {
        setXGridNumbers(gridsPerLargeGrid * numbers, keepGridWidth);
    }

    public void showLargeGrids(boolean b) {
        mStyles.showLargeGrids = b;
        if (autoInvalidate)
            invalidate();
    }
    public void showGrids(boolean b) {
        mStyles.showGrids = b;
        if (autoInvalidate)
            invalidate();
    }

    public void showVertical(boolean b) {
        mStyles.showVertical = b;
        if (autoInvalidate)
            invalidate();
    }
    public void showHorizontal(boolean b) {
        mStyles.showHorizontal = b;
        if (autoInvalidate)
            invalidate();
    }

    public void showYLabel(boolean b) {
        if (b)
            setYLabelPosition(YLabelPosition.LEFT, true);
        else
            setYLabelPosition(YLabelPosition.NONE, false);
    }

    public void showXLabel(boolean b) {
        if (b)
            setXLabelPosition(XLabelPosition.BOTTOM, true);
        else
            setXLabelPosition(XLabelPosition.NONE, false);
    }

    public void setYLabelPosition(YLabelPosition position, boolean changeLabelSize) {
        if (position == mStyles.yLabelPosition)
            return;
        mStyles.yLabelPosition = position;
        if (changeLabelSize || (position != YLabelPosition.NONE && labelWidth == NaN))
            calculateLabelWidth();
        if (autoInvalidate) {
            layoutRequired();
            invalidate();
        }
    }
    public void setXLabelPosition(XLabelPosition position, boolean changeLabelSize) {
        if (position == mStyles.xLabelPosition)
            return;
        mStyles.xLabelPosition = position;
        if (changeLabelSize || (position != XLabelPosition.NONE && labelHeight == NaN))
            calculateLabelHeight();
        if (autoInvalidate) {
            layoutRequired();
            invalidate();
        }
    }
    public void setLineGravity(LineGravity gravity, boolean keepBounds) {
        mStyles.lineGravity = gravity;
        if (!keepBounds) {
            yMin = Double.NaN;
            yMax = Double.NaN;
        }
        if (autoInvalidate)
            invalidate();
    }

    public void setGridYInterval(double mV, boolean keepBounds) {
        gridYInterval = mV;
        if (keepBounds) {
            yGridNumbers = NaN;
        } else {
            yMin = Double.NaN;
            yMax = Double.NaN;
            setBounds(xMin, yMin, xMax, yMax,true, false);
        }
        if (autoInvalidate)
            invalidate();
    }
    public void setGridXInterval(double sec, boolean keepBounds) {
        gridXInterval = sec;
        if (keepBounds) {
            xGridNumbers = NaN;
        } else {
            xMin = Double.NaN;
            xMax = Double.NaN;
            setBounds(xMin, yMin, xMax, yMax, true, false);
        }
        if (autoInvalidate)
            invalidate();
    }

    public void setGridsPerLargeGrid(int i) {
        gridsPerLargeGrid = i;
        if (autoInvalidate)
            invalidate();
    }

    public void setXMax(double i, boolean keepGridNumbers) {
        enableAutoBounds(false);
        this.xMax = round(i, gridXInterval);
        if (keepGridNumbers) {
            if (xGridNumbers != NaN)
                xMin = xMax - xGridNumbers * gridXInterval;
            else
                xMax = Double.NaN;
        } else {
            xGridNumbers = NaN;
        }

        if (autoInvalidate)
            invalidate();
    }

    public void setXMin(double i, boolean keepGridNumbers) {
        enableAutoBounds(false);
        this.xMin = round(i, gridXInterval);
        if (keepGridNumbers) {
            if (xGridNumbers != NaN)
                xMax = xMin + xGridNumbers * gridXInterval;
            else
                xMax = Double.NaN;
        } else {
            xGridNumbers = NaN;
        }

        if (autoInvalidate)
            invalidate();
    }

    public void setYMax(double i, boolean keepGridNumbers) {
        enableAutoBounds(false);
        this.yMax = round(i, gridYInterval);
        if (keepGridNumbers) {
            if (yGridNumbers != NaN)
                yMin = yMax - yGridNumbers * gridYInterval;
            else
                yMin = Double.NaN;
        } else {
            yGridNumbers = NaN;
        }

        if (autoInvalidate)
            invalidate();
    }

    public void setYMin(double i, boolean keepGridNumbers) {
        enableAutoBounds(false);
        this.yMin = round(i, gridYInterval);
        if (keepGridNumbers) {
            if (yGridNumbers != NaN)
                yMax = yMin + yGridNumbers * gridYInterval;
            else
                yMax = Double.NaN;
        } else {
            yGridNumbers = NaN;
        }

        if (autoInvalidate)
            invalidate();
    }

    public void setBounds(double xMin, double yMin, double xMax, double yMax, boolean keepGridNumbers, boolean changeLabelSize) {
        enableAutoBounds(false);
        this.xMin = round(xMin, gridXInterval);
        this.xMax = round(xMax, gridXInterval);
        this.yMin = round(yMin, gridYInterval);
        this.yMax = round(yMax, gridYInterval);
        if (!keepGridNumbers) {
            xGridNumbers = NaN;
            yGridNumbers = NaN;
        }
        if (changeLabelSize) {
            layoutRequired();
        }
        if (autoInvalidate)
            invalidate();
    }

    public void setXFormat(DecimalFormat format, boolean changeLabelSize) {
        xFormat = format;
        if (changeLabelSize)
            layoutRequired();
        if (autoInvalidate)
            invalidate();
    }

    public void setYFormat(DecimalFormat format, boolean changeLabelSize) {
        yFormat = format;
        if (changeLabelSize)
            layoutRequired();
        if (autoInvalidate)
            invalidate();
    }

    public void enableAutoInvalidate(boolean b) {
        autoInvalidate = b;
    }

    public void layoutRequired() {
        layoutRequired = true;
    }

    public void enableAutoBounds(boolean b) {
        syncBounds = b;
    }
}
