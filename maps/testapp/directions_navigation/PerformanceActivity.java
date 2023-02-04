package com.yandex.maps.testapp.directions_navigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yandex.mapkit.directions.navigation.PerformanceMonitor;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;

public class PerformanceActivity extends Activity  {
    public static final String PERCENTS_EXTRA = "percents";
    public static final String METRIC_TAGS_EXTRA = "metric_tags";
    public static final String QUANTILES_EXTRA = "quantiles";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.directions_navigation_performance);

        Button backButton = findViewById(R.id.performance_back_button);
        backButton.setOnClickListener((view) -> finish());

        Intent intent = getIntent();
        ArrayList<Float> percents =
                (ArrayList<Float>) intent.getSerializableExtra(PERCENTS_EXTRA);
        ArrayList<PerformanceMonitor.MetricTag> metricTags =
                (ArrayList<PerformanceMonitor.MetricTag>) intent.getSerializableExtra(METRIC_TAGS_EXTRA);
        ArrayList<List<Float>> quantiles =
                (ArrayList<List<Float>>) intent.getSerializableExtra(QUANTILES_EXTRA);

        LinearLayout performanceMetricsLayout = findViewById(R.id.performance_metrics_layout);
        LayoutInflater inflater = LayoutInflater.from(this);
        for(int i = 0; i < metricTags.size(); ++i) {
            String metricName = tagToString(metricTags.get(i));
            appendHeaderView(metricName, performanceMetricsLayout);

            if ( quantiles.get(i) == null ) {
                appendRecordView("No quantiles", inflater, performanceMetricsLayout);
                continue;
            }

            for(int j = 0; j < percents.size(); ++j) {
                Float percent = percents.get(j);
                Float quantile = quantiles.get(i).get(j);
                String content = String.format("Quantile %.2f: %.2f", percent, quantile);
                appendRecordView(
                        content,
                        inflater,
                        performanceMetricsLayout);
            }
        }
    }

    private void appendHeaderView(String metricName, LinearLayout layout) {
        TextView headerView = new TextView(this);
        headerView.setText(metricName);
        headerView.setTextSize(24.0f);
        layout.addView(headerView);
    }

    private void appendRecordView(String content, LayoutInflater inflater, LinearLayout layout) {
        View itemView = inflater.inflate(android.R.layout.simple_list_item_1, layout, false);
        TextView text = (TextView) itemView.findViewById(android.R.id.text1);
        text.setText(content);
        layout.addView(itemView);
    }

    private static String tagToString(PerformanceMonitor.MetricTag tag) {
        switch (tag) {
            case EMIT_FRAME_DURATION:
                return "Emit frame duration";
            case LOCATION_PROCESSING_TIME:
                return "Location processing time";
        }
        throw new IllegalArgumentException("Unknown tag");
    }
}
