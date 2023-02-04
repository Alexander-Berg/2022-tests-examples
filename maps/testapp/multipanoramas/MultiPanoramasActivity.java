package com.yandex.maps.testapp.multipanoramas;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.panorama.PanoramaView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

public class MultiPanoramasActivity extends TestAppActivity {
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_panoramas);

        String[] panoIds = {
           "1254609763_626282274_23_1372522303",
           "1297968142_673435466_23_1383399425",
           "1336101368_667121458_23_1352632553",
           "1580470607_665414401_23_1284497170",
           "1569412342_682750560_23_1283746141"
        };

        MultiPanoramasItemAdapter adapter = new MultiPanoramasItemAdapter(this, panoIds);
        listView = (ListView) findViewById(R.id.multipanoramas_item_list);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStopImpl() {
        for (int i = 0; i < listView.getCount(); i++) {
            View child = listView.getChildAt(i);
            if (child != null) {
                ((PanoramaView)child.findViewById(R.id.single_panoview)).onStop();
            }
        }
    }

    @Override
    protected void onStartImpl() {
        for (int i = 0; i < listView.getCount(); i++) {
            View child = listView.getChildAt(i);
            if (child != null) {
                ((PanoramaView)child.findViewById(R.id.single_panoview)).onStart();
            }
        }
    }
}
