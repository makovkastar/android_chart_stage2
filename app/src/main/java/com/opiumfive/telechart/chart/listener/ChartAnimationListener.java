package com.opiumfive.telechart.chart.listener;

import java.util.EventListener;

public interface ChartAnimationListener extends EventListener {
    public void onAnimationStarted();
    public void onAnimationFinished();
}