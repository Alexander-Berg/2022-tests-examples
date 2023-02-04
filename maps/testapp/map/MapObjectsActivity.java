package com.yandex.maps.testapp.map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.ConflictResolutionMode;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.LinearRing;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polygon;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.map.Arrow;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectDragListener;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapObjectVisitor;
import com.yandex.mapkit.map.ModelStyle;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.TextStyle;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PlacemarksStyler;
import com.yandex.mapkit.map.CompositeIcon;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.Rect;
import com.yandex.mapkit.map.Sublayer;
import com.yandex.mapkit.map.SublayerFeatureType;
import com.yandex.mapkit.map.SublayerManager;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.image.AnimatedImageProvider;
import com.yandex.runtime.model.ModelProvider;
import com.yandex.runtime.ui_view.ViewProvider;

import com.yandex.maps.testapp.driving.IconWithLetter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static com.yandex.maps.testapp.map.BitmapHelpers.createFilledCircle;
import static com.yandex.maps.testapp.map.BitmapHelpers.createFilledRect;
import static com.yandex.maps.testapp.map.BitmapHelpers.getDensity;

public class MapObjectsActivity extends MapBaseActivity {
    private MapView mapview;

    private static final Logger LOGGER =
            Logger.getLogger("yandex.maps");

    private Handler animationHandler;

    private ImageProvider tappedIconImage;
    private static final float DEFAULT_MODEL_SIZE = 75.f;

