package com.opiumfive.telechart.chart.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;

import com.opiumfive.telechart.R;
import com.opiumfive.telechart.chart.Util;
import com.opiumfive.telechart.chart.ILineChart;
import com.opiumfive.telechart.chart.model.Line;
import com.opiumfive.telechart.chart.model.LineChartData;
import com.opiumfive.telechart.chart.model.PointValue;
import com.opiumfive.telechart.chart.model.SelectedValues;
import com.opiumfive.telechart.chart.model.Viewrect;
import com.opiumfive.telechart.chart.ChartDataProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.opiumfive.telechart.Settings.SELECTED_VALUES_DATE_FORMAT;
import static com.opiumfive.telechart.chart.Util.getColorFromAttr;
import static com.opiumfive.telechart.chart.Util.getDrawableFromAttr;


public class LineChartRenderer {

    private static final String DETAIL_TITLE_PATTERN = "00000000000";

    private static final int DEFAULT_LINE_STROKE_WIDTH_DP = 2;
    private static final int DEFAULT_TOUCH_TOLERANCE_MARGIN_DP = 3;
    private static final float DEFAULT_MAX_ANGLE_VARIATION = 2f;

    public int DEFAULT_LABEL_MARGIN_DP = 2;
    protected ILineChart chart;
    protected ChartViewrectHandler chartViewrectHandler;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(SELECTED_VALUES_DATE_FORMAT, Locale.ENGLISH);

    protected Paint labelPaint = new Paint();

    protected RectF labelBackgroundRect = new RectF();
    protected Paint touchLinePaint = new Paint();
    protected Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
    protected boolean isViewportCalculationEnabled = true;
    protected float density;
    protected float scaledDensity;
    protected SelectedValues selectedValues = new SelectedValues();
    protected int labelOffset;
    protected int labelMargin;
    protected float maxAngleVariation = DEFAULT_MAX_ANGLE_VARIATION;
    private int detailsTitleColor;
    private NinePatchDrawable shadowDrawable;

    private ChartDataProvider dataProvider;
    private float detailCornerRadius;

    private int touchToleranceMargin;
    private Paint linePaint = new Paint();
    private Paint pointPaint = new Paint();
    private Paint innerPointPaint = new Paint();

    private Viewrect tempMaximumViewrect = new Viewrect();

    public LineChartRenderer(Context context, ILineChart chart, ChartDataProvider dataProvider) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        this.chart = chart;
        this.chartViewrectHandler = chart.getChartViewrectHandler();

        labelMargin = Util.dp2px(density, DEFAULT_LABEL_MARGIN_DP);
        labelOffset = labelMargin;

        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        labelPaint.setColor(Color.WHITE);

        this.dataProvider = dataProvider;

