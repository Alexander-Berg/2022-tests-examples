package com.yandex.mail.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.am.TestPassportExtensionsKt;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.service.MailWorkCreator;
import com.yandex.mail.service.work.UpdateNotificationWork;
import com.yandex.mail.settings.SwipeAction;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;

import static com.yandex.mail.BaseMailApplication.getApplicationComponent;
import static com.yandex.mail.notifications.NotificationActionKt.createNotificationIntent;
import static com.yandex.mail.notifications.NotificationInnerUtils.ARCHIVE_DELETE_ACTION;
import static com.yandex.mail.notifications.NotificationsConstants.FOLDERS_TO_DELETE;
import static com.yandex.mail.notifications.NotificationsConstants.FOLDER_ID;
import static com.yandex.mail.notifications.NotificationsConstants.NOTIFICATION_MESSAGE_IDS;
import static com.yandex.mail.provider.Constants.MESSAGE_ID_EXTRAS;
import static com.yandex.mail.provider.Constants.UID_EXTRA;
import static com.yandex.mail.settings.SwipeAction.ARCHIVE;
import static com.yandex.mail.settings.SwipeAction.DELETE;
import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(sdk = Build.VERSION_CODES.N)
@RunWith(IntegrationTestRunner.class)
public class MailWorkerCreatorAndroid7Test extends BaseIntegrationTest {

    @NonNull
    private final ShadowNotificationManager manager =
            shadowOf((NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE));

    @NonNull
    private Mailbox mailbox;

    @NonNull
    private NotificationsModel notificationsModel;

    @NonNull
    private MockPassportApi mockPassportApi;

    @NonNull
    private MailWorkCreator mailWorkCreator;

    private final long customFid = 200042;

    @Before
    public void beforeEachTest() {
        init(Accounts.testLoginData);
        mailbox = createEmptyMailbox();
        notificationsModel = getApplicationComponent(app).notificationsModel();
        mockPassportApi = app.getPassportApi();

        workerFactory.getWorkerWhiteList().add(UpdateNotificationWork.class);
        mailWorkCreator = new MailWorkCreator(app);
    }

    @Test
    public void showsEmptyNotificationIfPinForMultipleMessages() {
        mailbox = createFilledMailBox();
        TestPassportExtensionsKt.savePin(mockPassportApi, user.getLoginData().uid, "1111");

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(1);

        ShadowNotification notification = Shadows.shadowOf(notificationList.get(0));
        assertThat(notification.getContentTitle()).isNotEmpty();
        assertThat(notification.getContentText()).isEmpty();
        assertThat(notification.getContentInfo()).isNullOrEmpty();
    }

    @Test
    public void hidesNotificationInfoAfterTurningOnPin() {
        mailbox = createFilledMailBox();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        TestPassportExtensionsKt.savePin(mockPassportApi, user.getLoginData().uid, "1111");
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(1);
    }

