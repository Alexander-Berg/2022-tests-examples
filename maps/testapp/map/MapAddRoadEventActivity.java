package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events.RoadEventFailedError;
import com.yandex.mapkit.road_events.RoadEventMetadata;
import com.yandex.mapkit.road_events.RoadEventSession;
import com.yandex.mapkit.road_events.RoadEventsManager;
import com.yandex.maps.auth.internal.GetAccountResultReceiver;
import com.yandex.maps.auth.internal.GetTokenTask;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.auth.AuthStatusView;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.passport.api.Passport;
import com.yandex.passport.api.PassportAccount;
import com.yandex.passport.api.PassportAccountNotAuthorizedProperties;
import com.yandex.passport.api.PassportLoginProperties;
import com.yandex.passport.api.PassportToken;
import com.yandex.passport.api.PassportUid;
import com.yandex.runtime.Error;
import com.yandex.runtime.auth.AuthRequiredError;
import com.yandex.runtime.auth.PasswordRequiredError;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.runtime.logging.Logger;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MapAddRoadEventActivity extends TestAppActivity implements GetAccountResultReceiver {
    private static final int AUTH = 101;

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("yandex.maps");

    private RoadEventsManager roadEventsManager;
    private RoadEventSession currentSession;
    private Point point;

    private EventTag selectedTag = EventTag.OTHER;
    private AuthStatusView authStatusView;
    private EditText descriptionView;
    public static final String pointExtra = "point";

    @Override
    protected void onStopImpl(){}

    @Override
    protected void onStartImpl(){}

    @Override
    protected void onResume() {
        super.onResume();
        authStatusView.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_road_event);

        byte[] position = getIntent().getByteArrayExtra(pointExtra);

        point = Serialization.deserializeFromBytes(position, Point.class);
        authStatusView = findViewById(R.id.add_road_event_auth_status);
        descriptionView = findViewById(
            R.id.road_event_description);

        roadEventsManager = MapKitFactory.getInstance().createRoadEventsManager();

        updateContentLayoutOrientation(getResources().getConfiguration().orientation);

        setupTagsList();
    }

    public void setupTagsList() {
        final ListView tagsList = findViewById(R.id.tag_selection_view);

        if (tagsList == null) {
            return;
        }

        RoadEventTagsList tags = getIntent().getParcelableExtra("Tags");
        ArrayAdapter<EventTag> tagsListAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_single_choice,
                        tags.getTags());
        tagsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        tagsList.setAdapter(tagsListAdapter);

        if (!tags.getTags().isEmpty()) {
            tagsList.setItemChecked(0, true);
        }

        tagsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedTag = (EventTag) parent.getAdapter().getItem(position);
            }
        });
    }

    public void onCancelButtonClick(View view) {
        finish();
    }

    public void onAddButtonClick(View view) {
        currentSession = roadEventsManager.addEvent(
                selectedTag,
                descriptionView.getText().toString(),
                point,
                new RoadEventSession.RoadEventListener() {
                    @Override
                    public void onRoadEventReceived(GeoObject event) {
                        RoadEventMetadata eventMetadata = event.getMetadataContainer().getItem(RoadEventMetadata.class);
                        String message = "id: " + eventMetadata.getEventId();
                        String title = "Road event added";
                        LOGGER.info(title + ", " + message);
                        Toast.makeText(
                                MapAddRoadEventActivity.this,
                                title + "\n" + message,
                                Toast.LENGTH_LONG).show();
                        Logger.info(title + "\n" + message);
                        finish();
                    }

                    @Override
                    public void onRoadEventError(Error error) {
                        if (error instanceof RoadEventFailedError) {
                            Logger.error(((RoadEventFailedError) error).getDescription());
                            Utils.showMessage(
                                MapAddRoadEventActivity.this,
                                ((RoadEventFailedError) error).getDescription());
                        } else if (error instanceof PasswordRequiredError) {
                            Logger.error("Password is required");
                            onPasswordRequired((PasswordRequiredError) error);
                        } else if(error instanceof AuthRequiredError) {
                            Utils.showMessage(
                                MapAddRoadEventActivity.this,
                                "Authentication is required");
                        } else {
                            Logger.error("Error: " + error.getClass().getName());
                            Utils.showError(MapAddRoadEventActivity.this, error);
                        }

                        finish();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AuthUtil.REQUEST_CODE_RELOGIN && resultCode == RESULT_OK) {
            PassportAccount account = AuthUtil.getCurrentAccount();
            if (account == null)
                return;

            final GetTokenTask task = new GetTokenTask(this, AuthUtil.passportApi_);
            task.execute(account);
        }
    }

    private void onPasswordRequired(PasswordRequiredError tokenExpiredError) {
        PassportAccount account = AuthUtil.getCurrentAccount();
        if (account == null)
            return;

        final PassportLoginProperties loginProperties = AuthUtil.createPassportLoginProperties();
        PassportAccountNotAuthorizedProperties properties = PassportAccountNotAuthorizedProperties.Builder.Factory.create().
            setUid(account.getUid()).setLoginProperties(loginProperties).build();
        startActivityForResult(AuthUtil.passportApi_.createAccountNotAuthorizedIntent(MapAddRoadEventActivity.this, properties), AuthUtil.REQUEST_CODE_RELOGIN);
    }

    private void updateContentLayoutOrientation(int orientation) {
        LinearLayout contentLayout = (LinearLayout)findViewById(
            R.id.add_road_event_content_layout);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            contentLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    /**
     * Multiple layouts are used to rearrange controls on screen rotation.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.updateContentLayoutOrientation(newConfig.orientation);
    }

    @Override
    public void onGetAccountResultReceived(@NotNull PassportAccount passportAccount) {

    }

    @Override
    public void onGetTokenResultReceived(@NonNull PassportAccount account, @NotNull PassportToken passportToken) {
        AuthUtil.setToken(account, passportToken);
    }

    @Override
    public void onGetPassportAccountsResultReceived(@NotNull List<PassportAccount> list) {

    }

    @Override
    public void onSuccessDropToken(@NotNull String s) {

    }

    @Override
    public void onPassportApiErrorReceived(@NotNull Throwable throwable) {

    }

    @Override
    public void onSuccessLogout(@NotNull PassportUid passportUid) {

    }
}
