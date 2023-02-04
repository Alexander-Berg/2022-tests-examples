package com.yandex.maps.testapp.mrc.ridelist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.yandex.maps.testapp.R;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {
    private static final int LOCAL_RIDES_TAB_ID = 0;
    private static final int SERVER_RIDES_TAB_ID = 1;
    private static final int COMBINED_RIDES_TAB_ID = 2;
    private static final int NUMBER_OF_TABS = 3;

    @StringRes
    private static final int[] TAB_TITLES = new int[]{
            R.string.mrc_tab_local_rides,
            R.string.mrc_tab_server_rides,
            R.string.mrc_tab_all_rides};
    private final Context context;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case LOCAL_RIDES_TAB_ID:
                return new LocalRidesFragment();
            case SERVER_RIDES_TAB_ID:
                return new ServerRidesFragment();
            case COMBINED_RIDES_TAB_ID:
                return new CombinedRidesFragment();
            default: throw new RuntimeException("Invalid rides tab position: " + position);
        }

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return context.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        return NUMBER_OF_TABS;
    }
}
