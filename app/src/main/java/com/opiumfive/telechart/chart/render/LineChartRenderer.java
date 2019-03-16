package com.opiumfive.telechart.chart.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.opiumfive.telechart.R;
import com.opiumfive.telechart.chart.ILineChart;
import com.opiumfive.telechart.chart.model.Line;
import com.opiumfive.telechart.chart.model.LineChartData;
import com.opiumfive.telechart.chart.model.PointValue;
import com.opiumfive.telechart.chart.model.SelectedValue;
import com.opiumfive.telechart.chart.model.Viewport;
import com.opiumfive.telechart.chart.ChartDataProvider;
import com.opiumfive.telechart.chart.util.ChartUtils;

import java.util.List;

import static com.opiumfive.telechart.Util.getColorFromAttr;


public class LineChartRenderer {

    private static final int DEFAULT_LINE_STROKE_WIDTH_DP = 2;
    private static final int DEFAULT_TOUCH_TOLERANCE_MARGIN_DP = 3;
    private static final float DEFAULT_MAX_ANGLE_VARIATION = 2f;

    public int DEFAULT_LABEL_MARGIN_DP = 0;
    protected ILineChart chart;
    protected ChartViewportHandler chartViewportHandler;

    protected Paint labelPaint = new Paint();
    protected Paint labelBackgroundPaint = new Paint();
    protected RectF labelBackgroundRect = new RectF();
    protected Paint touchLinePaint = new Paint();
    protected Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
    protected boolean isViewportCalculationEnabled = true;
    protected float density;
    protected float scaledDensity;
    protected SelectedValue selectedValue = new SelectedValue();
    protected char[] labelBuffer = new char[64];
    protected int labelOffset;
    protected int labelMargin;
    protected boolean isValueLabelBackgroundEnabled;
    protected boolean isValueLabelBackgroundAuto;
    protected float maxAngleVariation = DEFAULT_MAX_ANGLE_VARIATION;

    private ChartDataProvider dataProvider;

    private int checkPrecision;

    private float baseValue;

    private int touchToleranceMargin;
    private Paint linePaint = new Paint();
    private Paint pointPaint = new Paint();
    private Paint innerPointPaint = new Paint();

    private Viewport tempMaximumViewport = new Viewport();

    public LineChartRenderer(Context context, ILineChart chart, ChartDataProvider dataProvider) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        this.chart = chart;
        this.chartViewportHandler = chart.getChartViewportHandler();

        labelMargin = ChartUtils.dp2px(density, DEFAULT_LABEL_MARGIN_DP);
        labelOffset = labelMargin;

        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        labelPaint.setColor(Color.WHITE);

        labelBackgroundPaint.setAntiAlias(true);
        labelBackgroundPaint.setStyle(Paint.Style.FILL);
        this.dataProvider = dataProvider;