    private MapObjectTapListener mapObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            if (origin instanceof PlacemarkMapObject) {
                ((PlacemarkMapObject)origin).setIcon(tappedIconImage);
            } else if (origin instanceof CircleMapObject) {
                CircleMapObject circle = (CircleMapObject)origin;
                radiusScale =  1.f / radiusScale;
                Circle curGeometry = circle.getGeometry();
                Circle newGeometry = new Circle(curGeometry.getCenter(), curGeometry.getRadius() * radiusScale);
                circle.setGeometry(newGeometry);
            } else {
                origin.setVisible(false);
            }
            return true;
        }

        private float radiusScale = 1.5f;
    };

    private MapObjectTapListener mapObjectTextTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            if (origin instanceof PlacemarkMapObject) {
                int i = ++index % 4;
                ((PlacemarkMapObject)origin).setText(texts[i], styles[i]);
            }
            return true;
        }
        private TextStyle[] styles = {
            new TextStyle().setColor(Color.RED).setOutlineColor(Color.WHITE).setSize(8)
                    .setOffset(0).setPlacement(TextStyle.Placement.TOP).setTextOptional(false),
            new TextStyle().setColor(Color.GREEN).setOutlineColor(Color.BLACK).setSize(10)
                    .setOffset(5).setPlacement(TextStyle.Placement.RIGHT).setTextOptional(true),
            new TextStyle().setColor(Color.BLUE).setOutlineColor(Color.WHITE).setSize(12)
                    .setOffset(10).setPlacement(TextStyle.Placement.BOTTOM).setTextOptional(false),
            new TextStyle().setColor(Color.YELLOW).setOutlineColor(Color.BLACK).setSize(10)
                    .setOffset(5).setPlacement(TextStyle.Placement.LEFT).setTextOptional(true),
        };
        private String[] texts = {
            "Color:RED; OutlineColor:WHITE; Size:8; Placement:TOP; Offset:0; TextOptional:false;",
            "Color:GREEN; OutlineColor:BLACK; Size:10; Placement:RIGH; Offset:5; TextOptional:true;",
            "Color:BLUE; OutlineColor:WHITE; Size:12; Placement:BOTTOM; Offset:10; TextOptional:false;",
            "Color:YELLOW; OutlineColor:BLACK; Size:10; Placement:LEFT; Offset:5; TextOptional:true;"
        };
        private int index = 0;
    };

    private MapObjectTapListener modelTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            tapCount++;
            ModelStyle modelStyle = new ModelStyle(
                MapObjectsActivity.DEFAULT_MODEL_SIZE * (tapCount % 2 + 1),
                ModelStyle.UnitType.NORMALIZED,
                ModelStyle.RenderMode.USER_MODEL);
            ((PlacemarkMapObject)origin).setModelStyle(modelStyle);
            return true;
        }

        private int tapCount = 0;
    };

    private final MapObjectTapListener rotatePlacemarkObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            if (origin instanceof PlacemarkMapObject) {
                PlacemarkMapObject p = (PlacemarkMapObject)origin;
                p.setDirection(p.getDirection() + 15);
            }
            return true;
        }
    };

    private final MapObjectTapListener changeOpacityPlacemarkObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            if (origin instanceof PlacemarkMapObject) {
                PlacemarkMapObject p = (PlacemarkMapObject)origin;
                p.setOpacity(1.5f - p.getOpacity());
            }
            return true;
        }
    };

    private MapObjectTapListener changePolylineZlevelObjectTapListener;
    private MapObjectTapListener inverseVisbilityTapListener;

    private final MapObjectDragListener mapObjectDragListener = new MapObjectDragListener() {
        @Override
        public void onMapObjectDragStart(MapObject origin) {
            LOGGER.info("onMapObjectDragStart");
        }

        @Override
        public void onMapObjectDrag(MapObject origin, Point point) {
            LOGGER.info("onMapObjectDrag: position=("
                    + String.valueOf(point.getLatitude()) + ", "
                    + String.valueOf(point.getLongitude()) + ")");
        }

        @Override
        public void onMapObjectDragEnd(MapObject origin) {
            LOGGER.info("onMapObjectDragEnd");
        }
    };

    private ImageProvider[] iconImages;
    private TextStyle[] iconTextStyles = {
            new TextStyle().setColor(Color.RED).setOutlineColor(Color.WHITE),
            new TextStyle().setColor(Color.GREEN).setOutlineColor(Color.WHITE),
            new TextStyle().setColor(Color.BLUE).setOutlineColor(Color.WHITE),
    };
    private MapObjectCollection longTapPlacemarks;

    private final InputListener inputListener = new InputListener() {
        private int index = 0;

        @Override
        public void onMapTap(Map map, Point position) {
            LOGGER.info("onMapTap");
            PlacemarkMapObject p = mapview.getMap().getMapObjects().addPlacemark(position);
            p.setZIndex(100);
            p.setText("Осьминожка", new TextStyle().setColor(0xffd90b86).setPlacement(TextStyle.Placement.BOTTOM).setSize(10));
            animatePlacemark(p);
        }

        @Override
        public void onMapLongTap(Map map, Point position) {
            PlacemarkMapObject p = longTapPlacemarks.addPlacemark(position);
            int i = index++ % 3;
            p.setIcon(iconImages[i]);
            p.setZIndex(-100);
            p.setText("ABC", iconTextStyles[i]);
        }
    };

    private MovableTapListener movableCircleTapListener;
    private MovableTapListener movablePlacemarkTapListener;
    private MovableTapListener scaleablePlacemarkTapListener;
    private float placemarkScale;

    private static class AnimationManager {
        private ArrayList<Animator> animators = new ArrayList<Animator>();

        public Animator createAnimator(Runnable task) {
            Animator animator = new Animator(task);
            animators.add(animator);
            return animator;
        }

        public void onDestroy() {
            for (Animator animator : animators) {
                animator.setStarted(false);
            }
        }
    }

    private AnimationManager animationManager = new AnimationManager();

    private static class Animator implements Runnable {
        private boolean started;

        private Runnable task;
        private Handler handler;

        private Animator(Runnable task) {
            this.task = task;
            handler = new Handler();
            started = false;
        }

        public void setStarted(boolean start) {
            if (start == this.started) {
                return;
            }

            if (start) {
                handler.post(this);
            }

            this.started = start;
        }

        public boolean started() { return started; }

        @Override
        public void run() {
            if (!started) {
                return;
            }

            task.run();
            handler.postDelayed(this, 16);
        }
    }

    private class MovableTapListener implements MapObjectTapListener {
        private Animator animator;

        MovableTapListener(Animator animator) {
            this.animator = animator;
        }

        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            animator.setStarted(!animator.started());
            return true;
        }
    }

    private ImageProvider[] animationFrames;

    private void createAnimationFrames() {
        animationFrames = new ImageProvider[] {
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a0),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a1),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a2),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a3),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a4),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a5),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a6),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a7),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a8),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a9),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a10),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a11),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a12),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a13),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a14),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a15),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a16),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a17),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a18),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a19),
            ImageProvider.fromResource(MapObjectsActivity.this, R.drawable.a20)
        };
    }

    private void animatePlacemark(final PlacemarkMapObject placemark) {
        animationHandler.post(new Runnable() {
            int frame = 0;

            @Override
            public void run() {
                placemark.setIcon(animationFrames[frame]);
                frame++;
                if (frame < animationFrames.length) {
                    animationHandler.postDelayed(this, 20);
                }
            }
        });
    }

    private void rotatePlacemark(final PlacemarkMapObject placemark) {
        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                placemark.setDirection(placemark.getDirection() + 1);
                animationHandler.postDelayed(this, 100);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_objects);
        super.onCreate(savedInstanceState);
        animationHandler = new Handler();
        mapview = (MapView)findViewById(R.id.mapview);

        tappedIconImage = ImageProvider.fromBitmap(createPlacemarkBitmap(Color.BLUE, 65));
        iconImages = new ImageProvider[]{
            ImageProvider.fromBitmap(createPlacemarkBitmap(Color.RED & 0x50FFFFFF, 50)),
            ImageProvider.fromBitmap(createPlacemarkBitmap(Color.GREEN & 0xA0FFFFFF, 50)),
            ImageProvider.fromBitmap(createPlacemarkBitmap(Color.BLUE & 0xF0FFFFFF, 50))
        };
        longTapPlacemarks = mapview.getMap().addMapObjectLayer("long_tap_placemarks");
        longTapPlacemarks.addTapListener(mapObjectTextTapListener);
        SublayerManager sublayerManager = mapview.getMap().getSublayerManager();
        Sublayer sublayer = sublayerManager.get(sublayerManager.findFirstOf(
                "long_tap_placemarks", SublayerFeatureType.PLACEMARKS_AND_LABELS));
        sublayer.setConflictResolutionMode(ConflictResolutionMode.EQUAL);
        createAnimationFrames();
        createMapObjects();
        createCameraMoveTesting();

        ToggleButton fastTapToggle = (ToggleButton)findViewById(R.id.fastTapToggle);
        fastTapToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setFastTapEnabled(isChecked);
            }
        });
    }

    private Bitmap createPlacemarkBitmap(int color, int size) {
        float density = getDensity(this);
        size = (int)(size * density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);
        Paint p = new Paint();
        float strokeWidth = 10f * density;
        float margin = 2f * density;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        p.setAntiAlias(true);
        Canvas c = new Canvas(bitmap);
        //MARGIN is needed otherwise circle with antialiasing bigger then bitmap.
        c.drawCircle(size / 2, size / 2 , (size - (strokeWidth + margin)) / 2, p);
        return bitmap;
    }

    private Bitmap createRectangleBitmap(int colorTop, int colorBottom, int width, int height) {
        float density = getDensity(this);
        width = (int)(width * density + 0.5);
        height = (int)(height * density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Paint p = new Paint();
        p.setColor(colorTop);
        p.setStyle(Paint.Style.FILL);
        Canvas c = new Canvas(bitmap);
        c.drawRect(0, 0, width, 0.5f * height, p);
        p.setColor(colorBottom);
        c.drawRect(0, 0.5f * height, width, height, p);
        return bitmap;
    }

    private static ArrayList<Point> generateSegmentedLine(
            Point start, Point end, int pointsCount) {
        ArrayList<Point> polyline = new ArrayList<Point>();
        for (int i = 0; i <= pointsCount; ++i)
            polyline.add(new Point(
                start.getLatitude() + i * (end.getLatitude() - start.getLatitude()) / pointsCount,
                start.getLongitude() + i * (end.getLongitude() - start.getLongitude()) / pointsCount));
        return polyline;
    }

    private static Point polarCoordsToLatLon(
            Point center, double latitudeRadius, double longitudeRadius, double angle) {
        return new Point(
            center.getLatitude() + latitudeRadius * Math.sin(angle),
            center.getLongitude() + longitudeRadius * Math.cos(angle));
    }

    private static ArrayList<Point> generateRhomb(
            Point center, double width, double height) {
        ArrayList<Point> polyline = new ArrayList<Point>();
        for (int i = 0; i < 5; ++i) {
            polyline.add(polarCoordsToLatLon(center, 0.5 * height, 0.5 * width, Math.PI * i * 0.5));
        }
        return polyline;
    }

    private void addTappablePlacemark(
            MapObjectCollection collection, double latitude, double longitude, RectF tappableRect, float scale) {
        final int RECT_SIZE = 100;
        RectF filledRect = new RectF(
            tappableRect.left * RECT_SIZE, tappableRect.top * RECT_SIZE,
            tappableRect.right * RECT_SIZE, tappableRect.bottom * RECT_SIZE);
        collection.addPlacemark(
            new Point(latitude, longitude),
            ImageProvider.fromBitmap(createFilledRect(
                Color.GREEN, filledRect, Color.BLACK, RECT_SIZE, getDensity(this))),
            new IconStyle().setScale(scale).setTappableArea(new Rect(
                new PointF(tappableRect.left, tappableRect.top),
                new PointF(tappableRect.right, tappableRect.bottom))));
    }

    private List<PlacemarkMapObject> lettersPlacemarks = new ArrayList<PlacemarkMapObject>();

    private static final double LEFT_LONGITUDE = 30.313;
    private static final double RIGHT_LONGITUDE = 30.325;
    private static final double STEP_LONGITUDE = (RIGHT_LONGITUDE - LEFT_LONGITUDE) / 3;
    private static final double CELL_WIDTH = 0.75 * STEP_LONGITUDE;
    private static final double BOTTOM_LATITUDE = 59.939;
    private static final double TOP_LATITUDE = 59.956;
    private static final double STEP_LATITUDE = (TOP_LATITUDE - BOTTOM_LATITUDE) / 13.0;


    private static class AddPolylineTask extends AsyncTask<Void, Void, Polyline> {
        private WeakReference<MapObjectCollection> weakCollection;

        AddPolylineTask(MapObjectCollection collection) {
            weakCollection = new WeakReference<>(collection);
        }

        @Override
        protected Polyline doInBackground(Void[] voids) {
            // long polyline
            double rowLatitude = BOTTOM_LATITUDE + 2 * STEP_LATITUDE;
            return new Polyline(
                    generateSegmentedLine(
                            new Point(rowLatitude, LEFT_LONGITUDE + 3 * STEP_LONGITUDE),
                            new Point(rowLatitude, LEFT_LONGITUDE + 18.0),
                            10000));
        }

        @Override
        protected void onPostExecute(Polyline polyline) {
            MapObjectCollection mapObjects = weakCollection.get();
            if (mapObjects != null) {
                PolylineMapObject polylineMapObject = mapObjects.addPolyline(polyline);
                polylineMapObject.setStrokeWidth(1.f);
            }
        }
    }

    private void createMapObjects() {
        final Map map = mapview.getMap();
        final MapObjectCollection mapObjects = map.getMapObjects().addCollection();
        final MapObjectCollection notClickableMapObjects = map.getMapObjects().addCollection();

        ImageProvider resourceBackedImage = ImageProvider.fromResource(this, R.drawable.pushpin);
        ImageProvider rectangleImage = ImageProvider.fromBitmap(createRectangleBitmap(Color.BLUE, Color.RED, 40, 80));

        double rowLatitude = BOTTOM_LATITUDE + 5 * STEP_LATITUDE;
        double columnLongitude = LEFT_LONGITUDE - STEP_LONGITUDE;

        for (int i = 0; i < 3; ++i, columnLongitude += STEP_LONGITUDE) {
            PlacemarkMapObject placemark = mapObjects.addPlacemark(new Point(rowLatitude, columnLongitude), resourceBackedImage);
            placemark.setDraggable(true);
            placemark.setDragListener(mapObjectDragListener);
            placemark.setZIndex(30.0f);
        }

        final TextView textView = new TextView(this);
        textView.setText("Hello, World!");
        textView.setTextColor(Color.DKGRAY);
        final ViewProvider viewProvider = new ViewProvider(textView);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);
        final PlacemarkMapObject viewPlacemark = mapObjects.addPlacemark(new Point(rowLatitude + 2 * STEP_LATITUDE, columnLongitude - 3 * STEP_LONGITUDE), viewProvider);

        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText("Big World!");
                viewProvider.snapshot();
                viewPlacemark.setView(viewProvider);
            }
        }, 5000);

        final Random random = new Random();

        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText("Some text version " + random.nextInt(1000));
                viewProvider.snapshot();
                viewPlacemark.setView(viewProvider);
                animationHandler.postDelayed(this, 500);
            }
        }, 5500);

        // rotating rectangle flat placemark
        PlacemarkMapObject rotatingFlatPlacemark = mapObjects.addPlacemark(
                new Point(rowLatitude, columnLongitude),
                rectangleImage,
                new IconStyle().setFlat(true).setRotationType(RotationType.ROTATE));
        rotatingFlatPlacemark.addTapListener(rotatePlacemarkObjectTapListener);
        rotatingFlatPlacemark.setZIndex(-10.0f);

        // rotating rectangle placemark on screen
        columnLongitude += STEP_LONGITUDE;
        PlacemarkMapObject rotatingScreenPlacemark = mapObjects.addPlacemark(
            new Point(rowLatitude, columnLongitude),
                rectangleImage,
                new IconStyle().setRotationType(RotationType.ROTATE));
        rotatingScreenPlacemark.addTapListener(rotatePlacemarkObjectTapListener);
        rotatingScreenPlacemark.setZIndex(-10.0f);

        // placemark with default icon
        columnLongitude += STEP_LONGITUDE;
        mapObjects.addPlacemark(new Point(rowLatitude, columnLongitude));

        // pair of placemarks, on tap hides and moves camera to other placemark
        columnLongitude = LEFT_LONGITUDE + 3 * STEP_LONGITUDE;
        createInversedVisibilityPlacemarks(
            new Point(BOTTOM_LATITUDE + STEP_LATITUDE, columnLongitude),
            new Point(BOTTOM_LATITUDE + 3 * STEP_LATITUDE, columnLongitude));

        // composite placemark
        columnLongitude = LEFT_LONGITUDE + 5 * STEP_LONGITUDE;
        createCompositePlacemark(new Point(rowLatitude, columnLongitude));

        // placemark with scale of zoom function
        List<PointF> scaleFunction = new ArrayList<>();
        for (float scale = 1.0f; scale <= 19.0f; scale += 0.1) {
            scaleFunction.add(new PointF(scale, (float)(scale / 19.0 * 4.0 * (Math.sin(scale) + 1.))));
        }

        columnLongitude = LEFT_LONGITUDE + 9 * STEP_LONGITUDE;
        PlacemarkMapObject scalableByZoomPlacemark = mapObjects.addPlacemark(
            new Point(rowLatitude, columnLongitude),
            ImageProvider.fromBitmap(createRectangleBitmap(Color.RED, Color.GREEN, 30, 30)),
            new IconStyle().setRotationType(RotationType.ROTATE));
        scalableByZoomPlacemark.setScaleFunction(scaleFunction);

        // collection with scale of zoom function for placemark
        MapObjectCollection scalableByZoomCollection = mapObjects.addCollection();
        ImageProvider scalableRectangleImage = ImageProvider.fromBitmap(createRectangleBitmap(Color.BLUE, Color.GREEN, 30, 30));
        IconStyle scalableIconStyle = new IconStyle().setRotationType(RotationType.ROTATE);
        for (int i = 10; i < 15; ++i) {
            columnLongitude = LEFT_LONGITUDE + i * STEP_LONGITUDE;
            scalableByZoomCollection.addPlacemark(
                new Point(rowLatitude, columnLongitude),
                scalableRectangleImage,
                scalableIconStyle);
        }
        PlacemarksStyler styler = scalableByZoomCollection.placemarksStyler();
        styler.setScaleFunction(scaleFunction);

        // intersecting placemarks with equal ZIndex
        columnLongitude = LEFT_LONGITUDE + 7 * STEP_LONGITUDE;
        MapObjectCollection collectionForPlacemark = mapObjects.addCollection();
        collectionForPlacemark.setZIndex(5.0f);
        PlacemarkMapObject intersectingPlacemark1 = collectionForPlacemark.addPlacemark(new Point(rowLatitude - STEP_LATITUDE / 4, columnLongitude), animationFrames[8]);
        intersectingPlacemark1.setZIndex(5.0f);
        PlacemarkMapObject intersectingPlacemark2 = mapObjects.addPlacemark(new Point(rowLatitude + STEP_LATITUDE / 4, columnLongitude), animationFrames[0]);
        intersectingPlacemark2.setZIndex(10.0f);

        // polylines
        rowLatitude = BOTTOM_LATITUDE + STEP_LATITUDE;
        ArrayList<Point> polyline = new ArrayList<Point>();
        polyline.add(new Point(rowLatitude, LEFT_LONGITUDE));
        polyline.add(new Point(rowLatitude, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        PolylineMapObject polylineBottom = mapObjects.addPolyline(new Polyline(polyline));
        polylineBottom.setStrokeColor(Color.BLUE);

        polyline.set(0, new Point(rowLatitude - 0.00025, LEFT_LONGITUDE));
        polyline.set(1, new Point(rowLatitude + 3 * STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE + 0.0005));

        PolylineMapObject polylineLeft = mapObjects.addPolyline(new Polyline(polyline));
        polylineLeft.setStrokeColor(Color.RED);
        polylineLeft.setStrokeWidth(7.0f);
        polylineLeft.setOutlineWidth(14.0f);
        polylineLeft.setOutlineColor(Color.GREEN);
        polylineLeft.setZIndex(10.0f);

        polyline.set(0, new Point(rowLatitude + 3 * STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE - 0.0005));
        polyline.set(1, new Point(rowLatitude - 0.00025, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        PolylineMapObject polylineRight = mapObjects.addPolyline(new Polyline(polyline));
        polylineRight.setStrokeColor(0x8000FF00);
        polylineRight.setStrokeWidth(9.0f);
        polylineRight.setOutlineColor(0x80FF0000);
        polylineRight.setOutlineWidth(polylineRight.getStrokeWidth() * 2.0f);
        polylineRight.setZIndex(20.0f);


        new AddPolylineTask(mapObjects).execute();

        // long dashed polyline
        rowLatitude = BOTTOM_LATITUDE;
        final PolylineMapObject longDashedLine = mapObjects.addPolyline(new Polyline(
            generateSegmentedLine(
                new Point(rowLatitude, LEFT_LONGITUDE + 3 * STEP_LONGITUDE),
                new Point(rowLatitude, LEFT_LONGITUDE - 18.0),
                100)));
        longDashedLine.setDashLength(20.0f);
        longDashedLine.setGapLength(10.0f);

        // dashed polylines
        rowLatitude = BOTTOM_LATITUDE + 6 * STEP_LATITUDE;
        columnLongitude = LEFT_LONGITUDE;
        polyline.set(0, new Point(rowLatitude, columnLongitude));
        polyline.set(1, new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        final PolylineMapObject dashedSegment = mapObjects.addPolyline(new Polyline(polyline));
        dashedSegment.setStrokeColor(Color.rgb(0, 170, 255));
        dashedSegment.setDashLength(10.0f);
        dashedSegment.setGapLength(5.0f);

        columnLongitude += STEP_LONGITUDE;
        polyline.set(0, new Point(rowLatitude, columnLongitude));
        polyline.set(1, new Point(rowLatitude + STEP_LATITUDE, columnLongitude));
        polyline.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        polyline.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        final PolylineMapObject rightAngledDashedLine = mapObjects.addPolyline(new Polyline(polyline));
        rightAngledDashedLine.setStrokeColor(Color.BLACK);
        rightAngledDashedLine.setDashLength(15.0f);
        rightAngledDashedLine.setGapLength(10.0f);

        columnLongitude += STEP_LONGITUDE;
        polyline.set(0, new Point(rowLatitude, columnLongitude));
        polyline.set(1, new Point(rowLatitude + 0.25 * STEP_LATITUDE, columnLongitude + CELL_WIDTH / 6));
        polyline.set(2, new Point(rowLatitude, columnLongitude + 2 * CELL_WIDTH / 6));
        polyline.set(3, new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 3 * CELL_WIDTH / 6));
        polyline.add(new Point(rowLatitude, columnLongitude + 4 * CELL_WIDTH / 6));
        polyline.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + 5 * CELL_WIDTH / 6));
        polyline.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        final PolylineMapObject sharpBendedDashedLine = mapObjects.addPolyline(new Polyline(polyline));
        sharpBendedDashedLine.setStrokeColor(Color.rgb(192, 144, 0));
        sharpBendedDashedLine.setDashLength(10.0f);
        sharpBendedDashedLine.setGapLength(3.0f);

        columnLongitude += STEP_LONGITUDE;
        ArrayList<Point> arcPoints = new ArrayList<Point>();
        for (double angle = 0.0; angle < Math.PI / 2; angle += Math.PI / 30)
            arcPoints.add(new Point(
                rowLatitude + STEP_LATITUDE * Math.sin(angle),
                columnLongitude + CELL_WIDTH * Math.cos(angle)));
        final PolylineMapObject dashedArc = mapObjects.addPolyline();
        dashedArc.setStrokeColor(Color.rgb(96, 160, 0));
        dashedArc.setDashLength(10.0f);
        dashedArc.setGapLength(5.0f);
        dashedArc.setGeometry(new Polyline(arcPoints));

        final PolylineMapObject emptyPolyline = mapObjects.addPolyline();
        emptyPolyline.setStrokeColor(Color.WHITE);
        emptyPolyline.setGeometry(new Polyline(new ArrayList<Point>()));

        // outlined lines
        columnLongitude += STEP_LONGITUDE;
        polyline = new ArrayList<Point>();
        polyline.add(0, new Point(rowLatitude, columnLongitude));
        polyline.add(1, new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        PolylineMapObject outlinedSegment = mapObjects.addPolyline(new Polyline(polyline));
        outlinedSegment.setStrokeColor(Color.rgb(0, 170, 255));
        outlinedSegment.setOutlineColor(Color.BLACK);
        outlinedSegment.setOutlineWidth(1.0f);

        columnLongitude += STEP_LONGITUDE;
        polyline.set(0, new Point(rowLatitude, columnLongitude));
        polyline.set(1, new Point(rowLatitude + STEP_LATITUDE, columnLongitude));
        polyline.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        polyline.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        final PolylineMapObject outlinedDashedLine = mapObjects.addPolyline(new Polyline(polyline));
        outlinedDashedLine.setStrokeColor(Color.BLACK);
        outlinedDashedLine.setOutlineColor(Color.RED);
        outlinedDashedLine.setOutlineWidth(2.0f);
        outlinedDashedLine.setDashLength(15.0f);
        outlinedDashedLine.setGapLength(10.0f);

        // polyline with default color
        columnLongitude += STEP_LONGITUDE;
        polyline.clear();
        polyline.add(0, new Point(rowLatitude, columnLongitude));
        polyline.add(1, new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        final PolylineMapObject defaultColorPolyline = mapObjects.addPolyline(new Polyline(polyline));

        // triangle
        rowLatitude = BOTTOM_LATITUDE + 7.5 * STEP_LATITUDE;
        columnLongitude = LEFT_LONGITUDE;
        ArrayList<Point> triangleOuterRing = new ArrayList<Point>();
        triangleOuterRing.add(new Point(rowLatitude, columnLongitude));
        triangleOuterRing.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        triangleOuterRing.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH));
        PolygonMapObject triangle = mapObjects.addPolygon(new Polygon(new LinearRing(triangleOuterRing), new ArrayList<LinearRing>()));
        triangle.setFillColor(Color.MAGENTA);
        triangle.setStrokeColor(Color.rgb(255, 160, 255));

        // circles
        columnLongitude += STEP_LONGITUDE;
        Point center = new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);
        mapObjects.addCircle(
                new Circle(center, 90),
                Color.argb(255, 0, 102, 255),
                5.f,
                Color.argb(153, 0, 102, 255));
        CircleMapObject circle = notClickableMapObjects.addCircle(
                new Circle(center, 50), Color.RED, 5.0f, Color.RED);
        circle.setZIndex(10000.0f);

        // star
        columnLongitude += STEP_LONGITUDE;
        ArrayList<Point> starPoints = new ArrayList<Point>();
        Point starCenter = new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);
        for (int i = 0; i < 10; ++i) {
            double angle = i * Math.PI / 5 + 0.5 * Math.PI;
            double coef = i % 2 == 0 ? 0.5 : 0.25;
            starPoints.add(polarCoordsToLatLon(starCenter, coef * STEP_LATITUDE, coef * CELL_WIDTH, angle));
        }
        PolygonMapObject star = mapObjects.addPolygon(new Polygon(new LinearRing(starPoints), new ArrayList<LinearRing>()));
        star.setFillColor(Color.argb(176, 224, 224, 0));
        star.setStrokeColor(Color.rgb(160, 160, 0));
        star.setStrokeWidth(2.0f);

        // window
        columnLongitude += STEP_LONGITUDE;
        ArrayList<LinearRing> windowInnerRings = new ArrayList<LinearRing>();
        final double windowHalfHeight = 0.125 * STEP_LATITUDE;
        final double windowHalfWidth = 0.125 * CELL_WIDTH;
        for (int quarterIndex = 0; quarterIndex < 4; ++quarterIndex) {
            double latitudeOffset = rowLatitude + STEP_LATITUDE * (0.5 + 0.225 * (1 - 2 * (quarterIndex & 1)));
            double longitudeOffset = columnLongitude + CELL_WIDTH * (0.5 + 0.225 * (1 - (quarterIndex & 2)));
            ArrayList<Point> innerRing = new ArrayList<Point>();
            innerRing.add(new Point(latitudeOffset - windowHalfHeight, longitudeOffset - windowHalfWidth));
            innerRing.add(new Point(latitudeOffset - windowHalfHeight, longitudeOffset + windowHalfWidth));
            innerRing.add(new Point(latitudeOffset + windowHalfHeight, longitudeOffset + windowHalfWidth));
            innerRing.add(new Point(latitudeOffset + windowHalfHeight, longitudeOffset - windowHalfWidth));
            windowInnerRings.add(new LinearRing(innerRing));
        }
        ArrayList<Point> windowOuterRing = new ArrayList<Point>();
        windowOuterRing.add(new Point(rowLatitude, columnLongitude));
        windowOuterRing.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        windowOuterRing.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        windowOuterRing.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude));
        PolygonMapObject window = mapObjects.addPolygon(new Polygon(
            new LinearRing(windowOuterRing), windowInnerRings));
        window.setFillColor(Color.rgb(160, 64, 64));
        window.setStrokeColor(Color.BLACK);
        window.setStrokeWidth(1.0f);

        // textured rect
        columnLongitude += STEP_LONGITUDE;
        ArrayList<Point> texturedRectOuterRing = new ArrayList<>();
        texturedRectOuterRing.add(new Point(rowLatitude, columnLongitude));
        texturedRectOuterRing.add(new Point(rowLatitude, columnLongitude + CELL_WIDTH));
        texturedRectOuterRing.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude + CELL_WIDTH));
        texturedRectOuterRing.add(new Point(rowLatitude + STEP_LATITUDE, columnLongitude));

        ArrayList<Point> texturedInnerRing = new ArrayList<>();
        final double innerRingLatitudeOffset = STEP_LATITUDE / 3.0;
        final double innerRingLongitudeOffset = CELL_WIDTH / 3.0;
        texturedInnerRing.add(
                new Point(rowLatitude + innerRingLatitudeOffset, columnLongitude + innerRingLongitudeOffset));
        texturedInnerRing.add(
                new Point(rowLatitude + innerRingLatitudeOffset, columnLongitude + CELL_WIDTH - innerRingLongitudeOffset));
        texturedInnerRing.add(
                new Point(rowLatitude + STEP_LATITUDE - innerRingLatitudeOffset, columnLongitude + CELL_WIDTH - innerRingLongitudeOffset));
        texturedInnerRing.add(
                new Point(rowLatitude + STEP_LATITUDE - innerRingLatitudeOffset, columnLongitude + innerRingLongitudeOffset));

        ArrayList<LinearRing> innerRings = new ArrayList<>();
        innerRings.add(new LinearRing(texturedInnerRing));
        PolygonMapObject texturedRect = mapObjects.addPolygon(new Polygon(
            new LinearRing(texturedRectOuterRing), innerRings));
        texturedRect.setStrokeColor(Color.TRANSPARENT);
        AnimatedImageProvider animatedImage = AnimatedImageProvider.fromAsset(this, "nyan_cat.png");
        texturedRect.setAnimatedImage(animatedImage, 64.f);

        // transparent stroked star
        rowLatitude = BOTTOM_LATITUDE + 9 * STEP_LATITUDE;
        columnLongitude = LEFT_LONGITUDE;
        starCenter = new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);
        polyline = new ArrayList<Point>();
        double angle = Math.PI * 0.5;
        for (int i = 0; i < 6; ++i, angle += Math.PI * 0.8) {
            polyline.add(polarCoordsToLatLon(starCenter, 0.5 * STEP_LATITUDE, 0.5 * CELL_WIDTH, angle));
        }
        PolylineMapObject transparentStrokedStar = mapObjects.addPolyline(new Polyline(polyline));
        transparentStrokedStar.setStrokeColor(Color.BLUE & 0x80ffffff);
        transparentStrokedStar.setStrokeWidth(6.0f);

        // two transparent rhombuses
        columnLongitude += STEP_LONGITUDE;
        PolylineMapObject leftTransparentRhomb = mapObjects.addPolyline(
            new Polyline(generateRhomb(
                new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH),
                CELL_WIDTH,
                STEP_LATITUDE)));
        leftTransparentRhomb.setStrokeColor(Color.GREEN & 0x80ffffff);
        leftTransparentRhomb.setStrokeWidth(8.0f);

        PolylineMapObject rightTransparentRhomb = mapObjects.addPolyline(
            new Polyline(generateRhomb(
                new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + CELL_WIDTH),
                CELL_WIDTH,
                STEP_LATITUDE)));
        rightTransparentRhomb.setStrokeColor(Color.BLACK & 0x80ffffff);
        rightTransparentRhomb.setStrokeWidth(8.0f);
        rightTransparentRhomb.setZIndex(1.0f);

        // three rhombuses
        columnLongitude += 1.5 * STEP_LONGITUDE;
        PolylineMapObject leftOpaqueRhomb = mapObjects.addPolyline(
            new Polyline(generateRhomb(
                new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH),
                CELL_WIDTH,
                STEP_LATITUDE)));
        leftOpaqueRhomb.setStrokeColor(Color.WHITE);
        leftOpaqueRhomb.setStrokeWidth(8.0f);

        PolylineMapObject centralTransparentRhomb = mapObjects.addPolyline(
            new Polyline(generateRhomb(
                new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + CELL_WIDTH),
                CELL_WIDTH,
                STEP_LATITUDE)));
        centralTransparentRhomb.setStrokeColor(Color.RED & 0x80ffffff);
        centralTransparentRhomb.setStrokeWidth(8.0f);
        centralTransparentRhomb.setZIndex(1.0f);

        PolylineMapObject rightOpaqueRhomb = mapObjects.addPolyline(
            new Polyline(generateRhomb(
                new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 1.5 * CELL_WIDTH),
                CELL_WIDTH,
                STEP_LATITUDE)));
        rightOpaqueRhomb.setStrokeColor(Color.BLACK);
        rightOpaqueRhomb.setStrokeWidth(8.0f);
        rightOpaqueRhomb.setZIndex(2.0f);

        // transparent rhombus with outline
        columnLongitude += 2.0 * STEP_LONGITUDE;
        Polyline rhombWithOutlineGeom = new Polyline(generateRhomb(
            new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH),
            CELL_WIDTH,
            STEP_LATITUDE));
        PolylineMapObject outlineRhomb = mapObjects.addPolyline(rhombWithOutlineGeom);
        outlineRhomb.setStrokeColor(Color.RED & 0x80ffffff);
        outlineRhomb.setStrokeWidth(10.0f);
        outlineRhomb.setZIndex(1001.00f);

        PolylineMapObject inlineRhomb = mapObjects.addPolyline(rhombWithOutlineGeom);
        inlineRhomb.setStrokeColor(Color.WHITE & 0xf0ffffff);
        inlineRhomb.setStrokeWidth(7.0f);
        inlineRhomb.setZIndex(1001.01f);

        // movable circle
        rowLatitude = BOTTOM_LATITUDE + 10 * STEP_LATITUDE;
        columnLongitude = LEFT_LONGITUDE;
        Point circleCenter = new Point(rowLatitude + STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);

        final CircleMapObject movableCircle = mapObjects.addCircle(
                new Circle(circleCenter, 70),
                Color.argb(255, 0, 102, 255),
                5.f,
                Color.argb(153, 0, 102, 255));
        movableCircleTapListener = new MovableTapListener(animationManager.createAnimator(new Runnable() {
            @Override
            public void run() {
                Circle circle = new Circle(movePoint(movableCircle.getGeometry().getCenter()),
                    movableCircle.getGeometry().getRadius());
                movableCircle.setGeometry(circle);
            }
        }));
        movableCircle.addTapListener(movableCircleTapListener);

        // movable placemark
        columnLongitude += STEP_LONGITUDE;

        Point pointPosition = new Point(rowLatitude + STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);
        final PlacemarkMapObject movablePlacemark = mapObjects.addPlacemark(pointPosition);
        movablePlacemark.setIcon(animationFrames[0]);
        movablePlacemarkTapListener = new MovableTapListener(animationManager.createAnimator(new Runnable() {
            @Override
            public void run() {
                movablePlacemark.setGeometry(movePoint(movablePlacemark.getGeometry()));
            }
        }));
        movablePlacemark.addTapListener(movablePlacemarkTapListener);

        // scaleable placemark
        columnLongitude += 2 * STEP_LONGITUDE;

        pointPosition = new Point(rowLatitude + STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH);
        final PlacemarkMapObject scaleablePlacemark = mapObjects.addPlacemark(pointPosition);
        scaleablePlacemark.setIcon(animationFrames[3]);
        placemarkScale = 1.0f;
        scaleablePlacemarkTapListener = new MovableTapListener(animationManager.createAnimator(new Runnable() {
            @Override
            public void run() {
                scaleablePlacemark.setIconStyle(new IconStyle().setScale(placemarkScale));
                placemarkScale += 0.01f;
                if (placemarkScale > 10.0f)
                    placemarkScale = 0.5f;
            }
        }));
        scaleablePlacemark.addTapListener(scaleablePlacemarkTapListener);

        createPolyline(mapObjects, BOTTOM_LATITUDE + 12 * STEP_LATITUDE);
        PolylineMapObject dashedPolyline = createDashedPolyline(mapObjects, BOTTOM_LATITUDE + 14 * STEP_LATITUDE);

        // car
        rowLatitude = BOTTOM_LATITUDE + 13 * STEP_LATITUDE;
        final PlacemarkMapObject carMapObject = mapObjects.addPlacemark(new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH));
        setModel(carMapObject, R.raw.camaro, R.drawable.camaro);

        // navi marker
        rowLatitude = BOTTOM_LATITUDE + 15 * STEP_LATITUDE;
        final PlacemarkMapObject naviMapObject = mapObjects.addPlacemark(new Point(rowLatitude + 0.5 * STEP_LATITUDE, columnLongitude + 0.5 * CELL_WIDTH));
        setModel(naviMapObject, R.raw.navi, R.drawable.navi);

        mapObjects.addTapListener(mapObjectTapListener);

        for (char letter = 'A'; letter <= 'Z'; ++letter) {
            PlacemarkMapObject p = notClickableMapObjects.addPlacemark(new Point(BOTTOM_LATITUDE - STEP_LATITUDE, LEFT_LONGITUDE + (letter - 'A') * STEP_LONGITUDE));
            int size = (int) getResources().getDisplayMetrics().density * 20;
            p.setIcon(IconWithLetter.iconWithLetter(letter, Color.RED, size));
            lettersPlacemarks.add(p);
            p.setZIndex(50);
        }

        // tappable placemarks
        final MapObjectCollection tappablePlacemarks = map.getMapObjects().addCollection();

        // fully tappable
        rowLatitude = BOTTOM_LATITUDE + 6 * STEP_LATITUDE;
        columnLongitude = LEFT_LONGITUDE - 8 * STEP_LONGITUDE;
        addTappablePlacemark(tappablePlacemarks, rowLatitude, columnLongitude, new RectF(0.f, 0.f, 1.0f, 1.0f), 1.f);

        // not tappable
        rowLatitude += 6 * STEP_LATITUDE;
        addTappablePlacemark(tappablePlacemarks, rowLatitude, columnLongitude, new RectF(0.f, 0.f, 0.0f, 0.0f), 1.f);

        // left top
        rowLatitude += 6 * STEP_LATITUDE;
        addTappablePlacemark(tappablePlacemarks, rowLatitude, columnLongitude, new RectF(0.f, 0.f, 0.5f, 0.5f), 1.f);

        // right bottom scaled
        rowLatitude += 6 * STEP_LATITUDE;
        addTappablePlacemark(tappablePlacemarks, rowLatitude, columnLongitude, new RectF(0.5f, 0.5f, 1.0f, 1.0f), 1.5f);

        tappablePlacemarks.setZIndex(-10.0f);
        tappablePlacemarks.addTapListener(changeOpacityPlacemarkObjectTapListener);

        map.addInputListener(inputListener);

        ToggleButton animateDashesButton = (ToggleButton) findViewById(R.id.animateDashesToggle);
        animateDashesButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private final Animator animator = animationManager.createAnimator(new Runnable() {
                @Override
                public void run() {
                    float dashOffset = longDashedLine.getDashOffset();
                    dashOffset += 5.0f / 60;
                    longDashedLine.setDashOffset(dashOffset);
                    dashedSegment.setDashOffset(dashOffset);
                    dashedArc.setDashOffset(dashOffset);
                    rightAngledDashedLine.setDashOffset(dashOffset);
                    sharpBendedDashedLine.setDashOffset(dashOffset);
                    outlinedDashedLine.setDashOffset(dashOffset);
                    dashedPolyline.setDashOffset(dashOffset);
                }
            });
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                animator.setStarted(b);
            }
        });

        // Hide first visited placemark and remove it
        final List<PlacemarkMapObject> placemarkToRemove = new ArrayList<>();
        mapObjects.traverse(new MapObjectVisitor() {
            private boolean first = true;

            @Override
            public void onPlacemarkVisited(PlacemarkMapObject placemark) {
                LOGGER.info("onPlacemarkVisited");
                if (first) {
                    first = false;
                    placemark.setVisible(false);
                    placemarkToRemove.add(placemark);
                }
            }

            @Override
            public void onPolylineVisited(PolylineMapObject polyline) {
                LOGGER.info("onPolylineVisited");
            }

            @Override
            public void onPolygonVisited(PolygonMapObject polygon) {
                LOGGER.info("onPolygonVisited");
            }

            @Override
            public void onCircleVisited(CircleMapObject circle) {
                LOGGER.info("onCircleVisited");
            }

            @Override
            public boolean onCollectionVisitStart(MapObjectCollection collection) {
                LOGGER.info("onCollectionVisitStart");
                return true;
            }

            @Override
            public void onCollectionVisitEnd(MapObjectCollection collection) {
                LOGGER.info("onCollectionVisitEnd");
            }

            @Override
            public boolean onClusterizedCollectionVisitStart(ClusterizedPlacemarkCollection collection) {
                LOGGER.info("onClusterizedCollectionVisitStart");
                return true;
            }

            @Override
            public void onClusterizedCollectionVisitEnd(ClusterizedPlacemarkCollection collection) {
                LOGGER.info("onClusterizedCollectionVisitEnd");
            }
        });

        placemarkToRemove.get(0).getParent().remove(placemarkToRemove.get(0));
    }

    void setModel(PlacemarkMapObject modelMapObject, int objResId, int textureId) {
        ImageProvider texture = ImageProvider.fromResource(this, textureId);
        ModelStyle modelStyle = new ModelStyle(
            DEFAULT_MODEL_SIZE, ModelStyle.UnitType.NORMALIZED, ModelStyle.RenderMode.USER_MODEL);
        modelMapObject.setModel(ModelProvider.fromResource(this, objResId, texture), modelStyle, null);
        modelMapObject.setOpacity(0.65f);
        modelMapObject.addTapListener(modelTapListener);
        rotatePlacemark(modelMapObject);
    }

    private void createPolyline(MapObjectCollection mapObjects, double latitude) {
        ArrayList<Point> geometry = new ArrayList<Point>();
        geometry.add(new Point(latitude, LEFT_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 3 * STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 4 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + 3 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + 4 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 0.97 * STEP_LATITUDE, LEFT_LONGITUDE + 4.5 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + 5 * STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 5 * STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 4.25 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2.5 * STEP_LATITUDE, LEFT_LONGITUDE + 4.25 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2.5 * STEP_LATITUDE, LEFT_LONGITUDE + 5 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2.3 * STEP_LATITUDE, LEFT_LONGITUDE + 5 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2.3 * STEP_LATITUDE, LEFT_LONGITUDE + 3 * STEP_LONGITUDE));

        final int GREEN = 0;
        final int YELLOW = 1;
        final int RED = 2;
        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));

        PolylineMapObject polyline = mapObjects.addPolyline(new Polyline(geometry));
        polyline.setPaletteColor(GREEN, Color.GREEN);
        polyline.setPaletteColor(YELLOW, Color.YELLOW);
        polyline.setPaletteColor(RED, Color.RED);

        polyline.setStrokeWidth(8.0f);
        polyline.setStrokeColors(colors);
        polyline.setInnerOutlineEnabled(true);
        polyline.setOutlineColor(Color.WHITE);
        polyline.setOutlineWidth(2);
        polyline.hide(new Subpolyline(new PolylinePosition(3, 0.8), new PolylinePosition(7, 0.5)));
        polyline.select(Color.BLUE, new Subpolyline(new PolylinePosition(1, 0.5), new PolylinePosition(3, 0.5)));

        Arrow arrow = polyline.addArrow(new PolylinePosition(15, 0.1), 250, Color.WHITE);
        arrow.setOutlineColor(Color.BLACK);
        arrow.setOutlineWidth(1);
        arrow.setTriangleHeight(20);

        changePolylineZlevelObjectTapListener = new MapObjectTapListener() {
            private final List<Integer> zlevels = Arrays.asList(-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
            @Override
            public boolean onMapObjectTap(MapObject origin, Point point) {
                ((PolylineMapObject)origin).setZlevels(zlevels);
                Collections.reverse(zlevels);
                return true;
            }
        };
        polyline.addTapListener(changePolylineZlevelObjectTapListener);
    }

    private PolylineMapObject createDashedPolyline(MapObjectCollection mapObjects, double latitude) {
        ArrayList<Point> geometry = new ArrayList<Point>();
        geometry.add(new Point(latitude, LEFT_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + STEP_LATITUDE, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2 * STEP_LATITUDE, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 3 * STEP_LATITUDE, LEFT_LONGITUDE + 2 * STEP_LONGITUDE));
        geometry.add(new Point(latitude + 3 * STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2 * STEP_LATITUDE, LEFT_LONGITUDE + STEP_LONGITUDE));
        geometry.add(new Point(latitude + 2 * STEP_LATITUDE, LEFT_LONGITUDE));
        geometry.add(new Point(latitude + 3 * STEP_LATITUDE, LEFT_LONGITUDE));

        final int GREEN = 0;
        final int YELLOW = 1;
        final int RED = 2;
        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));
        colors.add(Integer.valueOf(RED));
        colors.add(Integer.valueOf(GREEN));
        colors.add(Integer.valueOf(YELLOW));

        PolylineMapObject polyline = mapObjects.addPolyline(new Polyline(geometry));
        polyline.setPaletteColor(GREEN, Color.GREEN);
        polyline.setPaletteColor(YELLOW, Color.YELLOW);
        polyline.setPaletteColor(RED, Color.RED);

        polyline.setStrokeWidth(8.0f);
        polyline.setInnerOutlineEnabled(true);
        polyline.setOutlineColor(Color.WHITE);
        polyline.setOutlineWidth(2);
        polyline.setDashLength(20.f);
        polyline.setGapLength(10.f);
        polyline.setStrokeColors(colors);
        polyline.hide(new Subpolyline(new PolylinePosition(4, 0.5), new PolylinePosition(6, 0.5)));
        polyline.select(Color.BLUE, new Subpolyline(new PolylinePosition(1, 0.5), new PolylinePosition(3, 0.5)));

        return polyline;
    }

    private class CameraMoveTestTapListener implements MapObjectTapListener {
        CameraMoveTestTapListener(List<PlacemarkMapObject> placemarks) {
            ps_ = placemarks;
        }

        @Override
        public boolean onMapObjectTap(MapObject origin, Point point) {
            CameraPosition cp = mapview.getMap().getCameraPosition();

            Point target = null;
            int zoomStep = 0;
            for (int i = 0; i < ps_.size(); i++) {
                if (origin == ps_.get(i)) {
                    if (i % 2 == 0) {
                        target = ps_.get(i + 1).getGeometry();
                        zoomStep = i + 2;
                    } else {
                        target = ps_.get(i - 1).getGeometry();
                        zoomStep = -(i + 1);
                    }
                }
            }

            CameraPosition newCp = new CameraPosition(
                target,
                cp.getZoom() + zoomStep,
                cp.getAzimuth(),
                cp.getTilt());

            mapview.getMap().move(newCp, new Animation(Animation.Type.SMOOTH, 10), null);

            return true;
        }

        List<PlacemarkMapObject> ps_;
    }

    private CameraMoveTestTapListener cameraTestingTapListener;

    private void createCameraMoveTesting() {
        final MapObjectCollection mapObjects = mapview.getMap().getMapObjects().addCollection();

        List<PlacemarkMapObject> placemarks = new ArrayList<PlacemarkMapObject>();
        placemarks.add(mapObjects.addPlacemark(new Point(59.944499, 30.322803)));
        placemarks.add(mapObjects.addPlacemark(new Point(60.017495, 30.322803)));

        placemarks.add(mapObjects.addPlacemark(new Point(59.944499, 30.342803)));
        placemarks.add(mapObjects.addPlacemark(new Point(60.017495, 30.342803)));

        placemarks.add(mapObjects.addPlacemark(new Point(59.944499, 30.362803)));
        placemarks.add(mapObjects.addPlacemark(new Point(60.017495, 30.362803)));

        placemarks.add(mapObjects.addPlacemark(new Point(59.944499, 30.382803)));
        placemarks.add(mapObjects.addPlacemark(new Point(60.017495, 30.382803)));

        cameraTestingTapListener = new CameraMoveTestTapListener(placemarks);

        for (PlacemarkMapObject p : placemarks) {
            p.addTapListener(cameraTestingTapListener);
        }
    }

    private static Point movePoint(Point point) {
        return new Point(point.getLatitude() + 0.00001, point.getLongitude());
    }

    static class YellowCircleImageProvider extends ImageProvider {
        private WeakReference<MapObjectsActivity> weakActivity;

        YellowCircleImageProvider(MapObjectsActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        public String getId() {
            return "YellowCircleImage";
        }

        @Override
        public Bitmap getImage() {
            MapObjectsActivity activity = weakActivity.get();
            if (activity != null)
                return activity.createPlacemarkBitmap(Color.YELLOW, 50);
            return null;
        }
    }

    private void createInversedVisibilityPlacemarks(Point point1, Point point2) {
        final Map map = mapview.getMap();
        final MapObjectCollection mapObjects = map.getMapObjects();

        final ImageProvider yellowCircleImage = new YellowCircleImageProvider(this);


        final PlacemarkMapObject p1 = mapObjects.addPlacemark(point1);
        p1.setIcon(yellowCircleImage);

        final PlacemarkMapObject p2 = mapObjects.addPlacemark(point2);
        p2.setIcon(yellowCircleImage);

        //without animation
        p2.setVisible(false);

        inverseVisbilityTapListener = new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(MapObject origin, Point point) {
                p1.setVisible(!p1.isVisible(), new Animation(Animation.Type.SMOOTH, 0.15f), null);

                //without animation
                p2.setVisible(!p2.isVisible());

                CameraPosition position = map.getCameraPosition();
                map.move(
                        new CameraPosition(
                            p1.isVisible() ? p1.getGeometry() : p2.getGeometry(),
                            position.getZoom(),
                            position.getAzimuth(),
                            position.getTilt()),
                            new Animation(Animation.Type.SMOOTH, 0.4f),
                        null); // CameraCallback

                return true;
            }
        };

        p1.addTapListener(inverseVisbilityTapListener);
        p2.addTapListener(inverseVisbilityTapListener);
    }

    private void createCompositePlacemark(Point point) {
        ImageProvider thinRectangleImage = ImageProvider.fromBitmap(
            createRectangleBitmap(Color.BLUE, Color.RED, 16, 60));
        float density = getDensity(this);
        ImageProvider innerCircle = ImageProvider.fromBitmap(
                createFilledCircle(Color.WHITE, 100, density));
        ImageProvider borderCircle = ImageProvider.fromBitmap(
                createFilledCircle(Color.BLACK, 110, density));
        ImageProvider topRightCircle = ImageProvider.fromBitmap(
                createFilledCircle(Color.CYAN, 12, density));
        ImageProvider leftBottomCircle = ImageProvider.fromBitmap(
                createFilledCircle(Color.YELLOW, 12, density));

        final PlacemarkMapObject placemark = mapview.getMap().getMapObjects().addPlacemark(point);
        CompositeIcon compositeIcon = placemark.useCompositeIcon();
        compositeIcon.setIcon("topRight", topRightCircle, new IconStyle().setAnchor(new PointF(0.0f, 1.0f)).setZIndex(30.0f));
        compositeIcon.setIcon("rectangle", thinRectangleImage,
            new IconStyle().setFlat(true).setRotationType(RotationType.ROTATE).setZIndex(20.0f));
        compositeIcon.setIcon("inner", innerCircle, new IconStyle().setFlat(true).setZIndex(10.0f));
        compositeIcon.setIcon("border", borderCircle, new IconStyle());
        compositeIcon.setIcon("leftBottom", leftBottomCircle, new IconStyle().setAnchor(new PointF(1.0f, 0.0f)).setZIndex(30.0f));
        placemark.setOpacity(0.65f);

        rotatePlacemark(placemark);
    }

    @Override
    protected void onStop() {
        mapview.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapview.onStart();
    }

    @Override
    protected void onDestroy() {
        animationManager.onDestroy();
        animationHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public void onRenamePlacemarksClick(View view) {
        for (char letter = 'A'; letter <= 'Z'; ++letter) {
            PlacemarkMapObject p = lettersPlacemarks.get(letter - 'A');
            int size = (int) getResources().getDisplayMetrics().density * 20;
            p.setIcon(IconWithLetter.iconWithLetter(letter, Color.RED, size));
        }
    }
}
