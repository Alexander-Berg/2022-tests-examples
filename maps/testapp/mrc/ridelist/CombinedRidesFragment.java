package com.yandex.maps.testapp.mrc.ridelist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.maps.testapp.R;
import com.yandex.mrc.BriefRideInfo;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.LocalRidesListener;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.ServerRide;
import com.yandex.mrc.ServerRideIdentifier;
import com.yandex.mrc.ServerRidesLoadingSession;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CombinedRidesFragment extends Fragment {
    private RideManager rideManager;
    private RideListAdapter ridesListAdapter;

    private Map<String, ServerRide> serverRidesById = new HashMap<>();

    private static final String TAG = "CombinedRidesFragment";
    // Loading limited number of rides simplifies implementation
    // and should be enough for testing purposes.
    private static final int RIDES_LIMIT = 1000;

    public CombinedRidesFragment() {
        // Required empty public constructor
    }

    public static CombinedRidesFragment newInstance() {
        return new CombinedRidesFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        rideManager.subscribe(localRidesListener);
        rideManager.loadServerRides(null, 0, RIDES_LIMIT, serverRidesListener);
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

    ServerRidesLoadingSession.ServerRidesListener serverRidesListener = new ServerRidesLoadingSession.ServerRidesListener() {
        @Override
        public void onRidesLoaded(@NonNull List<ServerRide> loadedServerRides) {
            serverRidesById.clear();
            for (ServerRide ride : loadedServerRides) {
                serverRidesById.put(ride.getBriefRideInfo().getId(), ride);
            }
            refreshRides();
        }

        @Override
        public void onRidesLoadingError(@NonNull Error error) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(requireActivity(), "Error while loading server rides", Toast.LENGTH_LONG).show();
            }
        }
    };

    void refreshRides() {
        List<RideListItem> rideList = new ArrayList<>();
        Set<String> addedRideIds = new HashSet<>();

        for (LocalRide localRide : rideManager.getLocalRides()) {
            Integer totalPhotos = localRide.getBriefRideInfo().getPhotosCount();
            if (totalPhotos == null || totalPhotos == 0) {
                continue;
            }

            String rideId = localRide.getBriefRideInfo().getId();
            ServerRide serverRide = serverRidesById.get(rideId);
            if (serverRide != null) {
                BriefRideInfo mergedInfo = rideManager.mergeRideInfos(
                        localRide.getBriefRideInfo(),
                        serverRide.getBriefRideInfo());
                rideList.add(new RideListItem(localRide, serverRide, mergedInfo));
                addedRideIds.add(rideId);
            } else {
                rideList.add(new RideListItem(localRide));
            }
        }

        for (Map.Entry<String, ServerRide> entry : serverRidesById.entrySet()) {
            if (!addedRideIds.contains(entry.getKey())) {
                rideList.add(new RideListItem((ServerRide) entry.getValue()));
            }
        }
        ridesListAdapter.setRidesList(rideList);
    }
}
