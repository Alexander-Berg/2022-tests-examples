package com.yandex.maps.testapp.panorama;

import com.yandex.mapkit.TileId;
import com.yandex.mapkit.places.panorama.AngularBoundingBox;
import com.yandex.mapkit.places.panorama.ArrowConnection;
import com.yandex.mapkit.places.panorama.IconConnection;
import com.yandex.mapkit.places.panorama.IconImageFactory;
import com.yandex.mapkit.places.panorama.IconMarker;
import com.yandex.mapkit.places.panorama.IconUrlProvider;
import com.yandex.mapkit.places.panorama.ImageSize;
import com.yandex.mapkit.places.panorama.PanoramaDescription;
import com.yandex.mapkit.places.panorama.Position;
import com.yandex.mapkit.places.panorama.TextMarker;
import com.yandex.mapkit.places.panorama.TileImageFactory;
import com.yandex.mapkit.places.panorama.TileLevel;
import com.yandex.mapkit.places.panorama.TileUrlProvider;
import com.yandex.mapkit.places.panorama.UserPanoramaEventListener;
import com.yandex.maps.testapp.R;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.geometry.Direction;
import com.yandex.mapkit.geometry.Span;

import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.panorama.HistoricalPanorama;
import com.yandex.mapkit.places.panorama.PanoramaLayer;
import com.yandex.mapkit.places.panorama.PanoramaView;
import com.yandex.mapkit.places.panorama.PanoramaService;
import com.yandex.mapkit.places.panorama.PanoramaService.SearchSession;
import com.yandex.mapkit.places.panorama.Player;
import com.yandex.mapkit.places.panorama.PanoramaChangeListener;
import com.yandex.mapkit.places.panorama.DirectionChangeListener;
import com.yandex.mapkit.places.panorama.SpanChangeListener;
import com.yandex.mapkit.places.panorama.PanoramaService.SearchListener;
import com.yandex.mapkit.places.panorama.ErrorListener;
import com.yandex.mapkit.places.panorama.NotFoundError;

import com.yandex.runtime.Error;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.RemoteError;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.yandex.maps.testapp.TestAppActivity;

import static java.lang.Math.sin;