    @Test
    public void shouldNotShowNotificationIfArchived() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(ARCHIVE, inboxFid(), 10001L));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(2);
    }

    @Test
    public void shouldNotShowNotificationsIfAllArchived() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(ARCHIVE, inboxFid(), 10001L));
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(ARCHIVE, inboxFid(), 10002L));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).isEmpty();
    }

    @Test
    public void shouldNotShowNotificationIfDeleted() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createDeleteIntent(inboxFid(), new long[]{10001L}));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(2);
    }

    @Test
    public void shouldNotShowNotificationsIfAllDeleted() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createDeleteIntent(inboxFid(), new long[]{10001L, 10002L}));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).isEmpty();
    }

    @Test
    public void shouldCorrectlyUpdateNotificationsAfterAvatarUpdate() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L)));
        assertThat(manager.getAllNotifications()).hasSize(2);

        mailWorkCreator.updateNotifications(createNotificationIntent(user.getUid(), inboxFid(), new long[]{10001L}, true));
        assertThat(manager.getAllNotifications()).hasSize(2);
    }

    @Test
    public void shouldNotLoseNotificationsAfterAvatarUpdate() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createNotificationIntent(user.getUid(), inboxFid(), new long[]{10001L}, true));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(3);
    }

    @Test
    public void shouldNotShowNotificationsAfterAvatarUpdateIfDeleted() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(DELETE, inboxFid(), 10001L));
        mailWorkCreator.updateNotifications(createNotificationIntent(user.getUid(), inboxFid(), new long[]{10001L}, true));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(2);
    }

    @Test
    public void showsMultipleNotificationsContentIfNoPinForMultipleMessages() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldRemoveOldNotifications() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldShowOfflineNotification() {
        mailbox = createEmptyMailbox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(1);
    }

    @Test
    public void shouldShowAllMessagesWhenNotificationBecomeOnline() {
        mailbox = createEmptyMailbox();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(1);

        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldLeavePushWhenNotificationBecomeOffline() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L, 10004L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldShowNewNotificationInExistingGroup() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);
    }

    @Test
    public void shouldIgnoreIfNoUpdates() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldAddNewNotifications() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);
    }

    @Test
    public void shouldShowDifferentNotificationsForDifferentFolders() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(customFid, listOf(20001L)));
        assertThat(manager.getAllNotifications()).hasSize(5);
    }

    @Test
    public void shouldNotShowNotificationsIfAllWereRead() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf()));
        assertThat(manager.getAllNotifications()).hasSize(0);
    }

    @Test
    public void shouldShowNotificationsForNotClearedFolders() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        mailWorkCreator.updateNotifications(createShowNotificationIntent(customFid, listOf(20001L)));
        assertThat(manager.getAllNotifications()).hasSize(6);

        mailWorkCreator.updateNotifications(createDeleteForFolderIntent(new long[]{}));
        assertThat(manager.getAllNotifications()).hasSize(6);

        mailWorkCreator.updateNotifications(createDeleteForFolderIntent(new long[]{customFid}));
        assertThat(manager.getAllNotifications()).hasSize(4);
    }

    @Test
    public void shouldNotShowNotificationsForClearedFolders() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);

        mailWorkCreator.updateNotifications(createDeleteForFolderIntent(new long[]{inboxFid()}));
        assertThat(manager.getAllNotifications()).hasSize(0);
    }

    @Test
    public void shouldShowNewNotificationsAfterFullClear() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        assertThat(manager.getAllNotifications()).hasSize(4);

        mailWorkCreator.updateNotifications(createDeleteForFolderIntent(new long[]{inboxFid()}));
        assertThat(manager.getAllNotifications()).hasSize(0);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);
    }

    @Test
    public void shouldNotShowNotificationsAfterClearForAccount() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));
        mailWorkCreator.updateNotifications(createShowNotificationIntent(customFid, listOf(20001L)));
        assertThat(manager.getAllNotifications()).hasSize(6);

        mailWorkCreator.updateNotifications(createDeleteForAccountIntent());
        assertThat(manager.getAllNotifications()).hasSize(0);
    }

    @Test
    public void shouldNotShowNotificationsAfterDropNotifications() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createDropNotificationsIntent());
        assertThat(manager.getAllNotifications()).hasSize(0);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10003L)));
        assertThat(manager.getAllNotifications()).hasSize(0);
    }

    @Test
    public void shouldRestoreNotifications() {
        mailbox = createFilledMailBox();
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));
        assertThat(manager.getAllNotifications()).hasSize(3);

        mailWorkCreator.updateNotifications(createDropNotificationsIntent());
        assertThat(manager.getAllNotifications()).hasSize(0);

        mailWorkCreator.updateNotifications(createRestoreNotificationsIntent());
        assertThat(manager.getAllNotifications()).hasSize(0);

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10003L)));
        assertThat(manager.getAllNotifications()).hasSize(2);
    }

    @NonNull
    private Intent createShowNotificationIntent(long folderId, @NonNull List<Long> messagesIds) {
        Folder folder = mailbox.folder(folderId).folder();

        return notificationsModel.getShowNotificationIntent(
                user.getUid(),
                folder.getFid(),
                messagesIds
        ).blockingGet();
    }

    @NonNull
    private Intent createArchiveDeleteIntent(@NonNull SwipeAction action, long fid, long mid) {
        Intent intent = new Intent(NotificationActionKt.ARCHIVE_DELETE_MESSAGE);
        intent.putExtra(ARCHIVE_DELETE_ACTION, action.getValue());
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDER_ID, fid);
        intent.putExtra(MESSAGE_ID_EXTRAS, mid);
        return intent;
    }

    @NonNull
    private Intent createDeleteIntent(long fid, long[] mids) {
        Intent intent = new Intent(NotificationActionKt.DELETE_NOTIFICATION);
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDER_ID, fid);
        intent.putExtra(NOTIFICATION_MESSAGE_IDS, mids);
        return intent;
    }

    @NonNull
    private Intent createDeleteForFolderIntent(long[] fids) {
        Intent intent = new Intent(NotificationActionKt.DELETE_NOTIFICATIONS_FOR_FOLDERS);
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDERS_TO_DELETE, fids);
        return intent;
    }

    @NonNull
    private Intent createDeleteForAccountIntent() {
        Intent intent = new Intent(NotificationActionKt.DELETE_NOTIFICATIONS_FOR_ACCOUNT);
        intent.putExtra(UID_EXTRA, user.getUid());
        return intent;
    }

    @NonNull
    private Intent createDropNotificationsIntent() {
        return new Intent(NotificationActionKt.DROP_NOTIFICATION);
    }

    @NonNull
    private Intent createRestoreNotificationsIntent() {
        return new Intent(NotificationActionKt.RESTORE_NOTIFICATION);
    }

    private Mailbox createFilledMailBox() {
        return Mailbox.threaded(this)
                .folder(createFolder().folderId(inboxFid())
                                .addMessage(createMessage()
                                                    .messageId(10001L)
                                                    .from("from1@ya.ru", "name1")
                                                    .subject("earliest")
                                                    .timestamp(new Date(1000L))
                                )
                                .addMessage(createMessage()
                                                    .messageId(10002L)
                                                    .from("from2@ya.ru", "name2")
                                                    .subject("middle")
                                                    .timestamp(new Date(2000L))
                                )
                                .addMessage(createMessage()
                                                    .messageId(10003L)
                                                    .from("from3@ya.ru", "name3")
                                                    .subject("newest")
                                                    .timestamp(new Date(3000L))
                                )
                )
                .folder(createFolder().folderId(customFid)
                                .addMessage(createMessage()
                                                    .messageId(20001L)
                                                    .from("from4@ya.ru", "name4")
                                                    .subject("custom_folder")
                                                    .timestamp(new Date(4000L))
                                )
                )
                .applyAndSync();
    }

    private Mailbox createEmptyMailbox() {
        return Mailbox.threaded(this)
                .folder(createFolder().folderId(inboxFid()))
                .applyAndSync();
    }

}
