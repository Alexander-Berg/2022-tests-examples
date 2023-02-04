package com.yandex.maps.testapp.map;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.widget.RelativeLayout;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Button;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.ScreenRect;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.SizeChangedListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.map.PointOfView;
import com.yandex.mapkit.map.GestureFocusPointMode;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.bindings.Serialization;

public class FocusRectActivity extends MapBaseActivity implements InputListener, SizeChangedListener {
    public static String CAMERA_TARGET_EXTRA = "cameraTarget";
    public static int PICK_CAMERA_TARGET = 0;

    public static class FocusRectView extends View {
        Paint paint = new Paint();

        private ScreenRect focusRect;
        private ScreenPoint focusPoint;
        private static final float focusPointCrossSize = 32.f;

        public FocusRectView(Context context, AttributeSet attrs) {
            super(context, attrs);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(4.f);
        }

        public void setFocusPoint(ScreenPoint focusPoint) {
            this.focusPoint = focusPoint;
            invalidate();
        }

        public void setFocusRect(ScreenRect focusRect) {
            this.focusRect = focusRect;
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (focusRect != null) {
                canvas.drawRect(focusRect.getTopLeft().getX(), focusRect.getTopLeft().getY(),
                        focusRect.getBottomRight().getX(), focusRect.getBottomRight().getY(), paint);
            }

            if (focusPoint != null) {
                canvas.drawLine(focusPoint.getX() - focusPointCrossSize / 2.f, focusPoint.getY() - focusPointCrossSize / 2.f,
                        focusPoint.getX() + focusPointCrossSize / 2.f, focusPoint.getY() + focusPointCrossSize / 2.f, paint);
                canvas.drawLine(focusPoint.getX() - focusPointCrossSize / 2.f, focusPoint.getY() + focusPointCrossSize / 2.f,
                        focusPoint.getX() + focusPointCrossSize / 2.f, focusPoint.getY() - focusPointCrossSize / 2.f, paint);
            }
        }
    }

    private FocusRectView focusRectView;

    private BoundingBox boundingBox;
    @Override
    public void onMapTap(Map map, Point point) {
        map.getMapObjects().addPlacemark(point);
        BoundingBox pointBox = BoundingBoxHelper.getBounds(point);
        boundingBox = (boundingBox == null) ? pointBox : BoundingBoxHelper.getBounds(pointBox, boundingBox);
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        if (boundingBox == null) return;

        List<Point> points = new ArrayList<>();
        points.add(boundingBox.getNorthEast());
        points.add(new Point(boundingBox.getNorthEast().getLatitude(), boundingBox.getSouthWest().getLongitude()));
        points.add(boundingBox.getSouthWest());
        points.add(new Point(boundingBox.getSouthWest().getLatitude(), boundingBox.getNorthEast().getLongitude()));
        points.add(boundingBox.getNorthEast());
        map.getMapObjects().addPolyline(new Polyline(points));
        map.move(map.cameraPosition(boundingBox), new Animation(Animation.Type.SMOOTH, 1.0f), null);
    }

    @Override
    public void onMapWindowSizeChanged(MapWindow mapWindow, int newWidth, int newHeight) {
        double x = (double)focusPointXSB.getProgress() / focusPointXSB.getMax();
        double y = (double)focusPointYSB.getProgress() / focusPointYSB.getMax();
        setFocusPoint(x, y);
    }

    private SeekBar focusPointXSB = null;
    private SeekBar focusPointYSB = null;

    private int focusRectIndex = 0;
    private String focusRectIndexToString(int index) {
        switch (index) {
        case 0: return "Full";
        case 1: return "TopLeft";
        case 2: return "TopRight";
        case 3: return "BottomLeft";
        case 4: return "BottomRight";
        case 5: return "Center";
        };
        return "None";
    }

    public void onTestClick(View view) {
        if (focusRectIndex < 0)
            focusRectIndex = 0;
        else
            focusRectIndex = (focusRectIndex + 1) % 6;

        ((Button)findViewById(R.id.focus_quarter)).setText(focusRectIndexToString(focusRectIndex));

        switch (focusRectIndex) {
        case 0: setFocusRect(0.f, 1.f, 0.f, 1.f); break;
        case 1: setFocusRect(0.f, 0.5f, 0.f, 0.5f); break;
        case 2: setFocusRect(0.5f, 1.f, 0.f, 0.5f); break;
        case 3: setFocusRect(0.f, 0.5f, 0.5f, 1.f); break;
        case 4: setFocusRect(0.5f, 1.f, 0.5f, 1.f); break;
        case 5: setFocusRect(0.25f, 0.75f, 0.25f, 0.75f); break;
        };
    }

    private void moveMap(Point point) {
        moveMap(point, 1.0f);
    }