        touchToleranceMargin = ChartUtils.dp2px(density, DEFAULT_TOUCH_TOLERANCE_MARGIN_DP);

        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Cap.BUTT);
        linePaint.setDither(true);
        linePaint.setStrokeJoin(Paint.Join.BEVEL);
        linePaint.setStrokeWidth(ChartUtils.dp2px(density, DEFAULT_LINE_STROKE_WIDTH_DP));

        pointPaint.setAntiAlias(true);
        pointPaint.setStyle(Paint.Style.FILL);

        innerPointPaint.setAntiAlias(true);
        innerPointPaint.setStyle(Paint.Style.FILL);
        innerPointPaint.setColor(getColorFromAttr(context, R.attr.itemBackground));

        touchLinePaint.setAntiAlias(true);
        touchLinePaint.setStyle(Paint.Style.FILL);
        touchLinePaint.setColor(getColorFromAttr(context, R.attr.dividerColor));

        checkPrecision = ChartUtils.dp2px(density, 2);
    }

    public void setMaxAngleVariation(float maxAngleVariation) {
        this.maxAngleVariation = maxAngleVariation;
    }

    public void onChartSizeChanged() {
        final int internalMargin = calculateContentRectInternalMargin();
        chartViewportHandler.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);
    }

    public void onChartDataChanged() {
        final LineChartData data = chart.getChartData();

        Typeface typeface = chart.getChartData().getValueLabelTypeface();
        if (null != typeface) {
            labelPaint.setTypeface(typeface);
        }

        labelPaint.setColor(data.getValueLabelTextColor());
        labelPaint.setTextSize(ChartUtils.sp2px(scaledDensity, data.getValueLabelTextSize()));
        labelPaint.getFontMetricsInt(fontMetrics);

        this.isValueLabelBackgroundEnabled = data.isValueLabelBackgroundEnabled();
        this.isValueLabelBackgroundAuto = data.isValueLabelBackgroundAuto();
        this.labelBackgroundPaint.setColor(data.getValueLabelBackgroundColor());

        selectedValue.clear();

        final int internalMargin = calculateContentRectInternalMargin();
        chartViewportHandler.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);
        baseValue = dataProvider.getChartData().getBaseValue();

        onChartViewportChanged();
    }

    public void onChartViewportChanged() {
        if (isViewportCalculationEnabled) {
            calculateMaxViewport();
            chartViewportHandler.setMaxViewport(tempMaximumViewport);
            chartViewportHandler.setCurrentViewport(chartViewportHandler.getMaximumViewport());
        }
    }

    public void draw(Canvas canvas) {
        final LineChartData data = dataProvider.getChartData();


        for (Line line : data.getLines()) {
            if (line.isActive()) drawPath(canvas, line);
        }
    }

    public void drawUnclipped(Canvas canvas) {
        if (isTouched()) {
            Rect content = chartViewportHandler.getContentRectMinusAllMargins();
            canvas.drawLine(selectedValue.getTouchX(), content.top, selectedValue.getTouchX(), content.bottom, touchLinePaint);

            for (Line line : dataProvider.getChartData().getLines()) {
                if (!line.isActive()) continue;
                drawPoints(canvas, line);
            }
        }
    }

    private void drawPoints(Canvas canvas, Line line) {
        pointPaint.setColor(line.getPointColor());

        for (PointValue pointValue : selectedValue.getPoints()) {

            if (!line.getValues().contains(pointValue)) continue;

            final float rawX = chartViewportHandler.computeRawX(pointValue.getX());
            final float rawY = chartViewportHandler.computeRawY(pointValue.getY());

            highlightPoint(canvas, line, pointValue, rawX, rawY);
        }
    }

    public boolean checkTouch(float touchX) {
        selectedValue.clear();

        final LineChartData data = dataProvider.getChartData();
        for (Line line : data.getLines()) {
            if (!line.isActive()) continue;

            float minDistance = 100f;
            PointValue minPointDistanceValue = null;
            float minPointX = 0f;

            for (PointValue pointValue : line.getValues()) {
                float rawPointX = chartViewportHandler.computeRawX(pointValue.getX());

                float dist = Math.abs(touchX - rawPointX);
                if (dist <= minDistance) {
                    minDistance = dist;
                    minPointDistanceValue = pointValue;
                    minPointX = rawPointX;
                }
            }

            selectedValue.add(minPointDistanceValue);
            selectedValue.setTouchX(minPointX);
        }
        return isTouched();
    }

    private void calculateMaxViewport() {
        tempMaximumViewport.set(Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        LineChartData data = dataProvider.getChartData();

        for (Line line : data.getLines()) {
            if (!line.isActive()) continue;
            for (PointValue pointValue : line.getValues()) {
                if (pointValue.getX() < tempMaximumViewport.left) {
                    tempMaximumViewport.left = pointValue.getX();
                }
                if (pointValue.getX() > tempMaximumViewport.right) {
                    tempMaximumViewport.right = pointValue.getX();
                }
                if (pointValue.getY() < tempMaximumViewport.bottom) {
                    tempMaximumViewport.bottom = pointValue.getY();
                }
                if (pointValue.getY() > tempMaximumViewport.top) {
                    tempMaximumViewport.top = pointValue.getY();
                }
            }
        }
    }

    private int calculateContentRectInternalMargin() {
        int contentAreaMargin = 0;
        final LineChartData data = dataProvider.getChartData();
        for (Line line : data.getLines()) {
            if (!line.isActive()) continue;
            int margin = line.getPointRadius() + DEFAULT_TOUCH_TOLERANCE_MARGIN_DP;
            if (margin > contentAreaMargin) {
                contentAreaMargin = margin;
            }
        }
        return ChartUtils.dp2px(density, contentAreaMargin);
    }

    private void drawPath(Canvas canvas, final Line line) {
        prepareLinePaint(line);
        List<PointValue> optimizedList = chartViewportHandler.optimizeLine(line.getValues(), maxAngleVariation);
        float[] lines = new float[optimizedList.size() * 4];

        int valueIndex = 0;
        for (PointValue pointValue : optimizedList) {

            final float rawX = chartViewportHandler.computeRawX(pointValue.getX());
            final float rawY = chartViewportHandler.computeRawY(pointValue.getY());

            if (valueIndex == 0) {
                lines[valueIndex * 4] = rawX;
                lines[valueIndex * 4 + 1] = rawY;
            } else {
                lines[valueIndex * 4] =  lines[valueIndex * 4 - 2];
                lines[valueIndex * 4 + 1] =  lines[valueIndex * 4 - 1];
            }

            lines[valueIndex * 4 + 2] = rawX;
            lines[valueIndex * 4 + 3] = rawY;

            valueIndex++;
        }

        canvas.drawLines(lines, linePaint);
    }

    private void prepareLinePaint(final Line line) {
        linePaint.setStrokeWidth(ChartUtils.dp2px(density, line.getStrokeWidth()));
        linePaint.setColor(line.getColor());
    }

    private void drawPoint(Canvas canvas, float rawX, float rawY, float pointRadius) {
        canvas.drawCircle(rawX, rawY, pointRadius, pointPaint);
    }

    private void drawInnerPoint(Canvas canvas, float rawX, float rawY, float pointRadius) {
        canvas.drawCircle(rawX, rawY, pointRadius, innerPointPaint);
    }

    private void highlightPoint(Canvas canvas, Line line, PointValue pointValue, float rawX, float rawY) {
        int pointRadius = ChartUtils.dp2px(density, line.getPointRadius());
        pointPaint.setColor(line.getDarkenColor());
        drawPoint(canvas, rawX, rawY, pointRadius + touchToleranceMargin);
        drawInnerPoint(canvas, rawX, rawY, pointRadius * 0.3f + touchToleranceMargin);
        //drawLabel(canvas, line, pointValue, rawX, rawY, pointRadius + labelOffset);
    }

    private void drawLabel(Canvas canvas, Line line, PointValue pointValue, float rawX, float rawY, float offset) {
        final Rect contentRect = chartViewportHandler.getContentRectMinusAllMargins();
        final int numChars = line.getFormatter().formatChartValue(labelBuffer, pointValue);
        if (numChars == 0) {
            // No need to draw empty label
            return;
        }

        final float labelWidth = labelPaint.measureText(labelBuffer, labelBuffer.length - numChars, numChars);
        final int labelHeight = Math.abs(fontMetrics.ascent);
        float left = rawX - labelWidth / 2 - labelMargin;
        float right = rawX + labelWidth / 2 + labelMargin;

        float top;
        float bottom;

        if (pointValue.getY() >= baseValue) {
            top = rawY - offset - labelHeight - labelMargin * 2;
            bottom = rawY - offset;
        } else {
            top = rawY + offset;
            bottom = rawY + offset + labelHeight + labelMargin * 2;
        }

        if (top < contentRect.top) {
            top = rawY + offset;
            bottom = rawY + offset + labelHeight + labelMargin * 2;
        }
        if (bottom > contentRect.bottom) {
            top = rawY - offset - labelHeight - labelMargin * 2;
            bottom = rawY - offset;
        }
        if (left < contentRect.left) {
            left = rawX;
            right = rawX + labelWidth + labelMargin * 2;
        }
        if (right > contentRect.right) {
            left = rawX - labelWidth - labelMargin * 2;
            right = rawX;
        }

        labelBackgroundRect.set(left, top, right, bottom);
        drawLabelTextAndBackground(canvas, labelBuffer, labelBuffer.length - numChars, numChars, line.getDarkenColor());
    }

    public void resetRenderer() {
        this.chartViewportHandler = chart.getChartViewportHandler();
    }

    protected void drawLabelTextAndBackground(Canvas canvas, char[] labelBuffer, int startIndex, int numChars, int autoBackgroundColor) {
        final float textX;
        final float textY;

        if (isValueLabelBackgroundEnabled) {

            if (isValueLabelBackgroundAuto) {
                labelBackgroundPaint.setColor(autoBackgroundColor);
            }

            canvas.drawRect(labelBackgroundRect, labelBackgroundPaint);

            textX = labelBackgroundRect.left + labelMargin;
            textY = labelBackgroundRect.bottom - labelMargin;
        } else {
            textX = labelBackgroundRect.left;
            textY = labelBackgroundRect.bottom;
        }

        canvas.drawText(labelBuffer, startIndex, numChars, textX, textY, labelPaint);
    }

    public boolean isTouched() {
        return selectedValue.isSet();
    }

    public void clearTouch() {
        selectedValue.clear();
    }

    public Viewport getMaximumViewport() {
        return chartViewportHandler.getMaximumViewport();
    }

    public void setMaximumViewport(Viewport maxViewport) {
        if (null != maxViewport) {
            chartViewportHandler.setMaxViewport(maxViewport);
        }
    }

    public Viewport getCurrentViewport() {
        return chartViewportHandler.getCurrentViewport();
    }

    public void setCurrentViewport(Viewport viewport) {
        if (null != viewport) {
            chartViewportHandler.setCurrentViewport(viewport);
        }
    }

    public boolean isViewportCalculationEnabled() {
        return isViewportCalculationEnabled;
    }

    public void setViewportCalculationEnabled(boolean isEnabled) {
        this.isViewportCalculationEnabled = isEnabled;
    }

    public void selectValue(SelectedValue selectedValue) {
        this.selectedValue.set(selectedValue);
    }

    public SelectedValue getSelectedValue() {
        return selectedValue;
    }

}
