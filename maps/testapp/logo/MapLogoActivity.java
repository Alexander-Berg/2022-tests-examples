package com.yandex.maps.testapp.logo;

import android.os.Bundle;
import android.widget.RadioGroup;

import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.HorizontalAlignment;
import com.yandex.mapkit.logo.VerticalAlignment;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;

public class MapLogoActivity extends MapBaseActivity {
    private HorizontalAlignment horizontalAlignment;
    private VerticalAlignment verticalAlignment;
    private Map map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.logo);
        super.onCreate(savedInstanceState);

        verticalAlignment = VerticalAlignment.BOTTOM;
        horizontalAlignment = HorizontalAlignment.RIGHT;

        map = mapview.getMap();
        updateAlignment();

        RadioGroup verticalAlignmentSwitch = (RadioGroup)findViewById(R.id.vertical_alignment);
        verticalAlignmentSwitch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.logo_top) {
                    verticalAlignment =  VerticalAlignment.TOP;
                } else if (checkedId == R.id.logo_bottom) {
                    verticalAlignment =  VerticalAlignment.BOTTOM;
                }

                updateAlignment();
            }
        });

        RadioGroup horizontalAlignmentSwitch = (RadioGroup)findViewById(R.id.horizontal_alignment);
        horizontalAlignmentSwitch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.logo_left) {
                    horizontalAlignment =  HorizontalAlignment.LEFT;
                } else if (checkedId == R.id.logo_center) {
                    horizontalAlignment =  HorizontalAlignment.CENTER;
                } else if (checkedId == R.id.logo_right) {
                    horizontalAlignment =  HorizontalAlignment.RIGHT;
                }

                updateAlignment();
            }
        });
    }

    private void updateAlignment() {
        map.getLogo().setAlignment(new Alignment(horizontalAlignment, verticalAlignment));
    }
}