public class UserPanoActivity extends TestAppActivity implements PanoramaChangeListener,
                                                      DirectionChangeListener,
                                                      SpanChangeListener,
                                                      ErrorListener {
    private static final float MOVE_TIME = 0.5f;

    private PanoramaView panoramaView;
    private Player player;
    private PlacemarkMapObject iconHandle;
    private MapView mapView;
    private Map map;
    private String panoId;

    private static Logger LOGGER =
            Logger.getLogger("yandex.maps");

    @Override
    public void onPanoramaChanged(Player player) {
        LOGGER.info("Panorama changed");
        iconHandle.setGeometry(player.position());
        iconHandle.setVisible(true);
        CameraPosition cp = map.getCameraPosition();
        panoId = player.panoramaId();

        map.move(
                new CameraPosition(
                        player.position(),
                        cp.getZoom(),
                        (float) player.direction().getAzimuth(),
                        cp.getTilt()),
                new Animation(Animation.Type.SMOOTH, MOVE_TIME),
                null);
    }

    @Override
    public void onPanoramaDirectionChanged(Player player) {
        LOGGER.info("Direction changed");
        CameraPosition cp = map.getCameraPosition();
        map.move(
                new CameraPosition(
                        player.position(),
                        cp.getZoom(),
                        (float) player.direction().getAzimuth(),
                        cp.getTilt()),
                new Animation(Animation.Type.SMOOTH, 0.0f),
                null);
    }

    @Override
    public void onPanoramaSpanChanged(Player player) {
        LOGGER.info("Span changed");
    }

    @Override
    public void onPanoramaOpenError(Player player, Error error) {
        LOGGER.info("Failed to open panorama");
        String msg;
        if (error instanceof NetworkError) {
            msg = "No network connection available";
        } else if (error instanceof RemoteError) {
            msg = "An error occurred on the server while opening panorama";
        } else {
            msg = "An error has occurred while opening panorama";
        }

        if (panoId != null) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage(msg);
            dlgAlert.setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    openPanorama(panoId);
                }
            });
            dlgAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            dlgAlert.setCancelable(true);
            dlgAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            dlgAlert.create().show();
        } else {
            Toast.makeText(getApplicationContext(),
                    msg, Toast.LENGTH_LONG).show();
        }
    }

    public void onZoomIn(View view) {
        player.setSpan(new Span(0.0f, player.span().getVerticalAngle() / 1.2f));
    }

    public void onZoomOut(View view) {
        player.setSpan(new Span(0.0f, player.span().getVerticalAngle() * 1.2f));
    }

    public void onBackTap(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("UserPanoActivity.onDestroy");
        super.onDestroy();
    }

    IconUrlProvider iconUrlProvider = new IconUrlProvider() {
        @NonNull
        @Override
        public String formatUrl(@NonNull String iconId, double scale) {
            return String.format("https://core-stv-renderer.maps.yandex.net/2.x/pano_icons?id=%s&scale=%f", iconId, scale);
        }
    };
    TileUrlProvider tileUrlProvider = new TileUrlProvider() {
        @NonNull
        @Override
        public String formatUrl(String panoramaId, int x, int y, int tilelevel) {
            return String.format("https://pano.maps.yandex.net/%s/%d.%d.%d", panoramaId, tilelevel, x, y);
        }
    };

    class UrlImageProvider extends ImageProvider {
        public UrlImageProvider(String url) {
            this.url = url;
        }
        @Override
        public String getId() {
            return url;
        }

        @Override
        public Bitmap getImage() {
            while (true) {
                try {
                    URL url = new URL(this.url);
                    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setConnectTimeout(20000);
                    con.setReadTimeout(1000);
                    return BitmapFactory.decodeStream(con.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        final String url;
    }
    class TileImageProvider extends ImageProvider {
        private final int x;
        private final int y;
        private final int z;
        private final String panoramaId;
        public TileImageProvider(String panoramId, int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.panoramaId = panoramId;
        }
        @Override
        public String getId() {
            return String.format("%s/%d.%d.%d", panoramaId, z, x, y);
        }

        @Override
        public Bitmap getImage() {
            while (true) {
                Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                for (int dx = 0; dx <= 1; ++dx) {
                    for (int dy = 0; dy <= 1; ++dy) {
                        try {
                            URL url = new URL(tileUrlProvider.formatUrl(panoramaId, x * 2 + dx, y * 2 + dy, z));
                            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setConnectTimeout(2000);
                            con.setReadTimeout(1000);
                            canvas.drawBitmap(BitmapFactory.decodeStream(con.getInputStream()),
                                    256 * dx,
                                    256 * dy ,
                                    null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return bmp;

            }
        }
    };

    TileImageFactory tileImageFactory = new TileImageFactory() {
        @NonNull
        @Override
        public ImageProvider load(String panoramaId, int x, int y, int z) {
            return new TileImageProvider(panoramaId, x, y, z);
        }
    };

    IconImageFactory iconImageFactory = new IconImageFactory() {
        @NonNull
        @Override
        public ImageProvider load(@NonNull String iconId, double scale) {
            return new UrlImageProvider(iconUrlProvider.formatUrl(iconId, scale));
        }
    };
    UserPanoramaEventListener userPanoramaListener = new UserPanoramaEventListener() {
        @Override
        public void onPanoramaChangeIntent(@NonNull String panoramaId) {
            LOGGER.info("UserPanoramaEventListener.onPanoramaChangeIntent " + panoramaId);
            openPanorama(panoramaId);
        }
    };
    void openPanorama(String panoramaId) {
            LOGGER.info("open panorama " + panoramaId);
            List< TileLevel > tileLevels = new ArrayList< TileLevel >();
            tileLevels.add(new TileLevel(0, new ImageSize(18944, 9472)));
            tileLevels.add(new TileLevel(1, new ImageSize(7168, 3584)));
            tileLevels.add(new TileLevel(2, new ImageSize(3584, 1792)));
            tileLevels.add(new TileLevel(3, new ImageSize(1792, 896)));
            tileLevels.add(new TileLevel(4, new ImageSize(512, 256)));

            if (panoramaId.equals("d9096465")) {
                // https://core-stvdescr.testing.maps.n.yandex.ru/description?v=3.86.0&oid=1714671435_713034761_23_1498291669
                ArrayList< ArrowConnection > connections = new ArrayList< ArrowConnection >();
                connections.add(new ArrowConnection(
                        new Direction(117.38112719397371,0),
                        "Локальные тайлы 512x512",
                        ArrowConnection.Style.STREET,
                        "99095920"));
                connections.add(new ArrowConnection(
                        new Direction(-103.21919465199011, 0),
                        "Тайлы по сети 256x256",
                        ArrowConnection.Style.STREET,
                        "g9095919"));
                ArrayList< TextMarker > textMarkers= new ArrayList< TextMarker >();
                textMarkers.add(new TextMarker(
                        new Direction(0,0),
                        "short text",
                        "L  O  N  G       T   E   X   T"
                ));

                player.openUserPanoramaWithNetworkDataSource(
                        new PanoramaDescription(
                                panoramaId,
                                new Position(new Point(51.8047349999918, 107.444199), 100),
                                new AngularBoundingBox(-90.93f, 90, 269.07f, -90),
                                new ImageSize(256, 256),
                                tileLevels,
                                new ArrayList< IconMarker >(),
                                textMarkers,
                                new ArrayList< IconConnection >(),
                                connections,
                                new Direction(0,0),
                                new Span(130, 80),
                                /* attribution */ null),
                        tileUrlProvider,
                        iconUrlProvider,
                        userPanoramaListener
                );
            } else if (panoramaId.equals("99095920")) {
                // https://core-stvdescr.testing.maps.n.yandex.ru/description?v=3.86.0&oid=1714672348_713035233_23_1498291666
                ArrayList< ArrowConnection > connections = new ArrayList< ArrowConnection >();
                connections.add(new ArrowConnection(
                        new Direction(-62.618752562143271, 0),
                        "Тайлы по сети, панорама с маркером",
                        ArrowConnection.Style.STREET,
                        "d9096465"));

                ArrayList< IconMarker > iconMarkers = new ArrayList< IconMarker >();
                player.openUserPanoramaWithLocalDataSource(
                        new PanoramaDescription(
                                panoramaId,
                                new Position(new Point(51.8046859999918, 107.444352), 100),
                                new AngularBoundingBox(-29.19f, 90, 330.81f, -90),
                                new ImageSize(512, 512),
                                tileLevels,
                                iconMarkers,
                                new ArrayList< TextMarker >(),
                                new ArrayList< IconConnection >(),
                                connections,
                                new Direction(0,0),
                                new Span(130, 80),
                                /* attribution */ null),
                        tileImageFactory,
                        iconImageFactory,
                        userPanoramaListener
                );
            } else if (panoramaId.equals("g9095919")) {
                // https://core-stvdescr.testing.maps.n.yandex.ru/description?v=3.86.0&oid=1714669299_713035262_23_1498291674
                ArrayList< ArrowConnection > connections = new ArrayList< ArrowConnection >();
                connections.add(new ArrowConnection(
                        new Direction(76.780523993047566, 0),
                        "Тайлы по сети, панорама с маркером",
                        ArrowConnection.Style.STREET,
                        "d9096465"));
                player.openUserPanoramaWithNetworkDataSource(
                        new PanoramaDescription(
                                panoramaId,
                                new Position(new Point(51.8046829999918, 107.443841), 100),
                                new AngularBoundingBox(-102.13f, 90, 257.87f, -90),
                                new ImageSize(256, 256),
                                tileLevels,
                                new ArrayList< IconMarker >(),
                                new ArrayList< TextMarker >(),
                                new ArrayList< IconConnection >(),
                                connections,
                                new Direction(0,0),
                                new Span(130, 80),
                                /* attribution */ null),
                        tileUrlProvider,
                        iconUrlProvider,
                        userPanoramaListener
                );
            }
        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_panorama);

        panoramaView = (PanoramaView)findViewById(R.id.panoview);
        player = panoramaView.getPlayer();

        player.addPanoramaChangeListener(this);
        player.addDirectionChangeListener(this);
        player.addSpanChangeListener(this);
        player.addErrorListener(this);

        player.enableMove();
        player.enableRotation();
        player.enableZoom();
        player.enableMarkers();

        mapView = findViewById(R.id.panomapview);
        map = mapView.getMap();
        map.move(
                new CameraPosition(new Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        iconHandle = map.getMapObjects().addPlacemark(new Point(0.0f, 0.0f));
        iconHandle.setIcon(ImageProvider.fromResource(this, R.drawable.pano_marker));
        iconHandle.setVisible(false);
        panoId = "d9096465";
        openPanorama(panoId);
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        panoramaView.onStop();
        super.onStop();
    }

    @Override
    protected void onStartImpl() {

    }

    @Override
    protected void onStopImpl() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        panoramaView.onStart();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
