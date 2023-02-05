package com.yandex.mail.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;

import com.yandex.mail.R;
import com.yandex.mail.compose.ComposeAction;
import com.yandex.mail.data.dms.DMSActionStrings;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.maillist.BaseEmailListFragment;
import com.yandex.mail.message_container.Container2;
import com.yandex.mail.message_container.FolderContainer;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.react.PositionInList;
import com.yandex.mail.react.ReactMailViewFragment;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.service.CommandsServiceScheduler;
import com.yandex.mail.smartrate.NewSmartrateBottomSheetDialog;
import com.yandex.mail.smartrate.SmartRateUtils;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.LocalHelper;
import com.yandex.mail.tools.MainSchedulerSynchronizer;
import com.yandex.mail.tools.ServerHelper;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.ContainerDisplayUtils;
import com.yandex.mail.util.ShadowGlide;
import com.yandex.mail.util.annotations.NanoMailProxy;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.work.Data;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.BaseMailApplication.getAccountComponent;
import static com.yandex.mail.BaseMailApplication.getApplicationComponent;
import static com.yandex.mail.asserts.data.DataConditionsKt.action;
import static com.yandex.mail.asserts.data.DataConditionsKt.uid;
import static com.yandex.mail.main.MailActivity.FRAGMENT_TAG_DUMMY_LOADING;
import static com.yandex.mail.search.SearchActivity.createNewSearchIntent;
import static com.yandex.mail.settings.SettingsActivity.IS_CACHE_CLEARED_DURING_SETTINGS;
import static com.yandex.mail.smartrate.SmartRateUtils.INITIAL_WAIT_INTERVAL_MILLIS;
import static com.yandex.mail.ui.constants.ActivityConstants.OPEN_SETTINGS_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.condition.AllOf.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
@Config(shadows = {ShadowGlide.class}, sdk = Build.VERSION_CODES.O)
public class MailActivityTest extends BaseIntegrationTest implements LocalHelper, ServerHelper {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountWrapper account;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private User user;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private ActivityController<TestMailActivity> controller;

    @Before
    public void beforeEachTest() {
        account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);

        user = User.create(Accounts.testLoginData);
        user.initialLoad();

