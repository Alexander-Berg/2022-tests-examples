package com.yandex.mail.fakeserver;

import com.yandex.mail.entity.FolderType;
import com.yandex.mail.entity.Tab;
import com.yandex.mail.generators.ContainersGenerator;
import com.yandex.mail.wrappers.FolderWrapper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static kotlin.collections.CollectionsKt.listOf;

/**
 * Encapsulates 'server' folders and defines some handy manipulation methods.
 */
public class FoldersList {

    @NonNull
    public final List<FolderWrapper> foldersList;

    private FoldersList(@NonNull List<FolderWrapper> foldersList) {
        this.foldersList = new ArrayList<>(foldersList);
    }

    private FoldersList(@NonNull FolderWrapper... foldersList) {
        this(listOf(foldersList));
    }

    @NonNull
    public FoldersList add(@NonNull FolderWrapper wrapper) {
        this.foldersList.add(wrapper);
        return this;
    }

    public boolean remove(@NonNull FolderWrapper folderWrapper) {
      return this.foldersList.remove(folderWrapper);
    }

    @NonNull
    public static FoldersList generateDefault(@NonNull ContainersGenerator generator) {
        FolderWrapper inbox = createEmptyFolder(generator)
                .type(FolderType.INBOX)
                .name("Inbox")
                .build();
        FolderWrapper tab_relevant = createEmptyFolder(generator)
                .type(FolderType.TAB_RELEVANT)
                .serverFid(String.valueOf(Tab.RELEVANT.getFakeFid()))
                .name("Relevant")
                .build();
        FolderWrapper tab_news = createEmptyFolder(generator)
                .type(FolderType.TAB_NEWS)
                .serverFid(String.valueOf(Tab.NEWS.getFakeFid()))
                .name("News")
                .build();
        FolderWrapper tab_social = createEmptyFolder(generator)
                .type(FolderType.TAB_SOCIAL)
                .serverFid(String.valueOf(Tab.SOCIAL.getFakeFid()))
                .name("Social")
                .build();
        FolderWrapper outgoing = createEmptyFolder(generator)
                .type(FolderType.OUTGOING)
                .name("Outgoing")
                .build();
        FolderWrapper sent = createEmptyFolder(generator)
                .type(FolderType.SENT)
                .name("Sent")
                .build();
        FolderWrapper drafts = createEmptyFolder(generator)
                .type(FolderType.DRAFT)
                .name("Drafts")
                .build();
        FolderWrapper spam = createEmptyFolder(generator)
                .type(FolderType.SPAM)
                .name("Spam")
                .build();
        FolderWrapper trash = createEmptyFolder(generator)
                .type(FolderType.TRASH)
                .name("Trash")
                .build();
        // no Archive, since there is no archive by default
        return new FoldersList(inbox, tab_relevant, tab_news, tab_social, outgoing, sent, drafts, spam, trash);
    }

    @NonNull
    public FolderWrapper getByFid(@NonNull String fid) {
        return CollectionsKt.first(foldersList, folder -> fid.equals(folder.getServerFid()));
    }

    @NonNull
    public FolderWrapper getByName(@NonNull String name) {
        return CollectionsKt.first(foldersList, folder -> name.equals(folder.getName()));
    }

    @NonNull
    public FolderWrapper getByType(@NonNull FolderType folderType) {
        return CollectionsKt.first(foldersList, folder -> folderType.getServerType() == folder.getType());
    }

    /**
     * Check if folder with given name is present in list
     */
    public boolean isPresentFolderWithName(@NonNull final String name) {
        return CollectionsKt.any(foldersList, folder -> name.equals(folder.getName()));
    }

    /**
     * Check if folder with given fid is present in list
     */
    public boolean isPresentFolderWithFid(@NonNull final String fid) {
        return CollectionsKt.any(foldersList, folder -> fid.equals(folder.getServerFid()));
    }

    /**
     * Remove folder with given fid from list if present
     */
    public void removeFolderByFid(@NonNull final String fid) {
        if (isPresentFolderWithFid(fid)) {
            foldersList.remove(getByFid(fid));
        }
    }

    public void clear() {
        foldersList.clear();
    }

    @NonNull
    public static FolderWrapper.FolderWrapperBuilder createEmptyFolder(@NonNull ContainersGenerator generator) {
        return FolderWrapper
                .builder()
                .serverFid(generator.nextFid())
                .parent(""); // no parent
    }

    @NonNull
    public static FolderWrapper.FolderWrapperBuilder createEmptyUserFolder(@NonNull ContainersGenerator generator) {
        return createEmptyFolder(generator)
                .type(FolderType.USER);
    }
}
