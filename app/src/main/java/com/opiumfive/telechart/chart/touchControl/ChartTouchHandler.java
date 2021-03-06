package com.opiumfive.telechart.chart.touchControl;

import android.content.Context;
import android.view.MotionEvent;

import com.opiumfive.telechart.chart.CType;
import com.opiumfive.telechart.chart.IChart;
import com.opiumfive.telechart.chart.draw.ChartViewrectHandler;
import com.opiumfive.telechart.chart.model.SelectedValues;
import com.opiumfive.telechart.chart.draw.GodChartRenderer;

public class ChartTouchHandler {

    protected ChartScroller chartScroller;
    protected IChart chart;
    protected ChartViewrectHandler chartViewrectHandler;
    protected GodChartRenderer renderer;
    protected boolean isScrollEnabled = true;
    protected boolean isValueTouchEnabled = true;
    protected SelectedValues selectedValues = new SelectedValues();
    protected OnUpTouchListener onUpTouchListener;

    public ChartTouchHandler(Context context, IChart chart) {
        this.chart = chart;
        this.chartViewrectHandler = chart.getChartViewrectHandler();
        this.renderer = chart.getChartRenderer();
        chartScroller = new ChartScroller(context);
    }

    public void setOnUpTouchListener(OnUpTouchListener listener) {
        onUpTouchListener = listener;
    }

    public void resetTouchHandler() {
        this.chartViewrectHandler = chart.getChartViewrectHandler();
        this.renderer = chart.getChartRenderer();
    }

    public boolean computeScroll() {
        boolean needInvalidate = false;
        if (isScrollEnabled && chartScroller.computeScrollOffset(chartViewrectHandler)) {
            needInvalidate = true;
        }
        return needInvalidate;
    }

    public boolean handleTouchEvent(MotionEvent event) {
        boolean needInvalidate = false;

        if (isValueTouchEnabled) {
            needInvalidate = computeTouch(event);
        }

        return needInvalidate;
    }

    protected boolean computeTouch(MotionEvent event) {
        boolean needInvalidate = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                boolean isTouched = false;

                if (renderer.isTouched()) {
                    boolean izZoomTouch = renderer.checkDetailsTouch(event.getX(), event.getY());
                    if (izZoomTouch) {
                        if (chart.getType().equals(CType.AREA)) {
                            chart.startMorphling(CType.PIE);
                        }
                    } else {
                        isTouched = checkTouch(event.getX(), event.getY());
                        if (isTouched) {
                            needInvalidate = true;
                        }
                    }
                } else {
                    isTouched = checkTouch(event.getX(), event.getY());
                    if (isTouched) {
                        needInvalidate = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (renderer.isTouched()) {
                    //renderer.clearTouch();
                    //needInvalidate = true;
                }
                if (onUpTouchListener != null) {
                    onUpTouchListener.onUp();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                isTouched = checkTouch(event.getX(), event.getY());
                if (isTouched) {
                    needInvalidate = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (renderer.isTouched()) {
                    //renderer.clearTouch();
                    //needInvalidate = true;
                }
                break;
        }
        return needInvalidate;
    }

    private boolean checkTouch(float touchX, float touchY) {
        selectedValues.clear();

        if (renderer.checkTouch(touchX, touchY)) {
            selectedValues.set(renderer.getSelectedValues());
        }

        return renderer.isTouched();
    }

    public void setScrollEnabled(boolean isScrollEnabled) {
        this.isScrollEnabled = isScrollEnabled;
    }

    public void setValueTouchEnabled(boolean isValueTouchEnabled) {
        this.isValueTouchEnabled = isValueTouchEnabled;
    }

    public interface OnUpTouchListener {
        void onUp();
    }
}
