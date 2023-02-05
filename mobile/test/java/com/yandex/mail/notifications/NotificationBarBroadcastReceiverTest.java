package com.yandex.mail.notifications;

import android.content.Intent;

import com.yandex.mail.entity.Folder;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.maillist.EmailListFragment;
import com.yandex.mail.maillist.EmailListFragmentBuilder;
import com.yandex.mail.maillist.EmailListFragmentIntegrationTest;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.LocalHelper;
import com.yandex.mail.tools.ServerHelper;
import com.yandex.mail.tools.SupportFragmentController;
import com.yandex.mail.tools.TestContext;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.wrappers.MessageWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.notifications.NotificationActionKt.CREATE_NOTIFICATION;
import static com.yandex.mail.service.MailWorkCreator.UPDATE_NOTIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class NotificationBarBroadcastReceiverTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    protected User.LocalMessage msg;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    protected SupportFragmentController<EmailListFragment> controller;

    @Before
    public void setup() throws Exception {
        init(Accounts.testLoginData);

        MessageWrapper message = account.newReadMessage(serverInbox())
                .content("Hey")
                .build();
        account.addMessages(message);

        msg = user.getLocalMessage(message.getMid());
    }

    @NonNull
    protected Intent getNotificationBroadcastIntent() {
        final Folder folder = inbox().getFolder();
        return notificationsModel.getShowNotificationIntent(
                user.getUid(),
                folder.getFid(),
                CollectionsKt.listOf(msg.getLocalMid())
        ).blockingGet();
    }

    @Test
    public void testReceiverIsEnabled() {
        Intent intent = new Intent(CREATE_NOTIFICATION);
        assertThat(TestContext.shapp.hasReceiverForIntent(intent)).isTrue();
    }

    public void showMessageContainerContentFragmentIn(@NonNull User.LocalFolder folder) {
        EmailListFragment fragment = new EmailListFragmentBuilder(
                folder.getFolderContainer(), false, "source", user.getUid()
        ).build();
        Class<? extends FragmentActivity> activityClass = EmailListFragmentIntegrationTest.TestActivity.class;
        controller = SupportFragmentController.of(fragment, activityClass);
        controller.create().start().resume();
    }

    /*
        We don't want to show the notification if we are viewing the folder for which notification was created.
     */
    @Test
    public void shouldNot_showNotificationOnVisibleFolder() throws ExecutionException, InterruptedException {
        showMessageContainerContentFragmentIn(inbox());
        IntegrationTestRunner.app().sendOrderedBroadcast(getNotificationBroadcastIntent(), null);
        final List<WorkInfo> workInfos = WorkManager.getInstance(app).getWorkInfosForUniqueWork(UPDATE_NOTIFICATION).get();
        assertThat(workInfos).isEmpty();
    }

    /*
        If the app is running, we want to show the notification in case we are viewing other folder than notification's
     */
    @Test
    public void should_showNotificationOnOtherFolder() throws ExecutionException, InterruptedException {
        showMessageContainerContentFragmentIn(sent());
        shadowOf(getMainLooper()).idle();
        IntegrationTestRunner.app().sendOrderedBroadcast(getNotificationBroadcastIntent(), null);
        shadowOf(getMainLooper()).idle();
        final List<WorkInfo> workInfos = WorkManager.getInstance(app).getWorkInfosForUniqueWork(UPDATE_NOTIFICATION).get();

        assertThat(workInfos).hasSize(1);
    }

    @Test
    public void should_showNotificationWhileInBackground() throws ExecutionException, InterruptedException {
        // we do not start MessageContainerContentFragment here
        IntegrationTestRunner.app().sendOrderedBroadcast(getNotificationBroadcastIntent(), null);
        shadowOf(getMainLooper()).idle();
        final List<WorkInfo> workInfos = WorkManager.getInstance(app).getWorkInfosForUniqueWork(UPDATE_NOTIFICATION).get();
        shadowOf(getMainLooper()).idle();

        assertThat(workInfos).hasSize(1);
    }

    @NonNull
    @Override
    public User get(LocalHelper dummy) {
        return user;
    }

    @NonNull
    @Override
    public AccountWrapper get(ServerHelper dummy) {
        return account;
    }
}
