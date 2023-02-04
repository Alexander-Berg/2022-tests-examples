package com.yandex.maps.testapp.toponym_photo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoTapInfo;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoLayer;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoService;
import com.yandex.mapkit.places.toponym_photo.PhotoSession.PhotoListener;
import com.yandex.mapkit.places.toponym_photo.PhotoSession;
import com.yandex.mapkit.places.toponym_photo.PhotoDescription;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.runtime.Error;

import java.util.logging.Logger;

public class MapActivity extends MapBaseActivity
                             implements PhotoListener, InputListener, GeoObjectTapListener {

    private EditText photoId;
    private CheckBox showLayer;

    private ToponymPhotoService toponymPhotoService;
    private ToponymPhotoLayer toponymPhotoLayer;
    private PhotoSession photoSession;

    // Displaying clicked image
    private RelativeLayout bigImageLayout;
    private PhotoView bigImage;
    private TextView bigImageAuthor;
    private Bitmap notFoundImage;

    private static Logger LOGGER =
            Logger.getLogger("yandex.maps");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.toponym_photo_map);
        super.onCreate(savedInstanceState);

        photoId = (EditText)findViewById(R.id.toponym_photo_id);
        showLayer = (CheckBox)findViewById(R.id.toponym_photo_map_showlayer);

        showLayer.setChecked(true);
        photoId.setOnEditorActionListener(
            new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(
                    TextView view, int actionId, KeyEvent event)
                {
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        photoSession = toponymPhotoService.photo(
                            view.getText().toString(),
                            MapActivity.this
                        );
                    }
                    return false;
                }
            });

        toponymPhotoService = PlacesFactory.getInstance().createToponymPhotoService();
        mapview.getMap().addInputListener(this);
        toponymPhotoLayer = PlacesFactory.getInstance().createToponymPhotoLayer(mapview.getMapWindow());
        showLayer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toponymPhotoLayer.setVisible(b);
            }
        });
        mapview.getMap().addTapListener(this);

        notFoundImage = BitmapFactory.decodeResource(
                getResources(), R.drawable.notfound);
        bigImageLayout = (RelativeLayout)findViewById(
                R.id.toponym_photo_full_size_layout);
        bigImage = (PhotoView)findViewById(
                R.id.toponym_photo_full_size);
        bigImage.init(toponymPhotoService, notFoundImage);
        bigImageAuthor = (TextView)findViewById(R.id.toponym_photo_author);
    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        LOGGER.info("Object tapped");
        ToponymPhotoTapInfo info = event.getGeoObject().getMetadataContainer().getItem(ToponymPhotoTapInfo.class);
        if (info == null) {
            return false;
        }
        event.setSelected(true);
        photoSession = toponymPhotoService.photo(info.getPhotoId(), MapActivity.this);
        return true;
    }

    @Override
    public void onMapTap(Map map, Point position) {
        LOGGER.info("Map tapped");
        mapview.getMap().deselectGeoObject();
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        LOGGER.info("Map long tapped");
    }

    @Override
    public void onPhotoError(Error error) {
        LOGGER.info("Error occured");
        Toast.makeText(getApplicationContext(),
            "An error has occured while searching for photo by id", Toast.LENGTH_LONG).show();
        mapview.getMap().deselectGeoObject();
    }

    @Override
    public void onPhotoReceived(PhotoDescription photo) {
        LOGGER.info("Opening photo id: " + photo.getId());
        openPhoto(photo);
    }

    private void openPhoto(PhotoDescription photo) {
        bigImageLayout.setVisibility(View.VISIBLE);
        bigImage.setImage(photo.getGeoPhoto().getImage().getUrlTemplate(), "XXL");
        bigImageAuthor.setText(photo.getGeoPhoto().getAttribution().getAuthor().getName());
        bigImageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapview.getMap().deselectGeoObject();
                bigImageLayout.setVisibility(View.GONE);
            }
        });
    }
}
