package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yandex.mapkit.road_events.EventTag;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapRoadEventsTagsActivity extends FragmentActivity
{
    private RoadEventsTagsState tagsState;
    private ListView tagsList;
    private List<EventTag> tags;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_road_events_tags);

        tagsState = getIntent().getParcelableExtra("RoadEventsTagsState");
        RoadEventTagsList disabledTags = getIntent().getParcelableExtra("DisabledRoadEventTags");

        tagsList = findViewById(R.id.tags_list);
        tagsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        tags = new ArrayList<>(Arrays.asList(EventTag.values()));
        for (EventTag tag : disabledTags.getTags()) {
            tags.remove(tag);
        }

        ArrayAdapter<EventTag> tagsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, tags);

        tagsList.setAdapter(tagsAdapter);

        for (EventTag tag : tags) {
            tagsList.setItemChecked(tags.indexOf(tag), tagsState.isTagEnabled(tag));
        }

        tagsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!tagsState.isTagEnabled(tags.get(position))) {
                    tagsList.setItemChecked(position, true);
                    tagsState.setTagEnabled(tags.get(position), true);
                }
                else{
                    tagsList.setItemChecked(position, false);
                    tagsState.setTagEnabled(tags.get(position), false);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("RoadEventsTagsState", tagsState);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void enableAllTags(View v) {
        setAllTags(true);
    }

    public void disableAllTags(View v) {
        setAllTags(false);
    }

    private void setAllTags(boolean enabled) {
        for (EventTag tag : tags) {
            tagsList.setItemChecked(tags.indexOf(tag), enabled);
            tagsState.setTagEnabled(tag, enabled);
        }
    }
}

