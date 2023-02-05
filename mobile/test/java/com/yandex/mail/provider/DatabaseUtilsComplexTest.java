package com.yandex.mail.provider;

import android.annotation.SuppressLint;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.MessageWrapper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import kotlin.collections.CollectionsKt;
import kotlin.ranges.RangesKt;

import static android.os.Looper.getMainLooper;
import static kotlin.collections.CollectionsKt.emptyList;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class DatabaseUtilsComplexTest extends BaseIntegrationTest {

    @Before
    public void beforeEachTest() throws Exception {
        init(Accounts.testLoginData);
    }

    private void testGetFoldersHelper() {
        List<FolderWrapper> additional = listOf(
                account.newFolder("empty").build(),
                account.newFolder("test").build()
        );
        account.addFolders(additional);
        shadowOf(getMainLooper()).idle();
        user.fetchContainers();
        shadowOf(getMainLooper()).idle();
    }

    @Test
    public void testGetMidsInFolderByFIDEmpty() {
        testGetFoldersHelper();
        account.addMessages(emptyList());

        FolderWrapper serverFolder = getServerFolder("empty");
        User.LocalFolder localFolder = getLocalFolder(serverFolder);

        user.fetchMessages(localFolder);
        shadowOf(getMainLooper()).idle();

        assertThat(localFolder.queryCountTotal()).isEqualTo(0);
    }

    @Ignore("Problems with looper")
    @Test
    public void testGetMidsInFolderByFIDNonEmpty() {
        testGetFoldersHelper();
        FolderWrapper serverFolder = getServerFolder("test");
        List<MessageWrapper> messages = CollectionsKt.map(RangesKt.until(0, 3), i -> account.newReadMessage(serverFolder).build());
        account.addMessages(messages);
        shadowOf(getMainLooper()).idle();

        User.LocalFolder localFolder = getLocalFolder(serverFolder);

        user.fetchMessages(localFolder);
        shadowOf(getMainLooper()).idle();

        assertThat(localFolder.queryCountTotal()).isEqualTo(3);
    }
}
