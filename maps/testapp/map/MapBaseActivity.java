package com.yandex.maps.testapp.map;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.MemoryUsage;
import com.yandex.mapkit.ResourcesMemoryUsage;
import com.yandex.mapkit.StyleType;
import com.yandex.mapkit.TilesMemoryUsage;
import com.yandex.mapkit.ZoomRangedMemoryUsage;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.map.MapMode;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.traffic.TrafficLayer;
import com.yandex.maps.testapp.MapkitAdapter;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.VulkanTools;
import com.yandex.maps.testapp.experiments.Experiment;
import com.yandex.maps.testapp.experiments.ExperimentsUtils;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.runtime.view.GraphicsAPIType;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.yandex.maps.testapp.Utils.clamp;

public class MapBaseActivity extends TestAppActivity {
    public enum StyleTarget {
        BASEMAP,
        TRAFFIC,
        CARPARKS
    }

    private DrawerLayout drawerLayout;
    private ToggleButton[] mapModeSelectors = new ToggleButton[5];
    protected ViewGroup toolbar;
    protected MapView mapview;
    protected TrafficLayer traffic;
    private AlertDialog messageDialog;
    protected static final float ZOOM_TIME = 0.2f;
    private static final float ZOOM_DIFF = 1;
    protected CompoundButton vulkanSwitchBt;
    protected CompoundButton jamsSwitchBt;
    protected CompoundButton modelSwitchBt;
    protected CompoundButton mode2DBt;
    private static final String MAP_SETTINGS_FILE = "map_settings";
    private static final String MAP_TYPE_KEY = "map_type";
    private MapCustomizationDialog<StyleTarget> customizationDialog;
    private static final Point CAMERA_TARGET = new Point(59.945933, 30.320045);
    private float modelsScale = 1.f;
    private float boxesScale = 1.f;

    public static final String INTENT_CAMERA_POSITION = "intent_camera_position";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStopImpl() {
        mapview.onStop();

        //dialogs should be dismissed to avoid leaking
        if (messageDialog != null) {
            messageDialog.dismiss();
        }
    }

    @Override
    protected void onStartImpl() {
        mapview.onStart();
    }

    public void onZoomClick(View view) {
        int id = view.getId();

        if (id == R.id.zoom_in) {
            zoom(ZOOM_DIFF);
        } else {
            zoom(-ZOOM_DIFF);
        }
    }

    protected void zoom(float zoomChange) {
        Map map = mapview.getMap();
        CameraPosition cameraPosition = map.getCameraPosition();
        map.move(
                new CameraPosition(
                        cameraPosition.getTarget(),
                        cameraPosition.getZoom() + zoomChange,
                        cameraPosition.getAzimuth(),
                        cameraPosition.getTilt()),
                new Animation(Animation.Type.SMOOTH, ZOOM_TIME),
                null);
    }

    protected boolean vulkanPreferred() {
        if (getIntent().hasExtra(VulkanTools.VULKAN_PREFERRED_KEY)) {
            boolean vulkanPreferredByIntent = getIntent().getBooleanExtra(VulkanTools.VULKAN_PREFERRED_KEY, false);
            VulkanTools.storeVulkanPreferred(getApplicationContext(), vulkanPreferredByIntent);
        }
        return VulkanTools.readVulkanPreferred(getApplicationContext());
    }

    public boolean vulkanEnabled() {
        return mapview.getGraphicsAPI() == GraphicsAPIType.VULKAN;
    }

    protected void moveMap(Point point, float timeInSeconds) {
        CameraPosition currentPosition = mapview.getMap().getCameraPosition();
        CameraPosition position = new CameraPosition(
                point,
                currentPosition.getZoom(),
                currentPosition.getAzimuth(),
                currentPosition.getTilt());

        mapview.getMap().move(position, new Animation(Animation.Type.SMOOTH, timeInSeconds), null);
    }

    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private void logZoomRangeMemUsage(String what, List<ZoomRangedMemoryUsage> collection) {
        for (ZoomRangedMemoryUsage memUsage : collection) {
            LOGGER.warning(String.format("%s [%d, %d)=%.3f KB",
                what, memUsage.getZoomRange().getZMin(), memUsage.getZoomRange().getZMax(),
                memUsage.getValue() / 1024.f));
        }
    }

    public MemoryUsage memoryUsage() {
        return mapview.getMap().getDebugInfo().getMemUsage();
    }

