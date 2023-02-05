package com.yandex.mail.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.common.collect.ImmutableMap;
import com.yandex.mail.am.TestPassportExtensionsKt;
import com.yandex.mail.asserts.MetricaConditions;
import com.yandex.mail.entity.CalendarSaveEventDecision;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.MessageMeta;
import com.yandex.mail.metrica.MetricaConstns;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.service.MailWorkCreator;
import com.yandex.mail.service.work.CommandServiceWorker;
import com.yandex.mail.settings.SwipeAction;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.TestWorkerFactory;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.BaseMailApplication.getApplicationComponent;
import static com.yandex.mail.asserts.MetricaConditions.notificationEventAttribute;
import static com.yandex.mail.asserts.MetricaConditions.notificationEventWithName;
import static com.yandex.mail.notifications.NotificationActionKt.ARCHIVE_DELETE_MESSAGE;
import static com.yandex.mail.notifications.NotificationActionKt.MARK_READ;
import static com.yandex.mail.notifications.NotificationActionKt.REFRESH_NOTIFICATIONS;
import static com.yandex.mail.notifications.NotificationActionKt.REPLY_CALENDAR_INVITE;
import static com.yandex.mail.notifications.NotificationActionKt.createNotificationIntent;
import static com.yandex.mail.notifications.NotificationInnerUtils.ARCHIVE_DELETE_ACTION;
import static com.yandex.mail.notifications.NotificationsConstants.FOLDER_ID;
import static com.yandex.mail.provider.Constants.ACTION_EXTRA;
import static com.yandex.mail.provider.Constants.CALENDAR_INVITE_DECISION_EXTRA;
import static com.yandex.mail.provider.Constants.MESSAGE_ID_EXTRAS;
import static com.yandex.mail.provider.Constants.UID_EXTRA;
import static com.yandex.mail.service.CommandsServiceActions.ARCHIVE_ACTION;
import static com.yandex.mail.service.CommandsServiceActions.DELETE_ACTION;
import static com.yandex.mail.service.CommandsServiceActions.MARK_AS_READ_ACTION;
import static com.yandex.mail.service.MailWorkCreator.TAG_REPLY_CALENDAR_INVITE;
import static com.yandex.mail.settings.SwipeAction.ARCHIVE;
import static com.yandex.mail.settings.SwipeAction.DELETE;
import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static com.yandex.mail.util.mailbox.MailboxEditor.Thread.createThread;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.CollectionsKt.toLongArray;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class MailWorkerCreatorTest extends BaseIntegrationTest {

    private static final int CUSTOM_FID = 100000;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private ShadowNotificationManager manager =
            shadowOf((NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE));

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private Mailbox mailbox;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private NotificationsModel notificationsModel;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private MailWorkCreator mailWorkCreator;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private MessageMeta localMesssage;

    @Before
    public void beforeEachTest() throws Exception {
        init(Accounts.testLoginData);
        mailbox = Mailbox.nonThreaded(this)
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addReadMessages(1)
                )
                .folder(
                        createFolder()
                                .folderId(CUSTOM_FID)
                                .addReadMessages(1)
                )
                .applyAndSync();

        localMesssage = mailbox.folder(serverInbox()).messages().get(0).getMeta();

        notificationsModel = getApplicationComponent(IntegrationTestRunner.app()).notificationsModel();
        mailWorkCreator = new MailWorkCreator(app);
    }

    @Test
    public void showsEmptyNotificationIfPin() {
        TestPassportExtensionsKt.savePin(passportApi, user.getLoginData().uid, "1111");

        mailWorkCreator.updateNotifications(createShowNotificationIntent());

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).isNotEmpty();

        ShadowNotification notification = Shadows.shadowOf(notificationList.get(0));
        assertThat(notification.getContentTitle()).isNotEmpty();
        assertThat(notification.getContentText()).isEmpty();
        assertThat(notification.getContentInfo()).isNullOrEmpty();
    }

    @Test
    public void showsNotificationContentIfNoPin() {
        mailWorkCreator.updateNotifications(createShowNotificationIntent());

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).isNotEmpty();

        ShadowNotification notification = shadowOf(notificationList.get(0));
        assertThat(notification.getContentTitle()).isNotEmpty();
        assertThat(notification.getContentText()).isNotEmpty();
        assertThat(notification.getContentInfo()).isNullOrEmpty();

        assertThat(notification.getBigText()).isNotEmpty();
        assertThat(notification.getBigContentText()).isNotEmpty();
    }

    @Test
    public void shouldNotShowNotificationsAfterAvatarUpdateIfDeletedOldPushes() {
        mailbox = Mailbox.threaded(this)
                .folder(createFolder().folderId(inboxFid())
                                .addMessage(createMessage()
                                                    .messageId(10001L)
                                                    .from("from1@ya.ru", "name1")
                                                    .subject("earliest")
                                                    .timestamp(new Date(1000L))
                                )
                )
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L)));
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(DELETE, inboxFid(), 10001L));
        assertThat(manager.getAllNotifications()).isEmpty();

        mailWorkCreator.updateNotifications(createNotificationIntent(user.getUid(), inboxFid(), new long[]{10001L}, true));
        assertThat(manager.getAllNotifications()).isEmpty();
    }

    @Test
    public void shouldShowOfflineNotification() {
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10006L)));
        assertThat(manager.getAllNotifications()).hasSize(1);
    }

    @Test
    public void shouldUpdateNotificationAfterAvatarUpdateOldPushes() {
        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L)));
        mailWorkCreator.updateNotifications(createNotificationIntent(user.getUid(), inboxFid(), new long[]{10001L}, true));
        assertThat(manager.getAllNotifications()).hasSize(1);
    }

    @Test
    public void onHandleWork_submitsDeleteIntentOnDeleteFromNotification() {
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(DELETE));

        final List<TestWorkerFactory.WorkerInfo> workers = workerFactory.getAllStartedWorkers(CommandServiceWorker.class);
        assertThat(workers).hasSize(1);
        final Data inputData = workers.get(0).getWorkerParameters().getInputData();
        assertThat(inputData.getString(ACTION_EXTRA)).isEqualTo(DELETE_ACTION);
        assertThat(inputData.getLong(UID_EXTRA, -1)).isEqualTo(user.getUid());
    }

    @Test
    public void onHandleWork_reportsDeleteOnDeleteFromNotification() {
        metrica.clearEvents();
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(DELETE));

        assertThat(metrica).has(
                MetricaConditions.INSTANCE.notificationEvent(
                        MetricaConstns.Notification.DELETE_FROM_NOTIFICATION,
                        user.getUid(),
                        localMesssage.getMid()
                )
        );
    }

    @Test
    public void onHandleIntent_submitsArchiveIntentOnArchiveFromNotification() {
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(ARCHIVE));

        final List<TestWorkerFactory.WorkerInfo> workers = workerFactory.getAllStartedWorkers(CommandServiceWorker.class);
        assertThat(workers).hasSize(1);
        final Data inputData = workers.get(0).getWorkerParameters().getInputData();
        assertThat(inputData.getString(ACTION_EXTRA)).isEqualTo(ARCHIVE_ACTION);
        assertThat(inputData.getLong(UID_EXTRA, -1)).isEqualTo(user.getUid());
    }

    @Test
    public void onHandleWork_reportsArchiveOnArchiveFromNotification() {
        metrica.clearEvents();
        mailWorkCreator.updateNotifications(createArchiveDeleteIntent(ARCHIVE));

        assertThat(metrica).has(
                MetricaConditions.INSTANCE.notificationEvent(
                        MetricaConstns.Notification.ARCHIVE_FROM_NOTIFICATION,
                        user.getUid(),
                        localMesssage.getMid()
                )
        );
    }

    @Test
    public void onHandleWork_shouldCancelNotificationForNotRelevantFolders() {
        mailWorkCreator.updateNotifications(createShowNotificationIntent());

        final List<Long> foldersExceptInbox = CollectionsKt.mapNotNull(mailbox.folders(), folder -> {
            if (folder.folderId != Long.parseLong(serverInbox().getServerFid())) {
                return folder.folderId;
            } else {
                return null;
            }
        });

        mailWorkCreator.updateNotifications(createRefreshIntent(toLongArray(foldersExceptInbox)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).isEmpty();
    }

    @Config(sdk = Build.VERSION_CODES.N)
    @Test
    public void onHandleWork_reportsNotificationIsBlockedBySystem() {
        manager.setNotificationsEnabled(false);

        metrica.clearEvents();
        mailWorkCreator.updateNotifications(createShowNotificationIntent());
        assertThat(metrica).has(
                MetricaConditions.INSTANCE.notificationEvent(
                        MetricaConstns.Notification.BLOCKED_BY_SYSTEM_SETTINGS,
                        user.getUid()
                )
        );
    }

    @Test
    public void onHandleIntent_submitsMarkReadIntentOnReadFromNotification() {
        mailWorkCreator.updateNotifications(createReadIntent());

        final List<TestWorkerFactory.WorkerInfo> workers = workerFactory.getAllStartedWorkers(CommandServiceWorker.class);
        assertThat(workers).hasSize(1);
        final Data inputData = workers.get(0).getWorkerParameters().getInputData();
        assertThat(inputData.getString(ACTION_EXTRA)).isEqualTo(MARK_AS_READ_ACTION);
        assertThat(inputData.getLong(UID_EXTRA, -1)).isEqualTo(user.getUid());
    }

    @Test
    public void onHandleIntent_reportsMarkReadOnReadFromNotification() {
        metrica.clearEvents();
        mailWorkCreator.updateNotifications(createReadIntent());

        assertThat(metrica).has(
                MetricaConditions.INSTANCE.notificationEvent(
                        MetricaConstns.Notification.READ_FROM_NOTIFICATION,
                        user.getUid(),
                        localMesssage.getMid()
                )
        );
    }

    @Test
    public void onHandleIntent_submitsReplyCalendarAndMarkReadIntentsFromNotification() {
        mailWorkCreator.updateNotifications(createReplyCalendarInviteIntent(localMesssage.getMid(), CalendarSaveEventDecision.YES));

        final List<TestWorkerFactory.WorkerInfo> workers = workerFactory.getAllStartedWorkers(CommandServiceWorker.class);
        assertThat(workers).hasSize(1);
        final Data inputData = workers.get(0).getWorkerParameters().getInputData();
        assertThat(inputData.getString(ACTION_EXTRA)).isEqualTo(MARK_AS_READ_ACTION);
        assertThat(inputData.getLong(UID_EXTRA, -1)).isEqualTo(user.getUid());

        try {
            String uniqueWorkName = String.format("%s uid: %d mid: %d", TAG_REPLY_CALENDAR_INVITE, user.getUid(), localMesssage.getMid());
            List<WorkInfo> workInfos = WorkManager.getInstance(RuntimeEnvironment.application).getWorkInfosForUniqueWork(uniqueWorkName).get();
            assertThat(workInfos.size()).isEqualTo(1);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void onHandleIntent_reportsReplyCalendarInviteFromNotification() {
        metrica.clearEvents();
        mailWorkCreator.updateNotifications(createReplyCalendarInviteIntent(localMesssage.getMid(), CalendarSaveEventDecision.YES));

        assertThat(metrica.getEvents()).haveExactly(1,
                allOf(
                    notificationEventWithName(MetricaConstns.Notification.REPLY_CALENDAR_INVITE_FROM_NOTIFICATION),
                    notificationEventAttribute(MetricaConstns.Notification.EXTRA_UID, user.getUid()),
                    notificationEventAttribute(MetricaConstns.Notification.EXTRA_MESSAGE_IDS, localMesssage.getMid()),
                    notificationEventAttribute(MetricaConstns.Notification.EXTRA_DECISION, CalendarSaveEventDecision.YES.toString().toLowerCase())
                )
        );
    }

    @Test
    public void onHandleWork_shouldShowMessagesFromNotificationInRightOrder_threaded() {
        mailbox = Mailbox.threaded(this)
                .thread(createThread()
                                .threadId(1L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10001L)
                                                .from("from1@ya.ru", "name1")
                                                .subject("earliest")
                                                .timestamp(new Date(1000L)))
                                ))
                .thread(createThread()
                                .threadId(2L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10002L)
                                                .from("from2@ya.ru", "name2")
                                                .subject("middle")
                                                .timestamp(new Date(2000L)))
                                ))
                .thread(createThread()
                                .threadId(3L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10003L)
                                                .from("from3@ya.ru", "name3")
                                                .subject("newest")
                                                .timestamp(new Date(3000L)))
                                ))
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(1);
        assertThat(getMessagesFromNotification(notificationList.get(0))).containsExactly("name3 newest", "name2 middle", "name1 earliest");
    }

    @Test
    public void shouldNotChangeContentOnAVatarUpdate() {
        mailbox = Mailbox.threaded(this)
                .thread(createThread()
                                .threadId(1L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10001L)
                                                .from("from1@ya.ru", "name1")
                                                .subject("earliest")
                                                .timestamp(new Date(1000L)))
                                ))
                .thread(createThread()
                                .threadId(2L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10002L)
                                                .from("from2@ya.ru", "name2")
                                                .subject("middle")
                                                .timestamp(new Date(2000L)))
                                ))
                .thread(createThread()
                                .threadId(3L)
                                .folder(createFolder().folderId(inboxFid()).addMessage(
                                        createMessage()
                                                .messageId(10003L)
                                                .from("from3@ya.ru", "name3")
                                                .subject("newest")
                                                .timestamp(new Date(3000L)))
                                ))
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(1);
        assertThat(getMessagesFromNotification(notificationList.get(0))).containsExactly("name3 newest", "name2 middle", "name1 earliest");
    }

    @Test
    public void onHandleWork_shouldShowMessagesFromNotificationsInRightOrder_nonThreaded() {
        mailbox = Mailbox.threaded(this)
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
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L, 10003L)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(1);
        assertThat(getMessagesFromNotification(notificationList.get(0))).containsExactly("name3 newest", "name2 middle", "name1 earliest");
    }

    @Test
    public void onHandleWork_shouldReportNotificationMessageIds() {
        mailbox = Mailbox.threaded(this)
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
                )
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(inboxFid(), listOf(10001L, 10002L)));

        metrica.assertEvent("messages_show_in_notification");
        //noinspection ConstantConditions
        metrica.assertLastEvent("messages_show_in_notification", ImmutableMap.of(
                "uid", user.getUid(),
                "message_ids", "[10001, 10002]"
        ));
    }

    @Test
    public void onHandleWork_shouldShowNotificationForUserFolder() {
        mailbox = Mailbox.threaded(this)
                .folder(createFolder()
                                .folderId(1L)
                                .name("testFolder")
                                .addMessage(createMessage()
                                                    .messageId(10001L)
                                                    .from("from1@ya.ru", "name1")
                                                    .subject("subject")
                                                    .timestamp(new Date(1000L))
                                )
                )
                .applyAndSync();

        mailWorkCreator.updateNotifications(createShowNotificationIntent(1L, listOf(10001L)));

        List<Notification> notificationList = manager.getAllNotifications();
        assertThat(notificationList).hasSize(1);
        final Notification notification = notificationList.get(0);
        final String subjectText = notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString();
        final String folderText = notification.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT).toString();
        assertThat(subjectText).isEqualTo("subject");
        assertThat(folderText).contains("testFolder");
    }

    @Test
    public void onHandleWork_shouldNotCrashIfAccountDeleted() {
        final Intent intent = notificationsModel.getCancelNotificationsForAccountIntent(1L).blockingGet();
        mailWorkCreator.updateNotifications(intent); // should not crash
    }

    @NonNull
    private Intent createShowNotificationIntent() {
        return createShowNotificationIntent(inboxFid(), listOf(localMesssage.getMid()));
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
        Intent intent = new Intent(ARCHIVE_DELETE_MESSAGE);
        intent.putExtra(ARCHIVE_DELETE_ACTION, action.getValue());
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDER_ID, fid);
        intent.putExtra(MESSAGE_ID_EXTRAS, mid);
        return intent;
    }

    @NonNull
    private Intent createArchiveDeleteIntent(@NonNull SwipeAction action) {
        return createArchiveDeleteIntent(action, localMesssage.getFid(), localMesssage.getMid());
    }

    @NonNull
    private Intent createRefreshIntent(@NonNull long[] availableFolders) {
        Intent intent = new Intent(REFRESH_NOTIFICATIONS);
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(NotificationsConstants.FOLDER_IDS, availableFolders);
        return intent;
    }

    @NonNull
    private Intent createReadIntent(long fid, long mid) {
        Intent intent = new Intent(MARK_READ);
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDER_ID, fid);
        intent.putExtra(MESSAGE_ID_EXTRAS, mid);
        return intent;
    }

    @NonNull
    private Intent createReadIntent() {
        return createReadIntent(localMesssage.getFid(), localMesssage.getMid());
    }

    @NonNull
    private Intent createReplyCalendarInviteIntent(long mid, @NonNull CalendarSaveEventDecision decision) {
        Intent intent = new Intent(REPLY_CALENDAR_INVITE);
        intent.putExtra(UID_EXTRA, user.getUid());
        intent.putExtra(FOLDER_ID, localMesssage.getFid());
        intent.putExtra(MESSAGE_ID_EXTRAS, mid);
        intent.putExtra(CALENDAR_INVITE_DECISION_EXTRA, decision.getValue());
        return intent;
    }

    @NonNull
    private List<String> getMessagesFromNotification(@NonNull Notification notification) {
        return ArraysKt.map(notification.extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES), CharSequence::toString);
    }

}
