package com.yandex.maps.testapp.guidance;

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

import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.internal.RouteUtils;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.maps.testapp.common.internal.point_context.PointContextKt;
import com.yandex.maps.testapp.guidance.test.RouteSelector;
import com.yandex.maps.testapp.guidance.test.TestCase;
import com.yandex.maps.testapp.guidance.test.TestCaseData;
import com.yandex.maps.testapp.guidance.test.TestCaseFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.bindings.Serialization;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


class TollFreeRouteSelector implements RouteSelector {
    @Override
    public DrivingRoute selectRoute(List<DrivingRoute> routes) {
        for (DrivingRoute route : routes) {
            if (!route.getMetadata().getFlags().getHasTolls()) {
                return route;
            }
        }
        return null;
    }
}

class BestRouteWithNoViaPoints implements RouteSelector {
    @Override
    public DrivingRoute selectRoute(List<DrivingRoute> routes) {
        for (DrivingRoute route : routes) {
            return RouteUtils.dropRouteViaPoints(route);
        }
        return null;
    }
}

/**
 * Created by dbeliakov on 10.08.16.
 */
public class TestCasesActivity extends Activity {
    private ListView testCasesList;
    private ArrayAdapter<TestCaseData> testCasesAdapter;
    private ProgressDialog progressDialog;
    private AnnotationLanguage language;
    private EditText filterEditText;
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private VehicleOptions vehicleOptions;

