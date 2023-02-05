package com.yandex.mail.tools;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import androidx.annotation.NonNull;

public interface ServerHelper {

    @NonNull
    AccountWrapper get(@NonNull ServerHelper dummy);

    @NonNull
    default FolderWrapper getServerFolder(@NonNull String name) {
        return get(this).folders.getByName(name);
    }

    @NonNull
    default LabelWrapper getServerLabel(@NonNull String labelId) {
        return get(this).labels.getByServerLid(labelId);
    }

    @NonNull
    default FolderWrapper serverInbox() {
        return get(this).getInboxFolder();
    }

    @NonNull
    default FolderWrapper serverDrafts() {
        return get(this).getDraftFolder();
    }

    @NonNull
    default FolderWrapper serverTrash() {
        return get(this).getTrashFolder();
    }

    @NonNull
    default FolderWrapper serverOutgoing() {
        return get(this).getOutgoingFolder();
    }

    @NonNull
    default FolderWrapper serverSent() {
        return get(this).getSentFolder();
    }

    @NonNull
    default FolderWrapper serverSpam() {
        return get(this).getSpamFolder();
    }

    default long inboxFid() {
        return Long.parseLong(serverInbox().getServerFid());
    }

    default long outgoingFid() {
        return Long.parseLong(serverOutgoing().getServerFid());
    }

    default long sentFid() {
        return Long.parseLong(serverSent().getServerFid());
    }

    default long draftsFid() {
        return Long.parseLong(serverDrafts().getServerFid());
    }

    default long trashFid() {
        return Long.parseLong(serverTrash().getServerFid());
    }

    default long spamFid() {
        return Long.parseLong(serverSpam().getServerFid());
    }

    @NonNull
    default LabelWrapper serverImportant() {
        return get(this).getImportantLabel();
    }
}
