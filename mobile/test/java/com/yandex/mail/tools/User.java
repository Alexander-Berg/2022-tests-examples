package com.yandex.mail.tools;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.RemoteException;

import com.yandex.mail.LoginData;
import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.am.TestPassportExtensionsKt;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.Label;
import com.yandex.mail.entity.Message;
import com.yandex.mail.entity.Tab;
import com.yandex.mail.message_container.FolderContainer;
import com.yandex.mail.message_container.LabelContainer;
import com.yandex.mail.metrica.MetricaConstns.PerfMetrics.RootEvents;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.GeneralSettingsModel;
import com.yandex.mail.model.LabelsModel;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.model.SyncModel;
import com.yandex.mail.model.ThreadsModel;
import com.yandex.mail.network.tasks.DeleteTask;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.GeneralSettings;
import com.yandex.mail.theme.ThemeModel;
import com.yandex.mail.util.AccountNotInDBException;
import com.yandex.mail.util.ShouldNotHaveHappenedException;
import com.yandex.mail.util.annotations.NanoMailProxy;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;
import com.yandex.mail.wrappers.MessageWrapper;
import com.yandex.mail.wrappers.ThreadWrapper;

import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.WorkManagerTestInitHelper;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.BaseMailApplication.getAccountComponent;
import static com.yandex.mail.BaseMailApplication.getApplicationComponent;
import static com.yandex.mail.service.MailWorkCreator.TAG_LOAD_BODIES;
import static java.lang.String.valueOf;
import static kotlin.collections.CollectionsKt.listOf;
import static org.robolectric.Shadows.shadowOf;

/**
 * Client-side user abstraction.
 * Contains some handy methods, encapsulates passing uid and folderId everywhere
 */
@SuppressLint("NewApi")
public class User {

    public final LoginData loginData;

    public LoginData getLoginData() {
        return loginData;
    }

    public long getUid() {
        return loginData.uid;
    }

    @NonNull
    private final GeneralSettingsModel settingsModel;

    @NonNull
    private final ThemeModel themeModel;

    @NonNull
    private final FoldersModel foldersModel;

    @NonNull
    private final LabelsModel labelsModel;

    @NonNull
    private final MessagesModel messagesModel;

    @NonNull
    private final ThreadsModel threadsModel;

    @NonNull
    private final AccountModel accountModel;

    @NonNull
    private final MockPassportApi accountManager;

    @NonNull
    private final SyncModel syncModel;

    @NonNull
    private final File accountFolder;

    private User(@NonNull LoginData loginData) {
        final ApplicationComponent component = getApplicationComponent(RuntimeEnvironment.application);
        final AccountComponent accountComponent = getAccountComponent(RuntimeEnvironment.application, loginData.uid);
        this.settingsModel = component.settingsModel();
        this.themeModel = component.themeModel();
        this.foldersModel = accountComponent.foldersModel();
        this.labelsModel = accountComponent.labelsModel();
        this.messagesModel = accountComponent.messagesModel();
        this.threadsModel = accountComponent.threadsModel();
        this.accountModel = component.accountModel();
        this.accountManager = (MockPassportApi) component.passportApi();

        this.loginData = loginData;
        this.syncModel = accountComponent.syncModel();
        this.accountFolder = accountComponent.accountFolder();
    }

    @NonNull
    public static User createInactive(@NonNull LoginData data) {
        AccountsTools.insertAccount(data, false);
        return new User(data);
    }

    @NonNull
    public static User create(@NonNull LoginData data) {
        AccountsTools.insertAccount(data, true);
        return new User(data);
    }

    @NonNull
    public static User createNotLogged(@NonNull LoginData data) {
        AccountsTools.insertAccount(data, false, false);
        return new User(data);
    }

    @NonNull
    public static User get(@NonNull LoginData loginData) {
        return new User(loginData);
    }

    public void enableAccount() {
        IntegrationTestRunner.app().getApplicationComponent().accountModel().setAccountEnabled(getUid(), true);
    }