    private ArrayList<TestCaseData> testCases = new ArrayList<TestCaseData>() {{
        add(new TestCaseData(
                "Simple guidance",
                "Simple guidance test with route and couple of maneuvers",
                new Point[]{
                        new Point(55.733959, 37.589656),
                        new Point(55.733462, 37.596415)
                },
                new Point[]{
                        new Point(55.733959, 37.589656),
                        new Point(55.733462, 37.596415)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Turkey)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[]{
                        new Point(41.036620, 28.858054),
                        new Point(41.044320, 28.845737)
                },
                new Point[]{
                        new Point(41.036620, 28.858054),
                        new Point(41.044320, 28.845737)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Estonia)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[] {
                        new Point(59.430176, 24.731395),
                        new Point(59.429478, 24.728887)
                },
                new Point[] {
                        new Point(59.430176, 24.731395),
                        new Point(59.429478, 24.728887)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Latvia)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[] {
                        new Point(56.941500, 24.121300),
                        new Point(56.942741, 24.124762)
                },
                new Point[] {
                        new Point(56.941500, 24.121300),
                        new Point(56.942741, 24.124762)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Lithuania)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[]{
                        new Point(54.676585, 25.275235),
                        new Point(54.677884, 25.279355)
                },
                new Point[]{
                        new Point(54.676585, 25.275235),
                        new Point(54.677884, 25.279355)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Poland)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[] {
                        new Point(50.053159, 19.943274),
                        new Point(50.054772, 19.944545)
                },
                new Point[] {
                        new Point(50.053159, 19.943274),
                        new Point(50.054772, 19.944545)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (Czech Republic)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[] {
                        new Point(50.146233, 14.102222),
                        new Point(50.145561, 14.100286)
                },
                new Point[] {
                        new Point(50.146233, 14.102222),
                        new Point(50.145561, 14.100286)
                },
                null
        ));
        add(new TestCaseData(
                "Simple guidance (France)",
                "Simple guidance test with route and couple of maneuvers",
                new Point[]{
                        new Point(48.805980, 2.345317),
                        new Point(48.846784, 2.379306)
                },
                new Point[]{
                        new Point(48.805980, 2.345317),
                        new Point(48.846784, 2.379306)
                },
                null
        ));
        add(new TestCaseData(
                "Without route",
                "Guidance without route",
                new Point[]{},
                new Point[]{
                        new Point(55.733959, 37.589656),
                        new Point(55.808475, 37.494787)
                },
                null
        ));
        add(new TestCaseData(
                "Rerouting",
                "Multiple route lost for rerouting test",
                new Point[]{
                        new Point(55.733959, 37.589656),
                        new Point(55.733623, 37.596552)
                },
                new Point[]{
                        new Point(55.734047, 37.589685),
                        new Point(55.732082, 37.592082),
                        new Point(55.733148, 37.585773),
                        new Point(55.735128, 37.580910),
                        new Point(55.733623, 37.596552)
                },
                null
        ));
        add(new TestCaseData(
                "Rerouting with arrival points",
                "Multiple route lost for rerouting test, with arrival points",
                new RequestPoint[]{
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
                        )
                },
                new Point[]{
                        new Point(55.734047, 37.589685),
                        new Point(55.732082, 37.592082),
                        new Point(55.735128, 37.580910),
                        new Point(55.727102, 37.608619)
                },
                null,
                null,
                false
        ));
        add(new TestCaseData(
                "Landmarks",
                "After bridge and into courtyard landmarks",
                new Point[]{
                        new Point(55.923446, 37.798321),
                        new Point(55.931000, 37.817014),
                        new Point(55.929029, 37.816040)
                },
                new Point[]{
                        new Point(55.923446, 37.798321),
                        new Point(55.931000, 37.817014),
                        new Point(55.929029, 37.816040)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "Before bridge",
                new Point[]{
                        new Point(55.736870, 37.589959),
                        new Point(55.733461, 37.592795)
                },
                new Point[]{
                        new Point(55.736870, 37.589959),
                        new Point(55.733461, 37.592795)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "At traffic lights",
                new Point[]{
                        new Point(55.693763, 37.535447),
                        new Point(55.700555, 37.533085)
                },
                new Point[]{
                        new Point(55.693763, 37.535447),
                        new Point(55.700555, 37.533085)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "Before tunnel",
                new Point[]{
                        new Point(43.540658, 39.807844),
                        new Point(43.541563, 39.806594)
                },
                new Point[]{
                        new Point(43.540658, 39.807844),
                        new Point(43.541563, 39.806594)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "After tunnel",
                new Point[]{
                        new Point(55.740900, 37.534814),
                        new Point(55.740059, 37.535752)
                },
                new Point[]{
                        new Point(55.740900, 37.534814),
                        new Point(55.740059, 37.535752)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "To bridge",
                new Point[]{
                        new Point(55.716292, 37.687183),
                        new Point(55.719545, 37.701764),
                        new Point(55.716953, 37.707708)
                },
                new Point[]{
                        new Point(55.716292, 37.687183),
                        new Point(55.719545, 37.701764),
                        new Point(55.716953, 37.707708)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "Into tunnel",
                new Point[]{
                        new Point(55.758879, 37.677993),
                        new Point(55.759094, 37.683553),
                        new Point(55.75092, 37.696515)
                },
                new Point[]{
                        new Point(55.758879, 37.677993),
                        new Point(55.759094, 37.683553),
                        new Point(55.75092, 37.696515)
                },
                null
        ));
        add(new TestCaseData(
                "Landmarks",
                "To frontage road",
                new Point[]{
                        new Point(55.720411, 37.419415),
                        new Point(55.717964, 37.411111)
                },
                new Point[]{
                        new Point(55.720411, 37.419415),
                        new Point(55.717964, 37.411111)
                },
                null
        ));
        add(new TestCaseData(
                "Faster alternative",
                "Test faster alternative with not optimal route",
                new Point[]{
                        new Point(55.898407, 37.587315),
                        new Point(56.184934, 36.976922)
                },
                new Point[]{
                        new Point(55.898407, 37.587315),
                        new Point(56.184934, 36.976922)
                },
                null,
                new TollFreeRouteSelector()
        ));
        add(new TestCaseData(
                "Nearby alternative with arrival points",
                "Test nearby alternative with arrival points",
                new RequestPoint[]{
                        new RequestPoint(
                                new Point(54.646757, 56.212821),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.649847, 56.212617),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.663117, 56.210794),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode(
                                        "v1|56.218998,54.652554;56.202380,54.658962;56.215482,54.666596|")
                        )
                },
                new Point[]{
                        new Point(54.646757, 56.212821),
                        new Point(54.649847, 56.212617),
                        new Point(54.652755, 56.215524),
                        new Point(54.666596, 56.215482)
                },
                null,
                new BestRouteWithNoViaPoints(),
                false
        ));
        add(new TestCaseData(
                "Nearby alternative",
                "Test nearby alternative with a non-optimal route",
                new Point[]{
                        new Point(54.6658724513544, 56.2187538812834),
                        new Point(54.6499872957462, 56.2191167087220),
                        new Point(54.6496864870935, 56.2183900736068),
                        new Point(54.6500026484731, 56.2174820247723),
                        new Point(54.6659923111976, 56.2171182155120),
                        new Point(54.6665186542929, 56.2163141115018),
                        new Point(54.6663680276113, 56.2155100074915),
                        new Point(54.6468213924524, 56.2160302827002),
                        new Point(54.6468213924524, 56.2142916523427),
                        new Point(54.6635383799511, 56.2138420302869),
                        new Point(54.6633877421680, 56.2121288972041),
                        new Point(54.6469214451978, 56.2126486229089),
                        new Point(54.6482447346397, 56.2069404638824),
                        new Point(54.6601276849761, 56.2015264427847)
                },
                new Point[]{
                        new Point(54.6658724513544, 56.2187538812834),
                        new Point(54.6553480000000, 56.2190140000000),
                        new Point(54.6552730000000, 56.2141290000000),
                        new Point(54.6635383799511, 56.2138420302869),
                        new Point(54.6633877421680, 56.2121288972041),
                        new Point(54.6469214451978, 56.2126486229089),
                        new Point(54.6482447346397, 56.2069404638824),
                        new Point(54.6601276849761, 56.2015264427847)
                },
                null,
                new BestRouteWithNoViaPoints()
        ));
        add(new TestCaseData(
                "Nearby alternative with driving arrival points",
                "Test nearby alternative with driving arrival points",
                new RequestPoint[]{
                        new RequestPoint(
                                new Point(54.643561, 56.208942),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.650653, 56.212698),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.662477, 56.208748),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode(
                                        "v1||56.218998,54.652554,X;56.202380,54.658962,Y;56.215482,54.666596,Z")
                        )
                },
                new Point[]{
                        new Point(54.643561, 56.208942),
                        new Point(54.648538, 56.211110),
                        new Point(54.650653, 56.212698),
                        new Point(54.652520, 56.215665),
                        new Point(54.649708, 56.215836),
                        new Point(54.649683, 56.219269),
                        new Point(54.652554, 56.218998),
                        new Point(54.655273, 56.217375),
                        new Point(54.655313, 56.212286)
                },
                null,
                new BestRouteWithNoViaPoints(),
                false
        ));
        add(new TestCaseData(
                "Rerouting with traits",
                "Don't reroute to toll route",
                new Point[]{
                        new Point(50.451870, 40.139802),
                        new Point(50.456384, 40.140190),
                        new Point(55.714939, 37.410203)
                },
                new Point[]{
                        new Point(50.451870, 40.139802),
                        new Point(50.456384, 40.140190),
                        new Point(50.461552, 40.133411),
                        new Point(50.456384, 40.140190),
                        new Point(50.461552, 40.133411),
                        new Point(50.474187, 40.139150)
                },
                null,
                new TollFreeRouteSelector(),
                false,
                true
        ));
        add(new TestCaseData(
                "Lanes",
                "Lane before maneuver and bus lane",
                new Point[]{
                        new Point(55.799803, 37.532410),
                        new Point(55.792381, 37.545124)
                },
                new Point[]{
                        new Point(55.799803, 37.532410),
                        new Point(55.792381, 37.545124)
                },
                null
        ));
        add(new TestCaseData(
                "Lanes",
                "Highlight only suitable straight ahead lanes",
                new Point[]{
                        new Point(55.773731, 37.619759),
                        new Point(55.773353, 37.608156),
                        new Point(55.773834, 37.604092)
                },
                new Point[]{
                        new Point(55.773731, 37.619759),
                        new Point(55.773353, 37.608156),
                        new Point(55.773834, 37.604092)
                },
                null
        ));
        add(new TestCaseData(
                "Lanes",
                "Don't show lane sign where all straight ahead lines are highlighted",
                new Point[]{
                        new Point(55.764318, 37.536759),
                        new Point(55.767853, 37.538713)
                },
                new Point[]{
                        new Point(55.764318, 37.536759),
                        new Point(55.767853, 37.538713)
                },
                null
        ));
        add(new TestCaseData(
                "Lanes",
                "Show lanes when going straight on highway",
                new Point[]{
                        new Point(55.742984, 37.542315),
                        new Point(55.739143, 37.530301)
                },
                new Point[]{
                        new Point(55.742984, 37.542315),
                        new Point(55.739143, 37.530301)
                },
                null
        ));
        add(new TestCaseData(
                "Parking",
                "Show parking route when near finish",
                new Point[]{
                        new Point(55.738017, 37.595262),
                        new Point(55.733423, 37.587495)
                },
                new Point[]{
                        new Point(55.738017, 37.595262),
                        new Point(55.733423, 37.587495)
                },
                null,
                null,
                true
        ));
        add(new TestCaseData(
                "Street annotations",
                "Simple guidance test with street names in annotations",
                new Point[]{
                        new Point(55.734002, 37.578354),
                        new Point(55.731605, 37.577796),
                },
                new Point[]{
                        new Point(55.734002, 37.578354),
                        new Point(55.731605, 37.577796),
                },
                null
        ));
        add(new TestCaseData(
                "Danger road event",
                "Annotation test of danger road event",
                new Point[]{
                    new Point(58.690407, 52.949756),
                    new Point(58.665748, 52.962151),
                },
                new Point[]{
                    new Point(58.690407, 52.949756),
                    new Point(58.665748, 52.962151),
                },
                null
        ));
        add(new TestCaseData(
                "Specific danger road event",
                "Annotation test of specific danger road event",
                new Point[]{
                    new Point(58.650818, 53.060747),
                    new Point(58.655202, 52.989908),
                },
                new Point[]{
                    new Point(58.650818, 53.060747),
                    new Point(58.655202, 52.989908),
                },
                null
        ));
        add(new TestCaseData(
                "School road event",
                "Annotation test of school road event",
                new Point[]{
                    new Point(56.336139, 44.113948),
                    new Point(56.336473, 44.116341),
                },
                new Point[]{
                    new Point(56.336139, 44.113948),
                    new Point(56.336473, 44.116341),
                },
                null
        ));
        add(new TestCaseData(
                "Accident & reconstruction road event",
                "Annotation test of accident and reconstruction road event",
                new Point[]{
                    new Point(60.230107, 75.192658),
                    new Point(60.231624, 75.180546),
                },
                new Point[]{
                    new Point(60.230107, 75.192658),
                    new Point(60.231624, 75.180546),
                },
                null
        ));
        add(new TestCaseData(
                "Control road events",
                "Annotation test of road control events",
                new Point[]{
                    new Point(58.580448, 53.014451),
                    new Point(58.625393, 53.019123),
                },
                new Point[]{
                    new Point(58.580448, 53.014451),
                    new Point(58.625393, 53.019123),
                },
                null
        ));
        add(new TestCaseData(
                "SpeedControl mixed with other tags",
                "Annotation test of road control events",
                new Point[]{
                    new Point(58.652223, 53.068916),
                    new Point(58.651832, 53.047783),
                },
                new Point[]{
                    new Point(58.652223, 53.068916),
                    new Point(58.651832, 53.047783),
                },
                null
        ));
        add(new TestCaseData(
                "Control road events without route",
                "Annotation test of road control events",
                new Point[]{},
                new Point[]{
                    new Point(58.580448, 53.014451),
                    new Point(58.625393, 53.019123),
                },
                null
        ));
        add(new TestCaseData(
                "Multiple control road events",
                "Annotation test of road control events",
                new Point[]{
                    new Point(58.476380, 52.993517),
                    new Point(58.481975, 52.994511),
                },
                new Point[]{
                    new Point(58.476380, 52.993517),
                    new Point(58.481975, 52.994511),
                },
                null
        ));
        add(new TestCaseData(
                "Danger road event without route",
                "Annotation test of danger road event",
                new Point[]{},
                new Point[]{
                    new Point(58.690407, 52.949756),
                    new Point(58.665748, 52.962151),
                },
                null
        ));
        add(new TestCaseData(
                "School road event without route",
                "Annotation test of school road event",
                new Point[]{},
                new Point[]{
                    new Point(56.336139, 44.113948),
                    new Point(56.336473, 44.116341),
                },
                null
        ));
        add(new TestCaseData(
                "Accident & reconstruction road event without route",
                "Annotation test of accident and reconstruction road event",
                new Point[]{},
                new Point[]{
                        new Point(60.230107, 75.192658),
                        new Point(60.231624, 75.180546),
                },
                null
        ));
        add(new TestCaseData(
                "After end simulation",
                "Simulation with driving after route end",
                new Point[]{
                        new Point(55.731724, 37.591741),
                        new Point(55.728655, 37.584019),
                },
                new Point[]{
                        new Point(55.731724, 37.591741),
                        new Point(55.693623, 37.536428),
                },
                null
        ));
        add(new TestCaseData(
                "Alternatives after finish",
                "Route past finish with alternatives. Different behavior depending on continue_routing_after_finish experiment",
                new RequestPoint[]{
                        new RequestPoint(
                                new Point(54.643561, 56.208942),
                                RequestPointType.WAYPOINT,
                                null /* pointContext */
                        ),
                        new RequestPoint(
                                new Point(54.650653, 56.212698),
                                RequestPointType.WAYPOINT,
                                PointContextKt.encode(
                                        "v1||56.218998,54.652554,X;56.202380,54.658962,Y;56.215482,54.666596,Z")
                        )
                },
                new Point[]{
                        new Point(54.643561, 56.208942),
                        new Point(54.650653, 56.212698),
                        new Point(54.652554, 56.218998),
                        new Point(54.649708, 56.215836)
                },
                null,
                null,
                false
        ));
        add(new TestCaseData(
                "Frequent rerouting",
                "Suppress frequent \"route recalculated\" phrases",
                new Point[]{
                    new Point(55.821790, 37.573416),
                    new Point(55.831591, 37.573036),
                    new Point(55.840052, 37.548710)
                },
                new Point[]{
                    new Point(55.821790, 37.573416),
                    new Point(55.826027, 37.571491),
                    new Point(55.824789, 37.571524)
                },
                null,
                new BestRouteWithNoViaPoints()
        ));
        add(new TestCaseData(
                "Rugged road",
                "Rugged road on route",
                new Point[]{
                    new Point(55.631747, 37.980460),
                    new Point(55.631951, 37.976806),
                    new Point(55.632078, 37.972946)
                },
                new Point[]{
                    new Point(55.631747, 37.980460),
                    new Point(55.631951, 37.976806),
                    new Point(55.632078, 37.972946)
                },
                null
        ));
        add(new TestCaseData(
                "Toll road",
                "Toll road on route",
                new Point[]{
                    new Point(55.169469, 37.917044),
                    new Point(55.166716, 37.916602)
                },
                new Point[]{
                    new Point(55.169469, 37.917044),
                    new Point(55.166716, 37.916602)
                },
                null
        ));
        add(new TestCaseData(
                "Toll road rerouting",
                "Check annotation after rerouting to a route with toll roads",
                new Point[]{
                    new Point(55.743692, 37.370303),
                    new Point(55.699538, 37.345557),
                    new Point(55.681108, 37.275732)
                },
                new Point[]{
                    new Point(55.743692, 37.370303),
                    new Point(55.715235, 37.318314),
                    new Point(55.681108, 37.275732)
                },
                null,
                new BestRouteWithNoViaPoints()
        ));
        add(new TestCaseData(
                "Toll road ahead",
                "Check annotation while approaching a toll road",
                new Point[]{
                    new Point(55.934171, 37.452455),
                    new Point(55.937197, 37.450548)
                },
                new Point[]{
                    new Point(55.934171, 37.452455),
                    new Point(55.937197, 37.450548)
                },
                null
        ));
        add(new TestCaseData(
            "Waypoint annotation",
            "Show an annotation after passing a waypoint",
            new RequestPoint[]{
                new RequestPoint(
                        new Point(55.733897, 37.586789),
                        RequestPointType.WAYPOINT,
                        null
                ),
                new RequestPoint(
                        new Point(55.735293, 37.584805),
                        RequestPointType.WAYPOINT,
                        null
                ),
                new RequestPoint(
                        new Point(55.736149, 37.586441),
                        RequestPointType.WAYPOINT,
                        null
                ),
                new RequestPoint(
                        new Point(55.734864, 37.588449),
                        RequestPointType.WAYPOINT,
                        null
                )
            },
            new Point[]{
                new Point(55.733897, 37.586789),
                new Point(55.735293, 37.584805),
                new Point(55.736149, 37.586441),
                new Point(55.734864, 37.588449)
            },
            null,
            null,
            false
        ));
        add(new TestCaseData(
            "Drive past waypoint",
            "Do not lose waypoint when it's driven past",
            new RequestPoint[]{
                new RequestPoint(
                        new Point(56.277736, 43.908864),
                        RequestPointType.WAYPOINT,
                        null
                ),
                new RequestPoint(
                        new Point(56.277454, 43.906991),
                        RequestPointType.WAYPOINT,
                        null
                ),
                new RequestPoint(
                        new Point(56.280089, 43.905819),
                        RequestPointType.WAYPOINT,
                        null
                )
            },
            new Point[]{
                new Point(56.277736, 43.908864),
                new Point(56.280089, 43.905819),
            },
            null,
            null,
            false
        ));
        add(new TestCaseData(
            "Reroute to the finish",
            "Reroute after passing waypoint",
            new RequestPoint[]{
                new RequestPoint(
                    new Point(55.747305, 37.582627),
                    RequestPointType.WAYPOINT,
                    null
                ),
                new RequestPoint(
                    new Point(55.749752, 37.583101),
                    RequestPointType.WAYPOINT,
                    null
                ),
                new RequestPoint(
                    new Point(55.748791, 37.588282),
                    RequestPointType.WAYPOINT,
                    null
                )
            },
            new Point[]{
                new Point(55.747305, 37.582627),
                new Point(55.748791, 37.588282),
            },
            null,
            null,
            false
        ));
        add(new TestCaseData(
                "Waypoint alternatives",
                "Reset alternatives after passing waypoint",
                new RequestPoint[]{
                        new RequestPoint(
                                new Point(55.695075, 37.565611),
                                RequestPointType.WAYPOINT,
                                null
                        ),
                        new RequestPoint(
                                new Point(55.696309, 37.563336),
                                RequestPointType.WAYPOINT,
                                null
                        ),
                        new RequestPoint(
                                new Point(55.698485, 37.568554),
                                RequestPointType.VIAPOINT,
                                null
                        ),
                        new RequestPoint(
                                new Point(55.698286, 37.573339),
                                RequestPointType.VIAPOINT,
                                null
                        ),
                        new RequestPoint(
                                new Point(55.701618, 37.574637),
                                RequestPointType.VIAPOINT,
                                null
                        ),
                        new RequestPoint(
                                new Point(55.703843, 37.574011),
                                RequestPointType.WAYPOINT,
                                null
                        )
                },
                new Point[]{
                        new Point(55.695075, 37.565611),
                        new Point(55.696309, 37.563336),
                        new Point(55.698485, 37.568554),
                        new Point(55.698286, 37.573339),
                        new Point(55.701618, 37.574637),
                        new Point(55.703843, 37.574011),
                },
                null,
                new BestRouteWithNoViaPoints(),
                false
        ));
        add(new TestCaseData(
            "Direction signs with icon",
            "Icon + toponym + road",
            new Point[]{
                new Point(55.591287, 37.729130),
                new Point(55.591476, 37.727419)
            },
            new Point[]{
                new Point(55.591287, 37.729130),
                new Point(55.591476, 37.727419)
            },
            null
        ));
        add(new TestCaseData(
            "Direction signs with exit",
            "Exit + toponym + road",
            new Point[]{
                new Point(55.572580, 37.650284),
                new Point(55.572121, 37.652035)
            },
            new Point[]{
                new Point(55.572580, 37.650284),
                new Point(55.572121, 37.652035)
            },
            null
        ));
        add(new TestCaseData(
            "Unknown speed limit",
            "With route and without route",
            new Point[]{
                new Point(54.069516, 37.593124),
                new Point(54.068607, 37.598874)
            },
            new Point[]{
                new Point(54.069516, 37.593124),
                new Point(54.068607, 37.598874),
                new Point(54.069497, 37.600065),
                new Point(54.068102, 37.598112)
            },
            null
        ));

        add(new TestCaseData(
                "Weight constraints 20",
                "Weight constraints",
                new Point[]{
                        new Point(55.807285, 38.160756),
                        new Point(55.806055, 38.153101)
                },
                new Point[]{
                        new Point(55.807285, 38.160756),
                        new Point(55.806055, 38.153101)
                },
                null
        ));

        add(new TestCaseData(
                "Height constraints 2.5",
                "Height constraints",
                new Point[]{
                        new Point(55.621866, 37.678729),
                        new Point(55.622192, 37.673371)
                },
                new Point[]{
                        new Point(55.621866, 37.678729),
                        new Point(55.622192, 37.673371)
                },
                null
        ));

        add(new TestCaseData(
                "Tunnel on Kutuzovsky Prospekt with height 4.3",
                "Route goes into the tunnel",
                new Point[]{
                        new Point(55.742841, 37.542821),
                        new Point(55.748294, 37.559976)
                },
                new Point[]{
                        new Point(55.742841, 37.542821),
                        new Point(55.748294, 37.559976)
                },
                null
        ));

        add(new TestCaseData(
                "Custom annotations/Lva Tolstogo street",
                "Route to test custom annotations about the office of Yandex and pedestrians",
                new Point[]{
                        new Point(55.737515, 37.584184),
                        new Point(55.734275, 37.586265)
                },
                new Point[]{
                        new Point(55.737515, 37.584184),
                        new Point(55.734275, 37.586265)
                },
                null
        ));
    }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.guidance_test_cases);
        super.onCreate(savedInstanceState);
        language = (AnnotationLanguage)getIntent().getSerializableExtra("language");
        vehicleOptions = Serialization.deserializeFromBytes(
            (byte[])getIntent().getSerializableExtra("vehicle_options"),
            VehicleOptions.class);
        testCasesList = (ListView) findViewById(R.id.guidance_test_cases_list);
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
                TestCaseFactory factory = new TestCaseFactory(getApplicationContext(), language);
                TestCaseData testCaseData = testCasesAdapter.getItem(position);
                if (testCaseData.vehicleOptions == null) {
                    testCaseData.vehicleOptions = vehicleOptions;
                }
                try {
                    factory.createTestCase(
                        testCaseData,
                        new TestCaseFactory.TestCaseFactoryCallback() {
                            @Override
                            public void onTestCaseCreated(TestCase testCase) {
                                Intent intent = new Intent();
                                intent.putExtra("testCase", testCase);
                                setResult(RESULT_OK, intent);
                                hideOverlay();
                                finish();
                            }
                        });
                } catch (IOException e) {
                    Utils.showMessage(getApplicationContext(), e.getMessage());
                    hideOverlay();
                }
            }
        });

        updateListView("");
    }
}