    private ScreenRect createFocusRect(float xmin, float xmax, float ymin, float ymax) {
        int width = mapview.getWidth();
        int height = mapview.getHeight();
        ScreenPoint topLeft = new ScreenPoint(width * xmin, height * ymin);
        ScreenPoint bottomRight = new ScreenPoint(width * xmax, height * ymax);
        return new ScreenRect(topLeft, bottomRight);
    }

    private void setFocusRect(float xmin, float xmax, float ymin, float ymax) {
        ScreenRect focusRect = createFocusRect(xmin, xmax, ymin, ymax);
        mapview.setFocusRect(focusRect);
        focusRectView.setFocusRect(focusRect);
    }

    private void setFocusPoint(double x, double y) {
        ScreenPoint focusPoint = new ScreenPoint((float)(mapview.getWidth() * x), (float)(mapview.getHeight() * y));

        Point target = mapview.screenToWorld(focusPoint);
        CameraPosition newPosition = null;
        CameraPosition position = mapview.getMap().getCameraPosition();
        if (target != null)
            newPosition = new CameraPosition(target, position.getZoom(), position.getAzimuth(), position.getTilt());

        mapview.setGestureFocusPoint(focusPoint);
        mapview.setFocusPoint(focusPoint);
        focusRectView.setFocusPoint(focusPoint);

        if (((ToggleButton)findViewById(R.id.focusRect_mode)).isChecked() && target != null)
            mapview.getMap().move(newPosition);

        {
            StringBuilder msg = new StringBuilder();
            msg.append("focusRect: ");
            ScreenRect fr = mapview.getFocusRect();
            if (fr != null) {
                msg.append("(").append(fr.getTopLeft().getX()).append(", ").append(fr.getTopLeft().getY()).append("), ");
                msg.append("(").append(fr.getBottomRight().getX()).append(", ").append(fr.getBottomRight().getY()).append(")");
            } else {
                msg.append("no");
            }

            // msg.append("prevTarget: (").append(cp.getTarget().getLongitude()).append(", ").append(cp.getTarget().getLatitude()).append("); ");
            // msg.append("focusPoint: (").append(focusPointX).append(", ").append(focusPointY).append("), ");
            // msg.append("(").append(focusPointXscr).append(", ").append(focusPointYscr).append("); ");
            // if (worldFocus != null)
            //     msg.append("worldFocus: (").append(worldFocus.getLongitude()).append(", ").append(worldFocus.getLatitude()).append("); ");

            // msg.append("curTarget: (").append(mapview.getMap().getCameraPosition().getTarget().getLongitude());
            // msg.append(", ").append(mapview.getMap().getCameraPosition().getTarget().getLatitude()).append("); ");

            // ScreenPoint sp = mapview.worldToScreen(mapview.getMap().getCameraPosition().getTarget());
            // if (sp != null)
            //     msg.append("curTargetOnScreen: (").append(sp.getX()).append(", ").append(sp.getY()).append(");");

            Log.d("test_app", msg.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.focus_rect);
        super.onCreate(savedInstanceState);

        byte[] serializedTarget = getIntent().getByteArrayExtra(CAMERA_TARGET_EXTRA);
        if (serializedTarget != null) {
            Point cameraTarget = Serialization.deserializeFromBytes(
                serializedTarget,
                Point.class);
            moveMap(cameraTarget);
        }

        mapview.getMap().addInputListener(this);
        mapview.addSizeChangedListener(this);

        focusPointXSB = findViewById(R.id.focus_point_x);
        focusPointYSB = findViewById(R.id.focus_point_y);
        SeekBar.OnSeekBarChangeListener focusPointChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double x = (double)focusPointXSB.getProgress() / focusPointXSB.getMax();
                double y = (double)focusPointYSB.getProgress() / focusPointYSB.getMax();
                setFocusPoint(x, y);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        focusPointXSB.setOnSeekBarChangeListener(focusPointChangeListener);
        focusPointYSB.setOnSeekBarChangeListener(focusPointChangeListener);

        ToggleButton povMode = (ToggleButton)findViewById(R.id.pov_mode);
        povMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mapview.setPointOfView(PointOfView.ADAPT_TO_FOCUS_POINT_HORIZONTALLY);
                } else {
                    mapview.setPointOfView(PointOfView.SCREEN_CENTER);
                }
            }
        });

        ToggleButton zoomFocusPointMode = (ToggleButton)findViewById(R.id.zoom_focus_point_mode);
        zoomFocusPointMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mapview.setGestureFocusPointMode(GestureFocusPointMode.AFFECTS_ALL_GESTURES);
                } else {
                    mapview.setGestureFocusPointMode(GestureFocusPointMode.AFFECTS_TAP_GESTURES);
                }
            }
        });

        focusRectView = (FocusRectView)findViewById(R.id.focus_rect_view);
    }
}
