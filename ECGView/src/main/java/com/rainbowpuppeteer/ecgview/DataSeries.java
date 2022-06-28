package com.rainbowpuppeteer.ecgview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Data Series
 * Hold and render data points
 *
 * @author RainbowPuppeteer
 */
public class DataSeries {

    /**
     * Debug tag
     */
    private static final String TAG = "ECGView.DataSeries";

    /**
     * Static Graph Code
     * In this state the graph data can only be updated via setDataPoint
     *
     * @see #setGraphType(int)
     * @see #setDataPoints(Point[], boolean)
     */
    public static final int STATIC_GRAPH = 0;

    /**
     * Dynamic Graph Code
     * In this state the graph data can only be updated via appendDataPoint
     * ECG for real-time updating data
     *
     * @see #setGraphType(int)
     * @see #appendDataPoint(Point, boolean)
     * @see #appendDataPoint(double, double, boolean)
     */
    public static final int DYNAMIC_GRAPH = 1;

    /**
     * Style holder class
     */
    private static class Styles {

        /**
         * The color of line
         *
         * @see #getLineColor()
         * @see #setLineColor(int)
         */
        int lineColor;

        /**
         * The thickness of line
         *
         * @see #getLineSize()
         * @see #setLineSize(int)
         */
        int lineSize;
    }

    /**
     * Style holder
     */
    private final Styles mStyles;

    /**
     * Main view
     *
     * @see ECGView
     */
    private final ECGView mECGView;

    /**
     * Array to store drawing data
     *
     * @see #setDataPoints(Point[], boolean)
     * @see #appendDataPoint(Point, boolean)
     * @see #appendDataPoint(double, double, boolean)
     */
    private final ArrayList<Point> mDataPoints;

    /**
     * Type of graph
     *
     * @see #STATIC_GRAPH
     * @see #DYNAMIC_GRAPH
     */
    private int mGraphType;

    /**
     * Invalidate after any style or data changes
     *
     * @see #setAutoInvalidate(boolean)
     */
    private boolean autoInvalidate;

    /**
     * ONLY IN DYNAMIC MODE
     * Auto delete points that are out of x-bounds
     * to save memory
     *
     * @see #setAutoDeleteOutBoundsPoints(boolean)
     */
    private boolean autoDelete;

    /**
     * Is the invalidation already posted
     */
    private boolean invalidateRequired;

    /**
     * Paint of line
     */
    private Paint mLinePaint;

    /**
     * Data points to draw
     * In dynamic mode, x values work as delta time
     */
    public static class Point {

        /**
         * x value: sec / d sec
         */
        double x;

        /**
         * y value: mV
         */
        double y;

        /**
         * Constructor.
         *
         * @param x x value
         * @param y y value
         */
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Constructor.
         * Get a copy of Point
         *
         * @param p point
         */
        Point(Point p) {
            this.x = p.x;
            this.y = p.y;
        }

    }