    private void logMemUsage() {
        MemoryUsage memUsage = memoryUsage();
        TilesMemoryUsage tilesMemUsage = memUsage.getTiles();
        ResourcesMemoryUsage resourcesMemUsage = memUsage.getResources();
        LOGGER.warning(String.format("Total memory usage=%.3f KB", memUsage.getTotal() / 1024.f));
        LOGGER.warning(String.format("Resources: models count=%d, models=%.3f KB, textures=%.3f KB, glyphs=%.3f KB",
            resourcesMemUsage.getModelsCount(),
            resourcesMemUsage.getModels() / 1024.f,
            resourcesMemUsage.getTextures() / 1024.f,
            resourcesMemUsage.getGlyphs() / 1024.f));
        LOGGER.warning(String.format(
            "Tiles: tiles count=%d, point labels count=%d, polyline labels count=%d, placemarks count=%d",
            tilesMemUsage.getTilesCount(),
            tilesMemUsage.getPointLabelsCount(),
            tilesMemUsage.getPolylineLabelsCount(),
            tilesMemUsage.getPlacemarksCount()));
        LOGGER.warning(String.format(
            "Tiles: point labels=%.3f KB, polyline labels=%.3f KB, placemarks=%.3f KB, arena=%.3f KB, raster=%.3f KB, other=%.3f KB",
            tilesMemUsage.getPointLabels() / 1024.f,
            tilesMemUsage.getPolylineLabels() / 1024.f,
            tilesMemUsage.getPlacemarks() / 1024.f,
            tilesMemUsage.getArena() / 1024.f,
            tilesMemUsage.getRaster() / 1024.f,
            tilesMemUsage.getOtherMemUsage() / 1024.f));
        logZoomRangeMemUsage("Geometry render objects", tilesMemUsage.getGeometryRenderObjects());
        logZoomRangeMemUsage("Geometry render states", tilesMemUsage.getGeometryRenderStates());
        logZoomRangeMemUsage("Building render objects", tilesMemUsage.getBuildingRenderObjects());
        logZoomRangeMemUsage("Building render states", tilesMemUsage.getBuildingRenderStates());
        logZoomRangeMemUsage("Building indices", tilesMemUsage.getBuildingIndices());
    }

    private void testRedraw() {
        CameraPosition position = mapview.getMap().getCameraPosition();
        Point oldTarget = position.getTarget();
        Point newTarget = new Point(oldTarget.getLatitude(), oldTarget.getLongitude() - 0.01);
        moveMap(newTarget, 20.0f);
    }

    protected static String toString(MapLoadStatistics statistics) {
        return String.format("CzG:%.2f CzPl:%.2f CzL:%.2f DG:%.2f CzM:%.2f F:%.2f A:%.2f",
                statistics.getCurZoomGeometryLoaded() / 1000.f,
                statistics.getCurZoomPlacemarksLoaded() / 1000.f,
                statistics.getCurZoomLabelsLoaded() / 1000.f,
                statistics.getDelayedGeometryLoaded() / 1000.0f,
                statistics.getCurZoomModelsLoaded() / 1000.f,
                statistics.getFullyLoaded() / 1000.f,
                statistics.getFullyAppeared() / 1000.f);
    }

