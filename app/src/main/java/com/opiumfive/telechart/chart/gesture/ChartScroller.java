package com.opiumfive.telechart.chart.gesture;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.opiumfive.telechart.chart.renderer.ChartViewportHandler;
import com.opiumfive.telechart.chart.model.Viewport;

public class ChartScroller {

    private Viewport scrollerStartViewport = new Viewport();
    private Point surfaceSizeBuffer = new Point();
    private OverScroller scroller;

    public ChartScroller(Context context) {
        scroller = new OverScroller(context);
    }

    public boolean startScroll(ChartViewportHandler chartViewportHandler) {
        scroller.abortAnimation();
        scrollerStartViewport.set(chartViewportHandler.getCurrentViewport());
        return true;
    }

    public boolean scroll(ChartViewportHandler chartViewportHandler, float distanceX, float distanceY, ScrollResult scrollResult) {

        final Viewport maxViewport = chartViewportHandler.getMaximumViewport();
        final Viewport visibleViewport = chartViewportHandler.getVisibleViewport();
        final Viewport currentViewport = chartViewportHandler.getCurrentViewport();
        final Rect contentRect = chartViewportHandler.getContentRectMinusAllMargins();

        final boolean canScrollLeft = currentViewport.left > maxViewport.left;
        final boolean canScrollRight = currentViewport.right < maxViewport.right;
        final boolean canScrollTop = currentViewport.top < maxViewport.top;
        final boolean canScrollBottom = currentViewport.bottom > maxViewport.bottom;

        boolean canScrollX = false;
        boolean canScrollY = false;

        canScrollX = canScrollLeft && distanceX <= 0 || canScrollRight && distanceX >= 0;

        canScrollY = canScrollTop && distanceY <= 0 || canScrollBottom && distanceY >= 0;

        if (canScrollX || canScrollY) {
            chartViewportHandler.computeScrollSurfaceSize(surfaceSizeBuffer);

            float viewportOffsetX = distanceX * visibleViewport.width() / contentRect.width();
            float viewportOffsetY = -distanceY * visibleViewport.height() / contentRect.height();

            chartViewportHandler.setViewportTopLeft(currentViewport.left + viewportOffsetX, currentViewport.top + viewportOffsetY);
        }

        scrollResult.canScrollX = canScrollX;
        scrollResult.canScrollY = canScrollY;

        return canScrollX || canScrollY;
    }

    public boolean computeScrollOffset(ChartViewportHandler computator) {
        if (scroller.computeScrollOffset()) {
            final Viewport maxViewport = computator.getMaximumViewport();

            computator.computeScrollSurfaceSize(surfaceSizeBuffer);

            final float currXRange = maxViewport.left + maxViewport.width() * scroller.getCurrX() / surfaceSizeBuffer.x;
            final float currYRange = maxViewport.top - maxViewport.height() * scroller.getCurrY() / surfaceSizeBuffer.y;

            computator.setViewportTopLeft(currXRange, currYRange);

            return true;
        }

        return false;
    }

    public boolean fling(int velocityX, int velocityY, ChartViewportHandler computator) {
        computator.computeScrollSurfaceSize(surfaceSizeBuffer);
        scrollerStartViewport.set(computator.getCurrentViewport());

        int startX = (int) (surfaceSizeBuffer.x * (scrollerStartViewport.left - computator.getMaximumViewport().left) / computator.getMaximumViewport().width());
        int startY = (int) (surfaceSizeBuffer.y * (computator.getMaximumViewport().top - scrollerStartViewport.top) / computator.getMaximumViewport().height());

        scroller.forceFinished(true);

        final int width = computator.getContentRectMinusAllMargins().width();
        final int height = computator.getContentRectMinusAllMargins().height();
        scroller.fling(startX, startY, velocityX, velocityY, 0, surfaceSizeBuffer.x - width + 1, 0, surfaceSizeBuffer.y - height + 1);
        return true;
    }

    public static class ScrollResult {
        public boolean canScrollX;
        public boolean canScrollY;
    }

}
