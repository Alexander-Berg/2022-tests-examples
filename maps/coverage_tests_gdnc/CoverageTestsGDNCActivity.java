package com.yandex.maps.testapp.coverage_tests_gdnc;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.coverage.Coverage;
import com.yandex.mapkit.coverage.Region;
import com.yandex.mapkit.coverage.RegionsSession;
import com.yandex.mapkit.geometry.Point;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import java.util.ArrayList;
import java.util.List;

interface TestResultCallback {
    void onResult(RegionTest.Result result);
}

class RegionTest {
    public Point point;
    public ArrayList<Integer> expectedRegions;
    public String description;
    private Result result;

    public class Result {
        String text;
        int color;
    }

    public RegionsSession session;
    private final int ZOOM = 10;

    RegionTest(Point point, ArrayList<Integer> expectedRegions, String description) {
        this.point = point;
        this.expectedRegions = expectedRegions;
        this.description = description;
    }

    private Result fail(String message) {
        Result result = new Result();
        result.text = message;
        result.color = Color.RED;
        this.result = result;
        return result;
    }

    private Result success(String message) {
        Result result = new Result();
        result.text = message;
        result.color = Color.GREEN;
        this.result = result;
        return result;
    }

    void runTest(Coverage coverage, final TestResultCallback callback) {
        if (result != null) {
            callback.onResult(result);
            return;
        }
        session = coverage.regions(point, ZOOM, new RegionsSession.RegionsListener() {
            @Override
            public void onRegionsResponse(List<Region> regions) {
                ArrayList<Integer> regionIds = new ArrayList<Integer>();
                for (Region region : regions) {
                    regionIds.add(region.getId());
                }
                if (regionIds.size() != expectedRegions.size()) {
                    callback.onResult(
                            fail(description + " failed: expected " + expectedRegions +
                                    ", found " + regionIds));
                    return;
                }
                for (int i = 0; i < regionIds.size(); ++i) {
                    if (!regionIds.get(i).equals(expectedRegions.get(i))) {
                        callback.onResult(
                                fail(description + " failed: expected " + expectedRegions +
                                        ", found " + regionIds));
                        return;
                    }
                }
                callback.onResult(success(description + ": passed"));
            }

            @Override
            public void onRegionsError(com.yandex.runtime.Error error) {
                callback.onResult(fail("Error: " + error.toString()));
            }
        });
    }
}

/**
 * Created by dbeliakov on 16.06.16.
 */
public class CoverageTestsGDNCActivity extends TestAppActivity {
    private ListView testsList;
    private ArrayAdapter<RegionTest> testsAdapter;
    private final Coverage coverage = DirectionsFactory.getInstance().createGuidanceCoverage();
    private final ArrayList<RegionTest> tests = new ArrayList<RegionTest>() {{
        add(new RegionTest(new Point(55.757832, 37.614213), new ArrayList<Integer>() {{
            add(225);
        }}, "Russia"));
        add(new RegionTest(new Point(53.853975, 26.714000), new ArrayList<Integer>() {{
            add(149);
        }}, "Belarus"));
        add(new RegionTest(new Point(59.430176, 24.731395), new ArrayList<Integer>() {{
            add(179);
        }}, "Estonia"));
        add(new RegionTest(new Point(56.942213, 24.119912), new ArrayList<Integer>() {{
            add(206);
        }}, "Latvia"));
        add(new RegionTest(new Point(54.676585, 25.275235), new ArrayList<Integer>() {{
            add(117);
        }}, "Lithuania"));
        add(new RegionTest(new Point(50.053159, 19.943274), new ArrayList<Integer>() {{
            add(120);
        }}, "Poland"));
        add(new RegionTest(new Point(50.146233, 14.102222), new ArrayList<Integer>() {{
            add(125);
        }}, "Czech Republic"));
        add(new RegionTest(new Point(48.237646, 0.638317), new ArrayList<Integer>() {{
            add(124);
        }}, "France"));
        add(new RegionTest(new Point(39.641535, 37.252067), new ArrayList<Integer>() {{
            add(983);
        }}, "Turkey"));
        add(new RegionTest(new Point(47.774806, 31.786618), new ArrayList<Integer>() {{
            add(187);
        }}, "Ukraine"));
        add(new RegionTest(new Point(52.685931, 71.584609), new ArrayList<Integer>() {{
            add(159);
        }}, "Kazakhstan"));
        add(new RegionTest(new Point(41.013754, 48.759955), new ArrayList<Integer>() {{
            add(167);
        }}, "Azerbaijan"));
        add(new RegionTest(new Point(40.586152, 44.471679), new ArrayList<Integer>() {{
            add(168);
        }}, "Armenia"));
        add(new RegionTest(new Point(42.255124, 42.569647), new ArrayList<Integer>() {{
            add(169);
        }}, "Georgia"));
        add(new RegionTest(new Point(43.171062, 41.053380), new ArrayList<Integer>() {{
            add(29386);
        }}, "Abkhazia"));
        add(new RegionTest(new Point(42.293856, 44.224295), new ArrayList<Integer>() {{
            add(29387);
        }}, "South Ossetia"));
        add(new RegionTest(new Point(40.964753, 57.942635), new ArrayList<Integer>() {{
            add(170);
        }}, "Turkmenistan"));
        add(new RegionTest(new Point(42.677940, 64.101001), new ArrayList<Integer>() {{
            add(171);
        }}, "Uzbekistan"));
        add(new RegionTest(new Point(38.806115, 71.531308), new ArrayList<Integer>() {{
            add(209);
        }}, "Tajikistan"));
        add(new RegionTest(new Point(47.562486, 28.100616), new ArrayList<Integer>() {{
            add(208);
        }}, "Moldova"));
        add(new RegionTest(new Point(42.493779, 74.543681), new ArrayList<Integer>() {{
            add(207);
        }}, "Kyrgyzstan"));

        add(new RegionTest(new Point(45.180347, 34.419070), new ArrayList<Integer>() {{
            add(225);
        }}, "Crimea"));
        add(new RegionTest(new Point(45.308869, 36.506207), new ArrayList<Integer>() {{
            add(225);
        }}, "Crimean Bridge"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coverage_tests_gdnc);
        testsList = (ListView) findViewById(R.id.coverage_tests_gdnc_list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showTests();
    }

    private void showTests() {
        testsAdapter = new ArrayAdapter<RegionTest>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                tests) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText("Waiting for test result...");
                text1.setTextColor(Color.WHITE);

                RegionTest test = getItem(position);
                System.out.println(test.description + " " + position);
                test.runTest(coverage, new TestResultCallback() {
                    @Override
                    public void onResult(RegionTest.Result result) {
                        text1.setText(result.text);
                        text1.setTextColor(result.color);
                    }
                });
                return view;
            }
        };
        testsList.setAdapter(testsAdapter);
        testsList.setClickable(false);
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}
