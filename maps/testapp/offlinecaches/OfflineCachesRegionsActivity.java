package com.yandex.maps.testapp.offlinecaches;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.mapkit.offline_cache.RegionListUpdatesListener;
import com.yandex.mapkit.offline_cache.Region;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.runtime.LocalError;

import java.util.Comparator;

public class OfflineCachesRegionsActivity extends TestAppActivity {

    private class RegionListUpdatesListenerImpl implements RegionListUpdatesListener {
        private OfflineCachesRegionsActivity offlineCachesRegionsActivity;

         @Override
        public void onListUpdated() {
            offlineCachesRegionsActivity.updateListView();
        }

        public RegionListUpdatesListenerImpl(OfflineCachesRegionsActivity offlineCachesRegionsActivity) {
            this.offlineCachesRegionsActivity = offlineCachesRegionsActivity;
        }
    }

    private RegionListUpdatesListenerImpl regionListUpdatesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_caches_regions);
        regionView_ = (RegionView) findViewById(R.id.region_view);

        regionsAdapter_ = new RegionsAdapter(this);
        final ListView regionList = (ListView) findViewById(R.id.region_list);
        regionList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Region region = (Region) regionList.getItemAtPosition(position);

                regionView_.setRegion(region);
                showRegionView();
            }

        });
        regionList.setAdapter(regionsAdapter_);

        final EditText regionFilterEditText = (EditText)findViewById(R.id.region_filter);
        regionFilterEditText.addTextChangedListener(new FilterTextWatcher());

        offlineCacheManager_ = MapKitFactory.getInstance().getOfflineCacheManager();

        regionListUpdatesListener = new RegionListUpdatesListenerImpl(this);
        offlineCacheManager_.addRegionListUpdatesListener(regionListUpdatesListener);
        updateListView();
    }

    public void onRegionOpenClick(View view) {
        regionView_.onRegionOpenClick();
    }

    private static String getRegionText(Region region) {
        return region.getName() + ", " + region.getCountry();
    }

    private static String getRegionCities(Region region) {
        OfflineCacheManager offlineCacheManager = MapKitFactory.getInstance().getOfflineCacheManager();
        String cities = "";
        for (String city : offlineCacheManager.getCities(region.getId())) {
            if (!cities.isEmpty()) cities += ", ";
            cities += city;
        }
        return cities;
    }

    private void showRegionView() {
        regionView_.setVisibility(LinearLayout.VISIBLE);
    }

    private void hideRegionView() {
        regionView_.setVisibility(LinearLayout.GONE);
    }

    private void updateListView() {
        regionsAdapter_.clear();

        for (Region region : offlineCacheManager_.regions()) {
            boolean matched = filterRegionStr_.length() == 0
                || region.getName().toLowerCase().contains(filterRegionStr_)
                || region.getCountry().toLowerCase().contains(filterRegionStr_);
            if (!matched)
                for(String city : offlineCacheManager_.getCities(region.getId()))
                    if (city.toLowerCase().contains(filterRegionStr_)) {
                        matched = true;
                        break;
                    }
            if (matched)
                regionsAdapter_.add(region);
        }
        regionsAdapter_.sort(regionComparator_);

        if (!regionView_.isRegionValid()) {
            hideRegionView();
        } else {
            showRegionView();
        }
    }

    @Override
    protected void onStopImpl(){}
    @Override
    protected void onStartImpl(){}

    private class RegionsAdapter extends ArrayAdapter<Region> {

        public RegionsAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Region region = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView name = (TextView)convertView.findViewById(android.R.id.text1);
            name.setText(getRegionText(region));
            TextView cities = (TextView)convertView.findViewById(android.R.id.text2);
            cities.setText(getRegionCities(region));

            return convertView;
        }
    }

    private class FilterTextWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            filterRegionStr_ = s.toString().trim().toLowerCase();
            updateListView();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private class RegionComparator implements Comparator<Region> {

        @Override
        public int compare(Region lhs, Region rhs) {
            int countryCompare = lhs.getCountry().compareTo(rhs.getCountry());
            if (countryCompare != 0)
                return countryCompare;
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    private OfflineCacheManager offlineCacheManager_;
    private RegionsAdapter regionsAdapter_;
    private RegionView regionView_;
    private String filterRegionStr_ = "";
    private final RegionComparator regionComparator_ = new RegionComparator();
}
