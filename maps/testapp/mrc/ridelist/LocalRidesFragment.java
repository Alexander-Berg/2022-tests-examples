package com.yandex.maps.testapp.mrc.ridelist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.maps.testapp.R;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.LocalRidesListener;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.ride.MRCFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalRidesFragment extends Fragment {
    private RideManager rideManager;
    private RideListAdapter ridesListAdapter;

    private static final String TAG = "LocalRidesFragment";

    public LocalRidesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        rideManager.subscribe(localRidesListener);
        refreshRides();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        rideManager.unsubscribe(localRidesListener);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_mrc_local_rides, container, false);
        rideManager = MRCFactory.getInstance().getRideManager();
        ImageDownloader imageDownloader = MRCFactory.getInstance().getImageDownloader();
        ridesListAdapter = new RideListAdapter(requireContext(), imageDownloader, Collections.emptyList());

        RecyclerView ridesListView = view.findViewById(R.id.rides_list);
        ridesListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ridesListView.setAdapter(ridesListAdapter);

        return view;
    }

    private final LocalRidesListener localRidesListener = this::refreshRides;

    void refreshRides() {
        List<RideListItem> rideList = new ArrayList<>();
        for (LocalRide localRide : rideManager.getLocalRides()) {
            Integer totalPhotos = localRide.getBriefRideInfo().getPhotosCount();
            if (totalPhotos == null || totalPhotos == 0) {
                continue;
            }

            rideList.add(new RideListItem(localRide));
        }
        ridesListAdapter.setRidesList(rideList);
    }
}
