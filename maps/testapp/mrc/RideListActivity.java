package com.yandex.maps.testapp.mrc;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.yandex.maps.auth.AccountFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.mrc.ridelist.SectionsPagerAdapter;
import com.yandex.mrc.ride.MRCFactory;

public class RideListActivity extends BaseMrcActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MRCFactory.initialize(this);
        AccountFactory.initialize(this);

        setContentView(R.layout.activity_mrc_ride_list);
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
