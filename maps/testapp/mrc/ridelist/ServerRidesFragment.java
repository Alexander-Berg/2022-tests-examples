package com.yandex.maps.testapp.mrc.ridelist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.maps.testapp.R;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.LoadServerRidesStatSession;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.RidesStat;
import com.yandex.mrc.ServerRide;
import com.yandex.mrc.ServerRidesLoadingSession;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ServerRidesFragment extends Fragment {
    private RideManager rideManager;
    private RideListAdapter ridesListAdapter;

    private ServerRidesLoadingSession ridesLoadingSession;
    private LoadServerRidesStatSession statLoadingSession;

    TextView ridesTotalCount;
    TextView ridesTotalDistance;
    TextView ridesTotalDuration;
    TextView ridesTotalPhotosCount;

    private static final String TAG = "ServerRidesFragment";
    // Loading limited number of rides simplifies implementation
    // and should be enough for testing purposes.
    private static final int RIDES_LIMIT = 1000;

    public ServerRidesFragment() {
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
        ridesLoadingSession = rideManager.loadServerRides(null, 0, RIDES_LIMIT, serverRidesListener);
        statLoadingSession = rideManager.loadServerRidesStat(ridesStatListener);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_mrc_server_rides, container, false);

        rideManager = MRCFactory.getInstance().getRideManager();
        ImageDownloader imageDownloader = MRCFactory.getInstance().getImageDownloader();
        ridesListAdapter = new RideListAdapter(requireContext(), imageDownloader, Collections.emptyList());

        RecyclerView ridesListView = view.findViewById(R.id.rides_list);
        ridesListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ridesListView.setAdapter(ridesListAdapter);
        ridesListView.setItemViewCacheSize(20);
        ridesListView.setDrawingCacheEnabled(true);

        ridesTotalCount = view.findViewById(R.id.mrc_rides_total_count);
        ridesTotalDistance = view.findViewById(R.id.mrc_rides_total_distance);
        ridesTotalDuration = view.findViewById(R.id.mrc_rides_total_duration);
        ridesTotalPhotosCount = view.findViewById(R.id.mrc_rides_total_photos_count);

        return view;
    }

    void refreshRides(List<ServerRide> serverRides) {
        List<RideListItem> rideList = new ArrayList<>();
        for (ServerRide serverRide : serverRides) {
            rideList.add(new RideListItem(serverRide));
        }
        ridesListAdapter.setRidesList(rideList);
    }

    ServerRidesLoadingSession.ServerRidesListener serverRidesListener = new ServerRidesLoadingSession.ServerRidesListener() {
        @Override
        public void onRidesLoaded(@NonNull List<ServerRide> serverRides) {
            refreshRides(serverRides);
        }

        @Override
        public void onRidesLoadingError(@NonNull Error error) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(requireActivity(), "Error while loading server rides", Toast.LENGTH_LONG).show();
            }
        }
    };

    LoadServerRidesStatSession.LoadServerRidesStatListener ridesStatListener
            = new LoadServerRidesStatSession.LoadServerRidesStatListener() {
        @Override
        public void onServerRidesStatLoaded(@NonNull RidesStat ridesStat) {
            if (getActivity() == null || getActivity().isFinishing()) {
                return;
            }
            ridesTotalCount.setText(getString(R.string.mrc_rides_total_count, ridesStat.getRidesCount()));
            if (ridesStat.getTotalDistance() != null) {
                ridesTotalDistance.setText(
                        getString(R.string.mrc_rides_total_distance, ridesStat.getTotalDistance().getText()));
            }
            if (ridesStat.getTotalDuration() != null) {
                ridesTotalDuration.setText(
                        getString(R.string.mrc_rides_total_duration, ridesStat.getTotalDuration().getText()));
            }
            if (ridesStat.getTotalPhotosCount() != null && ridesStat.getTotalPublishedPhotosCount() != null) {
                ridesTotalPhotosCount.setText(
                        getString(R.string.mrc_rides_total_photos,
                            ridesStat.getTotalPublishedPhotosCount(),
                            ridesStat.getTotalPhotosCount()));
            }
        }

        @Override
        public void onServerRidesStatLoadingError(@NonNull Error error) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(requireActivity(), "Error while loading rides stat", Toast.LENGTH_LONG).show();
            }
        }
    };
}