    /**
     * Load styles from xml file
     *
     * @param context context
     * @param attrs attrs
     * @param defStyleAttr defStyleAttr
     * @see ECGView
     */
    public void loadXmlStyles(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ECGView, defStyleAttr, 0);
        mStyles.lineColor = array.getInt(R.styleable.ECGView_lineColor, Color.BLACK);
        final int px_0_5mm = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 0.4f, context.getResources().getDisplayMetrics());
        mStyles.lineSize = array.getDimensionPixelSize(R.styleable.ECGView_lineSize, px_0_5mm);
        array.recycle();

        reloadStyles();
    }

    /**
     * Reload Styles before drawing
     */
    private void reloadStyles() {
        mLinePaint = new Paint();
        mLinePaint.setColor(getLineColor());
        mLinePaint.setStrokeWidth(getLineSize());
    }

    /**
     * Constructor.
     * Initialize attributes
     *
     * @param ecgView parent view
     * @see ECGView
     */
    DataSeries(ECGView ecgView) {
        mStyles = new Styles();
        mDataPoints = new ArrayList<>();
        mECGView = ecgView;

        mGraphType = DYNAMIC_GRAPH;
        autoInvalidate = true;
        autoDelete = true;
        invalidateRequired = true;
    }

    /**
     * Draw the line
     *
     * @param canvas canvas
     * @param left graph left
     * @param top graph top
     * @param right graph right
     * @param bottom graph bottom
     * @param graphXMin graph x min value
     * @param graphYMin graph y min value
     * @param graphXMax graph x max
     * @param graphYMax graph y max
     * @see Graph#getGraphLeft()
     * @see Graph#getGraphTop()
     * @see Graph#getGraphWidth()
     * @see Graph#getGraphHeight()
     * @see Graph#getXMin()
     * @see Graph#getYMin()
     * @see Graph#getXMax()
     * @see Graph#getYMax()
     */
    public void draw(Canvas canvas, int left, int top, int right, int bottom,
                     double graphXMin, double graphYMin, double graphXMax, double graphYMax) {
        reloadStyles();

        if (mDataPoints.size() == 0)
            return;

        float width = right - left;
        float height = bottom - top;
        float x_interval = (float) graphXMax - (float) graphXMin;
        float y_interval = (float) graphYMax - (float) graphYMin;

        float lastXPos = Float.NaN, lastYPos = Float.NaN, xPos, yPos;

        float x_value = 0, y_value = 0;

        if (mGraphType == DYNAMIC_GRAPH) {
            graphXMin = -1 * x_interval;
        }

        for (int i=0; i < mDataPoints.size(); i++) {
            Point p;
            if (mGraphType == DYNAMIC_GRAPH)
                p = mDataPoints.get(mDataPoints.size() - 1 - i);
            else
                p = mDataPoints.get(i);

            y_value = (float) p.y;
            if (mGraphType == DYNAMIC_GRAPH) {
                if (i != 0)
                    x_value -= (float) mDataPoints.get(mDataPoints.size() - i).x;
            } else {
                x_value = (float) p.x;
            }

            // Calculate the position of current point
            xPos = left + ((x_value - (float) graphXMin) / x_interval) * width;
            yPos = bottom - ((y_value - (float) graphYMin) / y_interval) * height;

            boolean draw = true;

            if (i != 0) {
                float x = xPos;
                float y = yPos;

                // Check if this point is out of bounds
                if (xPos < left) {
                    if (lastXPos < left) {
                        draw = false;
                        if (mGraphType == DYNAMIC_GRAPH && autoDelete) {
//                            Log.d(TAG, mDataPoints.size() - i + " Points deleted");
                            mDataPoints.subList(0, mDataPoints.size() - i + 1).clear();
                            break;
                        }
                    } else {
                        float b = (left - xPos) / (lastXPos - xPos) * (lastYPos - yPos);
                        x = left;
                        y = lastYPos + b;
                    }
                } else if (xPos > right) {
                    if (lastXPos > right) {
                        draw = false;
                    } else {
                        float b = (right - lastXPos) / (xPos - lastXPos) * (yPos - lastYPos);
                        x = right;
                        y = lastYPos + b;
                    }
                }

                if (y < top) {
                    if (lastYPos < top) {
                        draw = false;
                    } else {
                        float b = (top - lastYPos) / (y - lastYPos) * (x - lastXPos);
                        y = top;
                        x = lastXPos + b;
                    }
                } else if (y > bottom) {
                    if (lastYPos > bottom) {
                        draw = false;
                    } else {
                        float b = (bottom - lastYPos) / (y - lastYPos) * (x - lastXPos);
                        y = bottom;
                        x = lastXPos + b;
                    }
                }

                if (draw) {
                    // Check if the previous point if out of bounds
                    if (lastXPos < left) {
                        float b = (left - lastXPos) / (x - lastXPos) * (y - lastYPos);
                        lastXPos = left;
                        lastYPos = lastYPos + b;
                    }

                    if (lastYPos < top) {
                        float b = (top - lastYPos) / (y - lastYPos) * (x - lastXPos);
                        lastYPos = top;
                        lastXPos = lastXPos + b;
                    } else if (y > bottom) {
                        float b = (bottom - lastYPos) / (y - lastYPos) * (x - lastXPos);
                        lastYPos = bottom;
                        lastXPos = lastXPos + b;
                    }
                    canvas.drawLine(lastXPos, lastYPos, x, y, mLinePaint);
                }
            }

            lastXPos = xPos;
            lastYPos = yPos;
        }
    invalidateRequired = false;
    }

    /**
     * Call the main view to invalidate
     */
    private void invalidate() {
        if (!invalidateRequired) {
            invalidateRequired = true;
            mECGView.postInvalidate();
        }
    }

    /**
     * Set the graph type
     *
     * @param graphType graph type
     * @see DataSeries#DYNAMIC_GRAPH
     * @see DataSeries#STATIC_GRAPH
     */
    public void setGraphType(int graphType) {
        if (graphType != STATIC_GRAPH && graphType != DYNAMIC_GRAPH) {
            Log.e(TAG, "Illegal argument");
        }
        mGraphType = graphType;
        mDataPoints.clear();
    }

    /**
     * Clear data points
     */
    public void clear() {
        mDataPoints.clear();
    }

    /**
     * Set the graph type
     *
     * @see #DYNAMIC_GRAPH
     * @see #STATIC_GRAPH
     */
    public int getGraphType() {
        return mGraphType;
    }

    /**
     * Set the points of the static graph
     *
     * @param points data points
     * @param invalidate invalidate or not
     * @see #setDataPoints(Point[], boolean)
     */
    public void setDataPoints(Point[] points, boolean invalidate) {
        if (mGraphType != STATIC_GRAPH) {
            Log.e(TAG, "Set graph to static first to set data points");
            return;
        }
        mDataPoints.clear();
        mDataPoints.addAll(Arrays.asList(points));
        if (autoInvalidate || invalidate)
            invalidate();
    }

    /**
     * Append a new point to the dynamic graph
     *
     * @param point point to append
     * @param invalidate invalidate or not
     * @see #setDataPoints(Point[], boolean)
     * @see #appendDataPoint(double, double, boolean)
     */
    public void appendDataPoint(Point point, boolean invalidate) {
        if (mGraphType != DYNAMIC_GRAPH) {
            Log.e(TAG, "Set graph to dynamic first to append a data point data");
            return;
        }
        mDataPoints.add(point);
        if (autoInvalidate || invalidate)
                invalidate();
    }

    /**
     * Append a new point to the dynamic graph
     *
     * @param y_value y value
     * @param d_time delta time from last point
     * @param invalidate invalidate or not
     * @see #setDataPoints(Point[], boolean)
     * @see #appendDataPoint(Point, boolean)
     */
    public void appendDataPoint(double y_value, double d_time, boolean invalidate) {
        if (mGraphType != DYNAMIC_GRAPH) {
            Log.e(TAG, "Set graph to dynamic first to append a data point data");
            return;
        }
        Point p = new Point(d_time, y_value);
        mDataPoints.add(p);
        if (autoInvalidate || invalidate)
            invalidate();
    }

    public int getLineColor() { return mStyles.lineColor; }

    public void setLineColor(int color) {
        mStyles.lineColor = color;
        if (autoInvalidate)
            invalidate();
    }
    public int getLineSize() { return mStyles.lineSize; }

    public void setLineSize(int size) {
        mStyles.lineSize = size;
        if (autoInvalidate)
            invalidate();
    }

    public double getYMax() {
        if (mDataPoints.size() == 0) {
            Log.e(TAG, "No point stored!");
            return 0;
        }
        double tmp = Double.NEGATIVE_INFINITY;
        for (Point p: mDataPoints) {
            if (p.y > tmp)
                tmp = p.y;
        }
        return tmp;
    }

    public double getYMin() {
        if (mDataPoints.size() == 0) {
            Log.e(TAG, "No point stored!");
            return 0;
        }
        double tmp = Double.POSITIVE_INFINITY;
        for (Point p: mDataPoints) {
            if (p.y < tmp)
                tmp = p.y;
        }
        return tmp;
    }

    public double getXMax() {
        if (mDataPoints.size() == 0) {
            Log.e(TAG, "No point stored!");
            return 0;
        }
        return mDataPoints.get(mDataPoints.size() - 1).x;
    }

    public double getXMin() {
        if (mDataPoints.size() == 0) {
            Log.e(TAG, "No point stored!");
            return 0;
        }
        return mDataPoints.get(0).x;
    }

    public void setAutoInvalidate(boolean b) {
        autoInvalidate = b;
    }

    public void setAutoDeleteOutBoundsPoints(boolean b) {
        autoDelete = b;
    }
}
