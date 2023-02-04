package com.yandex.maps.testapp.map;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.Attribution;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.road_events.Entry;
import com.yandex.mapkit.road_events.EntrySession;
import com.yandex.mapkit.road_events.EventInfoSession;
import com.yandex.mapkit.road_events.Feed;
import com.yandex.mapkit.road_events.FeedSession;
import com.yandex.mapkit.road_events.RoadEventFailedError;
import com.yandex.mapkit.road_events.RoadEventMetadata;
import com.yandex.mapkit.road_events.RoadEventsManager;
import com.yandex.mapkit.road_events.VoteSession;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;
import com.yandex.runtime.auth.AuthRequiredError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapRoadEventFullCardActivity extends TestAppActivity
        implements EventInfoSession.EventInfoListener,
        VoteSession.VoteListener,
        FeedSession.FeedListener,
        EntrySession.EntryListener
{
    private RoadEventsManager roadEventsManager;
    private String eventId;
    private List<Entry> comments = new ArrayList<Entry>();
    private EventInfoSession eventInfoSession;
    private VoteSession voteSession;
    private EntrySession addCommentSession;
    private FeedSession commentsSession;
    private MapView mapView;

    private class CommentsAdapter extends ArrayAdapter<Entry> {
        public CommentsAdapter(Context context, List<Entry> entry) {
            super(context, R.layout.road_event_comment, entry);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)
                    getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.road_event_comment, parent, false);
            }

            TextView description = (TextView)
                view.findViewById(R.id.road_event_comment_text);
            description.setText(getItem(position).getContent().getText());
            TextView attribution = (TextView)
                view.findViewById(R.id.road_event_comment_attribution);
            attribution.setText(getItem(position).getAtomEntry().getAuthor().getName());
            TextView updateTime = (TextView)
                view.findViewById(R.id.road_event_comment_time);
            updateTime.setText(getItem(position).getAtomEntry().getUpdateTime());
            if (position + 1 == comments.size()) {
                fetchNextPage();
            }

            return view;
        }
    }
    private CommentsAdapter commentsAdapter;

    public void showRoadEventsError(Error error) {
        if (error instanceof RoadEventFailedError) {
            Utils.showMessage(
                this,
                ((RoadEventFailedError) error).getDescription());
        } else if (error instanceof AuthRequiredError) {
            Utils.showMessage(
                this,
                "Authentication is required");
        } else {
            Utils.showError(this, error);
        }
    }

    @Override
    public void onVoteCompleted() {
        notifyUser(getString(R.string.road_event_vote_success));
    }

    @Override
    public void onVoteError(Error error) {
        showRoadEventsError(error);
    }

    @Override
    public void onFeedReceived(Feed feed) {
        comments.addAll(feed.getEntries());
        commentsAdapter.notifyDataSetChanged();
    }

     @Override
     public void onFeedError(Error error) {
        showRoadEventsError(error);
    }

    @Override
    public void onEventInfoReceived(GeoObject event) {
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.road_event_card_loading);
        progressBar.setVisibility(View.GONE);
        LinearLayout cardLayout = (LinearLayout)findViewById(R.id.road_event_card_layout);
        cardLayout.setVisibility(View.VISIBLE);
        Map map = mapView.getMap();
        CameraPosition oldPosition = map.getCameraPosition();
        CameraPosition position = new CameraPosition(
            event.getGeometry().get(0).getPoint(),
            oldPosition.getZoom(),
            oldPosition.getAzimuth(),
            oldPosition.getTilt());
        map.move(position);

        TextView description = (TextView)findViewById(R.id.road_event_description);
        RoadEventMetadata eventMetadata = event.getMetadataContainer().getItem(RoadEventMetadata.class);
        if (eventMetadata.getDescription() != null) {
            description.setText(eventMetadata.getDescription());
        }
        TextView attribution = (TextView)findViewById(R.id.road_event_attribution);
        if (eventMetadata.getAuthor() != null) {
            attribution.setText(eventMetadata.getAuthor().getName());
        } else {
            Iterator<Attribution> iter = event.getAttributionMap().values().iterator();
            attribution.setText(iter.next().getAuthor().getName());
        }
        TextView updateTime = (TextView)findViewById(R.id.road_event_update_time);
        updateTime.setText(eventMetadata.getTimePeriod().getBegin().getText());
        TextView commentsText = (TextView)findViewById(R.id.road_event_comments_text);
        String titleTemplate = getString(R.string.road_event_comments_text);
        Integer commentsCount = (eventMetadata.getCommentsCount() == null)
            ? Integer.valueOf(0)
            : eventMetadata.getCommentsCount();
        commentsText.setText(String.format(titleTemplate, commentsCount));
    }

    @Override
    public void onEventInfoError(Error error) {
        showRoadEventsError(error);
        finish();
    }

    @Override
    public void onEntryReceived(Entry result) {
        eventInfoSession = roadEventsManager.requestEventInfo(eventId, this);
        notifyUser(getString(R.string.road_event_comment_added));
        reloadComments();
    }

    @Override
    public void onEntryError(Error error) {
        showRoadEventsError(error);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.road_event_full_card);
        mapView = findViewById(R.id.road_event_card_map_view);
        mapView.getMap().move(
                new CameraPosition(new Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        eventId = getIntent().getExtras().getString(MapRoadEventsActivity.eventIdExtra);
        ListView commentsView = (ListView)findViewById(R.id.road_event_comment_list);
        commentsAdapter = new CommentsAdapter(this, comments);
        commentsView.setAdapter(commentsAdapter);
        roadEventsManager = MapKitFactory.getInstance().createRoadEventsManager();
        eventInfoSession = roadEventsManager.requestEventInfo(eventId, this);
        reloadComments();
    }

    @Override
    public void onDestroy() {
        if (voteSession != null) {
            voteSession.cancel();
        }

        if (addCommentSession != null) {
            addCommentSession.cancel();
        }

        super.onDestroy();
    }

    private void reloadComments() {
        comments.clear();
        commentsSession = roadEventsManager.comments(eventId);
        fetchNextPage();
    }

    private void fetchNextPage() {
        if (commentsSession.hasNextPage()) {
            commentsSession.fetchNextPage(this);
        }
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void onAddClick(View view) {
        EditText commentView = (EditText) findViewById(R.id.road_event_comment);
        addCommentSession = roadEventsManager.addComment(
            eventId,
            commentView.getText().toString(),
            MapRoadEventFullCardActivity.this);
    }

    public void onVoteUpClick(View view) {
        voteSession = roadEventsManager.voteUp(
            eventId,
            MapRoadEventFullCardActivity.this);
    }

    public void onVoteDownClick(View view) {
        voteSession = roadEventsManager.voteDown(
            eventId,
            MapRoadEventFullCardActivity.this);
    }

    @Override
    protected void onStopImpl() {
        mapView.onStop();
    }

    @Override
    protected void onStartImpl() {
        mapView.onStart();
    }
}
