package com.yandex.maps.testapp.directions_navigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.common.internal.point_context.PointContextKt;
import com.yandex.maps.testapp.directions_navigation.test.RouteSelector;
import com.yandex.maps.testapp.directions_navigation.test.TestCase;
import com.yandex.maps.testapp.directions_navigation.test.TestCaseData;
import com.yandex.maps.testapp.directions_navigation.test.TestCaseDataBuilder;
import com.yandex.runtime.bindings.Serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class TestCasesActivity extends Activity {
    private ListView testCasesList;
    private ArrayAdapter<TestCaseData> testCasesAdapter;
    private ProgressDialog progressDialog;
    private AnnotationLanguage language;
    private EditText filterEditText;
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private VehicleOptions vehicleOptions;

    private final ArrayList<TestCaseData> testCases = new ArrayList<TestCaseData>() {{
        add(TestCaseDataBuilder
                .getInstance(
                    "Simple guidance",
                    "Simple guidance test with route and couple of maneuvers")
                    .setRoutePoints(new Point[]{
                            new Point(55.734675, 37.588696),
                            new Point(55.733462, 37.596415)})
                    .setSimulationRoutePoints(new Point[]{
                            new Point(55.734675, 37.588696),
                            new Point(55.733462, 37.596415)})
                    .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Annotations",
                        "Simple route to test annotations")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(55.734675, 37.588696),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.733543, 37.596457),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.733462, 37.596415),
                                RequestPointType.WAYPOINT,
                                null)))
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.734675, 37.588696),
                        new Point(55.733462, 37.596415),
                        new Point(55.733150, 37.596028)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Standing Segments",
                    "Guidance test with standing segments")
                    .setRoutePoints(new Point[]{
                            new Point(59.931100, 30.360900),
                            new Point(59.934685, 30.348144),
                            new Point(59.937877, 30.357859)})
                    .setSimulationRoutePoints(new Point[]{
                            new Point(59.931100, 30.360900),
                            new Point(59.934685, 30.348144),
                            new Point(59.937877, 30.357859)})
                    .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Standing Segments - Free Drive",
                    "Guidance test with standing segments in free drive mode")
                    .setFreeDrive(true)
                    .setSimulationRoutePoints(new Point[]{
                            new Point(59.931100, 30.360900),
                            new Point(59.934685, 30.348144),
                            new Point(59.937877, 30.357859)})
                    .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Speed limit - urban",
                        "Simple route with the speed limit of 60 km/h")
                .setRoutePoints(new Point[]{
                        new Point(55.730452, 37.631894),
                        new Point(55.747778, 37.655691),
                        new Point(55.771444, 37.640151)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.730452, 37.631894),
                        new Point(55.747778, 37.655691),
                        new Point(55.771444, 37.640151)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Faster alternative",
                        "Test faster alternative with not optimal route")
                    .setRoutePoints(new Point[]{
                            new Point(55.898407, 37.587315),
                            new Point(56.184934, 36.976922)})
                    .setSimulationRoutePoints(new Point[]{
                            new Point(55.898407, 37.587315),
                            new Point(56.184934, 36.976922)})
                    .setSelectorType(RouteSelector.SelectorType.TOLL_FREE_ROUTE_SELECTOR)
                    .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Global alternatives",
                        "Simple route with a bunch of alternatives")
                .setRoutePoints(new Point[]{
                        new Point(55.710084, 37.520312),
                        new Point(55.731220, 37.635728)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.710084, 37.520312),
                        new Point(55.731220, 37.635728)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Danger road event",
                        "Annotation test of danger road event")
                .setRoutePoints(new Point[]{
                        new Point(58.690407, 52.949756),
                        new Point(58.665748, 52.962151)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.690407, 52.949756),
                        new Point(58.665748, 52.962151)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Specific danger road event",
                        "Annotation test of specific danger road event")
                .setRoutePoints(new Point[]{
                        new Point(58.650818, 53.060747),
                        new Point(58.655202, 52.989908)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.650818, 53.060747),
                        new Point(58.655202, 52.989908)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "School road event",
                        "Annotation test of school road event")
                .setRoutePoints(new Point[]{
                        new Point(56.336139, 44.113948),
                        new Point(56.336473, 44.116341)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(56.336139, 44.113948),
                        new Point(56.336473, 44.116341)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Accident & reconstruction road event",
                        "Annotation test of accident and reconstruction road event")
                .setRoutePoints(new Point[]{
                        new Point(60.230107, 75.192658),
                        new Point(60.231624, 75.180546)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(60.230107, 75.192658),
                        new Point(60.231624, 75.180546)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Control road events",
                        "Annotation test of road control events")
                .setRoutePoints(new Point[]{
                        new Point(58.580448, 53.014451),
                        new Point(58.625393, 53.019123)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.580448, 53.014451),
                        new Point(58.625393, 53.019123)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "SpeedControl mixed with other tags",
                        "Annotation test of road control events")
                .setRoutePoints(new Point[]{
                        new Point(58.652223, 53.068916),
                        new Point(58.651832, 53.047783)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.652223, 53.068916),
                        new Point(58.651832, 53.047783)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Control road events without route",
                        "Annotation test of road control events")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.580448, 53.014451),
                        new Point(58.625393, 53.019123)
                })
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Multiple control road events",
                        "Annotation test of road control events")
                .setRoutePoints(new Point[]{
                        new Point(58.476380, 52.993517),
                        new Point(58.481975, 52.994511)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.476380, 52.993517),
                        new Point(58.481975, 52.994511)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Danger road event without route",
                        "Annotation test of danger road event")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.690407, 52.949756),
                        new Point(58.665748, 52.962151)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "School road event without route",
                        "Annotation test of school road event")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(56.336139, 44.113948),
                        new Point(56.336473, 44.116341)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Specific danger road event without route",
                        "Annotation test of specific danger road event")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.650818, 53.060747),
                        new Point(58.655202, 52.989908)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Accident & reconstruction road event without route",
                        "Annotation test of accident and reconstruction road event")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(60.230107, 75.192658),
                        new Point(60.231624, 75.180546)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Chat & LocalChat road events",
                        "Chats visible in radius")
                .setRoutePoints(new Point[]{
                        new Point(55.921745, 42.908597),
                        new Point(55.937926, 42.953977)
                })
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.921745, 42.908597),
                        new Point(55.937926, 42.953977)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Chat & LocalChat road events without route",
                        "Chats visible in radius")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.921745, 42.908597),
                        new Point(55.937926, 42.953977)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Rerouting",
                        "Multiple route lost for rerouting test")
                .setRoutePoints(new Point[]{
                        new Point(55.733959, 37.589656),
                        new Point(55.733623, 37.596552)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.734047, 37.589685),
                        new Point(55.732082, 37.592082),
                        new Point(55.733148, 37.585773),
                        new Point(55.735128, 37.580910),
                        new Point(55.733623, 37.596552)})
                .setDisableAlternatives(true)
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Rerouting after waypoint",
                        "Rerouting test for a route with waypoints")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(55.914756, 37.474187),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.915577, 37.473305),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.919626, 37.468709),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.922685, 37.465137),
                                RequestPointType.WAYPOINT,
                                null)))
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.914756, 37.474187),
                        new Point(55.915577, 37.473305),
                        new Point(55.917725, 37.476724),
                        new Point(55.918738, 37.475904),
                        new Point(55.919626, 37.468709),
                        new Point(55.922685, 37.465137)})
                .setDisableAlternatives(true)
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Toll route",
                        "Route with toll roads and free alternatives")
                .setRoutePoints(new Point[]{
                        new Point(55.898407, 37.587315),
                        new Point(56.184934, 36.976922)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.898407, 37.587315),
                        new Point(56.184934, 36.976922)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Rerouting with traits",
                        "Test for frequent rerouting to toll routes")
                .setRoutePoints(new Point[]{
                        new Point(55.936429, 37.370717),
                        new Point(59.940438, 30.310438)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.936429, 37.370717),
                        new Point(55.941049, 37.360918),
                        new Point(55.942375, 37.360749)})
                .setDisableAlternatives(true)
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Landmarks/ After bridge",
                    "Simple route to test \"After bridge\" annotation")
                .setRoutePoints(new Point[]{
                        new Point(55.926576, 37.800747),
                        new Point(55.929885, 37.803715)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.926576, 37.800747),
                        new Point(55.929885, 37.803715)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Landmarks/ Before bridge",
                    "Simple route to test \"Before bridge\" annotation")
                .setRoutePoints(new Point[]{
                        new Point(55.736179, 37.591943),
                        new Point(55.734423, 37.593430)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.736179, 37.591943),
                        new Point(55.734423, 37.593430)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Landmarks/ To bridge",
                    "Simple route to test \"To bridge\" annotation")
                .setRoutePoints(new Point[]{
                        new Point(55.718096, 37.694979),
                        new Point(55.718830, 37.703604)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.718096, 37.694979),
                        new Point(55.718830, 37.703604)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Landmarks/ Courtyard",
                    "Simple route to test \"Courtyard\" annotation")
                .setRoutePoints(new Point[] {
                        new Point(55.930950, 37.817587),
                        new Point(55.928963, 37.816152)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(55.930950, 37.817587),
                        new Point(55.928963, 37.816152)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Toll road ahead",
                    "Simple route to test \"Toll road ahead\" annotation")
                .setRoutePoints(new Point[] {
                        new Point(55.970704, 37.374146),
                        new Point(55.959784, 37.361278)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(55.970704, 37.374146),
                        new Point(55.959784, 37.361278)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                    "Street annotations",
                    "Simple route to test street annotations")
                .setRoutePoints(new Point[]{
                        new Point(55.734002, 37.578354),
                        new Point(55.731605, 37.577796)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.734002, 37.578354),
                        new Point(55.731605, 37.577796)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Free Drive",
                        "Simple freedrive route")
                .setFreeDrive(true)
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.737490, 37.588938),
                        new Point(55.734822, 37.593635),
                        new Point(55.734116, 37.590754),
                        new Point(55.731803, 37.590547),
                        new Point(55.731998, 37.584786)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Nearby alternatives",
                        "A non-optimal route with alternatives")
                .setRouteUri("ymapsbm1://route/driving/v1/Cr0CClyQ0s410AHoAdgCII8Mrw3HAc8BpwGvASeHDa8MqAH4AagCoAIwD58DhwbfEB_fAZcCrwI_pxqAAmAYmAKwAUCvGfcl7wS_B-cFjwcvpwGvAt8DhwTPBZcPpzOfAhJloIqRNK9Kh0THX5cKrwXIBdA6uEXYM_A4oA3oB_cBtzm3Rq9Zt1HfBr8E7wOXAfgB0AfwQehXiFvAEK8CvzefKN8K10DfRacQpwKYBbABwAO4BPAHWMgCgA7oIKgUmA_AF_BJmAMaZLMBswGyAbIBsgGNAucC5gLnAucC5wLnApoCswGzAbMBswGxAbEBwwH6AY4C5wLnAucC5gLnAucCsgGzAbIBsgGzAbIBsgGaAqUCsAK7AscC1ALUAt4C5ALjAt4C1QLSAtEC0QIiBggAEgIQDyIICDESBBBYGBQSABoAIJpe")
                .setSimulationRoutePoints(new Point[]{
                        new Point(54.6658724513544, 56.2187538812834),
                        new Point(54.6553480000000, 56.2190140000000),
                        new Point(54.6552730000000, 56.2141290000000),
                        new Point(54.6635383799511, 56.2138420302869),
                        new Point(54.6633877421680, 56.2121288972041),
                        new Point(54.6469214451978, 56.2126486229089),
                        new Point(54.6482447346397, 56.2069404638824),
                        new Point(54.6601276849761, 56.2015264427847)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Lanes",
                        "Bus lane")
                .setRoutePoints(new Point[]{
                        new Point(55.799803, 37.532410),
                        new Point(55.792381, 37.545124)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.799803, 37.532410),
                        new Point(55.792381, 37.545124)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Lanes",
                        "Highlight only suitable straight ahead lanes")
                .setRoutePoints(new Point[]{
                        new Point(55.773731, 37.619759),
                        new Point(55.773353, 37.608156),
                        new Point(55.773834, 37.604092)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.773731, 37.619759),
                        new Point(55.773353, 37.608156),
                        new Point(55.773834, 37.604092)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Lanes",
                        "Don't show lane sign where all straight ahead lines are highlighted")
                .setRoutePoints(new Point[]{
                        new Point(55.764318, 37.536759),
                        new Point(55.767853, 37.538713)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.764318, 37.536759),
                        new Point(55.767853, 37.538713)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Lanes",
                        "Show lanes when going straight on highway")
                .setRoutePoints(new Point[]{
                        new Point(55.742984, 37.542315),
                        new Point(55.739143, 37.530301)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.742984, 37.542315),
                        new Point(55.739143, 37.530301)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance("Direction signs",
                        "Icon + toponym + road")
                .setRoutePoints(new Point[]{
                        new Point(55.591287, 37.729130),
                        new Point(55.591476, 37.727419)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.591287, 37.729130),
                        new Point(55.591476, 37.727419)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance("Direction signs",
                        "Exit + toponym + road")
                .setRoutePoints(new Point[]{
                        new Point(55.572580, 37.650284),
                        new Point(55.572196, 37.653172)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.572580, 37.650284),
                        new Point(55.572196, 37.653172)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Manoeuvres",
                        "Simple testcase to test manoeuvre balloon")
                .setRoutePoints(new Point[]{
                        new Point(55.804498, 37.476749),
                        new Point(55.805843, 37.484174)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.804498, 37.476749),
                        new Point(55.805843, 37.484174)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Manoeuvres",
                        "Simple testcase to test roundabout")
                .setRoutePoints(new Point[] {
                        new Point(58.047173, 38.861433),
                        new Point(58.047392, 38.858335)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(58.047173, 38.861433),
                        new Point(58.047392, 38.858335)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Manoeuvres",
                        "Simple testcase to test turning manoeuvre")
                .setRoutePoints(new Point[] {
                        new Point(55.805657, 37.511200),
                        new Point(55.806312, 37.510541)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(55.805657, 37.511200),
                        new Point(55.806312, 37.510541)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Long route",
                        "Test the application operability when working with long routes")
                .setRoutePoints(new Point[] {
                        new Point(43.106995, 131.896335),
                        new Point(38.710055, -9.484861)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(43.106995, 131.896335),
                        new Point(38.710055, -9.484861)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Reroute to the finish",
                        "Reroute after passing waypoint")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(55.747305, 37.582627),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.749752, 37.583101),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.748791, 37.588282),
                                RequestPointType.WAYPOINT,
                                null)))
                .setSimulationRoutePoints(new Point[] {
                        new Point(55.747305, 37.582627),
                        new Point(55.748791, 37.588282)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Waypoint alternatives",
                        "Reset alternatives after waypoint")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(55.898407, 37.587315),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(55.904446, 37.587315),
                                RequestPointType.WAYPOINT,
                                null),
                        new RequestPoint(
                                new Point(56.184934, 36.976922),
                                RequestPointType.WAYPOINT,
                                null)))
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.898407, 37.587315),
                        new Point(55.904446, 37.587315),
                        new Point(56.184934, 36.976922)})
                .setSelectorType(RouteSelector.SelectorType.TOLL_FREE_ROUTE_SELECTOR)
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Speed camera annotation",
                        "Test speed camera annotation when its limit is greater than the road speed limit")
                .setRoutePoints(new Point[] {
                        new Point(55.551529, 37.547987),
                        new Point(55.551395, 37.531779)})
                .setSimulationRoutePoints(new Point[] {
                        new Point(55.551529, 37.547987),
                        new Point(55.551395, 37.531779)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Rerouting with arrival points",
                        "Multiple route lost for rerouting test, with arrival points")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(55.733959, 37.589656),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(55.733623, 37.596552),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode("v1|37.597066,55.735368;37.597828,55.734297|")
                        ),
                        new RequestPoint(
                                new Point(55.727933, 37.600451),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode(
                                        "v1|37.609609,55.733290;37.605124,55.731680;37.604105,55.727367;37.610489,55.730602|")
                        )))
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.734047, 37.589685),
                        new Point(55.732082, 37.592082),
                        new Point(55.735128, 37.580910),
                        new Point(55.727102, 37.608619)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Nearby alternative with arrival points",
                        "Test nearby alternative with arrival points")
                .setRoutePoints(Arrays.asList(
                        new RequestPoint(
                                new Point(54.643479, 56.209021),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.645537, 56.208282),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.661040, 56.211431),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode("v1|56.212259,54.662571|")
                        )))
                .setSimulationRoutePoints(new Point[]{
                        new Point(54.643479, 56.209021),
                        new Point(54.645537, 56.208282),
                        new Point(54.652755, 56.215524),
                        new Point(54.662571, 56.212259)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Custom annotations/Lva Tolstogo street",
                        "Route to test custom annotations about the office of Yandex and pedestrians")
                .setRoutePoints(new Point[]{
                        new Point(55.737515, 37.584184),
                        new Point(55.734275, 37.586265)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.737515, 37.584184),
                        new Point(55.734275, 37.586265)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Custom annotations/Gagarinskiy tunnel",
                        "Route to test \"second exit\" annotation")
                .setRoutePoints(new Point[]{
                        new Point(55.704117, 37.598110),
                        new Point(55.707355, 37.587496)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.704117, 37.598110),
                        new Point(55.707355, 37.587496)})
                .build()
        );
        add(TestCaseDataBuilder
                .getInstance(
                        "Custom annotations/Serebryakova drive",
                        "Route to test \"under bridge\" annotation")
                .setRoutePoints(new Point[]{
                        new Point(55.847069, 37.667496),
                        new Point(55.845800, 37.670454)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.847069, 37.667496),
                        new Point(55.845800, 37.670454)})
                .build()
        );

	    add(TestCaseDataBuilder
                .getInstance(
                        "Long route without turns",
                        "Route to test camera at freedrive end")
                .setRoutePoints(new Point[]{
                        new Point(58.500458, 106.991684),
                        new Point(58.754859, 107.068471)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(58.500458, 106.991684),
                        new Point(58.754859, 107.068471)})
                .build()
        );

	    add(TestCaseDataBuilder
                .getInstance(
                        "Speed limit annotations",
                        "Test speed limit annotation repeating")
                .setRoutePoints(new Point[]{
                        new Point(59.379188, 31.021164),
                        new Point(59.186509, 31.268736)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(59.379188, 31.021164),
                        new Point(59.186509, 31.268736)})
                .build()
        );

        add(TestCaseDataBuilder
                .getInstance(
                        "Railway crossings",
                        "Route with railway crossings")
                .setRoutePoints(new Point[]{
                        new Point(56.654010, 37.265956),
                        new Point(56.668758, 37.245056)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(56.654010, 37.265956),
                        new Point(56.668758, 37.245056)})
                .build()
        );

        add(TestCaseDataBuilder
                .getInstance(
                        "Speed bumps",
                        "Route with speed bumps")
                .setRoutePoints(new Point[]{
                        new Point(55.681047, 37.608614),
                        new Point(55.679483, 37.596919)})
                .setSimulationRoutePoints(new Point[]{
                        new Point(55.681047, 37.608614),
                        new Point(55.679483, 37.596919)})
                .build()
        );
    }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.directions_navigation_test_cases);
        super.onCreate(savedInstanceState);
        language = (AnnotationLanguage)getIntent().getSerializableExtra("language");
        vehicleOptions = Serialization.deserializeFromBytes(
                (byte[])getIntent().getSerializableExtra("vehicle_options"),
                VehicleOptions.class);
        testCasesList = (ListView) findViewById(R.id.directions_navigation_test_cases_list);
        filterEditText = (EditText) findViewById(R.id.filter_edit_text);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showTests();
    }

    private void showOverlay() {
        hideOverlay();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading routes");
        progressDialog.setMessage("Wait while loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideOverlay() {
        if (progressDialog == null) return;
        progressDialog.dismiss();
        progressDialog = null;
    }

    private String prepareForSearch(String string) {
        return string.trim().toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }

    private boolean match(String searchText, TestCaseData testCase) {
        String title = prepareForSearch(testCase.title);
        String description = prepareForSearch(testCase.description);
        for (String part : searchText.split(" ")) {
            if (part.length() != 0 &&
                    !title.contains(part) &&
                    !description.contains(part)) {
                return false;
            }
        }

        return true;
    }

    private void updateListView(String rawSearchText) {
        String searchText = prepareForSearch(rawSearchText);
        testCasesAdapter.clear();
        for (TestCaseData testCase : testCases) {
            if (match(searchText, testCase)) {
                testCasesAdapter.add(testCase);
            }
        }
    }

    private void showTests() {
        testCasesAdapter = new ArrayAdapter<TestCaseData>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TestCaseData testCaseData = getItem(position);

                assert testCaseData != null;

                final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(testCaseData.title);
                text1.setTextColor(Color.WHITE);

                final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text2.setText(testCaseData.description);
                text2.setTextColor(Color.WHITE);

                return view;
            }
        };
        testCasesList.setAdapter(testCasesAdapter);
        filterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateListView(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        testCasesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                showOverlay();

                TestCaseData testCaseData = testCasesAdapter.getItem(position);

                assert testCaseData != null;

                if (testCaseData.vehicleOptions == null) {
                    testCaseData.vehicleOptions = vehicleOptions;
                }

                TestCase testCase = new TestCase(
                        testCaseData.routePoints,
                        testCaseData.simulationRoutePoints,
                        testCaseData.routeUri,
                        testCaseData.simulationRouteUri,
                        testCaseData.selectorType,
                        testCaseData.vehicleOptions,
                        testCaseData.useParkingRoutes,
                        testCaseData.disableAlternatives,
                        testCaseData.freeDrive
                );

                Intent intent = new Intent();
                intent.putExtra("testCase", testCase);
                setResult(RESULT_OK, intent);
                hideOverlay();
                finish();
            }
        });

        updateListView("");
    }
}