    public void disableAccount() {
        IntegrationTestRunner.app().getApplicationComponent().accountModel().setAccountEnabled(getUid(), false);
    }

    public void select() {
        AccountsTools.switchAccountTo(getUid());
    }

    @NonNull
    public AccountSettings getAccountSettings() {
        return settingsModel.accountSettings(getUid());
    }

    @NonNull
    public AccountEntity getDbAccount() {
        return accountModel.getAccountByUidSingle(getUid()).blockingGet().get();
    }

    @NonNull
    public static GeneralSettings getGeneralSettings() {
        return getApplicationComponent(IntegrationTestRunner.app()).settingsModel().getGeneralSettings();
    }

    public void setThreaded(boolean threaded) {
        getAccountSettings().editAndSync().setThreadModeEnabled(threaded).commitAndSync();
    }

    public boolean isThreaded() {
        return getAccountSettings().isThreadMode();
    }

    public void setThemeEnabled(boolean themeEnabled) {
        getAccountSettings().editAndSync().setThemeEnabled(themeEnabled).commitAndSync();
    }

    public void setSignature(@NonNull String signature) {
        getAccountSettings().editAndSync().setSignature(signature).commitAndSync();
    }

    public String getDefaultEmail() {
        return settingsModel.getCurrentSender(getUid());
    }

    /**
     * Only fetches folders, the corresponding pending response should be created by the caller
     */
    @NanoMailProxy
    public void fetchContainers() {
        syncModel.xlistMetaUpdate(RootEvents.NO_OP);
    }

    public void fetchMessages(@NonNull String folderName) {
        List<Folder> folders = foldersModel.getFolders().blockingGet();

        fetchMessageNano(CollectionsKt.first(folders, folder -> folderName.equals(folder.getName())).getFid());
    }

    private void fetchMessageNano(long fid) {
        syncModel.containerFullUpdate(fid, RootEvents.NO_OP);
        shadowOf(getMainLooper()).idle();
        runLoadingBodies();
        shadowOf(getMainLooper()).idle();
    }

    @Deprecated
    public void fetchMessages(@NonNull LocalFolder folder) {
        fetchMessageNano(folder.folderId);
        shadowOf(getMainLooper()).idle();
        ContentResolver contentResolver = IntegrationTestRunner.app()
                .getContentResolver();
        contentResolver.notifyChange(
                Uri.fromFile(accountFolder),
                null
        ); //to trigger start wizard @see {@link MessagesModel.waitInboxReady}
    }

    public void loadSingleBody(@NonNull LocalMessage localMessage) {
        syncModel.loadSingleBody(Long.valueOf(localMessage.getServerMid()), RootEvents.NO_OP);
    }

