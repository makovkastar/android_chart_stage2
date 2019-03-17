package com.opiumfive.telechart.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.opiumfive.telechart.chart.listener.ChartAnimationListener;
import com.opiumfive.telechart.chart.listener.ChartViewrectAnimator;
import com.opiumfive.telechart.chart.render.ChartViewrectHandler;
import com.opiumfive.telechart.chart.touchControl.ChartTouchHandler;
import com.opiumfive.telechart.chart.listener.ViewrectChangeListener;
import com.opiumfive.telechart.chart.model.LineChartData;
import com.opiumfive.telechart.chart.model.SelectedValues;
import com.opiumfive.telechart.chart.model.Viewrect;
import com.opiumfive.telechart.chart.render.AxesRenderer;
import com.opiumfive.telechart.chart.render.LineChartRenderer;


public class ChartView extends View implements ILineChart, ChartDataProvider {

    protected LineChartData data;

    protected ChartViewrectHandler chartViewrectHandler;
    protected AxesRenderer axesRenderer;
    protected ChartTouchHandler touchHandler;
    protected LineChartRenderer chartRenderer;
    protected ChartViewrectAnimator viewportAnimator;

    public ChartView(Context context) {
        this(context, null, 0);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        chartViewrectHandler = new ChartViewrectHandler();
        touchHandler = new ChartTouchHandler(context, this);
        axesRenderer = new AxesRenderer(context, this);
        this.viewportAnimator = new ChartViewrectAnimator(this);

        setChartRenderer(new LineChartRenderer(context, this, this));
        setChartData(LineChartData.generateDummyData());
    }

    @Override
    public LineChartData getChartData() {
        return data;
    }

    @Override
    public void setChartData(LineChartData data) {

        if (null == data) {
            this.data = LineChartData.generateDummyData();
        } else {
            this.data = data;
        }

        onChartDataChange();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        chartViewrectHandler.setContentRect(getWidth(), getHeight(), getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        chartRenderer.onChartSizeChanged();
        axesRenderer.onChartSizeChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isEnabled()) {
            axesRenderer.drawInBackground(canvas);
            int clipRestoreCount = canvas.save();
            canvas.clipRect(chartViewrectHandler.getContentRectMinusAllMargins());
            chartRenderer.draw(canvas);
            canvas.restoreToCount(clipRestoreCount);

            axesRenderer.drawInForeground(canvas);
            chartRenderer.drawUnclipped(canvas);
        } else {
            canvas.drawColor(Util.DEFAULT_COLOR);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        boolean needInvalidate = touchHandler.handleTouchEvent(event);

        if (needInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }

        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (touchHandler.computeScroll()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setViewportAnimationListener(ChartAnimationListener animationListener) {
        viewportAnimator.setChartAnimationListener(animationListener);
    }

    public void setViewportChangeListener(ViewrectChangeListener viewrectChangeListener) {
        chartViewrectHandler.setViewrectChangeListener(viewrectChangeListener);
    }

    public LineChartRenderer getChartRenderer() {
        return chartRenderer;
    }

    public void setChartRenderer(LineChartRenderer renderer) {
        chartRenderer = renderer;
        resetRendererAndTouchHandler();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public ChartViewrectHandler getChartViewrectHandler() {
        return chartViewrectHandler;
    }

    public boolean isScrollEnabled() {
        return touchHandler.isScrollEnabled();
    }

    public void setScrollEnabled(boolean isScrollEnabled) {
        touchHandler.setScrollEnabled(isScrollEnabled);
    }

    public boolean isValueTouchEnabled() {
        return touchHandler.isValueTouchEnabled();
    }

    public void setValueTouchEnabled(boolean isValueTouchEnabled) {
        touchHandler.setValueTouchEnabled(isValueTouchEnabled);
    }

    public Viewrect getMaximumViewport() {
        return chartRenderer.getMaximumViewrect();
    }

    public void setMaximumViewport(Viewrect maxViewrect) {
        chartRenderer.setMaximumViewrect(maxViewrect);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setCurrentViewrectAnimated(Viewrect targetViewrect) {

        if (null != targetViewrect) {
            viewportAnimator.cancelAnimation();
            viewportAnimator.startAnimation(getCurrentViewrect(), targetViewrect);
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public Viewrect getCurrentViewrect() {
        return getChartRenderer().getCurrentViewrect();
    }

    public void setCurrentViewrect(Viewrect targetViewrect) {

        if (null != targetViewrect) {
            chartRenderer.setCurrentViewrect(targetViewrect);
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setViewportCalculationEnabled(boolean isEnabled) {
        chartRenderer.setViewrectCalculationEnabled(isEnabled);
    }

    public void onChartDataChange() {
        chartViewrectHandler.resetContentRect();
        chartRenderer.onChartDataChanged();
        axesRenderer.onChartDataChanged();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    protected void resetRendererAndTouchHandler() {
        this.chartRenderer.resetRenderer();
        this.axesRenderer.resetRenderer();
        this.touchHandler.resetTouchHandler();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        final Viewrect currentViewrect = getCurrentViewrect();
        final Viewrect maximumViewrect = getMaximumViewport();
        if (direction < 0) {
            return currentViewrect.left > maximumViewrect.left;
        } else {
            return currentViewrect.right < maximumViewrect.right;
        }
    }
}
