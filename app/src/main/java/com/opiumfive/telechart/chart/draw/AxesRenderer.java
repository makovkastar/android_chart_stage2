package com.opiumfive.telechart.chart.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;

import com.opiumfive.telechart.chart.CType;
import com.opiumfive.telechart.chart.Util;
import com.opiumfive.telechart.chart.IChart;
import com.opiumfive.telechart.chart.model.Axis;

import com.opiumfive.telechart.chart.model.Viewrect;
import com.opiumfive.telechart.chart.model.AxisValues;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AxesRenderer {

    private static final int DEFAULT_AXIS_MARGIN_DP = 2;
    private static final int LEFT = 0;
    private static final int BOTTOM = 1;
    private static final int RIGHT = 2;
    private static final int LABEL_ANIM_STEPS = 15;

    // for text measure
    private static final char[] labelWidthChars = new char[]{
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'};

    private IChart chart;
    private ChartViewrectHandler chartViewrectHandler;

    private int axisMargin;
    private float density;
    private float scaledDensity;
    private Paint[] labelPaintTab = new Paint[] {new Paint(), new Paint(), new Paint()};
    private Paint[] linePaintTab = new Paint[] {new Paint(), new Paint(), new Paint()};
    private float[] labelBaselineTab = new float[3];
    private int[] labelWidthTab = new int[3];
    private int[] labelTextAscentTab = new int[3];
    private int[] labelTextDescentTab = new int[3];
    private int[] labelDimensionForMarginsTab = new int[3];
    private int[] labelDimensionForStepsTab = new int[3];
    private FontMetricsInt[] fontMetricsTab = new FontMetricsInt[]{new FontMetricsInt(), new FontMetricsInt(), new FontMetricsInt()};
    private char[] labelBuffer = new char[64];
    private int[] valuesToDrawNumTab = new int[3];
    private float[][] rawValuesTab = new float[3][0];
    private float[][] autoValuesToDrawTab = new float[3][0];
    private List<Label> additionalAutoValuesAxisX = new ArrayList<>();
    private float[][] linesDrawBufferTab = new float[3][0];
    private AxisValues[] autoValuesBufferTab = new AxisValues[]{new AxisValues(), new AxisValues(), new AxisValues()};

    private AxisValues autoValuesYBuff = new AxisValues();
    private AxisValues autoValuesY2Buff = new AxisValues();
    private AxisValues targetValuesYBuff = new AxisValues();
    private AxisValues targetValuesY2Buff = new AxisValues();
    private AxisValues currentValuesYBuff = new AxisValues();
    private AxisValues currentValuesY2Buff = new AxisValues();
    private float[] linesDrawBufferTabY = new float[] {};
    private float[] rawValuesTabY = new float[] {};
    private float[] autoValuesToDrawTabY = new float[] {};

    private boolean needUpdateYBuffer = true;
    private boolean initialLabelAddition = true;
    private boolean isCurrentlyAnimatingLabels = false;
    private float animDirection = 1f;
    private float animStep = 1f;
    private boolean toggleAnimation = false;
    int framesAnim = 0;
    private Viewrect targetViewrect = null;
    private Viewrect currentLabelViewrect = null;


    public void setToggleAnimation(Viewrect targetViewrect) {
        toggleAnimation = true;
        this.targetViewrect = targetViewrect;
        framesAnim = 0;
    }

    public boolean isCurrentlyAnimatingLabels() {
        return isCurrentlyAnimatingLabels;
    }

    public AxesRenderer(Context context, IChart chart) {
        this.chart = chart;
        chartViewrectHandler = chart.getChartViewrectHandler();
        density = context.getResources().getDisplayMetrics().density;
        scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        axisMargin = Util.dp2px(density, DEFAULT_AXIS_MARGIN_DP);

        for (int position = 0; position < 3; ++position) {
            labelPaintTab[position].setStyle(Paint.Style.FILL);
            labelPaintTab[position].setAntiAlias(true);
            linePaintTab[position].setStyle(Paint.Style.STROKE);
            linePaintTab[position].setStrokeWidth(Util.dp2px(density, 1));
            linePaintTab[position].setAntiAlias(true);
        }
    }

    public void onChartSizeChanged() {
        onChartDataOrSizeChanged();
    }

    public void onChartDataChanged() {
        onChartDataOrSizeChanged();
    }

    private void onChartDataOrSizeChanged() {
        initAxis(chart.getChartData().getAxisYLeft(), LEFT);
        initAxis(chart.getChartData().getAxisXBottom(), BOTTOM);
        initAxis(chart.getChartData().getAxisYRight(), RIGHT);
    }

    public void resetRenderer() {
        this.chartViewrectHandler = chart.getChartViewrectHandler();
    }

    private void initAxis(Axis axis, int position) {
        if (null == axis) {
            return;
        }
        initAxisAttributes(axis, position);
        initAxisMargin(axis, position);
        initAxisMeasurements(axis, position);
    }

    private void initAxisAttributes(Axis axis, int position) {
        initAxisPaints(axis, position);
        initAxisTextAlignment( position);
        initAxisDimension(position);
    }

    private void initAxisPaints(Axis axis, int position) {
        labelPaintTab[position].setColor(axis.getTextColor());
        labelPaintTab[position].setTextSize(Util.sp2px(scaledDensity, axis.getTextSize()));
        labelPaintTab[position].getFontMetricsInt(fontMetricsTab[position]);

        linePaintTab[position].setColor(axis.getLineColor());

        labelTextAscentTab[position] = Math.abs(fontMetricsTab[position].ascent);
        labelTextDescentTab[position] = Math.abs(fontMetricsTab[position].descent);
        labelWidthTab[position] = (int) labelPaintTab[position].measureText(labelWidthChars, 0, Axis.DEFAULT_MAX_AXIS_LABEL_CHARS);
    }

    private void initAxisTextAlignment(int position) {
        if (BOTTOM == position) {
            labelPaintTab[position].setTextAlign(Align.CENTER);
        } else if (LEFT == position) {
            labelPaintTab[position].setTextAlign(Align.LEFT);
        } else if (RIGHT == position) {
            labelPaintTab[position].setTextAlign(Align.RIGHT);
        }
    }

    private void initAxisDimension(int position) {
        if (LEFT == position) {
            labelDimensionForMarginsTab[position] = labelWidthTab[position];
            labelDimensionForStepsTab[position] = labelTextAscentTab[position];
        } else if (BOTTOM == position) {
            labelDimensionForMarginsTab[position] = labelTextAscentTab[position] + labelTextDescentTab[position];
            labelDimensionForStepsTab[position] = labelWidthTab[position];
        } else if (RIGHT == position) {
            labelDimensionForMarginsTab[position] = labelWidthTab[position];
            labelDimensionForStepsTab[position] = labelTextAscentTab[position];
        }
    }

    private void initAxisMargin(Axis axis, int position) {
        int margin = 0;
        if (!axis.isInside()) {
            margin += axisMargin + labelDimensionForMarginsTab[position];
        }
        insetContentRectWithAxesMargins(margin, position);
    }

    private void insetContentRectWithAxesMargins(int axisMargin, int position) {
        if (LEFT == position) {
            chart.getChartViewrectHandler().insetContentRect(axisMargin, 0, 0, 0);
        } else if (BOTTOM == position) {
            chart.getChartViewrectHandler().insetContentRect(0, 0, 0, axisMargin);
        } else if (RIGHT == position) {
            chart.getChartViewrectHandler().insetContentRect(0, 0, axisMargin, 0);
        }
    }

    private void initAxisMeasurements(Axis axis, int position) {
        if (LEFT == position) {
            labelBaselineTab[position] = chartViewrectHandler.getContentRectMinusAllMargins().left + axisMargin;
        } else if (BOTTOM == position) {
            labelBaselineTab[position] = chartViewrectHandler.getContentRectMinusAxesMargins().bottom + axisMargin / 2 + labelTextAscentTab[position];
        } else if (RIGHT == position) {
            labelBaselineTab[position] = chartViewrectHandler.getContentRectMinusAllMargins().right - axisMargin;
        }
    }

    public void drawInBackground(Canvas canvas) {
        Axis axis = chart.getChartData().getAxisYLeft();
        if (null != axis) {
            prepareAxisToDraw(axis, LEFT);
            drawAxisLines(canvas, axis, LEFT);
        }

        axis = chart.getChartData().getAxisXBottom();
        if (null != axis) {
            prepareAxisToDraw(axis, BOTTOM);
            drawAxisLines(canvas, axis, BOTTOM);
        }
    }

    public static float calcYAnimOffsetFactor(int step, int lineNum, float factor) {
        return (lineNum + 1) * factor * step * (step + 1);
    }

    public Viewrect getCurrentLabelViewrect() {
        return currentLabelViewrect;
    }

    private void prepareAxisToDraw(Axis axis, int position) {
        final Viewrect visibleViewrect = chartViewrectHandler.getVisibleViewport();
        final Rect contentRect = chartViewrectHandler.getContentRectMinusAllMargins();
        boolean isAxisVertical = isAxisVertical(position);
        float start, stop;
        int contentRectDimension;

        if (isAxisVertical) {
            start = visibleViewrect.bottom;
            stop = visibleViewrect.top;
            contentRectDimension = contentRect.height();
        } else {
            start = visibleViewrect.left;
            stop = visibleViewrect.right;
            contentRectDimension = contentRect.width();
        }

        int dim = labelDimensionForStepsTab[position] * 4;
        if (dim == 0) dim = 1;
        int steps = Math.abs(contentRectDimension) / dim;

        if (position == BOTTOM) {
            Util.generatedAxisValues(start, stop, steps, autoValuesBufferTab[position]);
        } else if (position == LEFT) {
            if (toggleAnimation) {
                framesAnim++;
                currentLabelViewrect = new Viewrect(chartViewrectHandler.getCurrentViewrect());
                if (needUpdateYBuffer) {

                    float targetStart = targetViewrect.bottom;
                    float targetStop = targetViewrect.top;
                    Util.generatedVerticalAxisValues(targetStart, targetStop, 6, targetValuesYBuff);
                    autoValuesYBuff = new AxisValues(targetValuesYBuff);
                    autoValuesYBuff.step = 0;
                    needUpdateYBuffer = false;

                    if (chart.getType().equals(CType.LINE_2Y)) {
                        float scale = chart.getChartData().getLines().get(1).getScale();
                        float offset = chart.getChartData().getLines().get(1).getOffset();
                        float min = chart.getChartData().getLines().get(1).getMinY();
                        targetStart = (targetViewrect.bottom - offset) / scale + min;
                        targetStop = (targetViewrect.top - offset) / scale + min;
                        Util.generatedVerticalAxisValues(targetStart, targetStop, 6, targetValuesY2Buff);
                        autoValuesY2Buff = new AxisValues(targetValuesY2Buff);
                        autoValuesY2Buff.step = 0;
                    }
                }

                if (!targetValuesYBuff.equals(currentValuesYBuff) || isCurrentlyAnimatingLabels) {
                    if (currentValuesYBuff.valuesNumber == 0) {
                        currentValuesYBuff = new AxisValues(autoValuesYBuff);
                        autoValuesBufferTab[position] = new AxisValues(currentValuesYBuff);

                        if (chart.getType().equals(CType.LINE_2Y)) {
                            currentValuesY2Buff = new AxisValues(autoValuesY2Buff);
                        }

                        needUpdateYBuffer = true;
                    } else {

                        if (autoValuesBufferTab[position].step == 0) {
                            autoValuesYBuff = new AxisValues(targetValuesYBuff);
                            autoValuesBufferTab[position] = new AxisValues(currentValuesYBuff);

                            if (chart.getType().equals(CType.LINE_2Y)) {
                                autoValuesY2Buff = new AxisValues(targetValuesY2Buff);
                            }

                            float targetDiff = targetValuesYBuff.values[targetValuesYBuff.values.length - 1] - targetValuesYBuff.values[0];

                            animStep = Math.abs(targetDiff / 150f / LABEL_ANIM_STEPS);

                            float targetTop = targetValuesYBuff.values[targetValuesYBuff.values.length - 1];
                            float targetBottom = targetValuesYBuff.values[0];
                            float currentTop = currentValuesYBuff.values[currentValuesYBuff.values.length - 1];
                            float currentBottom = currentValuesYBuff.values[0];

                            if (currentTop >= targetTop && currentBottom <= targetBottom || currentTop >= targetTop && currentBottom >= targetBottom) {
                                animDirection = -1f;
                            } else {
                                animDirection = 1f;
                            }

                            for (int i = 0; i < autoValuesYBuff.values.length; i++) {
                                autoValuesYBuff.values[i] -= calcYAnimOffsetFactor(LABEL_ANIM_STEPS, i, animStep) * animDirection;
                            }
                            autoValuesYBuff.alpha = 0f;

                            if (chart.getType().equals(CType.LINE_2Y)) {
                                autoValuesY2Buff.alpha = 0f;
                            }

                            autoValuesBufferTab[position].alpha = 1f;
                            isCurrentlyAnimatingLabels = true;
                        }

                        // start animating
                        if (autoValuesBufferTab[position].step >= LABEL_ANIM_STEPS - 1) {
                            currentValuesYBuff = new AxisValues(targetValuesYBuff);
                            autoValuesBufferTab[position] = new AxisValues(targetValuesYBuff);

                            if (chart.getType().equals(CType.LINE_2Y)) {
                                currentValuesY2Buff = new AxisValues(targetValuesY2Buff);
                            }

                            needUpdateYBuffer = true;
                            isCurrentlyAnimatingLabels = false;
                            toggleAnimation = false;
                        } else {
                            autoValuesBufferTab[position].step++;
                            autoValuesYBuff.step++;

                            autoValuesBufferTab[position].alpha -= 1.0f / LABEL_ANIM_STEPS* 1.5f;
                            autoValuesYBuff.alpha += 1.0f / LABEL_ANIM_STEPS * 1.5f;

                            if (autoValuesBufferTab[position].alpha < 0f) autoValuesBufferTab[position].alpha = 0f;
                            if (autoValuesYBuff.alpha > 1f) autoValuesYBuff.alpha = 1f;

                            if (chart.getType().equals(CType.LINE_2Y)) {
                                autoValuesY2Buff.step++;

                                autoValuesY2Buff.alpha += 1.0f / LABEL_ANIM_STEPS;
                                if (autoValuesY2Buff.alpha > 1f) autoValuesY2Buff.alpha = 1f;
                            }
                        }
                    }
                } else {
                    needUpdateYBuffer = true;
                }
            } else {
                if (chart.getType().equals(CType.LINE_2Y)) {

                    float scale = chart.getChartData().getLines().get(1).getScale();
                    float offset = chart.getChartData().getLines().get(1).getOffset();
                    float min = chart.getChartData().getLines().get(1).getMinY();
                    float targetStart = (start - offset) / scale + min;
                    float targetStop = (stop - offset) / scale + min;

                    Util.generatedVerticalAxisValues(targetStart, targetStop, 6, targetValuesY2Buff);

                    autoValuesY2Buff = new AxisValues(targetValuesY2Buff);
                    if (initialLabelAddition) {
                        currentValuesY2Buff = new AxisValues(autoValuesY2Buff);
                    }
                }

                Util.generatedVerticalAxisValues(start, stop, 6, targetValuesYBuff);

                autoValuesYBuff = new AxisValues(targetValuesYBuff);
                if (initialLabelAddition) {
                    currentValuesYBuff = new AxisValues(autoValuesYBuff);
                    currentLabelViewrect = new Viewrect(chartViewrectHandler.getCurrentViewrect());
                }
                autoValuesBufferTab[position] = new AxisValues(targetValuesYBuff);
            }
        }

        if (axis.hasLines() && (linesDrawBufferTab[position].length < autoValuesBufferTab[position].valuesNumber * 4)) {
            linesDrawBufferTab[position] = new float[autoValuesBufferTab[position].valuesNumber * 4];
            linesDrawBufferTabY = new float[autoValuesYBuff.valuesNumber * 4];
        }

        if (rawValuesTab[position].length < autoValuesBufferTab[position].valuesNumber) {
            rawValuesTab[position] = new float[autoValuesBufferTab[position].valuesNumber];
            rawValuesTabY = new float[autoValuesYBuff.valuesNumber];
        }
        if (autoValuesToDrawTab[position].length < autoValuesBufferTab[position].valuesNumber) {
            autoValuesToDrawTab[position] = new float[autoValuesBufferTab[position].valuesNumber];
            autoValuesToDrawTabY = new float[autoValuesYBuff.valuesNumber];
        }

        float rawValue;
        int valueToDrawIndex = 0;
        for (int i = 0; i < autoValuesBufferTab[position].valuesNumber; ++i) {
            if (isAxisVertical) {
                rawValue = chartViewrectHandler.computeRawYByZone(i, autoValuesBufferTab[position].step, animDirection);
            } else {
                rawValue = chartViewrectHandler.computeRawX(autoValuesBufferTab[position].values[i]);
            }

            rawValuesTab[position][valueToDrawIndex] = rawValue;
            autoValuesToDrawTab[position][valueToDrawIndex] = autoValuesBufferTab[position].values[i];

            if (position == LEFT && (isCurrentlyAnimatingLabels || initialLabelAddition)) {
                if (isCurrentlyAnimatingLabels) {
                    if (animDirection < 0) {
                        rawValuesTabY[valueToDrawIndex] = chartViewrectHandler.computeRawYByZone(i, autoValuesYBuff.step, animDirection);
                    } else {
                        rawValuesTabY[valueToDrawIndex] = chartViewrectHandler.computeRawYByZone(i, LABEL_ANIM_STEPS - autoValuesYBuff.step, -1f);
                    }

                } else {
                    rawValuesTabY[valueToDrawIndex] = chartViewrectHandler.computeRawYByZone(i, autoValuesYBuff.step, animDirection);
                }
                autoValuesToDrawTabY[valueToDrawIndex] = autoValuesYBuff.values[i];
            }

            if (position == BOTTOM) {
                Label label = new Label(autoValuesToDrawTab[position][valueToDrawIndex]);
                if (!additionalAutoValuesAxisX.contains(label)) {
                    label.alpha = initialLabelAddition ? 1.0f : 0.0f;
                    additionalAutoValuesAxisX.add(label);
                }
            }

            ++valueToDrawIndex;
        }

        if (position == BOTTOM) {

            initialLabelAddition = false;

            Iterator<Label> iterator = additionalAutoValuesAxisX.iterator();

            while(iterator.hasNext()) {
                Label label = iterator.next();

                boolean existsInCurrentLabelList = false;
                for (int i = 0; i < valueToDrawIndex; i++) {
                    if (autoValuesToDrawTab[position][i] == label.value) {
                        existsInCurrentLabelList = true;
                        break;
                    }
                }

                if (existsInCurrentLabelList) {
                    if (label.alpha < 1f) {
                        label.alpha += 0.1f;
                    }
                } else {
                    if (label.alpha > 0f) {
                        label.alpha -= 0.1f;
                    }
                }

                if (label.alpha > 0f) {
                    label.raw = chartViewrectHandler.computeRawX(label.value);
                } else {
                    iterator.remove();
                }
            }
            valuesToDrawNumTab[position] = additionalAutoValuesAxisX.size();
        } else {
            valuesToDrawNumTab[position] = valueToDrawIndex;
        }
    }

    public void drawInForeground(Canvas canvas) {
        Axis axis = chart.getChartData().getAxisYLeft();
        if (null != axis) {
            drawAxisLabels(canvas, axis, LEFT);
        }

        axis = chart.getChartData().getAxisXBottom();
        if (null != axis) {
            drawAxisLabels(canvas, axis, BOTTOM);
        }

        axis = chart.getChartData().getAxisYRight();
        if (null != axis) {
            drawAxisLabels(canvas, axis, RIGHT);
        }
    }

    private void drawAxisLines(Canvas canvas, Axis axis, int position) {
        final Rect contentRectMargins = chartViewrectHandler.getContentRectMinusAxesMargins();
        float lineX1, lineY1, lineX2, lineY2;
        lineX1 = lineY1 = lineX2 = lineY2 = 0;
        boolean isAxisVertical = isAxisVertical(position);

        if (LEFT == position) {
            lineX1 = axisMargin + contentRectMargins.left;
            lineX2 = contentRectMargins.right;
        } else if (BOTTOM == position) {
            lineY1 = contentRectMargins.top;
            lineY2 = contentRectMargins.bottom;
        }

        if (axis.hasLines()) {

            linePaintTab[position].setAlpha(255 / 10);

            int valueToDrawIndex = 0;
            linePaintTab[position].setAlpha((int)(255 / 10f * autoValuesBufferTab[position].alpha));

            for (; valueToDrawIndex < valuesToDrawNumTab[position]; ++valueToDrawIndex) {
                if (isAxisVertical) {
                    lineY1 = lineY2 = rawValuesTab[position][valueToDrawIndex];
                } else {
                    lineX1 = lineX2 = rawValuesTab[position][valueToDrawIndex];
                }
                linesDrawBufferTab[position][valueToDrawIndex * 4] = lineX1;
                linesDrawBufferTab[position][valueToDrawIndex * 4 + 1] = lineY1;
                linesDrawBufferTab[position][valueToDrawIndex * 4 + 2] = lineX2;
                linesDrawBufferTab[position][valueToDrawIndex * 4 + 3] = lineY2;
            }
            canvas.drawLines(linesDrawBufferTab[position], 0, valueToDrawIndex * 4, linePaintTab[position]);

            if (isCurrentlyAnimatingLabels) {
                linePaintTab[position].setAlpha((int) (255 / 10f * autoValuesYBuff.alpha));
                valueToDrawIndex = 0;
                for (; valueToDrawIndex < valuesToDrawNumTab[position]; ++valueToDrawIndex) {
                    if (isAxisVertical) {
                        lineY1 = lineY2 = rawValuesTabY[valueToDrawIndex];
                    } else {
                        lineX1 = lineX2 = rawValuesTabY[valueToDrawIndex];
                    }
                    linesDrawBufferTabY[valueToDrawIndex * 4] = lineX1;
                    linesDrawBufferTabY[valueToDrawIndex * 4 + 1] = lineY1;
                    linesDrawBufferTabY[valueToDrawIndex * 4 + 2] = lineX2;
                    linesDrawBufferTabY[valueToDrawIndex * 4 + 3] = lineY2;
                }
                canvas.drawLines(linesDrawBufferTabY, 0, valueToDrawIndex * 4, linePaintTab[position]);
            }
        }
    }

    private void drawAxisLabels(Canvas canvas, Axis axis, int position) {
        float labelX, labelY;
        if (LEFT == position) {
            labelX = labelBaselineTab[position];

            labelPaintTab[position].setAlpha((int)(255 * autoValuesBufferTab[position].alpha));

            for (int valueToDrawIndex = 0; valueToDrawIndex < valuesToDrawNumTab[position]; ++valueToDrawIndex) {
                int charsNumber = 0;

                final float value = currentValuesYBuff.values[valueToDrawIndex];
                charsNumber = axis.getFormatter().formatValue(labelBuffer, value);

                labelY = rawValuesTab[position][valueToDrawIndex] - Util.dp2px(density, 4);

                canvas.drawText(labelBuffer, labelBuffer.length - charsNumber, charsNumber, labelX, labelY, labelPaintTab[position]);
            }

            if (isCurrentlyAnimatingLabels) {
                labelPaintTab[position].setAlpha((int) (255 * autoValuesYBuff.alpha));

                for (int valueToDrawIndex = 0; valueToDrawIndex < valuesToDrawNumTab[position]; ++valueToDrawIndex) {
                    int charsNumber = 0;

                    final float value = targetValuesYBuff.values[valueToDrawIndex];
                    charsNumber = axis.getFormatter().formatValue(labelBuffer, value);

                    labelY = rawValuesTabY[valueToDrawIndex] - Util.dp2px(density, 4);

                    canvas.drawText(labelBuffer, labelBuffer.length - charsNumber, charsNumber, labelX, labelY, labelPaintTab[position]);
                }
            }
        } else if (BOTTOM == position) {
            labelY = labelBaselineTab[position];

            for (Label label : additionalAutoValuesAxisX) {
                labelPaintTab[position].setAlpha((int)(255 * label.alpha));
                int charsNumber = 0;

                final float value = label.value;
                charsNumber = axis.getFormatter().formatValue(labelBuffer, value);

                labelX = label.raw;

                canvas.drawText(labelBuffer, labelBuffer.length - charsNumber, charsNumber, labelX, labelY, labelPaintTab[position]);
            }
        } else if (RIGHT == position) {
            labelX = labelBaselineTab[position];

            labelPaintTab[position].setAlpha((int)(255 * autoValuesBufferTab[LEFT].alpha));

            for (int valueToDrawIndex = 0; valueToDrawIndex < valuesToDrawNumTab[LEFT]; ++valueToDrawIndex) {
                int charsNumber = 0;

                final float value = currentValuesY2Buff.values[valueToDrawIndex];
                charsNumber = axis.getFormatter().formatValue(labelBuffer, value);

                labelY = rawValuesTab[LEFT][valueToDrawIndex] - Util.dp2px(density, 4);

                canvas.drawText(labelBuffer, labelBuffer.length - charsNumber, charsNumber, labelX, labelY, labelPaintTab[position]);
            }

            if (isCurrentlyAnimatingLabels) {
                labelPaintTab[position].setAlpha((int) (255 * autoValuesYBuff.alpha));

                for (int valueToDrawIndex = 0; valueToDrawIndex < valuesToDrawNumTab[LEFT]; ++valueToDrawIndex) {
                    int charsNumber = 0;

                    final float value = targetValuesY2Buff.values[valueToDrawIndex];
                    charsNumber = axis.getFormatter().formatValue(labelBuffer, value);

                    labelY = rawValuesTabY[valueToDrawIndex] - Util.dp2px(density, 4);

                    canvas.drawText(labelBuffer, labelBuffer.length - charsNumber, charsNumber, labelX, labelY, labelPaintTab[position]);
                }
            }
        }
    }

    private boolean isAxisVertical(int position) {
        return LEFT == position || RIGHT == position;
    }

    private static class Label {

        public Label(float value) {
            this.value = value;
        }

        float value;
        float alpha;
        float raw;

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            return ((Label) obj).value == value;
        }
    }

}