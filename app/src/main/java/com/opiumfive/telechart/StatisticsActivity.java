package com.opiumfive.telechart;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;

import com.opiumfive.telechart.chart.formatter.AxisValueFormatter;
import com.opiumfive.telechart.chart.listener.DummyLineChartOnValueSelectListener;
import com.opiumfive.telechart.chart.listener.ViewportChangeListener;
import com.opiumfive.telechart.chart.model.Axis;
import com.opiumfive.telechart.chart.model.AxisValue;
import com.opiumfive.telechart.chart.model.Line;
import com.opiumfive.telechart.chart.model.LineChartData;
import com.opiumfive.telechart.chart.model.PointValue;
import com.opiumfive.telechart.chart.model.Viewport;
import com.opiumfive.telechart.chart.LineChartView;
import com.opiumfive.telechart.chart.PreviewLineChartView;
import com.opiumfive.telechart.data.ChartData;
import com.opiumfive.telechart.data.ChartDataParser;
import com.opiumfive.telechart.data.ColumnData;
import com.opiumfive.telechart.fastdraw.FastSurfaceView;
import com.opiumfive.telechart.fastdraw.FastTextureView;
import com.opiumfive.telechart.theming.ChangeThemeActivity;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends ChangeThemeActivity {

    private LineChartView chart;
    private PreviewLineChartView previewChart;
    private LineChartData data;
    private LineChartData previewData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        chart = findViewById(R.id.chart);
        previewChart = findViewById(R.id.chart_preview);

        ChartData chartData = ChartDataParser.loadAndParseInput(this, 4);
        inflateCharts(chartData);
    }

    private void inflateCharts(@Nullable ChartData chartData) {
        if (chartData == null) return;

        // TODO rm divider
        int divider = 1;

        List<List<PointValue>> plotsValuesList = new ArrayList<>(chartData.getColumns().size() - 1);

        ColumnData xValuesData = chartData.getColumns().get(0);
        for (int columnIndex = 1; columnIndex < chartData.getColumns().size(); columnIndex++) {
            ColumnData yValuesData = chartData.getColumns().get(columnIndex);
            List<PointValue> values = new ArrayList<>(yValuesData.getList().size() / divider);

            for (int i = 0; i < chartData.getColumns().get(0).getList().size() / divider; i++) {
                values.add(new PointValue(xValuesData.getList().get(i), yValuesData.getList().get(i)));
            }

            plotsValuesList.add(values);
        }

        List<Line> lines = new ArrayList<>(chartData.getColumns().size() - 1);
        for (int columnIndex = 1; columnIndex < chartData.getColumns().size(); columnIndex++) {
            Line line = new Line(plotsValuesList.get(columnIndex - 1));
            line.setColor(Color.parseColor(chartData.getColors().get(chartData.getColumns().get(columnIndex).getTitle())));
            lines.add(line);
        }

        data = new LineChartData(lines);
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        data.setAxisYLeft(new Axis().setHasLines(true));

        previewData = new LineChartData(data);

        chart.setLineChartData(data);
        chart.setZoomEnabled(false);
        chart.setScrollEnabled(false);
        chart.setValueSelectionEnabled(true);
        chart.setValueTouchEnabled(true);
        chart.setOnValueTouchListener(new DummyLineChartOnValueSelectListener());


        previewChart.setLineChartData(previewData);
        previewChart.setViewportChangeListener(new ViewportListener());
        previewChart.setZoomEnabled(false);
        previewChart.setViewportCalculationEnabled(false);

         /*final AppCompatCheckBox checkBox1 = findViewById(R.id.checkbox1);
        final AppCompatCheckBox checkBox2 = findViewById(R.id.checkbox2);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               List<Line> lines = new ArrayList<>();

                if (checkBox1.isChecked()) {
                    lines.add(line);
                }

                if (checkBox2.isChecked()) {
                    lines.add(line2);
                }

                data.setLines(lines);
                previewData.setLines(lines);
                chart.setLineChartData(data);
                previewChart.setLineChartData(previewData);

                previewX(true);
            }
        };

        checkBox1.setOnClickListener(listener);
        checkBox2.setOnClickListener(listener);*/

        previewX(true);
    }

    private class ViewportListener implements ViewportChangeListener {

        @Override
        public void onViewportChanged(Viewport newViewport) {
            chart.setCurrentViewport(newViewport);
        }

    }

    private void previewX(boolean animate) {
        Viewport tempViewport = new Viewport(chart.getMaximumViewport());
        float dx = tempViewport.width() / 3;
        tempViewport.inset(dx, 0);
        if (animate) {
            previewChart.setCurrentViewportWithAnimation(tempViewport);
        } else {
            previewChart.setCurrentViewport(tempViewport);
        }
    }
}
