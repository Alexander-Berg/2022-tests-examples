package com.yandex.maps.testapp.mrc.walklist;

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
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.mrc.walk.ServerPlacemark;
import com.yandex.mrc.walk.ServerPlacemarksLoadingSession;
import com.yandex.mrc.walk.WalkManager;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ServerPlacemarksFragment extends Fragment {
    private WalkManager walkManager;
    private PlacemarkListAdapter placemarkListAdapter;

    private ServerPlacemarksLoadingSession placemarksLoadingSession;

    private static final String TAG = "LocalPlacemarksFragment";
    private static final int PLACEMARKS_LIMIT = 1000;

    public ServerPlacemarksFragment() {
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
        placemarksLoadingSession = walkManager.loadServerPlacemarks(null, 0, PLACEMARKS_LIMIT, serverPlacemarksListener);
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
        View view = inflater.inflate(R.layout.fragment_mrc_server_placemarks, container, false);

        walkManager = MRCFactory.getInstance().getWalkManager();
        ImageDownloader imageDownloader = MRCFactory.getInstance().getImageDownloader();
        placemarkListAdapter = new PlacemarkListAdapter(requireContext(), imageDownloader, Collections.emptyList());

        RecyclerView placemarksListView = view.findViewById(R.id.placemarks_list);
        placemarksListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placemarksListView.setAdapter(placemarkListAdapter);

        return view;
    }

    void refreshPlacemarks(List<ServerPlacemark> serverPlacemarks) {
        List<PlacemarkListItem> placemarksList = new ArrayList<>();
        for (ServerPlacemark serverPlacemark : serverPlacemarks) {
            placemarksList.add(new PlacemarkListItem(serverPlacemark));
        }
        placemarkListAdapter.setPlacemarksList(placemarksList);
    }

    ServerPlacemarksLoadingSession.ServerPlacemarksListener serverPlacemarksListener
            = new ServerPlacemarksLoadingSession.ServerPlacemarksListener() {
        @Override
        public void onPlacemarksLoaded(@NonNull List<ServerPlacemark> serverPlacemarks) {
            refreshPlacemarks(serverPlacemarks);
        }

        @Override
        public void onPlacemarksLoadingError(@NonNull Error error) {
            Log.e(TAG, "ServerPlacemarksListener::onPlacemarksLoadingError: " + error);
            if (getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(requireActivity(), "Error while loading server placemarks", Toast.LENGTH_LONG).show();
            }
        }
    };
}
