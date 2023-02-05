package ru.yandex.disk.notifications;

import android.content.Intent;
import android.os.Bundle;
import org.junit.Test;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.feed.FeedConstants;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.MoreMatchers;
import ru.yandex.disk.test.TestObjectsFactory;

import static org.mockito.Mockito.*;
import static ru.yandex.disk.test.MoreMatchers.anyBundle;

public class PushDispatcherTest extends AndroidTestCase2 {

    private static final Bundle GMC_PUSH_MESSAGE_BASE;

    static {
        GMC_PUSH_MESSAGE_BASE = new Bundle();
        GMC_PUSH_MESSAGE_BASE.putString("from", "gmail.com");
        GMC_PUSH_MESSAGE_BASE.putString("collapse_key", "do_not_collapse");
    }

    private PushWatcher invitesWatcher;
    private PushWatcher freeSpaceWatcher;
    private PushWatcher feedUpdaterWatcher;

    private PushRegistrator registrator;

    private PushDispatcher dispatcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        registrator = mock(PushRegistrator.class);
        when(registrator.isRegistered()).thenReturn(true);
        when(registrator.getRegistrationId()).thenReturn("reg-id");

        dispatcher = new PushDispatcher(TestObjectsFactory.createCredentials(),
                registrator, mock(DeveloperSettings.class), mock(PushEventLogHelper.class), 0);

        invitesWatcher = mock(PushWatcher.class);
        freeSpaceWatcher = mock(PushWatcher.class);
        feedUpdaterWatcher = mock(PushWatcher.class);

        dispatcher.addWatcher(ServerEvents.SHARE_INVITE_NEW, invitesWatcher);
        dispatcher.addWatcher(ServerEvents.QUOTA_LOW_SPACE, freeSpaceWatcher);
        dispatcher.addWatcher(ServerEvents.QUOTA_OUT_OF_SPACE, freeSpaceWatcher);
        dispatcher.addDatabaseWatcher(FeedConstants.DATABASE_ID, feedUpdaterWatcher);
    }

    @Test
    public void shouldCallInvitesWatcher() throws Exception {
        final Bundle message = makeInviteMessage();

        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(invitesWatcher).onMessage(MoreMatchers.eq(message));
    }

    @Test
    public void shouldCallFreeSpaceWatcherOnQuotaSpaceLow() {
        final Bundle bundle = makeTemplateMessage(ServerEvents.QUOTA_LOW_SPACE);
        dispatcher.dispatchMessage(makeIntent(bundle).getExtras());
        verify(freeSpaceWatcher).onMessage(MoreMatchers.eq(bundle));
    }

    @Test
    public void shouldCallFreeSpaceWatcherOnQuotaOutOfSpace() {
        final Bundle bundle = makeTemplateMessage(ServerEvents.QUOTA_OUT_OF_SPACE);
        dispatcher.dispatchMessage(makeIntent(bundle).getExtras());
        verify(freeSpaceWatcher).onMessage(MoreMatchers.eq(bundle));
    }

    @Test
    public void shouldCallFeedUpdaterOnLentaBlocks() {
        final Bundle message = new Bundle(GMC_PUSH_MESSAGE_BASE);
        message.putString("r", "reg-id");
        message.putString("t", ServerEvents.DATASYNC_DATABASE_CHANGED);
        message.putString(ServerEvents.Params.DATABASE_ID, ".ext.lenta@lenta_blocks");
        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(feedUpdaterWatcher).onMessage(MoreMatchers.eq(message));
    }

    @Test
    public void shouldIngnoreUnknownDatabaseIds() {
        final Bundle message = new Bundle(GMC_PUSH_MESSAGE_BASE);
        message.putString("r", "reg-id");
        message.putString(ServerEvents.Params.DATABASE_ID, "someUnknownDatabase");
        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(feedUpdaterWatcher, never()).onMessage(MoreMatchers.eq(message));
    }

    private Bundle makeInviteMessage() {
        final Bundle message = makeTemplateMessage(ServerEvents.SHARE_INVITE_NEW);
        message.putString("m", "Vasily Pupkin приглашает Вас в папку «Share Folder # 14»");
        return message;
    }

    private static Intent makeIntent(final Bundle message) {
        return new Intent().putExtras(message);
    }

    @Test
    public void shouldSkipUnknownMessage() throws Exception {
        final Bundle message = makeTemplateMessage("unknown");

        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(invitesWatcher, never()).onMessage(anyBundle());
    }

    @Test
    public void shouldIgnoreMessageIfNoRegId() throws Exception {
        reset(registrator);

        final Bundle message = makeInviteMessage();
        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(invitesWatcher, never()).onMessage(anyBundle());
        verify(registrator).requestUnregistration();
    }

    @Test
    public void shouldIgnoreMessageIfMismatchRegId() throws Exception {
        final Bundle message = makeInviteMessage();
        message.putString("r", "old-reg-id");
        dispatcher.dispatchMessage(makeIntent(message).getExtras());

        verify(invitesWatcher, never()).onMessage(anyBundle());
        verify(registrator, never()).requestUnregistration();
    }

    public static Bundle makeTemplateMessage(final String type) {
        final Bundle message = new Bundle(GMC_PUSH_MESSAGE_BASE);
        message.putString("r", "reg-id");
        message.putString(ServerEvents.Params.TYPE, type);
        return message;
    }
}