        touchToleranceMargin = Util.dp2px(density, DEFAULT_TOUCH_TOLERANCE_MARGIN_DP);

        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Cap.BUTT);
        linePaint.setDither(true);
        linePaint.setStrokeJoin(Paint.Join.BEVEL);
        linePaint.setStrokeWidth(Util.dp2px(density, DEFAULT_LINE_STROKE_WIDTH_DP));

        detailCornerRadius = Util.dp2px(density, 5);

        pointPaint.setAntiAlias(true);
        pointPaint.setStyle(Paint.Style.FILL);

        innerPointPaint.setAntiAlias(true);
        innerPointPaint.setStyle(Paint.Style.FILL);
        innerPointPaint.setColor(getColorFromAttr(context, R.attr.itemBackground));

        touchLinePaint.setAntiAlias(true);
        touchLinePaint.setStyle(Paint.Style.FILL);
        touchLinePaint.setColor(getColorFromAttr(context, R.attr.dividerColor));
        touchLinePaint.setStrokeWidth(Util.dp2px(density, DEFAULT_LINE_STROKE_WIDTH_DP) / 2f);

        detailsTitleColor = getColorFromAttr(context, R.attr.detailTitleColor);

        shadowDrawable = (NinePatchDrawable) getDrawableFromAttr(context, R.attr.detailBackground);
    }

    public void setMaxAngleVariation(float maxAngleVariation) {
        this.maxAngleVariation = maxAngleVariation;
    }

    public void onChartSizeChanged() {
        final int internalMargin = calculateContentRectInternalMargin();
        chartViewrectHandler.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);
    }

    public void onChartDataChanged() {
        final LineChartData data = chart.getChartData();

        Typeface typeface = chart.getChartData().getValueLabelTypeface();
        if (null != typeface) {
            labelPaint.setTypeface(typeface);
        }

        labelPaint.setColor(data.getValueLabelTextColor());
        labelPaint.setTextSize(Util.sp2px(scaledDensity, data.getValueLabelTextSize()));
        labelPaint.getFontMetricsInt(fontMetrics);

        selectedValues.clear();

        final int internalMargin = calculateContentRectInternalMargin();
        chartViewrectHandler.insetContentRectByInternalMargins(internalMargin, internalMargin, internalMargin, internalMargin);

        onChartViewportChanged();
    }

    public void onChartViewportChanged() {
        if (isViewportCalculationEnabled) {
            calculateMaxViewport();
            chartViewrectHandler.setMaxViewrect(tempMaximumViewrect);
            chartViewrectHandler.setCurrentViewrect(chartViewrectHandler.getMaximumViewport());
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
            Rect content = chartViewrectHandler.getContentRectMinusAllMargins();
            canvas.drawLine(selectedValues.getTouchX(), content.top, selectedValues.getTouchX(), content.bottom, touchLinePaint);

            for (PointValue pointValue : selectedValues.getPoints()) {

                final float rawX = chartViewrectHandler.computeRawX(pointValue.getX());
                final float rawY = chartViewrectHandler.computeRawY(pointValue.getY());

                highlightPoint(canvas, pointValue, rawX, rawY);
            }

            drawLabel(canvas);
        }
    }

    public boolean checkTouch(float touchX) {
        selectedValues.clear();

        final LineChartData data = dataProvider.getChartData();
        for (Line line : data.getLines()) {
            if (!line.isActive()) continue;

            float minDistance = 100f;
            PointValue minPointDistanceValue = null;
            float minPointX = 0f;

            for (PointValue pointValue : line.getValues()) {
                float rawPointX = chartViewrectHandler.computeRawX(pointValue.getX());

                if (rawPointX < chartViewrectHandler.getContentRectMinusAllMargins().left ||
                    rawPointX > chartViewrectHandler.getContentRectMinusAllMargins().right) continue;

                float dist = Math.abs(touchX - rawPointX);
                if (dist <= minDistance) {
                    minDistance = dist;
                    minPointDistanceValue = pointValue;
                    minPointX = rawPointX;
                }
            }

            if (minPointDistanceValue != null) {
                minPointDistanceValue.setPointRadius(line.getPointRadius());
                minPointDistanceValue.setColor(line.getColor());
                minPointDistanceValue.setLine(line.getTitle());
                selectedValues.add(minPointDistanceValue);
                selectedValues.setTouchX(minPointX);
            }
        }

        return isTouched();
    }

    private void calculateMaxViewport() {
        tempMaximumViewrect.set(Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
        LineChartData data = dataProvider.getChartData();

        for (Line line : data.getLines()) {
            if (!line.isActive()) continue;
            for (PointValue pointValue : line.getValues()) {
                if (pointValue.getX() < tempMaximumViewrect.left) {
                    tempMaximumViewrect.left = pointValue.getX();
                }
                if (pointValue.getX() > tempMaximumViewrect.right) {
                    tempMaximumViewrect.right = pointValue.getX();
                }
                if (pointValue.getY() < tempMaximumViewrect.bottom) {
                    tempMaximumViewrect.bottom = pointValue.getY();
                }
                if (pointValue.getY() > tempMaximumViewrect.top) {
                    tempMaximumViewrect.top = pointValue.getY();
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
        return Util.dp2px(density, contentAreaMargin);
    }

    private void drawPath(Canvas canvas, final Line line) {
        prepareLinePaint(line);
        List<PointValue> optimizedList = chartViewrectHandler.optimizeLine(line.getValues(), maxAngleVariation);
        float[] lines = new float[optimizedList.size() * 4];

        int valueIndex = 0;
        for (PointValue pointValue : optimizedList) {

            final float rawX = chartViewrectHandler.computeRawX(pointValue.getX());
            final float rawY = chartViewrectHandler.computeRawY(pointValue.getY());

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
        linePaint.setStrokeWidth(Util.dp2px(density, line.getStrokeWidth()));
        linePaint.setColor(line.getColor());
    }

    private void drawPoint(Canvas canvas, float rawX, float rawY, float pointRadius) {
        canvas.drawCircle(rawX, rawY, pointRadius, pointPaint);
    }

    private void drawInnerPoint(Canvas canvas, float rawX, float rawY, float pointRadius) {
        canvas.drawCircle(rawX, rawY, pointRadius, innerPointPaint);
    }

    private void highlightPoint(Canvas canvas, PointValue pointValue, float rawX, float rawY) {
        int pointRadius = Util.dp2px(density, pointValue.getPointRadius());
        pointPaint.setColor(pointValue.getColor());
        drawPoint(canvas, rawX, rawY, pointRadius + touchToleranceMargin);
        drawInnerPoint(canvas, rawX, rawY, touchToleranceMargin);
    }

    private void drawLabel(Canvas canvas) {
        float rawX = selectedValues.getTouchX();
        Rect contentRect = chartViewrectHandler.getContentRectMinusAllMargins();

        float titleWidth = labelPaint.measureText(DETAIL_TITLE_PATTERN.toCharArray(), 0, DETAIL_TITLE_PATTERN.length());

        float maxValueWidth = 0f;

        for (PointValue pointValue : selectedValues.getPoints()) {
            labelPaint.setColor(pointValue.getColor());
            String valueText = String.valueOf((long) pointValue.getY());
            String nameText = pointValue.getLine();
            float valueWidth = Math.max(labelPaint.measureText(valueText.toCharArray(), 0, valueText.length()), labelPaint.measureText(nameText.toCharArray(), 0, nameText.length()));
            if (valueWidth > maxValueWidth) {
                maxValueWidth = valueWidth;
            }
        }

        float calculatedWidth = maxValueWidth * selectedValues.getPoints().size() + labelMargin * 4 * (selectedValues.getPoints().size() - 1);
        float contentWidth = Math.max(calculatedWidth, titleWidth);

        int oneLineHeight = Math.abs(fontMetrics.ascent);
        int lines = 5;
        float left = rawX - contentWidth / 2 - labelMargin * 3;
        float right = rawX + contentWidth / 2 + labelMargin * 3;

        if (left < contentRect.left) {
            float diff = contentRect.left - left;
            left = contentRect.left;
            right += diff;
        } else if (right > contentRect.right) {
            float diff = right - contentRect.right;
            right = contentRect.right;
            left -= diff;
        }

        labelBackgroundRect.set(left, 0, right, oneLineHeight * lines);

        Rect shadowBackgroundRect = new Rect((int)(labelBackgroundRect.left - detailCornerRadius / 5), (int)(labelBackgroundRect.top - detailCornerRadius/ 5),
                (int)(labelBackgroundRect.right + detailCornerRadius/ 5), (int)(labelBackgroundRect.bottom + detailCornerRadius/ 5));

        shadowDrawable.setBounds(shadowBackgroundRect);
        shadowDrawable.draw(canvas);

        float textX = labelBackgroundRect.left + labelMargin * 5;
        float textY = labelBackgroundRect.top + oneLineHeight + labelMargin * 3;

        String text = dateFormat.format(new Date((long) selectedValues.getPoints().get(0).getX()));
        labelPaint.setColor(detailsTitleColor);
        canvas.drawText(text.toCharArray(), 0, text.length(), textX, textY, labelPaint);

        textY += oneLineHeight + labelMargin * 4;
        float textYName = textY + oneLineHeight + labelMargin;

        for (PointValue pointValue : selectedValues.getPoints()) {
            labelPaint.setColor(pointValue.getColor());
            String valueText = String.valueOf((long) pointValue.getY());
            String nameText = pointValue.getLine();

            canvas.drawText(valueText.toCharArray(), 0, valueText.length(), textX, textY, labelPaint);
            canvas.drawText(nameText.toCharArray(), 0, nameText.length(), textX, textYName, labelPaint);

            textX += maxValueWidth + labelMargin * 4;
        }
    }

    public void resetRenderer() {
        this.chartViewrectHandler = chart.getChartViewrectHandler();
    }

    public boolean isTouched() {
        return selectedValues.isSet();
    }

    public void clearTouch() {
        selectedValues.clear();
    }

    public Viewrect getMaximumViewrect() {
        return chartViewrectHandler.getMaximumViewport();
    }

    public void setMaximumViewrect(Viewrect maxViewrect) {
        if (null != maxViewrect) {
            chartViewrectHandler.setMaxViewrect(maxViewrect);
        }
    }

    public Viewrect getCurrentViewrect() {
        return chartViewrectHandler.getCurrentViewrect();
    }

    public void setCurrentViewrect(Viewrect viewrect) {
        if (null != viewrect) {
            chartViewrectHandler.setCurrentViewrect(viewrect);
        }
    }

    public boolean isViewrectCalculationEnabled() {
        return isViewportCalculationEnabled;
    }

    public void setViewrectCalculationEnabled(boolean isEnabled) {
        this.isViewportCalculationEnabled = isEnabled;
    }

    public void selectValue(SelectedValues selectedValues) {
        this.selectedValues.set(selectedValues);
    }

    public SelectedValues getSelectedValues() {
        return selectedValues;
    }

}
