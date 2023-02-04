package com.yandex.maps.testapp.panorama;

import com.yandex.mapkit.Attribution;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.VulkanTools;

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

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.logging.Logger;

public class PanoActivity extends Activity implements PanoramaChangeListener,
                                                      DirectionChangeListener,
                                                      SpanChangeListener,
                                                      SearchListener,
                                                      InputListener,
                                                      ErrorListener,
                                                      AdapterView.OnItemSelectedListener {
    private static final float MOVE_TIME = 0.5f;

    private PanoramaView panoramaView;
    private Player player;
    private PlacemarkMapObject iconHandle;
    private MapView mapView;
    private PanoramaLayer panoramaLayer;
    private Map map;
    private PanoramaService service;
    private SearchSession searchSession;
    private String panoId;
    private Spinner historicalSpinner;
    private List<HistoricalPanorama> historicalPanoramas;
    private ArrayAdapter<String> historicalAdapter;
    private TextView panoramaAuthor;

    private static Logger LOGGER =
            Logger.getLogger("yandex.maps");

    @Override
    public void onPanoramaChanged(Player player) {
        LOGGER.info("Panorana changed");
        iconHandle.setGeometry(player.position());
        iconHandle.setVisible(true);
        CameraPosition cp = map.getCameraPosition();
        panoId = null;

        historicalPanoramas = player.historicalPanoramas();
        historicalAdapter.clear();
        for (HistoricalPanorama histPano : historicalPanoramas) {
            historicalAdapter.add(histPano.getName());
        }
        historicalAdapter.notifyDataSetChanged();
        historicalSpinner.setSelection(0);
        final Attribution attrib = player.attribution();
        if (attrib != null) {
            panoramaAuthor.setText("Author: " + attrib.getAuthor().getName());
        } else {
            panoramaAuthor.setText("Author: unknown");
        }

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

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        LOGGER.info("Historical select: " + parent.getItemAtPosition(pos));
        player.openPanorama(historicalPanoramas.get(pos).getPanoramaId());
    }

    public void onNothingSelected(AdapterView<?> parent) {
        LOGGER.info("Historical select: nothing");
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
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
            dlgAlert.setMessage(msg);
            dlgAlert.setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    openPanorama();
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

    @Override
    public void onPanoramaSearchError(Error error) {
        LOGGER.info("Error occured");
        if (error instanceof NotFoundError) {
            Toast.makeText(getApplicationContext(),
                "No panorama found in the neighborhood of given location",
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),
                "An error has occured while searching for nearest panorama",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPanoramaSearchResult(String panoid) {
        if (panoid.length() == 0) {
            LOGGER.info("Panorama not found");
        } else {
            LOGGER.info("Opening panorama id: " + panoid);
            player.openPanorama(panoid);
            player.setDirection(new Direction(map.getCameraPosition().getAzimuth(), 0.0f));
        }
    }

    @Override
    public void onMapTap(Map map, Point position) {
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        searchSession = service.findNearest(position, this);
    }

    public void onZoomIn(View view) {
        player.setSpan(new Span(0.0f, player.span().getVerticalAngle() / 1.2f));
    }

    public void onZoomOut(View view) {
        player.setSpan(new Span(0.0f, player.span().getVerticalAngle() * 1.2f));
    }

    public void onMarkersToggle(View view) {
        final int ACTIVE_COLOR = 0xBB00FF00;
        final int INACTIVE_COLOR = 0xBBFFFFFF;
        Button markersButton = (Button)view;
        if (player.markersEnabled()) {
            markersButton.setTextColor(INACTIVE_COLOR);
            player.disableMarkers();
        } else {
            markersButton.setTextColor(ACTIVE_COLOR);
            player.enableMarkers();
        }
    }

    public void onBackTap(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("PanoActivity.onDestory");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (vulkanPreferred()) {
            setTheme(R.style.VulkanTheme);
        }
        setContentView(R.layout.panorama);

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

        historicalAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        historicalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        historicalSpinner = findViewById(R.id.historical_spinner);
        historicalSpinner.setOnItemSelectedListener(this);
        historicalSpinner.setAdapter(historicalAdapter);

        panoramaAuthor = findViewById(R.id.panorama_author);

        service = PlacesFactory.getInstance().createPanoramaService();
        mapView = findViewById(R.id.panomapview);
        panoramaLayer = PlacesFactory.getInstance().createPanoramaLayer(mapView.getMapWindow());
        map = mapView.getMap();
        map.move(
                new CameraPosition(new Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);
        map.addInputListener(this);

        iconHandle = map.getMapObjects().addPlacemark(new Point(0.0f, 0.0f));
        iconHandle.setIcon(ImageProvider.fromResource(this, R.drawable.pano_marker));
        iconHandle.setVisible(false);

        Bundle bundle = getIntent().getExtras();
        panoId = bundle.getString("panoid");
        player.openPanorama(panoId);
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        panoramaView.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        panoramaView.onStart();
    }

    protected void openPanorama() {
        if (panoId != null)
            player.openPanorama(panoId);
    }

    protected boolean vulkanPreferred() {
        return VulkanTools.readVulkanPreferred(getApplicationContext());
    }
}
