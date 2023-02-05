package com.yandex.mail.fakeserver;

import com.yandex.mail.storage.MessageStatus;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;
import com.yandex.mail.wrappers.MessageWrapper;
import com.yandex.mail.wrappers.ThreadWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static org.apache.commons.collections4.CollectionUtils.intersection;

public final class Messages {

    @NonNull
    public final List<MessageWrapper> messages = new ArrayList<>();

    @NonNull
    public List<MessageWrapper> getMessagesIn(@NonNull FolderWrapper folder) {
        return CollectionsKt.filter(messages, message -> message.getFolder().getServerFid().equals(folder.getServerFid()));
    }

    @NonNull
    public List<ThreadWrapper> getThreadsIn(@NonNull FolderWrapper folder) {
        Set<String> tids = CollectionsKt.toHashSet(CollectionsKt.map(getMessagesIn(folder), MessageWrapper::getTid));
        return CollectionsKt.map(tids, tid -> ThreadWrapper.builder().tid(tid).messages(withServerTid(tid)).build());
    }

    @NonNull
    public MessageWrapper withServerMid(@NonNull String serverMid) {
        return CollectionsKt.first(messages, message -> serverMid.equals(message.getMid()));
    }

    @NonNull
    public List<MessageWrapper> withServerFid(@NonNull FolderWrapper folder) {
        return CollectionsKt.filter(messages, message -> folder.equals(message.getFolder()));
    }

    @NonNull
    public List<MessageWrapper> withTypes(@NonNull List<MessageStatus.Type> types) {
        return CollectionsKt.filter(messages, messageWrapper -> !intersection(messageWrapper.getTypes(), types).isEmpty());
    }

    @NonNull
    public List<MessageWrapper> withServerLid(@NonNull LabelWrapper label) {
        return CollectionsKt.filter(messages, messageWrapper -> messageWrapper.getLabels().contains(label));
    }

    @NonNull
    public List<MessageWrapper> withServerTid(@NonNull String serverTid) {
        return CollectionsKt.filter(messages, message -> serverTid.equals(message.getTid()));
    }

    @NonNull
    public List<MessageWrapper> unread() {
        return CollectionsKt.filter(messages, MessageWrapper::isUnread);
    }

    @NonNull
    public List<MessageWrapper> unreadInFolder(@NonNull FolderWrapper folder) {
        return CollectionsKt.filter(messages, message -> message.getFolder().getServerFid().equals(folder.getServerFid()) && message.isUnread());
    }

    @NonNull
    public List<MessageWrapper> withAttachment() {
        return CollectionsKt.filter(messages, messageWrapper -> messageWrapper.getAttachments().size() > 0);
    }

    public void remove(@NonNull List<MessageWrapper> messagesToRemove) {
        messages.removeAll(messagesToRemove);
    }
}
