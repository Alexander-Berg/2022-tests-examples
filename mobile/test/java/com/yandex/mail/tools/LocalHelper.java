package com.yandex.mail.tools;

import com.yandex.mail.wrappers.FolderWrapper;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.fakeserver.AccountWrapper.IMPORTANT_LID;
import static java.util.Collections.singletonList;

public interface LocalHelper {

    @NonNull
    User get(@NonNull LocalHelper dummy);

    @NonNull
    default User.LocalFolder getFolder(@NonNull String name) {
        return get(this).getLocalFolder(name);
    }

    @NonNull
    default User.LocalFolder getLocalFolder(@NonNull FolderWrapper serverFolder) {
        return get(this).getLocalFolder(serverFolder);
    }

    @NonNull
    default User.LocalFolder inbox() {
        return getFolder("Inbox");
    }

    @NonNull
    default User.LocalFolder trash() {
        return getFolder("Trash");
    }

    @NonNull
    default User.LocalFolder drafts() {
        return getFolder("Drafts");
    }

    @NonNull
    default User.LocalFolder spam() {
        return getFolder("Spam");
    }

    @NonNull
    default User.LocalFolder sent() {
        return getFolder("Sent");
    }

    @NonNull
    default User.LocalFolder outgoing() {
        return getFolder("Outgoing");
    }

    @NonNull
    default User.LocalFolder archive() {
        return getFolder("Archive");
    }

    @NonNull
    default User.LocalLabel important() {
        return get(this).getLocalLabel(IMPORTANT_LID);
    }

    @NonNull
    default List<User.LocalFolder> defaultFolders() {
        return CollectionsKt.listOf(inbox(), trash(), drafts(), spam(), sent());
    }

    @NonNull
    default List<User.LocalLabel> defaultLabels() {
        return singletonList(important());
    }
}