    protected void showMessage(final String message, final String title) {
        final Context context = this;
        if (messageDialog != null) {
            //should be dissmissed otherwise it is leaked
            messageDialog.dismiss();
        }
        messageDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("Copy to Clipboard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(null, message);
                        assert clipboard != null;
                        clipboard.setPrimaryClip(clip);
                    }
                })
                .show();
    }

    private void initMap() {
        mapview = findViewById(R.id.mapview);
        MapType mapType = readMapType(getApplicationContext());
        mapview.getMap().setMapType(mapType);
        onInitMap();
    }

    protected void onInitMap() {
        traffic = MapKitFactory.getInstance().createTrafficLayer(mapview.getMapWindow());

        CameraPosition initCameraPosition = new CameraPosition(CAMERA_TARGET, 15.0f, 0.0f, 0.0f);
        byte[] position = getIntent().getByteArrayExtra(INTENT_CAMERA_POSITION);
        if (position != null) {
            initCameraPosition = Serialization.deserializeFromBytes(position, CameraPosition.class);
        }

        mapview.getMap().move(initCameraPosition);
    }

    private void showMapType() {
        MapType mapType = mapview.getMap().getMapType();
        for (ToggleButton bt : mapModeSelectors) {
            bt.setChecked(false);
        }
        mapModeSelectors[mapType.ordinal()].setChecked(true);
    }

    protected void setMapType(MapType mapType) {
        mapview.getMap().setMapType(mapType);
        storeMapType(getApplicationContext(), mapType);
        showMapType();
    }

    private void initMapModeSelectors() {
        mapModeSelectors[MapType.MAP.ordinal()] = findViewById(R.id.raster_map);
        mapModeSelectors[MapType.MAP.ordinal()].setTag(MapType.MAP.ordinal());

        mapModeSelectors[MapType.VECTOR_MAP.ordinal()] = findViewById(R.id.vector_map);
        mapModeSelectors[MapType.VECTOR_MAP.ordinal()].setTag(MapType.VECTOR_MAP.ordinal());

        mapModeSelectors[MapType.SATELLITE.ordinal()] = findViewById(R.id.sattelite_map);
        mapModeSelectors[MapType.SATELLITE.ordinal()].setTag(MapType.SATELLITE.ordinal());

        mapModeSelectors[MapType.HYBRID.ordinal()] = findViewById(R.id.hybrid_map);
        mapModeSelectors[MapType.HYBRID.ordinal()].setTag(MapType.HYBRID.ordinal());

        mapModeSelectors[MapType.NONE.ordinal()] = findViewById(R.id.none_map);
        mapModeSelectors[MapType.NONE.ordinal()].setTag(MapType.NONE.ordinal());

        showMapType();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToggleButton selectedBt = (ToggleButton)view;
                setMapType(MapType.values()[(Integer) selectedBt.getTag()]);
            }
        };

        for (ToggleButton bt : mapModeSelectors) {
            bt.setOnClickListener(listener);
        }
    }

    private void initToolBar() {
        findViewById(R.id.options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        toolbar = findViewById(R.id.toolbar);
        initMapModeSelectors();
    }

    private void enableScaleFactorControl() {
        final EditText scaleFactorEdit = findViewById(R.id.scaleFactor);
        scaleFactorEdit.setText(String.valueOf(mapview.getScaleFactor()));
        scaleFactorEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String scaleFactorStr = scaleFactorEdit.getText().toString();
                if (scaleFactorStr.isEmpty()) {
                    scaleFactorEdit.setText(String.valueOf(mapview.getScaleFactor()));
                    return false;
                }
                Float value = Float.valueOf(scaleFactorStr);
                float clampedValue = clamp(value, 1, 8);
                scaleFactorEdit.setText(String.valueOf(clampedValue));
                mapview.setScaleFactor(clampedValue);
                return false;
            }
        });
    }

    private void syncPoiLimitEditText() {
        final EditText poiLimitEdit = (EditText) findViewById(R.id.poiLimit);
        Integer poiLimit = mapview.getMap().getPoiLimit();
        if (poiLimit != null) {
            poiLimitEdit.setText(String.valueOf(poiLimit));
        } else {
            poiLimitEdit.setText("");
        }
    }

    protected void setPoiLimit(Integer poiLimit) {
        mapview.getMap().setPoiLimit(poiLimit);
        syncPoiLimitEditText();
    }

    private void enablePoiLimitControl() {
        final EditText poiLimitEdit = findViewById(R.id.poiLimit);
        syncPoiLimitEditText();

        poiLimitEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Integer newPoiLimit = null;
                try {
                    String value = poiLimitEdit.getText().toString().trim();
                    if (!value.isEmpty()) {
                        newPoiLimit = Integer.valueOf(value);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Failed to parse poiLimit: " + e.toString(),
                            Toast.LENGTH_LONG).show();

                    syncPoiLimitEditText();
                    return false;
                }
                setPoiLimit(newPoiLimit);
                return false;
            }
        });

        poiLimitEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        vulkanSwitchBt = findViewById(R.id.vulkan_switch);
        vulkanSwitchBt.setChecked(vulkanEnabled());
        vulkanSwitchBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                VulkanTools.storeVulkanPreferred(getApplicationContext(), isChecked);
                restartApp();
            }
        });

        if (vulkanEnabled()) {
            findViewById(R.id.lite_mode_switch).setEnabled(false);
        } else {
            CompoundButton liteModeBt = findViewById(R.id.lite_mode_switch);
            liteModeBt.setChecked(mapview.getMap().isLiteModeEnabled());
            liteModeBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mapview.getMap().setLiteModeEnabled(isChecked);
                }
            });
        }

        CompoundButton switchFontExperimentButton = findViewById(R.id.switch_experimental_v3font);
        if (MapkitAdapter.getMapStyleType(getApplicationContext()) == StyleType.V_MAP3) {
            final Experiment experiment = new Experiment("MAPS_CONFIG", "experimental_config", "MAPS_RENDERER:experimental_fonts=test");
            switchFontExperimentButton.setChecked(ExperimentsUtils.loadExperimentsList(getApplicationContext()).contains(experiment));
            switchFontExperimentButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        ExperimentsUtils.refreshCustomExperiment(experiment);
                        ExperimentsUtils.addExperimentToDump(experiment, getApplicationContext());
                    } else {
                        ExperimentsUtils.resetCustomExperiment(experiment);
                        ExperimentsUtils.removeExperimentFromDump(experiment, getApplicationContext());
                    }
                }
            });
        } else {
            switchFontExperimentButton.setVisibility(View.GONE);
        }

        jamsSwitchBt = findViewById(R.id.jams_switch);
        jamsSwitchBt.setChecked(traffic.isTrafficVisible());
        jamsSwitchBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                traffic.setTrafficVisible(isChecked);
            }
        });

        CompoundButton nightModeBt = findViewById(R.id.night_mode_switch);
        nightModeBt.setChecked(mapview.getMap().isNightModeEnabled());
        nightModeBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setNightModeEnabled(isChecked);
            }
        });

        CompoundButton debugInfoBt = findViewById(R.id.debug_info_switch);
        debugInfoBt.setChecked(mapview.getMap().isDebugInfoEnabled());
        debugInfoBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setDebugInfoEnabled(isChecked);
            }
        });

        CompoundButton tappableAreaRenderingBt = findViewById(R.id.tappable_area_rendering_switch);
        tappableAreaRenderingBt.setChecked(mapview.getMap().isTappableAreaRenderingEnabled());
        tappableAreaRenderingBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setTappableAreaRenderingEnabled(isChecked);
            }
        });

        modelSwitchBt = findViewById(R.id.model_switch);
        modelSwitchBt.setChecked(mapview.getMap().isModelsEnabled());
        modelSwitchBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 setModelsEnabled(isChecked);
             }
        });

        mode2DBt = findViewById(R.id.mode_2d_switch);
        mode2DBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().set2DMode(isChecked);
            }
        });

        Spinner styleSpinner = findViewById(R.id.map_style_type);
        styleSpinner.setSelection(MapkitAdapter.getMapStyleType(getApplicationContext()).ordinal());
        styleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (MapkitAdapter.getMapStyleType(getApplicationContext()).ordinal() == (int) id) {
                    return;
                }
                MapkitAdapter.setMapStyleType(getApplicationContext(), StyleType.values()[(int) id]);
                restartApp();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner scenarioSpinner = findViewById(R.id.scenario_switch);
        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mapview.getMap().setMode(MapMode.values()[(int)id]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.log_mem_usage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logMemUsage();
            }
        });

        findViewById(R.id.redraw_20s_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testRedraw();
            }
        });

        findViewById(R.id.memory_warning).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapview.onMemoryWarning();
            }
        });

        enableScaleFactorControl();
        enablePoiLimitControl();
        initCustomization();
        initBuildingsScaleControl();
    }

    protected void setNightModeEnabled(boolean enabled) {
        mapview.getMap().setNightModeEnabled(enabled);
    }

    protected void setModelsEnabled(boolean enabled) {
        mapview.getMap().setModelsEnabled(enabled);
    }

    private void initBuildingsScaleControl() {
        final TextView modelsScaleTextValue = findViewById(R.id.modelsScaleValue);
        final TextView boxesScaleTextValue = findViewById(R.id.boxesScaleValue);

        findViewById(R.id.modelsScaleInc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelsScale += 0.25f;
                if (modelsScale >= 3.f) modelsScale = 3.f;
                modelsScaleTextValue.setText(Float.toString(modelsScale));
                mapview.getMap().setBuildingsHeightScale(boxesScale, modelsScale);
            }
        });

        findViewById(R.id.modelsScaleDec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelsScale -= 0.25f;
                if (modelsScale <= 0.f) modelsScale = 0.f;
                modelsScaleTextValue.setText(Float.toString(modelsScale));
                mapview.getMap().setBuildingsHeightScale(boxesScale, modelsScale);
            }
        });

        findViewById(R.id.boxesScaleInc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boxesScale += 0.25f;
                if (boxesScale >= 3.f) boxesScale = 3.f;
                boxesScaleTextValue.setText(Float.toString(boxesScale));
                mapview.getMap().setBuildingsHeightScale(boxesScale, modelsScale);
            }
        });

        findViewById(R.id.boxesScaleDec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boxesScale -= 0.25f;
                if (boxesScale <= 0.f) boxesScale = 0.f;
                boxesScaleTextValue.setText(Float.toString(boxesScale));
                mapview.getMap().setBuildingsHeightScale(boxesScale, modelsScale);
            }
        });
    }

    private void initCustomization() {
        HashMap<StyleTarget, Integer> templates = new HashMap<>();
        templates.put(StyleTarget.BASEMAP, R.menu.basemap_customization_templates);
        templates.put(StyleTarget.TRAFFIC, R.menu.traffic_customization_templates);

        MapCustomizationDialog.StyleHandler<StyleTarget> handler = new MapCustomizationDialog.StyleHandler<StyleTarget>() {

            private HashMap<StyleTarget, String> postponedCustomization =
                    new HashMap<>();

            private Button customizeButton = null;
            private Button getCustomizeButton() {
                if (customizeButton == null) {
                    customizeButton = findViewById(R.id.customize_button);
                    customizeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            applyStyleImpl();
                        }
                    });
                }
                return customizeButton;
            }

            private void applyStyleImpl() {
                for (HashMap.Entry<StyleTarget, String> entry : postponedCustomization.entrySet()) {
                    final StyleTarget styleTarget = entry.getKey();
                    final String style = entry.getValue();
                    switch (styleTarget) {
                        case BASEMAP:
                            mapview.getMap().setMapStyle(style);
                            break;
                        case TRAFFIC:
                            traffic.setTrafficStyle(style);
                            traffic.setRoadEventsStyle(style);
                            break;
                    }
                }
                postponedCustomization.clear();

                getCustomizeButton().setVisibility(View.GONE);
            }

            @Override
            public void applyStyle(StyleTarget styleTarget, final String style) {
                MapBaseActivity.this.drawerLayout.closeDrawer(GravityCompat.START);
                postponedCustomization.put(styleTarget, style);
                applyStyleImpl();
            }

            @Override
            public void saveStyle(StyleTarget styleTarget, final String style) {
                MapBaseActivity.this.drawerLayout.closeDrawer(GravityCompat.START);
                getCustomizeButton().setVisibility(View.VISIBLE);
                postponedCustomization.put(styleTarget, style);
            }
        };

        customizationDialog = new MapCustomizationDialog(this, handler, templates);

        findViewById(R.id.basemap_customization_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customizationDialog.show(StyleTarget.BASEMAP);
            }
        });

        findViewById(R.id.traffic_customization_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customizationDialog.show(StyleTarget.TRAFFIC);
            }
        });
    }

    private void restartApp() {
        Intent startCurActivity = new Intent(this, getClass());
        startCurActivity.putExtra(INTENT_CAMERA_POSITION,
                Serialization.serializeToBytes(mapview.getMap().getCameraPosition()));

        startCurActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(startCurActivity);

        finish();
        Runtime.getRuntime().exit(0);
    }

    @Override
    public void setContentView(int layoutResID) {

        if (vulkanPreferred()) {
            setTheme(R.style.VulkanTheme);
        }

        super.setContentView(R.layout.map_base);
        ViewGroup contentView = findViewById(R.id.content_frame);
        View.inflate(this, layoutResID, contentView);

        initMap();

        if (vulkanPreferred() && !vulkanEnabled()) {
            Toast.makeText(getApplicationContext(), "Vulkan is not supported by this device", Toast.LENGTH_SHORT).show();
            VulkanTools.storeVulkanPreferred(getApplicationContext(), false);
        }

        initDrawer();
        initToolBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        if (this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public static MapType readMapType(final Context context) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        int index = sharedPref.getInt(MAP_TYPE_KEY, MapType.VECTOR_MAP.ordinal());
        return MapType.values()[index];
    }

    public static void storeMapType(final Context context, final MapType mapType) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(MAP_TYPE_KEY, mapType.ordinal());
        editor.commit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(MAP_SETTINGS_FILE, Context.MODE_PRIVATE);
    }
}