        init(Accounts.testLoginData);
    }

    @Test
    public void onAccountChanged_shouldRequestInitialLoad() {
        CommandsServiceScheduler.INSTANCE.observeTaskRequestsAndSubmitAllTasks(app);

        prepareActivity();
        sendAccountChanged(user.getUid());

        shadowOf(getMainLooper()).idle();

        final List<Data> startedWorkers = dataManagingExecutor.getWorks();
        assertThat(startedWorkers).haveExactly(1, allOf(action(DMSActionStrings.LOAD_CONTAINERS), uid(user.getUid())));
        assertThat(startedWorkers).haveExactly(1, allOf(action(DMSActionStrings.LOAD_SETTINGS), uid(user.getUid())));
    }

    private void prepareActivity() {
        prepareActivity(null);
        shadowOf(getMainLooper()).idle();
    }

    private void sendAccountChanged(long uid) {
        getApplicationComponent(RuntimeEnvironment.application).accountModel().selectAccount(uid);
    }

    private void changeEmailSourceTo(@Nullable Container2 emailSource) {
        controller.get().onMessageContainerChanged(emailSource, false);
    }

    private void prepareActivity(@Nullable Intent intent) {
        controller = Robolectric.buildActivity(TestMailActivity.class, intent);
        controller.create().start().resume();
    }

    @Test
    public void should_updateTitleOnMessageContainerChange() {
        prepareActivity();
        controller.get().initialContentShown();
        sendAccountChanged(user.getUid());
        shadowOf(getMainLooper()).idle();

        changeEmailSourceTo(sent().getFolderContainer());
        shadowOf(getMainLooper()).idle();

        final CharSequence title = controller.get().getSupportActionBar().getTitle();
        assertThat(title).isEqualTo(sent().getFolder().getName());
    }

    @Test
    public void should_setDefaultFolderTitleOfNewSelectedAccountOnAccountChange() {
        FakeServer.getInstance().createAccountWrapper(Accounts.teamLoginData);
        User newUser = User.createInactive(Accounts.teamLoginData);
        newUser.initialLoad();

        MainSchedulerSynchronizer synchronizer = MainSchedulerSynchronizer.create();
        prepareActivity();
        synchronizer.sync();

        sendAccountChanged(newUser.getUid());
        synchronizer.sync();

        final CharSequence actualTitle = controller.get().getSupportActionBar().getTitle();
        final Folder folder = getAccountComponent(app, controller.get().uid).foldersModel().getDefaultFolder().blockingGet().get();
        final String expectedTitle = app.getString(ContainerDisplayUtils.getFolderNameResource(folder.getType()));
        assertThat(actualTitle).isEqualTo(expectedTitle);
    }

    @Test
    public void should_setDefaultFolderTitleOfNewSelectedAccountOnDisableCurrent() {
        FakeServer.getInstance().createAccountWrapper(Accounts.teamLoginData);
        final User secondUserAccount = User.create(Accounts.teamLoginData);

        prepareActivity();
        controller.get().initialContentShown();

        shadowOf(getMainLooper()).idle();

        secondUserAccount.disableAccount();

        shadowOf(getMainLooper()).idle();

        final CharSequence actualTitle = controller.get().getSupportActionBar().getTitle();
        final Folder folder = getAccountComponent(app, controller.get().uid).foldersModel().getDefaultFolder().blockingGet().get();
        final String expectedTitle = app.getString(ContainerDisplayUtils.getFolderNameResource(folder.getType()));
        assertThat(actualTitle).isEqualTo(expectedTitle);
    }

    /**
     * If we are in folder A and receive a multimessage notification on folder A, we shouldn't close the detail fragment.
     * <p>
     */
    @Ignore("TODO: we've got a bug, will uncomment the test once fixed")
    @Test
    public void should_processMultiNotificationIntentOnSameFolder() {
        prepareActivity();
        sendAccountChanged(user.getUid());
        changeEmailSourceTo(inbox().getFolderContainer());

        Intent notificationIntent = createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L, 112L}, false);

        ReactMailViewFragment mockReactMailViewFragment = controller.get().getReactMailViewFragment();
        reset(mockReactMailViewFragment);
        controller
                .pause()
                .stop()
                .newIntent(notificationIntent)
                .start()
                .resume();

        assertThat(controller.get().containerChangeEvents).isEmpty();
        verify(mockReactMailViewFragment, never()).setMessageInfoToDisplay(anyLong(), anyLong(), any(), any(), any());
        verify(mockReactMailViewFragment, never()).setThreadInfoToDisplay(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    /**
     * If we are in folder A and receive a multimessage notification on folder B, we should switch to folder B and close the detail fragment
     */
    @NanoMailProxy
    @Ignore("TODO: we've got a bug, will uncomment the test once fixed")
    @Test
    public void should_processMultiNotificationIntentOnOtherFolder() {
        prepareActivity();
        sendAccountChanged(user.getUid());
        changeEmailSourceTo(trash().getFolderContainer());

        Intent notificationIntent = createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L, 112L}, false);

        controller
                .pause()
                .stop()
                .newIntent(notificationIntent)
                .start()
                .resume();

        Pair<Container2, Boolean> mccEvent = controller.get().containerChangeEvents.get(controller.get().containerChangeEvents.size() - 1);
        assertThat(mccEvent.getFirst()).isEqualTo(new FolderContainer(inboxFid(), FolderType.INBOX.getServerType()));
        assertThat(controller.get().detailMailViewFragment.isHidden()).isTrue();
        verify(controller.get().detailMailViewFragment, never()).setMessageInfoToDisplay(anyLong(), anyLong(), any(), any(), any());
        verify(controller.get().detailMailViewFragment, never()).setThreadInfoToDisplay(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    public void fragment_shouldShowListOnSoloOfflineNotification() {
        prepareActivity(createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L}, true));
        controller.get().initialContentShown();
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();
        assertThat(controller.get().masterFragment.isHidden()).isFalse();
        assertThat(controller.get().detailMailViewFragment.isHidden()).isTrue();
    }

    @Test
    public void fragment_shouldShowListOnMultiOfflineNotification() {
        prepareActivity(createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L, 112L}, true));
        controller.get().initialContentShown();
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();
        assertThat(controller.get().masterFragment.isHidden()).isFalse();
        assertThat(controller.get().detailMailViewFragment.isHidden()).isTrue();
    }

    @Test
    public void fragment_shouldShowDetailsOnSoloOnlineNotification_threaded() {
        prepareActivity(createThreadNotificationIntent(inbox().getFolderContainer(), 111L, false));
        shadowOf(getMainLooper()).idle();

        assertThat(controller.get().detailMailViewFragment).isNotNull();
        assertThat(controller.get().detailMailViewFragment.isHidden()).isFalse();
        assertThat(controller.get().detailMailViewFragment.getThreadId()).isEqualTo(111L);
        assertThat(controller.get().detailMailViewFragment.getMessageId()).isEqualTo(111L);

        controller.get().initialContentShown();
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();
        assertThat(controller.get().masterFragment.isHidden()).isTrue();
        assertThat(controller.get().detailMailViewFragment).isNotNull();
    }

    @Test
    public void fragment_shouldShowDetailsOnSoloOnlineNotification_nonThreaded() {
        prepareActivity(createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L}, false));
        shadowOf(getMainLooper()).idle();

        assertThat(controller.get().detailMailViewFragment).isNotNull();
        assertThat(controller.get().detailMailViewFragment.isHidden()).isFalse();
        assertThat(controller.get().detailMailViewFragment.getThreadId()).isEqualTo(Constants.NO_THREAD_ID);
        assertThat(controller.get().detailMailViewFragment.getMessageId()).isEqualTo(111L);

        controller.get().initialContentShown();
        shadowOf(getMainLooper()).idle();

        assertThat(controller.get().masterFragment).isNotNull();
        assertThat(controller.get().masterFragment.isHidden()).isTrue();
        assertThat(controller.get().detailMailViewFragment).isNotNull();
    }

    @Test
    public void fragment_shouldShowListOnMultiOnlineNotification() {
        prepareActivity(createMessageNotificationIntent(inbox().getFolderContainer(), new long[]{111L, 112L}, false));
        controller.get().initialContentShown();
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();
        assertThat(controller.get().masterFragment.isHidden()).isFalse();
        assertThat(controller.get().detailMailViewFragment.isHidden()).isTrue();
    }

    /**
     * If we are in folder A and receive a notification on a single message from folder A, we should just open the message
     */
    @NanoMailProxy
    @Ignore("TODO: we've got a bug, will uncomment the test once fixed")
    @Test
    public void should_processMessageNotificationIntentOnSameFolder() {
        prepareActivity();
        sendAccountChanged(user.getUid());
        changeEmailSourceTo(inbox().getFolderContainer());

        FolderContainer emailSource = inbox().getFolderContainer();
        Intent notificationIntent = createMessageNotificationIntent(emailSource, new long[]{111L}, false);

        ReactMailViewFragment mockReactMailViewFragment = controller.get().getReactMailViewFragment();
        reset(mockReactMailViewFragment);
        controller
                .pause()
                .stop()
                .newIntent(notificationIntent)
                .start()
                .resume();

        assertThat(controller.get().containerChangeEvents).isEmpty();
        verify(mockReactMailViewFragment).setMessageInfoToDisplay(user.getUid(), 111L, emailSource, PositionInList.NONE, false);
        verify(mockReactMailViewFragment, never()).setThreadInfoToDisplay(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    private void should_processThreadNotificationIntentHelper(@NonNull Container2 currentContainer, @NonNull FolderContainer folderToNotify) {
        prepareActivity();
        sendAccountChanged(user.getUid());
        changeEmailSourceTo(currentContainer);

        ReactMailViewFragment mockReactMailViewFragment = controller.get().getReactMailViewFragment();
        reset(mockReactMailViewFragment);
        controller
                .pause()
                .stop()
                .newIntent(createThreadNotificationIntent(folderToNotify, 111L, false))
                .start()
                .resume();
        Pair<Container2, Boolean> mccEvent = controller.get().containerChangeEvents.get(controller.get().containerChangeEvents.size() - 1);
        assertThat(mccEvent.getFirst()).isEqualTo(folderToNotify);
        verify(mockReactMailViewFragment).setThreadInfoToDisplay(user.getUid(), 111L, Constants.NO_POSITION, folderToNotify, PositionInList.NONE, false);
        verify(mockReactMailViewFragment, never()).setMessageInfoToDisplay(anyLong(), anyLong(), any(), any(), any());
    }

    /**
     * If we are in folder A and receive a notification on a single thread from folder B, we should change the folder to B and open the thread
     */
    @NanoMailProxy
    @Ignore("TODO: we've got a bug, will uncomment the test once fixed")
    @Test
    public void should_processThreadNotificationIntentOnOtherFolder() {
        should_processThreadNotificationIntentHelper(trash().getFolderContainer(), inbox().getFolderContainer());
    }

    @Ignore("TODO: we've got a bug, will uncomment the test once fixed")
    @Test
    public void should_processMessageNotificationIfInLabel() {
        User.LocalFolder folder = user.getLocalFolder(1);
        User.LocalLabel label = user.getLocalLabel("1");

        should_processThreadNotificationIntentHelper(label.getLabelContainer(), folder.getFolderContainer());
    }

    @Test
    public void setCurrentContainer_shouldPutAndRemoveDummyLoadingFragment() {
        prepareActivity(null);
        sendAccountChanged(user.getUid());
        FragmentManager fragmentManager = controller.get().getSupportFragmentManager();

        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();

        assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG_DUMMY_LOADING)).isNull();

        changeEmailSourceTo(null);
        shadowOf(getMainLooper()).idle();

        assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG_DUMMY_LOADING)).isNotNull();

        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();

        assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG_DUMMY_LOADING)).isNull();
    }

    @Test
    public void setCurrentContainer_shouldPutAndRemoveMasterFragmentOnNullContainer() {
        prepareActivity(null);
        sendAccountChanged(user.getUid());

        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();

        changeEmailSourceTo(null);
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNull();

        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();
        assertThat(controller.get().masterFragment).isNotNull();
    }

    @Test
    public void setCurrentContainer_shouldNotCrashIfAccountChangeInBackground() {
        prepareActivity();
        controller.pause().stop();

        sendAccountChanged(user.getUid());

        changeEmailSourceTo(null);
        shadowOf(getMainLooper()).idle();

        assertThat(controller.get().emailSource).isNull();

        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();

        assertThat(controller.get().emailSource).isNotNull();

        controller.start().resume();
        // should not crash :)
    }

    @Test
    public void setCurrentContainer_shouldNotCrashAfterChangingThreadModeAndClearingCacheInBackground() {
        prepareActivity();

        controller.get().onThreadModeChanged(true);

        final FolderContainer container = inbox().getFolderContainer();
        changeEmailSourceTo(container);

        controller.pause().stop();

        controller.get().onThreadModeChanged(false);

        Intent intent = new Intent();
        intent.putExtra(IS_CACHE_CLEARED_DURING_SETTINGS, true);
        controller.get().onActivityResult(OPEN_SETTINGS_REQUEST, Activity.RESULT_OK, intent);

        controller.start().resume();
        // should not crash :)
    }

    @Test
    public void onThreadModeChanged_shouldNotUpdateMasterFragmentForLabels() {
        prepareActivity();

        controller.get().onThreadModeChanged(true);

        changeEmailSourceTo(important().getLabelContainer());

        final BaseEmailListFragment masterFragmentBeforeChangingThreadMode = controller.get().masterFragment;

        controller.get().onThreadModeChanged(false);
        final BaseEmailListFragment masterFragmentAfterChangingThreadMode = controller.get().masterFragment;

        assertThat(masterFragmentBeforeChangingThreadMode).isEqualTo(masterFragmentAfterChangingThreadMode);
    }

    @Test
    public void activity_shouldNotCrashIfAccountIsNotLoadedOnRestore() {
        ActivityController<MailActivity> oldController = Robolectric.buildActivity(MailActivity.class);
        MainSchedulerSynchronizer synchronizer = MainSchedulerSynchronizer.create();
        oldController.create().start().resume();
        synchronizer.sync();

        FakeServer.getInstance().createAccountWrapper(Accounts.teamLoginData);
        User.create(Accounts.teamLoginData);
        synchronizer.sync();

        Bundle state = new Bundle();
        oldController.saveInstanceState(state);

        oldController.pause().stop().destroy();

        ActivityController<MailActivity> newController = Robolectric.buildActivity(MailActivity.class);

        newController.create(state).start().resume();
    }

    @Test
    public void activityStartsComposeOnOpeningTemplate() {
        prepareActivity();
        ShadowActivity shadowActivity = shadowOf(controller.get());
        changeEmailSourceTo(inbox().getFolderContainer());

        long itemId = 1L;

        controller.get().onThreadOrMessageClicked(
                user.getUid(),
                Constants.NO_THREAD_ID,
                itemId,
                new FolderContainer(1, FolderType.TEMPLATES.getServerType()),
                PositionInList.FIRST
        );
        shadowOf(getMainLooper()).idle();

        ShadowActivity.IntentForResult composeActivity = shadowActivity.getNextStartedActivityForResult();

        shadowOf(getMainLooper()).idle();
        Intent intent = composeActivity.intent;
        assertThat(intent.getAction()).isEqualTo(ComposeAction.EDIT_TEMPLATE);
        assertThat(intent.getLongExtra(Constants.MESSAGE_ID_EXTRAS, 0L)).isEqualTo(itemId);
        assertThat(intent.getLongExtra(Constants.UID_EXTRA, 0L)).isEqualTo(user.getUid());
    }

    @Test
    public void activityStartsComposeOnOpeningDraft() {
        prepareActivity();
        controller.get().initialContentShown();
        ShadowActivity shadowActivity = shadowOf(controller.get());
        changeEmailSourceTo(inbox().getFolderContainer());
        shadowOf(getMainLooper()).idle();

        long itemId = 1L;

        controller.get().onThreadOrMessageClicked(
                user.getUid(),
                Constants.NO_THREAD_ID,
                itemId,
                new FolderContainer(1, FolderType.DRAFT.getServerType()),
                PositionInList.FIRST
        );
        shadowOf(getMainLooper()).idle();

        ShadowActivity.IntentForResult composeActivity = shadowActivity.getNextStartedActivityForResult();

        shadowOf(getMainLooper()).idle();

        Intent intent = composeActivity.intent;
        assertThat(intent.getAction()).isEqualTo(ComposeAction.EDIT_DRAFT);
        assertThat(intent.getLongExtra(Constants.MESSAGE_ID_EXTRAS, 0L)).isEqualTo(itemId);
        assertThat(intent.getLongExtra(Constants.UID_EXTRA, 0L)).isEqualTo(user.getUid());
    }

    @Test
    public void activityDoesNotStartComposeOnOpeningInboxMessage() {
        prepareActivity();
        ShadowActivity shadowActivity = shadowOf(controller.get());
        changeEmailSourceTo(inbox().getFolderContainer());

        long itemId = 1L;

        controller.get().onThreadOrMessageClicked(
                user.getUid(),
                Constants.NO_THREAD_ID,
                itemId,
                new FolderContainer(1, FolderType.INBOX.getServerType()),
                PositionInList.FIRST
        );

        ShadowActivity.IntentForResult composeActivity = shadowActivity.getNextStartedActivityForResult();
        assertThat(composeActivity).isNull();
    }

    @Test
    public void handleOptionsItemSelected_clickSearch_shouldOpenSearch() {
        testClickMenu(R.id.menu_search, createNewSearchIntent(app, user.getUid()));
    }

    @Test
    public void actionBar_shouldNotShowTelemostIfYandex() {
        prepareActivity();

        final Menu menu = new RoboMenu(IntegrationTestRunner.app());
        controller.get().onCreateOptionsMenu(menu);
        controller.get().onPrepareOptionsMenu(menu);
        shadowOf(getMainLooper()).idle();

        assertThat(menu.findItem(R.id.menu_search).isVisible()).isTrue();
    }

    private void testClickMenu(@MenuRes int menuItem, @Nullable Intent expectedIntent) {
        prepareActivity();
        controller.get().initialContentShown();

        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        assertThat(application.getNextStartedActivity()).isNull();

        controller.get().onOptionsItemSelected(new RoboMenuItem(menuItem));

        Intent startedActivity = application.getNextStartedActivity();
        if (expectedIntent != null) {
            assertThat(startedActivity).isNotNull();
            assertThat(startedActivity.filterEquals(expectedIntent)).isTrue();
        } else {
            assertThat(startedActivity).isNull();
        }
    }

    @NonNull
    private Intent createThreadNotificationIntent(@NonNull FolderContainer folder, long threadId, boolean offline) {
        Intent notificationIntent = createMessageNotificationIntent(folder, new long[]{threadId}, offline);
        notificationIntent.putExtra(Constants.THREAD_ID_KEY, threadId);
        return notificationIntent;
    }

    @NonNull
    private Intent createMessageNotificationIntent(@NonNull FolderContainer folder, long[] messagesIds, boolean offline) {
        Intent notificationIntent = createBaseNotificationIntent(folder, offline);
        notificationIntent.putExtra(Constants.MESSAGE_ID_EXTRAS, messagesIds);
        return notificationIntent;
    }

    @NonNull
    private Intent createBaseNotificationIntent(@NonNull FolderContainer folder, boolean offline) {
        Intent notificationIntent = new Intent();
        notificationIntent.putExtra(Constants.UID_EXTRA, user.getUid());
        notificationIntent.putExtra(Constants.FID_EXTRA, folder.getFid());
        notificationIntent.putExtra(Constants.FOLDER_TYPE_EXTRA, folder.getFolderType());
        notificationIntent.putExtra(Constants.FROM_NOTIFICATION, true);
        notificationIntent.putExtra(Constants.OFFLINE_EXTRA, offline);
        return notificationIntent;
    }

    private void assertSmartRateFragment(@NonNull MailActivity activity, boolean shown) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        List<Fragment> smartRateFragments = CollectionsKt.filter(
                fragments, fragment -> fragment instanceof NewSmartrateBottomSheetDialog);
        assertThat(smartRateFragments).hasSize(shown ? 1 : 0);
    }

    private void setShouldRate(@NonNull Context context) {
        SystemClock.setCurrentTimeMillis(INITIAL_WAIT_INTERVAL_MILLIS + 1);
        SharedPreferences preferences = SmartRateUtils.getPreferences(context);
        preferences.edit()
                .putInt(SmartRateUtils.KEY.LAUNCH_COUNT, 4)
                .putInt(SmartRateUtils.KEY.SHOWS_COUNT, 0)
                .putLong(SmartRateUtils.KEY.LAST_CRASH_MILLIS, 0)
                .putInt(SmartRateUtils.KEY.LAST_RATING, 0)
                .apply();
    }

    @Override
    @NonNull
    public User get(@Nullable LocalHelper dummy) {
        return user;
    }

    @Override
    @NonNull
    public AccountWrapper get(@Nullable ServerHelper dummy) {
        return account;
    }

    public static class TestMailActivity extends MailActivity {

        @NonNull
        public final List<Pair<Container2, Boolean>> containerChangeEvents = new ArrayList<>();

        @Override
        public void onMessageContainerChanged(@Nullable Container2 newEmailSource, boolean automatic) {
            containerChangeEvents.add(new Pair(newEmailSource, automatic));
            super.onMessageContainerChanged(newEmailSource, automatic);
        }
    }
}
