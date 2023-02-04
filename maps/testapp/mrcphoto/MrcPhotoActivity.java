package com.yandex.maps.testapp.mrcphoto;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoPhoto.ShootingPoint;
import com.yandex.mapkit.geometry.Direction;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.mrc.MrcPhotoLayer;
import com.yandex.mapkit.places.mrc.MrcPhotoPlayer;
import com.yandex.mapkit.places.mrc.MrcPhotoService;
import com.yandex.mapkit.places.mrc.MrcPhotoView;
import com.yandex.mapkit.places.mrc.HistoricalPhoto;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.settings.SharedPreferencesConsts;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class MrcPhotoActivity extends Activity
        implements InputListener, MrcPhotoPlayer.MrcPhotoPlayerListener {

    private static Logger LOGGER = Logger.getLogger("yandex.maps.MrcPhotoActivity");
    public static final String PARAM_PHOTO_ID = "PHOTO_ID";
    public static final String PARAM_VISIBLE_LAYER = "VISIBLE_LAYER";

    private MrcPhotoLayer mrcPhotoLayer;
    private MrcPhotoService mrcPhotoService;
    private MrcPhotoService.SearchSession searchSession;

    private String photoId;
    private MrcPhotoLayer.VisibleLayer layer;

    private MrcPhotoView mrcPhotoView;
    private MrcPhotoPlayer mrcPhotoPlayer;
    private MapView mapView;
    private Map map;
    private PlacemarkMapObject placemark;

    private EditText photoIdEditText;
    private CheckBox showConnectionsCheckbox;

    private Spinner historicalSpinner;
    private List<HistoricalPhoto> historicalPhotos;
    private ArrayAdapter<CharSequence> historicalAdapter;

    private Locale locale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.info("onCreate");

        setContentView(R.layout.mrc_photo);

        mrcPhotoView = findViewById(R.id.mrcphoto_view);

        mapView = findViewById(R.id.mrcphoto_mapview);
        map = mapView.getMap();
        placemark = map.getMapObjects().addPlacemark(new Point(0.0f, 0.0f));
        placemark.setIcon(
                ImageProvider.fromResource(this, R.drawable.mrc_photo_marker),
                new IconStyle().setRotationType(RotationType.ROTATE));
        placemark.setVisible(false);

        map.move(
                new CameraPosition(new Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);
        map.addInputListener(this);

        photoIdEditText = findViewById(R.id.photo_id_input);
        showConnectionsCheckbox = findViewById(R.id.show_connections_checkbox);
        showConnectionsCheckbox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                mrcPhotoPlayer.enableMove();
            } else {
                mrcPhotoPlayer.disableMove();
            }
        });

        setupLocale();
        setupHistoricalSpinner();

        mrcPhotoLayer = PlacesFactory.getInstance().createMrcPhotoLayer(mapView.getMapWindow());
        mrcPhotoService = PlacesFactory.getInstance().createMrcPhotoService();
        mrcPhotoPlayer = mrcPhotoView.getPlayer();

        photoId = getIntent().getStringExtra(PARAM_PHOTO_ID);
        layer = (MrcPhotoLayer.VisibleLayer)getIntent().getSerializableExtra(PARAM_VISIBLE_LAYER);
        if (photoId == null) {
            LOGGER.severe("Photo ID must be provided in intent extra");
            finish();
            return;
        }

        if (layer == null) {
            LOGGER.severe("Visible layer must be provided in intent extra");
            finish();
            return;
        }

        LOGGER.info("photo id: " + photoId);
        LOGGER.info("visible layer: " + layer.name());
        mrcPhotoLayer.enable(layer);
        mrcPhotoPlayer.setVisibleLayer(layer);
        mrcPhotoPlayer.enableMove();
        mrcPhotoPlayer.openPhoto(photoId);
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        LOGGER.info("onStop");
        mrcPhotoPlayer.removeListener(this);
        mapView.onStop();
        mrcPhotoView.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOGGER.info("onStart");
        mapView.onStart();
        mrcPhotoView.onStart();
        mrcPhotoPlayer.addListener(this);
    }

    @Override
    public void onPhotoUpdated(@NonNull MrcPhotoPlayer player) {
        String photoId = player.getPhotoId();
        photoIdEditText.setText(photoId != null ? photoId : "");

        updateHistoricalSpinner(player, photoId);

        ShootingPoint shootingPoint = player.getShootingPoint();
        if (shootingPoint != null) {
            placemark.setGeometry(shootingPoint.getPoint().getPoint());

            Direction direction = shootingPoint.getDirection();
            placemark.setDirection(direction != null ? (float)direction.getAzimuth() : 0f);
            placemark.setVisible(true);

            CameraPosition cp = map.getCameraPosition();
            map.move(
                    new CameraPosition(
                            shootingPoint.getPoint().getPoint(),
                            cp.getZoom(),
                            cp.getAzimuth(),
                            cp.getTilt()),
                    new Animation(Animation.Type.SMOOTH, 0.0f),
                    null);
        } else {
            placemark.setVisible(false);
        }

    }

    @Override
    public void onPhotoOpenError(@NonNull MrcPhotoPlayer mrcPhotoPlayer, @NonNull Error error) {
        Toast.makeText(this, "onPhotoOpenError", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
        LOGGER.info("onMapTap");
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        MrcPhotoLayer.VisibleLayer layer = mrcPhotoLayer.getVisibleLayer();
        if (layer != null) {
            searchSession = mrcPhotoService.findNearestPhoto(
                    layer,
                    point,
                    (int) map.getCameraPosition().getZoom(),
                    searchListener);
        } else {
            Toast.makeText(this, "Visible layer must be set", Toast.LENGTH_LONG).show();
        }
    }

    public void onOpenPhotoIdClick(View view) {
        String photoId = photoIdEditText.getText().toString();
        if (!photoId.trim().isEmpty()) {
            LOGGER.info("Opening photo ID " + photoId);
            mrcPhotoPlayer.openPhoto(photoId);
        }
    }

    private void setupLocale() {
        SharedPreferences i18nPrefs = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
        String lang = i18nPrefs.getString(SharedPreferencesConsts.LOCALE_KEY, "");

        locale = Locale.forLanguageTag(lang);
    }

    private void setupHistoricalSpinner() {
        historicalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        historicalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        historicalSpinner = findViewById(R.id.historical_spinner);
        historicalSpinner.setOnItemSelectedListener(historicalSpinnerListener);
        historicalSpinner.setAdapter(historicalAdapter);
    }

    private void updateHistoricalSpinner(@NonNull MrcPhotoPlayer player, String photoId) {
        historicalPhotos = player.historicalPhotos();
        historicalAdapter.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", locale);
        for (HistoricalPhoto histPhoto : historicalPhotos) {
            Date date = new Date(histPhoto.getTimestamp() * 1000);
            historicalAdapter.add(dateFormat.format(date));
        }
        historicalAdapter.notifyDataSetChanged();
        historicalSpinner.setSelection(historicalAdapter.getPosition(photoId));
    }

    private MrcPhotoService.SearchListener searchListener = new MrcPhotoService.SearchListener() {
        @Override
        public void onPhotoSearchResult(@NonNull String photoId) {
            mrcPhotoPlayer.openPhoto(photoId);
        }

        @Override
        public void onPhotoSearchError(@NonNull Error error) {
            Toast.makeText(MrcPhotoActivity.this,
                    "Failed to open mrc photo: " + error.toString(), Toast.LENGTH_LONG).show();
        }
    };

    private AdapterView.OnItemSelectedListener historicalSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {

            LOGGER.info("Selected historical photo: " + adapterView.getItemAtPosition(pos)
                + ", photo ID: " + historicalPhotos.get(pos).getPhotoId());
            mrcPhotoPlayer.openPhoto(historicalPhotos.get(pos).getPhotoId());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
}