    public void runLoadingBodies() {
        try {
            List<WorkInfo> workInfos = WorkManager.getInstance(RuntimeEnvironment.application).getWorkInfosByTag(TAG_LOAD_BODIES).get();
            for (WorkInfo workInfo : workInfos) {
                try {
                    WorkManagerTestInitHelper.getTestDriver(RuntimeEnvironment.application).setAllConstraintsMet(workInfo.getId());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fetchMessages(@NonNull LocalFolder... folders) {
        for (LocalFolder folder : folders) {
            fetchMessages(folder);
        }
    }

    public void fetchMessagesNano(long... folderIds) {
        for (long folderId : folderIds) {
            fetchMessageNano(folderId);
        }
    }

    public void fetchTabMessagesNano() {
        for (Tab tab : Tab.values()) {
            tab.getLocalId();
        }
    }

    public void fetchMessages(@NonNull LocalLabel label) {
        syncModel.updateMessagesInLabel(label.getServerLid(), false, RootEvents.LABEL_REFRESH);
    }

    public void loadSettings() {
        syncModel.loadSettings(RootEvents.SETTINGS_LOAD);
    }

    public void initialLoad() {
        syncModel.loadSettings(RootEvents.NO_OP);
        fetchContainers();
    }

    /**
     * @return whether account exists in the local database
     */
    public boolean existsIsDB() {
        return accountModel.observeAccountByUid(getUid()).blockingFirst().isPresent();
    }

    public void removeAccountFromAM() {
        TestPassportExtensionsKt.removeAccount(accountManager, loginData);
    }

    public Long getLocalTidByServerTid(@NonNull String serverTid) {
        return Long.parseLong(serverTid);
    }

    @Deprecated
    @NonNull
    public String getScnOfThread(@NonNull String serverTid) {
        throw new UnsupportedOperationException("We will remove this method shortly");
    }

    public boolean messageExists(long localMid) {
        return messagesModel.getMessageByMid(localMid).blockingGet().isPresent();
    }

    @Deprecated
    @NonNull
    public List<LocalMessage> getSearchMessages() {
        throw new UnsupportedOperationException("We will remove this method shortly");
    }

    @Deprecated
    @NonNull
    public Stream<LocalMessage> getShownForLabelsMessages(@NonNull String serverLid) {
        throw new UnsupportedOperationException("We will remove this method shortly");
    }

    @NonNull
    public LocalFolder getLocalFolder(@NonNull FolderWrapper serverFolder) {
        return getLocalFolder(serverFolder.getName());
    }

    @NanoMailProxy
    @NonNull
    public LocalFolder getLocalFolder(@NonNull String name) {
        List<Folder> folders = foldersModel.getFolders().blockingGet();

        long fid = CollectionsKt.first(folders, folder -> name.equals(folder.getName())).getFid();

        return new LocalFolder(fid);
    }

    @NonNull
    public LocalFolder getLocalFolder(long localFolderId) {
        return new LocalFolder(localFolderId);
    }

    @NonNull
    public LocalLabel getLocalLabel(@NonNull LabelWrapper labelWrapper) {
        return getLocalLabel(labelWrapper.getServerLid());
    }

    @NonNull
    public LocalLabel getLocalLabel(@NonNull String labelId) {
        return new LocalLabel(labelId);
    }

    @NonNull
    public LocalMessage getLocalMessage(@NonNull MessageWrapper serverMessage) {
        return getLocalMessage(serverMessage.getMid());
    }

    @Deprecated
    @NonNull
    public LocalMessage getLocalMessage(@NonNull String serverMid) {
        return new LocalMessage(Long.parseLong(serverMid));
    }

    public long getLocalMid(@NonNull MessageWrapper serverMessage) {
        return getLocalMessage(serverMessage).getLocalMid();
    }

    @NonNull
    public LocalThread getLocalThread(@NonNull String serverTid) {
        return new LocalThread(Long.parseLong(serverTid));
    }

    @NonNull
    public LocalThread getLocalThread(@NonNull ThreadWrapper serverThread) {
        return new LocalThread(getLocalTidByServerTid(serverThread.getTid()));
    }

    public class LocalFolder {

        @Override
        public String toString() {
            return "LocalFolder{" +
                   "folderId=" + folderId +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LocalFolder that = (LocalFolder) o;

            return folderId == that.folderId;
        }

        @Override
        public int hashCode() {
            return (int) (folderId ^ (folderId >>> 32));
        }

        public final long folderId;

        public LocalFolder(long folderId) {
            this.folderId = folderId;
        }

        public long getLocalFid() {
            return folderId;
        }

        @NonNull
        public Folder getFolder() {
            return foldersModel.getFolderByFidFromCache(folderId).blockingGet().get();
        }

        @NonNull
        public long getLastAccessTime() {
            return foldersModel.getFolderLat(folderId).blockingGet().get();
        }

        @NonNull
        public String getServerFid() {
            return String.valueOf(folderId);
        }

        @NonNull
        public FolderContainer getFolderContainer() {
            return new FolderContainer(folderId, getFolder().getType());
        }

        public long queryCountTotal() {
            return getFolder().getTotal_counter();
        }

        public long queryCountUnread() {
            return getFolder().getUnread_counter();
        }
    }


    public class LocalLabel {

        @Override
        public String toString() {
            return "LocalLabel{" +
                   "labelId='" + labelId + '\'' +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LocalLabel that = (LocalLabel) o;

            return labelId.equals(that.labelId);
        }

        @Override
        public int hashCode() {
            return labelId.hashCode();
        }

        @NonNull
        public final String labelId;

        public LocalLabel(@NonNull String labelId) {
            this.labelId = labelId;
        }

        @NonNull
        public Label getLabel() {
            return CollectionsKt.first(labelsModel.getLabelsByLids(listOf(labelId)).blockingGet());
        }

        public long queryCountTotal() {
            return getLabel().getTotal_counter();
        }

        public long getLocalLid() {
            return getLabel().getLid().hashCode();
        }

        @NonNull
        public String getServerLid() {
            return labelId;
        }

        @NonNull
        public LabelContainer getLabelContainer() {
            final Label label = getLabel();
            return new LabelContainer(label.getType(), label.getLid(), label.getName());
        }
    }


    public class LocalMessage {

        @Override
        public String toString() {
            return "LocalMessage{" +
                   "messageId=" + messageId +
                   '}';
        }

        public final long messageId;

        public LocalMessage(long messageId) {
            this.messageId = messageId;
        }

        @NonNull
        private Message getMessage() {
            return messagesModel.observeCompositeMessageByMid(messageId).blockingFirst().get();
        }

        public long getLocalMid() {
            return messageId;
        }

        public long getUid() {
            return getUid();
        }

        public long getLocalFid() {
            return getMessage().getMeta().getFid();
        }

        @NonNull
        public List<String> getServerLids() {
            return messagesModel
                    .observeCompositeMessageByMid(messageId)
                    .map(message -> message.get().getLabels())
                    .blockingFirst();
        }

        @NonNull
        public File getBodyFile() {
            return new File(IntegrationTestRunner.app().getFilesDir(), valueOf(messageId));
        }

        @NonNull
        public LocalFolder getLocalFolder() {
            return new LocalFolder(Long.parseLong(getServerFid()));
        }

        @NonNull
        public String getServerMid() {
            return String.valueOf(messageId);
        }

        @NonNull
        public String getServerFid() {
            return getLocalFolder().getServerFid();
        }

        public boolean hasAttach() {
            return getMessage().getMeta().getHasAttach();
        }

        public void delete() {
            try {
                DeleteTask task = DeleteTask.create(IntegrationTestRunner.app(), listOf(Long.parseLong(getServerMid())), getUid(), false);
                task.updateDatabase(IntegrationTestRunner.app());
            } catch (AccountNotInDBException | RemoteException e) {
                throw new ShouldNotHaveHappenedException(e);
            }
        }

        @Deprecated
        public void forceDelete() {
            delete();
            delete();
        }
    }


    public class LocalThread {

        @Override
        public String toString() {
            return "LocalThread{" +
                   "threadId=" + threadId +
                   '}';
        }

        public final long threadId;

        public LocalThread(long threadId) {
            this.threadId = threadId;
        }

        public long getLocalTid() {
            return threadId;
        }

        public List<LocalMessage> getMessages() {
            List<Long> mids = messagesModel.getMidsByTids(listOf(threadId)).blockingGet();
            return CollectionsKt.map(mids, LocalMessage::new);
        }

        public boolean isFake() {
            return threadId <= Constants.MAX_FAKE_TID;
        }

        /**
         * @deprecated this method don't use threads model
         */
        @Deprecated
        public int getTotalCount() {
            return messagesModel.getMessagesInThread(threadId).blockingGet().size();
        }

        /**
         * @deprecated this method don't use threads model
         */
        @Deprecated
        public int getUnreadCount() {
            return messagesModel.getMessagesInThread(threadId)
                    .map(list -> CollectionsKt.sumBy(list, meta -> meta.getUnread() ? 1 : 0))
                    .blockingGet();
        }
    }
}
