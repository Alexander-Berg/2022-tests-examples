package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.content.DialogInterface;
import android.text.InputType;
import android.app.AlertDialog;

import com.yandex.mapkit.StyleType;
import com.yandex.mapkit.ZoomRange;
import com.yandex.mapkit.layers.TileFormat;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.geometry.geo.Projections;
import com.yandex.mapkit.glyphs.GlyphUrlProvider;
import com.yandex.mapkit.glyphs.GlyphIdRange;
import com.yandex.mapkit.glyphs.DefaultGlyphUrlProvider;
import com.yandex.mapkit.images.DefaultImageUrlProvider;
import com.yandex.mapkit.layers.LayerOptions;
import com.yandex.mapkit.layers.Layer;
import com.yandex.mapkit.resource_url_provider.DefaultUrlProvider;
import com.yandex.maps.testapp.MapkitAdapter;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;

public class MapUserLayerActivity extends MapBaseActivity {
    private static final List<ZoomRange> ZOOM_RANGES = new ArrayList<ZoomRange>() {{
        add(new ZoomRange(0, 2 + 1));
        add(new ZoomRange(3, 5 + 1));
        add(new ZoomRange(6, 8 + 1));
        add(new ZoomRange(9, 10 + 1));
        add(new ZoomRange(11, 12 + 1));
        add(new ZoomRange(13, 14 + 1));
        add(new ZoomRange(15, 21 + 1));
    }};

    private class UserLayer {
        private final com.yandex.mapkit.tiles.DefaultUrlProvider tileUrlProvider =
            new com.yandex.mapkit.tiles.DefaultUrlProvider();
        private final DefaultImageUrlProvider imageUrlProvider = new DefaultImageUrlProvider();
        private final DefaultUrlProvider meshUrlProvider = new DefaultUrlProvider();
        private final DefaultUrlProvider stylesUrlProvider = new DefaultUrlProvider();
        private final DefaultGlyphUrlProvider glyphUrlProvider = new DefaultGlyphUrlProvider();
        private Layer layer = null;

        UserLayer(Map map, String layerBaseURL, String tileURLArguments, String version) {
            if (!tileURLArguments.isEmpty() && tileURLArguments.charAt(0) != '?')
                tileURLArguments = "?" + tileURLArguments;
            tileUrlProvider.setUrlPattern(layerBaseURL + "/tiles" + tileURLArguments);
            tileUrlProvider.setZoomRanges(ZOOM_RANGES);
            glyphUrlProvider.setUrlPattern(layerBaseURL + "/glyphs");
            imageUrlProvider.setUrlBase(layerBaseURL + "/icons");
            meshUrlProvider.setUrlBase(layerBaseURL + "/meshes");
            stylesUrlProvider.setUrlBase(layerBaseURL + "/styles");

            LayerOptions options = new LayerOptions();
            options.setNightModeAvailable(true);
            layer = map.addLayer(
                "user_layer",
                "application/octet-stream",
                MapkitAdapter.getMapStyleType(getApplicationContext()) ==
                        StyleType.V_MAP3 ? TileFormat.VECTOR3 : TileFormat.VECTOR,
                options,
                tileUrlProvider,
                imageUrlProvider,
                meshUrlProvider,
                stylesUrlProvider,
                glyphUrlProvider,
                Projections.getWgs84Mercator(),
                ZOOM_RANGES);
            layer.invalidate(version);
        }

        void remove() {
            layer.remove();
        }
    }

    private UserLayer userLayer;

    private String layerBaseURL = "https://proxy.mob.maps.yandex.net:443/mapkit2/layers/2.x/vmap2";
    private String tileURLArguments = "";
    EditText versionEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_user_layer);
        super.onCreate(savedInstanceState);

        versionEdit = (EditText)findViewById(R.id.version_edit);
        versionEdit.setText("17.04.16-0");

        setMapType(MapType.NONE);
        mapview.getMap().addYandexLayerId("user_layer");
    }

    public void showLayerBaseURLDialog(View view) {
        execDialog("Layer base URL", layerBaseURL, new AcceptCallback() {
            @Override
            public void onAccept(String result) {
                layerBaseURL = result;
            }
        });
    }

    public void showTileURLArgumentsDialog(View view) {
        execDialog("Tile URL additional arguments", tileURLArguments, new AcceptCallback() {
            @Override
            public void onAccept(String result) {
                tileURLArguments = result;
            }
        });
    }

    public void createLayer(View view) {
        String version = versionEdit.getText().toString();
        if (userLayer != null)
            userLayer.remove();
        userLayer = new UserLayer(mapview.getMap(), layerBaseURL, tileURLArguments, version);
    }

    interface AcceptCallback {
        void onAccept(String result);
    }

    private void execDialog(String title, String value, final AcceptCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(value);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onAccept(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
