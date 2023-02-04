package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.Cluster;
import com.yandex.mapkit.map.ClusterListener;
import com.yandex.mapkit.map.ClusterTapListener;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectVisitor;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class MapClusteringActivity extends MapBaseActivity implements ClusterListener, ClusterTapListener {
    private ImageProvider imageProvider;
    private ClusterizedPlacemarkCollection clusterizedCollection;
    private static final float FONT_SIZE = 15;
    private static final float MARGIN_SIZE = 3;
    private static final float STROKE_SIZE = 3;
    private final List<Point> CLUSTER_CENTERS = Arrays.asList(
            new Point(55.756, 37.618),
            new Point(59.956, 30.313),
            new Point(56.838, 60.597),
            new Point(43.117, 131.900),
            new Point(56.852, 53.204)
    );
    private int count = 0;
    public class TextImageProvider extends ImageProvider {
        @Override
        public String getId() {
            return "text_" + text;
        }

        private final String text;
        @Override
        public Bitmap getImage() {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager manager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            manager.getDefaultDisplay().getMetrics(metrics);

            Paint textPaint = new Paint();
            textPaint.setTextSize(FONT_SIZE * metrics.density);
            textPaint.setTextAlign(Align.CENTER);
            textPaint.setStyle(Style.FILL);
            textPaint.setAntiAlias(true);

            float widthF = textPaint.measureText(text);
            FontMetrics textMetrics = textPaint.getFontMetrics();
            float heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top);
            float textRadius = (float)Math.sqrt(widthF * widthF + heightF * heightF) / 2;
            float internalRadius = textRadius + MARGIN_SIZE * metrics.density;
            float externalRadius = internalRadius + STROKE_SIZE * metrics.density;

            int width = (int) (2 * externalRadius + 0.5);

            Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Paint backgroundPaint = new Paint();
            backgroundPaint.setAntiAlias(true);
            backgroundPaint.setColor(Color.RED);
            canvas.drawCircle(width / 2, width / 2, externalRadius, backgroundPaint);

            backgroundPaint.setColor(Color.WHITE);
            canvas.drawCircle(width / 2, width / 2, internalRadius, backgroundPaint);

            canvas.drawText(
                    text,
                    width / 2,
                    width / 2 - (textMetrics.ascent + textMetrics.descent) / 2,
                    textPaint);

            return bitmap;
        }

        public TextImageProvider(String text) {
            this.text = text;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_clustering);
        super.onCreate(savedInstanceState);

        mapview.getMap().move(new CameraPosition(
            CLUSTER_CENTERS.get(0), 3, 0, 0));

        clusterizedCollection = mapview.getMap().getMapObjects().addClusterizedPlacemarkCollection(this);
        imageProvider = ImageProvider.fromResource(MapClusteringActivity.this, R.drawable.a0);
        EditText placemarkNumEdit = (EditText) findViewById(R.id.clustered_placemark_num);
        placemarkNumEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                try {
                    String text = view.getText().toString();
                    int placemarkNum = text.isEmpty() ? 0 : Integer.parseInt(text);
                    setPlacemarkNum(placemarkNum);
                } catch (NumberFormatException e) {
                    Toast.makeText(
                        getApplicationContext(),
                        "Number format exception",
                        Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    @Override
    public void onClusterAdded(Cluster cluster) {
        cluster.getAppearance().setIcon(
            new TextImageProvider(Integer.toString(cluster.getSize())));
        cluster.addClusterTapListener(this);
    }

    @Override
    public boolean onClusterTap(Cluster cluster) {
        Toast.makeText(
            getApplicationContext(),
            String.format("Tapped cluster with %d items", cluster.getSize()),
            Toast.LENGTH_SHORT).show();

        return true;
    }

    public void onRemoveClustersClick(View sender) {
        clusterizedCollection.clear();
    }

    private void setPlacemarkNum(int placemarkNum) {
        clusterizedCollection.clear();

        List<Point> points = initPosition(placemarkNum);
        clusterizedCollection.addPlacemarks(
            points, imageProvider, new IconStyle());

        clusterizedCollection.clusterPlacemarks(60, 15);
    }

    private List<Point> initPosition(int size) {
        ArrayList<Point> points = new ArrayList<Point>();
        Random random = new Random();

        for (int i = 0; i < size; ++i) {
            Point clusterCenter = CLUSTER_CENTERS.get(random.nextInt(CLUSTER_CENTERS.size()));
            double latitude = clusterCenter.getLatitude() + Math.random() - 0.5;
            double longitude = clusterCenter.getLongitude() + Math.random() - 0.5;

            points.add(new Point(latitude, longitude));
        }

        return points;
    }
}
