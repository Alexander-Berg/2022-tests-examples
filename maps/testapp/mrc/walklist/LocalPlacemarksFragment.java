package com.yandex.maps.testapp.mrc.walklist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.mrc.PlacemarkEditActivity;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.walk.LocalPlacemark;
import com.yandex.mrc.walk.WalkListener;
import com.yandex.mrc.walk.WalkManager;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LocalPlacemarksFragment extends Fragment {
    private WalkManager walkManager;
    private PlacemarkListAdapter placemarkListAdapter;

    private static final String TAG = "LocalPlacemarksFragment";

    public LocalPlacemarksFragment() {
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
        walkManager.subscribe(walkListener);
        refreshPlacemarks();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        walkManager.unsubscribe(walkListener);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_mrc_local_placemarks, container, false);
        Button createNewPlacemarkButton = view.findViewById(R.id.create_new_placemark);
        createNewPlacemarkButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(getContext(), PlacemarkEditActivity.class);
            requireContext().startActivity(intent);
        });

        walkManager = MRCFactory.getInstance().getWalkManager();
        ImageDownloader imageDownloader = MRCFactory.getInstance().getImageDownloader();
        placemarkListAdapter = new PlacemarkListAdapter(requireContext(), imageDownloader, Collections.emptyList());

        RecyclerView ridesListView = view.findViewById(R.id.placemarks_list);
        ridesListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ridesListView.setAdapter(placemarkListAdapter);

        return view;
    }

    private final WalkListener walkListener = new WalkListener() {
        @Override
        public void onPlacemarksUpdated() {
            refreshPlacemarks();
        }

        @Override
        public void onError(@NonNull Error error) {
            Log.e(TAG, "WalkListener::onError: " + error);
            Toast.makeText(requireActivity(), "Local placemarks error", Toast.LENGTH_LONG).show();
        }
    };

    void refreshPlacemarks() {
        Log.i(TAG, "refreshPlacemarks");
        List<PlacemarkListItem> placemarksList = new ArrayList<>();

        for (LocalPlacemark localPlacemark : walkManager.getLocalPlacemarks()) {
            if (localPlacemark != null) {
                placemarksList.add(new PlacemarkListItem(localPlacemark));
            }
        }
        placemarkListAdapter.setPlacemarksList(placemarksList);
    }
}
