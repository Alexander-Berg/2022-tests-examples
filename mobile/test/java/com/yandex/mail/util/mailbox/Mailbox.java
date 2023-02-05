package com.yandex.mail.util.mailbox;

import android.content.Context;

import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.Message;
import com.yandex.mail.entity.Thread;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.model.ThreadsModel;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.LocalHelper;
import com.yandex.mail.tools.ServerHelper;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.wrappers.FolderWrapper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.BaseMailApplication.getAccountComponent;
import static org.robolectric.Shadows.shadowOf;

public class Mailbox implements ServerHelper, LocalHelper {

    @NonNull
    private final User user;

    @NonNull
    private final AccountWrapper account;

    @NonNull
    private final MessagesModel messagesModel;

    @NonNull
    private final ThreadsModel threadsModel;

    @NonNull
    private final FoldersModel foldersModel;

    @NonNull
    private final MailboxEditor editor;

    public Mailbox(
            @NonNull Context context,
            @NonNull User user,
            @NonNull AccountWrapper account
    ) {
        this.user = user;
        this.account = account;
        this.editor = new MailboxEditor(user, account, this);

        AccountComponent accountComponent = getAccountComponent(context, user.getUid());
        messagesModel = accountComponent.messagesModel();
        threadsModel = accountComponent.threadsModel();
        foldersModel = accountComponent.foldersModel();
    }

    @NonNull
    private static Mailbox create(@NonNull BaseIntegrationTest baseIntegrationTest, boolean isThreaded) {
        //noinspection ConstantConditions account or user can be null if init() wasn't loaded
        if (baseIntegrationTest.account == null || baseIntegrationTest.user == null) {
            AccountWrapper account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
            User user = User.create(Accounts.testLoginData);
            user.initialLoad();
            baseIntegrationTest.initFromMailbox(account, user);
        }
        baseIntegrationTest.user.setThreaded(isThreaded);
        return new Mailbox(IntegrationTestRunner.app(), baseIntegrationTest.user, baseIntegrationTest.account);
    }

    @NonNull
    public static MailboxEditor nonThreaded(BaseIntegrationTest baseIntegrationTest) {
        return create(baseIntegrationTest, false).edit();
    }

    @NonNull
    public static MailboxEditor threaded(BaseIntegrationTest baseIntegrationTest) {
        return create(baseIntegrationTest, true).edit();
    }

    /**
     * Use only if you really need this. In new test we will not interact with FolderWrapper directly.
     */
    @Deprecated
    @Nullable
    public FolderWrapper getFolderWrapper(long folderId) {
        String folderIdStr = String.valueOf(folderId);
        FolderWrapper wrapper = editor.getDefaultWrapperOrNull(folderId);
        if (wrapper == null) {
            wrapper = getServerFolder("Folder " + folderIdStr);
        }
        return wrapper;
    }

    @NonNull
    public List<ClientFolder> folders() {
        final List<Folder> folders = foldersModel.getFolders().blockingGet();
        return CollectionsKt.map(folders, folder -> new ClientFolder(folder.getFid()));
    }

    /**
     * Use only if you really need this. In new test we will not interact with FolderWrapper directly.
     * @deprecated use {@link #folder(long)} instead of this
     */
    @Deprecated
    @NonNull
    public ClientFolder folder(@NonNull FolderWrapper folderWrapper) {
        return folder(Long.valueOf(folderWrapper.getServerFid()));
    }

    @NonNull
    public ClientFolder folder(long folderId) {
        Folder folder = foldersModel.getFolderByFidFromCache(folderId).blockingGet().orElse(null);
        if (folder == null) {
            throw new IllegalArgumentException("No folder found with id = " + folderId);
        }
        return new ClientFolder(folderId);
    }

    public boolean isMessageExists(long messageId) {
        return messagesModel.getMessageByMid(messageId).blockingGet().isPresent();
    }

    @NonNull
    public MailboxEditor edit() {
        return editor;
    }

    @NonNull
    public Mailbox sync() {
        user.fetchContainers();
        final List<Long> folderIds = CollectionsKt.map(account.folders.foldersList, folder -> Long.parseLong(folder.getServerFid()));
        user.fetchMessagesNano(CollectionsKt.toLongArray(folderIds));
        shadowOf(getMainLooper()).idle();
        return this;
    }

    @Override
    @NonNull
    public User get(@NonNull LocalHelper dummy) {
        return user;
    }

    @Override
    @NonNull
    public AccountWrapper get(@NonNull ServerHelper dummy) {
        return account;
    }

    public final class ClientFolder {

        public final long folderId;

        ClientFolder(long folderId) {
            this.folderId = folderId;
        }

        @NonNull
        public Folder folder() {
            return foldersModel.getFolderByFidFromCache(folderId).blockingGet().get();
        }

        @NonNull
        public List<Message> messages() {
            if (user.isThreaded()) {
                if (folder().isThreaded()) {
                    return CollectionsKt.flatMap(threads(), thread -> {
                        // only messages in this folder
                        return CollectionsKt.filter(thread.messages(), message -> message.getMeta().getFid() == folderId);
                    });
                }
            }

            return messagesModel.observeMessagesInFolder(folderId).blockingFirst();
        }

        @NonNull
        public Message message(long messageId) {
            return CollectionsKt.first(messages(), message -> message.getMeta().getMid() == messageId);
        }

        @NonNull
        public List<ClientThread> threads() {
            if (!user.isThreaded()) {
                throw new IllegalStateException("User must be in threaded mode in order to get threads");
            }

            return CollectionsKt.map(
                    threadsModel.threadsInFolder(folderId).blockingFirst(),
                    thread -> new ClientThread(folderId, thread.getThread().getTid())
            );
        }
    }

    public final class ClientThread {

        public final long folderId;

        public final long threadId;

        ClientThread(long folderId, long threadId) {
            this.folderId = folderId;
            this.threadId = threadId;
        }

        @NonNull
        public Thread thread() {
            Flowable<List<Thread>> threadFlowable = threadsModel.threadsInFolder(folderId);
            return threadFlowable
                    .firstOrError()
                    .map(folders -> CollectionsKt.filter(folders, folder -> folder.getThread().getTid() == threadId).get(0))
                    .blockingGet();
        }

        @NonNull
        public List<Message> messages() {
            return messagesModel.observeCompositeMessagesInThread(threadId).blockingFirst();
        }
    }
}
