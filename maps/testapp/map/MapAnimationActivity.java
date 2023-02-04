package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.animation.ValueAnimator;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Callback;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapObjectVisitor;
import com.yandex.mapkit.map.ModelStyle;
import com.yandex.mapkit.map.PlacemarkAnimation;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.AnimatedImageProvider;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.image.AnimatedImage;
import com.yandex.runtime.model.AnimatedModel;
import com.yandex.runtime.model.AnimatedModelProvider;
import com.yandex.runtime.model.ModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MapAnimationActivity extends MapBaseActivity {
    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    private ImageProvider imageProvider;
    private MapObjectCollection animatedPlacemarksCollection;
    private MapObjectCollection mapObjectCollection;
    private List<PlacemarkMapObject> bindingPerformanceTestPlacemarks = new ArrayList<PlacemarkMapObject>();
    private ValueAnimator animation = null;
    private TextView timeView;
    private MapObjectTapListener objectTapListener;
    private InputListener mapTapListener;
    private ImageProvider redCircleImageProvider;

    private AnimatedImageProvider createRedToYellowAnimation() {
        final int FRAME_DURATION = 200;
        AnimatedImage image = new AnimatedImage(1);
        image.addFrame(redCircleImageProvider, FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights1), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights2), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights3), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights4), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights5), FRAME_DURATION);
        return AnimatedImageProvider.fromAnimatedImage(image);
    }

    private AnimatedImageProvider createYellowToGreenAnimation() {
        final int FRAME_DURATION = 200;
        AnimatedImage image = new AnimatedImage(1);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights5), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights6), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights7), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights8), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights9), FRAME_DURATION);
        image.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights10), FRAME_DURATION);
        return AnimatedImageProvider.fromAnimatedImage(image);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_animation);
        super.onCreate(savedInstanceState);
        mapview.getMap().setFastTapEnabled(true);

        animatedPlacemarksCollection = mapview.getMap().getMapObjects().addCollection();
        mapObjectCollection = mapview.getMap().getMapObjects().addCollection();
        imageProvider = ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a0);
        redCircleImageProvider = ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.traffic_lights0);

        timeView = (TextView) findViewById(R.id.time_view);

        EditText placemarkNumEdit = (EditText) findViewById(R.id.placemark_num);
        placemarkNumEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                try {
                    String text = view.getText().toString();
                    int placemarkNum = text.isEmpty() ? 0 : Integer.parseInt(text);
                    setPlacemarkNum(placemarkNum);
                } catch (NumberFormatException e) {
                    LOGGER.severe("Number format exception");
                }
                return false;
            }
        });

        Point point = mapview.getMap().getCameraPosition().getTarget();

        AnimatedImageProvider imageProvider = AnimatedImageProvider.fromAsset(this, "nyan_cat.png");
        final PlacemarkMapObject infinitePlacemark = animatedPlacemarksCollection.addPlacemark(
                point, imageProvider, new IconStyle());
        infinitePlacemark.useAnimation().play();

        final int OCTOPUS_FRAME_DURATION = 20;
        AnimatedImage octopus = new AnimatedImage(AnimatedImage.INFINITE_ANIMATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a0), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a0), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a1), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a2), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a3), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a4), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a5), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a6), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a7), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a8), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a9), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a10), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a11), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a12), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a13), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a14), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a15), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a16), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a17), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a18), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a19), OCTOPUS_FRAME_DURATION);
        octopus.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.a20), OCTOPUS_FRAME_DURATION);
        AnimatedImageProvider octopusProvider = AnimatedImageProvider.fromAnimatedImage(octopus);
        final PlacemarkMapObject octopusPlacemark = animatedPlacemarksCollection.addPlacemark(
                new Point(point.getLatitude(), point.getLongitude() + 0.005),
                octopusProvider, new IconStyle());
        octopusPlacemark.useAnimation().play();

        final int RAINBOW_FRAME_DURATION = 400;
        AnimatedImage rainbow = new AnimatedImage(1);
        rainbow.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.rainbow0), RAINBOW_FRAME_DURATION);
        rainbow.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.rainbow1), RAINBOW_FRAME_DURATION);
        rainbow.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.rainbow2), RAINBOW_FRAME_DURATION);
        rainbow.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.rainbow3), RAINBOW_FRAME_DURATION);
        rainbow.addFrame(ImageProvider.fromResource(MapAnimationActivity.this, R.drawable.rainbow4), RAINBOW_FRAME_DURATION);
        AnimatedImageProvider rainbowProvider = AnimatedImageProvider.fromAnimatedImage(rainbow);
        final PlacemarkMapObject rainbowPlacemark = animatedPlacemarksCollection.addPlacemark(
                new Point(point.getLatitude() - 0.0035, point.getLongitude()),
                rainbowProvider, new IconStyle());

        final PlacemarkMapObject trafficLightPlacemark = animatedPlacemarksCollection.addPlacemark(
                new Point(point.getLatitude() + 0.0035, point.getLongitude()),
                redCircleImageProvider, new IconStyle());

        objectTapListener = new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(MapObject mapObject, Point point) {
                if (mapObject instanceof PlacemarkMapObject) {
                    final PlacemarkMapObject placemark = (PlacemarkMapObject) mapObject;
                    final PlacemarkAnimation animation = placemark.useAnimation();
                    if (placemark == rainbowPlacemark) {
                        animation.stop();
                        animation.play();
                        return true;
                    }
                    if (placemark == trafficLightPlacemark) {
                        animation.setIcon(
                            createRedToYellowAnimation(),
                            new IconStyle(),
                            new Callback() {
                                @Override
                                public void onTaskFinished() {
                                    if (!animation.isValid()) {
                                        return;
                                    }
                                    animation.play(new Callback() {
                                        @Override
                                        public void onTaskFinished() {
                                            if (!animation.isValid()) {
                                                return;
                                            }
                                            animation.setIcon(
                                                createYellowToGreenAnimation(),
                                                new IconStyle(),
                                                new Callback() {
                                                    @Override
                                                    public void onTaskFinished() {
                                                        if (!animation.isValid()) {
                                                            return;
                                                        }
                                                        animation.play(new Callback() {
                                                            @Override
                                                            public void onTaskFinished() {
                                                                placemark.setIcon(redCircleImageProvider);
                                                            }
                                                        });
                                                    }
                                                });
                                        }
                                    });
                                }
                            });
                        return true;
                    }
                    animation.setReversed(!animation.isReversed());
                    if (animation.isReversed()) {
                        animation.play(new Callback() {
                            @Override
                            public void onTaskFinished() {
                                animatedPlacemarksCollection.remove(placemark);
                            }
                        });
                    } else {
                        animation.play();
                    }
                    return true;
                }
                return false;
            }
        };
        animatedPlacemarksCollection.addTapListener(objectTapListener);

        mapTapListener = new InputListener() {
            private final AnimatedImageProvider imageProvider = AnimatedImageProvider.fromAsset(
                    MapAnimationActivity.this, "nyan_cat_slow_one_loop.png");

            @Override
            public void onMapTap(Map map, Point point) {
                PlacemarkMapObject placemark = animatedPlacemarksCollection.addEmptyPlacemark(point);
                final PlacemarkAnimation animatedIcon = placemark.useAnimation();
                animatedIcon.setIcon(imageProvider, new IconStyle(), new Callback() {
                    @Override
                    public void onTaskFinished() {
                        animatedIcon.play();
                    }
                });
            }

            @Override
            public void onMapLongTap(Map map, Point point) {
            }
        };
        mapview.getMap().addInputListener(mapTapListener);

        MapObjectCollection nonTappableMapObjects = animatedPlacemarksCollection.addCollection();
        AnimatedModel animatedModel = new AnimatedModel(AnimatedModel.INFINITE_ANIMATION);
        final int FRAME_DURATION = 1000;
        animatedModel.addFrame(
                ModelProvider.fromResource(
                        this, R.raw.camaro, ImageProvider.fromResource(this, R.drawable.camaro)),
                FRAME_DURATION);
        animatedModel.addFrame(
                ModelProvider.fromResource(
                        this, R.raw.navi, ImageProvider.fromResource(this, R.drawable.navi)),
                FRAME_DURATION);
        AnimatedModelProvider animatedModelProvider = AnimatedModelProvider.fromAnimatedModel(
                animatedModel);
        final float MODEL_SIZE = 100;
        PlacemarkMapObject animatedModelPlacemark = nonTappableMapObjects.addPlacemark(
            new Point(point.getLatitude(), point.getLongitude() - 0.005),
            animatedModelProvider,
            new ModelStyle(MODEL_SIZE, ModelStyle.UnitType.NORMALIZED, ModelStyle.RenderMode.USER_MODEL));
        animatedModelPlacemark.useAnimation().play();
    }

    private static class PlacemarkDescriptor {
        private final PlacemarkMapObject mapObject;
        private final Point startGeometry;

        private PlacemarkDescriptor(PlacemarkMapObject mapObject) {
            this.mapObject = mapObject;
            this.startGeometry = mapObject.getGeometry();
        }
    }

    private static class AnimationVisitor implements MapObjectVisitor {
        private final boolean shouldResume;

        public AnimationVisitor(boolean shouldResume) {
            this.shouldResume = shouldResume;
        }

        @Override
        public void onPlacemarkVisited(PlacemarkMapObject placemark) {
            PlacemarkAnimation animation = placemark.useAnimation();
            if (shouldResume) {
                animation.resume();
            } else {
                animation.pause();
            }
        }

        @Override
        public void onPolylineVisited(PolylineMapObject polyline) {
        }

        @Override
        public void onPolygonVisited(PolygonMapObject polygon) {
        }

        @Override
        public void onCircleVisited(CircleMapObject circle) {
        }

        @Override
        public boolean onCollectionVisitStart(MapObjectCollection collection) {
            return true;
        }

        @Override
        public void onCollectionVisitEnd(MapObjectCollection collection) {
        }

        @Override
        public boolean onClusterizedCollectionVisitStart(ClusterizedPlacemarkCollection collection) {
            return true;
        }

        @Override
        public void onClusterizedCollectionVisitEnd(ClusterizedPlacemarkCollection collection) {
        }
    }

    public void startAnimation(View view) {
        if (!bindingPerformanceTestPlacemarks.isEmpty()) {
            startBindingPerformanceTest();
        }
        animatedPlacemarksCollection.traverse(new AnimationVisitor(true));
    }

    public void stopAnimation(View view) {
        if (!bindingPerformanceTestPlacemarks.isEmpty()) {
            stopBindingPerformanceTest();
        }
        animatedPlacemarksCollection.traverse(new AnimationVisitor(false));
    }

    private void startBindingPerformanceTest() {
        final long DURATION = 10000;
        final double LATITUDE_SPEED = -0.0003 / 1000.0;
        final double LONGITUDE_SPEED = 0.0006 / 1000.0;
        final float ROTATION_SPEED = 360.f / DURATION;

        if (animation != null) {
            return;
        }
        timeView.setText(getResources().getString(R.string.no_time));
        initPosition();
        final long startTime = SystemClock.uptimeMillis();
        final List<PlacemarkDescriptor> animatedPlacemarks = new ArrayList<PlacemarkDescriptor>(bindingPerformanceTestPlacemarks.size());
        for (PlacemarkMapObject placemark : bindingPerformanceTestPlacemarks) {
            animatedPlacemarks.add(new PlacemarkDescriptor(placemark));
        }

        animation = new ValueAnimator();
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            long workTime = 0;
            int stepCount = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                long time = SystemClock.uptimeMillis();

                long elapsed = animation.getCurrentPlayTime();
                float direction = ROTATION_SPEED * elapsed;

                for (PlacemarkDescriptor placemark : animatedPlacemarks) {
                    placemark.mapObject.setDirection(direction);
                    double latitude = placemark.startGeometry.getLatitude() + elapsed * LATITUDE_SPEED;
                    double longitude = placemark.startGeometry.getLongitude() + elapsed * LONGITUDE_SPEED;
                    placemark.mapObject.setGeometry(new Point(latitude, longitude));
                }

                long finishWorkTime = SystemClock.uptimeMillis();
                workTime += finishWorkTime - time;
                stepCount++;
                if (stepCount == 1000) {
                    double averageTimePerStep = workTime / (double) stepCount;
                    timeView.setText(getResources().getString(R.string.time_per_step, averageTimePerStep));
                    workTime = 0;
                    stepCount = 0;
                }
            }
        });
        animation.setFloatValues(0.f, 1.f);
        animation.setDuration(DURATION);
        animation.setRepeatCount(ValueAnimator.INFINITE);
        animation.setRepeatMode(ValueAnimator.RESTART);
        animation.start();
    }

    void stopBindingPerformanceTest() {
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    private void setPlacemarkNum(int placemarkNum) {
        if (placemarkNum == bindingPerformanceTestPlacemarks.size()) {
            return;
        }
        stopBindingPerformanceTest();
        for (int i = bindingPerformanceTestPlacemarks.size() - 1; i >= placemarkNum; --i) {
            PlacemarkMapObject placemark = bindingPerformanceTestPlacemarks.get(i);
            bindingPerformanceTestPlacemarks.remove(i);
            mapObjectCollection.remove(placemark);
        }
        while (bindingPerformanceTestPlacemarks.size() < placemarkNum) {
            PlacemarkMapObject placemark = mapObjectCollection.addPlacemark(
                new Point(0.0, 0.0), imageProvider, new IconStyle().setRotationType(RotationType.ROTATE));
            bindingPerformanceTestPlacemarks.add(placemark);
        }
        initPosition();
    }

    private void initPosition() {
        final double LEFT_LONGITUDE = 30.313;
        final double STEP_LONGITUDE = 0.004;
        final double TOP_LATITUDE = 59.956;
        final double STEP_LATITUDE = -0.002;
        final int ROW_SIZE = 10;

        for (int i = 0; i < bindingPerformanceTestPlacemarks.size(); ++i) {
            int y = i / ROW_SIZE;
            int x = i % ROW_SIZE;
            double longitude = LEFT_LONGITUDE + x * STEP_LONGITUDE;
            double latitude = TOP_LATITUDE + y * STEP_LATITUDE;
            PlacemarkMapObject placemark = bindingPerformanceTestPlacemarks.get(i);
            placemark.setGeometry(new Point(latitude, longitude));
            placemark.setDirection(0.0f);
        }
    }
}
