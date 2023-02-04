package com.yandex.maps.testapp.mrc;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import android.widget.Toast;
import com.yandex.maps.auth.AccountFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.mrc.walklist.SectionsPagerAdapter;
import com.yandex.mrc.ride.MRCFactory;

public class PlacemarksListActivity extends BaseMrcActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MRCFactory.initialize(this);
        AccountFactory.initialize(this);

        setContentView(R.layout.activity_mrc_placemarks_list);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AuthUtil.getCurrentAccount() == null) {
            Toast.makeText(this, R.string.sign_into_account, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
