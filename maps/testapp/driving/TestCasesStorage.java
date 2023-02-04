package com.yandex.maps.testapp.driving;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.List;

public class TestCasesStorage {
    private static final ArrayList<TestCase> testCases = new ArrayList<TestCase>() {{
        add(new TestCase(
                "Long route",
                "Just a very long route",
                new RequestPoint[] {
                        new RequestPoint(new Point(55.809298, 37.481765), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(59.571726, 150.80312), RequestPointType.WAYPOINT, null)
                }));

        add(new TestCase(
                "Unpaved",
                "Route with an unpaved alternative",
                new RequestPoint[] {
                        new RequestPoint(new Point(54.253768, 26.481339), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(54.497113, 26.919419), RequestPointType.WAYPOINT, null)
                }));
        add(new TestCase(
                "Poor condition",
                "Route with an alternative in poor condition",
                new RequestPoint[] {
                        new RequestPoint(new Point(55.654286, 50.092193), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(56.156086, 49.930222), RequestPointType.WAYPOINT, null)
                }));
        add(new TestCase(
                "Railway crossings",
                "Route with railway crossings",
                new RequestPoint[] {
                        new RequestPoint(new Point(56.654010, 37.265956), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(56.668758, 37.245056), RequestPointType.WAYPOINT, null)
                }));
        add(new TestCase(
                "Pedestrian crossings",
                "Route with pedestrian crossings",
                new RequestPoint[] {
                        new RequestPoint(new Point(55.684645, 37.583221), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(55.688948, 37.574874), RequestPointType.WAYPOINT, null)
                }));
        add(new TestCase(
                "Speed bumps",
                "Route with speed bumps",
                new RequestPoint[] {
                        new RequestPoint(new Point(55.679483, 37.596919), RequestPointType.WAYPOINT, null),
                        new RequestPoint(new Point(55.681047, 37.608614), RequestPointType.WAYPOINT, null)
                }));
    }};

    public static List<TestCase> filteredTestCases(String query) {
        query = prepareForSearch(query);
        List<TestCase> result = new ArrayList<>();
        for (TestCase testCase: testCases) {
            if (match(query, testCase)) {
                result.add(testCase);
            }
        }
        return result;
    }

    private static String prepareForSearch(String text) {
        return text.trim().toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }

    private static boolean match(String query, TestCase testCase) {
        String title = prepareForSearch(testCase.getTitle());
        String description = prepareForSearch(testCase.getDescription());
        for (String token: query.split(" ")) {
            if (token.length() == 0) {
                continue;
            }
            if (!title.contains(token) && !description.contains(token)) {
                return false;
            }
        }
        return true;
    }
}
