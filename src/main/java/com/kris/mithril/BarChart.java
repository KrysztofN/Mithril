package com.kris.mithril;

import org.jfree.chart.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BarChart extends ApplicationFrame {
    private final JFreeChart barChart;

    public BarChart(String appTitle, String chartTitle, HashMap<String, Long[]> times) {
        super(appTitle);
         barChart = ChartFactory.createBarChart(
                chartTitle,
                "benchmarks",
                "time (ms)",
                createDataset(times),
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 400));
        setContentPane(chartPanel);
    }

    private CategoryDataset createDataset(HashMap<String, Long[]> times) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        final String vm = "vm";
        final String ast = "ast";

        Iterator it = times.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            String key = pair.getKey().toString();
            Long[] values = (Long[]) pair.getValue();

            dataset.addValue(values[0] / 1_000_000.0, ast, key);
            dataset.addValue(values[1] / 1_000_000.0, vm, key);

        }
        System.out.println("Row count: " + dataset.getRowCount());
        System.out.println("Row keys: " + dataset.getRowKeys());

        return dataset;
    }

    public void save(String filename) throws IOException {
        File BarChart = new File(filename);
        ChartUtilities.saveChartAsJPEG(BarChart, barChart, 640, 480);
    }
}
