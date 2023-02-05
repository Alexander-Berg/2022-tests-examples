package com.yandex.mail.util.mailbox;

import android.text.TextUtils;

import com.google.common.collect.Lists;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.model.FoldersCache;
import com.yandex.mail.storage.MessageStatus.Type;
import com.yandex.mail.tools.LocalHelper;
import com.yandex.mail.tools.ServerHelper;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.UnexpectedCaseException;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.MessageWrapper;
import com.yandex.mail.wrappers.MessageWrapper.MessageWrapperBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.entity.MessageBodyMeta.DEFAULT_CONTENT_TYPE;
import static com.yandex.mail.provider.Constants.NO_FOLDER_ID;
import static com.yandex.mail.provider.Constants.NO_MESSAGE_ID;
import static com.yandex.mail.provider.Constants.NO_THREAD_ID;
import static com.yandex.mail.storage.entities.LabelTypeKt.USER;
import static com.yandex.mail.util.SolidUtils.collect;
import static java.util.Collections.nCopies;
import static org.robolectric.Shadows.shadowOf;

public class MailboxEditor implements ServerHelper, LocalHelper {

    private final static String LABEL_NAME_PREFIX = "Label:";

    @NonNull
    private final User user;

    @NonNull
    private final AccountWrapper account;

    @NonNull
    private final Mailbox editableMailbox;

    // now we have messages and threads in sorted order
    // it's nice to support "first add - first in list" order in tests so we need to apply all actions backwards
    @NonNull
    private final List<Runnable> actionsToApply = new ArrayList<>();

    public MailboxEditor(@NonNull User user, @NonNull AccountWrapper account, @NonNull Mailbox editableMailbox) {
        this.user = user;
        this.account = account;
        this.editableMailbox = editableMailbox;
    }

    @NonNull
    public Mailbox apply() {
        for (Runnable action0: Lists.reverse(actionsToApply)) {
            action0.run();
            shadowOf(getMainLooper()).idle();
        }
        actionsToApply.clear();
        return editableMailbox;
    }

    @NonNull
    public Mailbox applyAndSync() {
        return apply().sync();
    }

    @NonNull
    public MailboxEditor label(@NonNull Label.Builder customLabelBuilder) {
        final Label label = customLabelBuilder.build();

        String name = !TextUtils.isEmpty(label.name) ? label.name : LABEL_NAME_PREFIX + label.labelId;
        account.addLabels(account.newLabel(name).serverLid(label.labelId).type(label.type).build());
        return this;
    }

    @NonNull
    public MailboxEditor folder(@NonNull Folder.Builder folderBuilder) {
        actionsToApply.add(() -> {
            final Folder folder = folderBuilder.build();

            account.addMessages(CollectionsKt.map(messagesFromFolder(folder), MessageWrapperBuilder::build));
            account.removeMessages(findMessagesInFolder(folder.messageIdsToDelete));
        });
        return this;
    }

    @NonNull
    public MailboxEditor threadList(@NonNull Thread.Builder baseThreadBuilder, int count) {
        for (int i = 0; i < count; i++) {
            thread(baseThreadBuilder);
        }
        return this;
    }

    @NonNull
    public MailboxEditor thread(@NonNull Thread.Builder threadBuilder) {
        actionsToApply.add(() -> {
            final Thread thread = threadBuilder.build();

            List<MessageWrapperBuilder> threadMessages = new ArrayList<>();
            for (Folder folder : Lists.reverse(thread.folders)) {
                threadMessages.addAll(messagesFromFolder(folder));
            }

            account.addThreads(
                    account.newThread(
                            thread.threadId == NO_THREAD_ID ? null : String.valueOf(thread.threadId),
                            threadMessages
                    )
                            .build()
            );
        });
        return this;
    }

    /**
     * Creates {@link FolderWrapper} if needed and create {@link MessageWrapperBuilder} from list of messages
     * @return list of messages from folder which you could add to account.
     */
    @NonNull
    private List<MessageWrapperBuilder> messagesFromFolder(@NonNull Folder folder) {
        FolderWrapper folderWrapper = getOrCreateFolderWrapper(folder);

        List<MessageWrapperBuilder> builders = new ArrayList<>(folder.messages.size());
        for (Pair<MessageType, Message> message : Lists.reverse(folder.messages)) {
            switch (message.getFirst()) {
                case READ:
                    builders.add(account.newReadMessage(folderWrapper));
                    break;
                case UNREAD:
                    builders.add(account.newUnreadMessage(folderWrapper));
                    break;
                case CUSTOM:
                    builders.add(createCustomMessage(folderWrapper, message.getSecond()));
                    break;
                default:
                    throw new UnexpectedCaseException("Message type must be one of READ, UNREAD, CUSTOM");
            }
        }

        return Lists.reverse(builders);
    }

    @NonNull
    private List<MessageWrapper> findMessagesInFolder(@NonNull Set<Long> messageIds) {
        return CollectionsKt.filter(account.messages.messages, message -> messageIds.contains(Long.parseLong(message.getMid())));
    }

    @NonNull
    private MessageWrapperBuilder createCustomMessage(
            @NonNull FolderWrapper folderWrapper,
            @NonNull Message message
    ) {
        MessageWrapperBuilder wrapperBuilder;
        if (message.unread) {
            wrapperBuilder = account.newUnreadMessage(folderWrapper);
        } else {
            wrapperBuilder = account.newReadMessage(folderWrapper);
        }

        if (message.messageId != NO_MESSAGE_ID) {
            wrapperBuilder.mid(String.valueOf(message.messageId));
            wrapperBuilder.tid(String.valueOf(message.messageId));
        }

        wrapperBuilder.labels(collect(message.labelIds, lid -> {
            if (lid.equals(serverImportant().getServerLid())) {
                return serverImportant();
            } else {
                return getServerLabel(lid);
            }
        }));

        if (message.timestamp != null) {
            wrapperBuilder.timestamp(message.timestamp);
        }

        if (message.fromEmail != null && message.fromName != null) {
            wrapperBuilder.from(message.fromEmail, message.fromName);
        }

        wrapperBuilder.to(message.to);
        wrapperBuilder.cc(message.cc);
        wrapperBuilder.bcc(message.bcc);

        if (message.prefix != null) {
            wrapperBuilder.subjPrefix(message.prefix);
        }

        if (message.subject != null) {
            wrapperBuilder.subjText(message.subject);
        }

        if (message.types != null) {
            wrapperBuilder.types(message.types);
        }

        if (message.content != null) {
            wrapperBuilder.content(message.content);
        }

        for (Attachment attachment : message.attachments) {
            AttachmentWrapper attachmentWrapper = createCustomAttachment(attachment);
            account.attachments.add(attachmentWrapper);
            wrapperBuilder.attachment(attachmentWrapper);
        }

        if (FoldersCache.THREAD_IGNORED_FOLDER_TYPES_LIST.contains(FolderType.forServerType(folderWrapper.getType()))) {
            wrapperBuilder.tid(null);
        }

        return wrapperBuilder;
    }

    @NonNull
    private AttachmentWrapper createCustomAttachment(@NonNull Attachment attachment) {
        final AttachmentWrapper.AttachmentWrapperBuilder wrapperBuilder = AttachmentWrapper.newBaseAttachmentBuilder(attachment.hid);

        if (attachment.name != null) {
            wrapperBuilder.name(attachment.name);
        }

        if (attachment.size != -1) {
            wrapperBuilder.size(attachment.size);
        }

        if (attachment.mimeType != null) {
            wrapperBuilder.mimeType(attachment.mimeType);
        }

        if (attachment.downloadUrl != null) {
            wrapperBuilder.url(attachment.downloadUrl);
        }

        wrapperBuilder.disk(attachment.isDisk);

        return wrapperBuilder.build();
    }

    @Nullable
    FolderWrapper getDefaultWrapperOrNull(long folderId) {
        String folderIdStr = String.valueOf(folderId);

        if (account.folders.isPresentFolderWithFid(folderIdStr)) {
            return account.folders.getByFid(folderIdStr);
        }

        if (folderIdStr.equals(serverInbox().getServerFid())) {
            return serverInbox();
        } else if (folderIdStr.equals(serverOutgoing().getServerFid())) {
            return serverOutgoing();
        } else if (folderIdStr.equals(serverSent().getServerFid())) {
            return serverSent();
        } else if (folderIdStr.equals(serverSpam().getServerFid())) {
            return serverSpam();
        } else if (folderIdStr.equals(serverTrash().getServerFid())) {
            return serverTrash();
        } else if (folderIdStr.equals(serverDrafts().getServerFid())) {
            return serverDrafts();
        } else {
            return null;
        }
    }

    @NonNull
    private FolderWrapper getOrCreateFolderWrapper(@NonNull Folder folder) {
        FolderWrapper wrapper = getDefaultWrapperOrNull(folder.folderId);
        if (wrapper == null) {
            String folderIdStr = String.valueOf(folder.folderId);
            String parentIdStr = folder.parentId != NO_FOLDER_ID ? String.valueOf(folder.parentId) : "";
            String name = !TextUtils.isEmpty(folder.name) ? folder.name : "Folder " + folderIdStr;
            wrapper = account.newFolder(name).serverFid(folderIdStr).parent(parentIdStr).build();
            account.addFolders(wrapper);
        }

        return wrapper;
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

    public static class Thread {

        long threadId = NO_THREAD_ID;

        @NonNull
        final List<Folder> folders;

        public Thread(long threadId, @NonNull List<Folder> folders) {
            this.threadId = threadId;
            this.folders = folders;
        }

        @NonNull
        public static Builder createThread() {
            return new Builder();
        }

        public static class Builder {

            long threadId = NO_THREAD_ID;

            @NonNull
            final List<Folder> folders = new ArrayList<>();

            @NonNull
            public Builder threadId(long threadId) {
                this.threadId = threadId;
                return this;
            }

            @NonNull
            public Builder folder(@NonNull Folder.Builder folderBuilder) {
                this.folders.add(folderBuilder.build());
                return this;
            }

            @NonNull
            public Thread build() {
                return new Thread(threadId, folders);
            }
        }
    }

    public static class Folder {

        final long folderId;

        final long parentId;

        @Nullable
        final String name;

        @NonNull
        final List<Pair<MessageType, Message>> messages;

        @NonNull
        final Set<Long> messageIdsToDelete;

        Folder(
                long folderId,
                long parentId,
                @Nullable String name,
                @NonNull List<Pair<MessageType, Message>> messages,
                @NonNull Set<Long> messageIdsToDelete
        ) {
            this.folderId = folderId;
            this.parentId = parentId;
            this.name = name;
            this.messages = messages;
            this.messageIdsToDelete = messageIdsToDelete;
        }

        @NonNull
        public static Builder createFolder() {
            return new Builder();
        }

        @NonNull
        public static Builder withId(long folderId) {
            return new Builder().folderId(folderId);
        }

        public static class Builder {

            long folderId = NO_FOLDER_ID;

            long parentId = NO_FOLDER_ID;

            @Nullable
            String name;

            @NonNull
            final List<Pair<MessageType, Message>> messages = new ArrayList<>();

            @NonNull
            final Set<Long> messageIdsToDelete = new HashSet<>();

            @NonNull
            public Builder folderId(long folderId) {
                this.folderId = folderId;
                return this;
            }

            @NonNull
            public Builder parentId(long parentId) {
                this.parentId = parentId;
                return this;
            }

            @NonNull
            public Builder name(@NonNull String name) {
                this.name = name;
                return this;
            }

            @NonNull
            public Builder addReadMessages(int count) {
                messages.addAll(nCopies(count, new Pair<>(MessageType.READ, null)));
                return this;
            }

            @NonNull
            public Builder addUnreadMessages(int count) {
                messages.addAll(nCopies(count, new Pair<>(MessageType.UNREAD, null)));
                return this;
            }

            @NonNull
            public Builder addMessage(@NonNull Message.Builder customMessageBuilder) {
                return addMessages(1, i -> customMessageBuilder);
            }

            @NonNull
            public Builder addMessages(int count, @NonNull Function1<Long, Message.Builder> messageBuilderMapper) {
                for (long i = 0; i < count; i++) {
                    messages.add(new Pair<>(MessageType.CUSTOM, messageBuilderMapper.invoke(i).build()));
                }
                return this;
            }

            @NonNull
            public Builder removeMessages(@NonNull Collection<Long> messageIdsToDelete) {
                this.messageIdsToDelete.addAll(messageIdsToDelete);
                return this;
            }

            @NonNull
            public Folder build() {
                if (folderId == NO_FOLDER_ID) {
                    throw new IllegalStateException("folderId is required");
                }

                return new Folder(folderId, parentId, name, messages, messageIdsToDelete);
            }
        }
    }

    public static class Message {

        long messageId = NO_MESSAGE_ID;

        boolean unread = true;

        @NonNull
        List<String> labelIds;

        @Nullable
        Date timestamp;

        @Nullable
        String fromEmail;

        @Nullable
        String fromName;

        @NonNull
        List<String> to;

        @NonNull
        List<String> cc;

        @NonNull
        List<String> bcc;

        @Nullable
        String prefix;

        @Nullable
        String subject;

        @Nullable
        Type[] types;

        @Nullable
        String content;

        @NonNull
        String contentType;

        @NonNull
        List<Attachment> attachments;

        public Message(
                long messageId,
                boolean unread,
                @NonNull List<String> labelIds,
                @Nullable Date timestamp,
                @Nullable String fromEmail,
                @Nullable String fromName,
                @NonNull List<String> to,
                @NonNull List<String> cc,
                @NonNull List<String> bcc,
                @Nullable String prefix,
                @Nullable String subject,
                @Nullable Type[] types,
                @Nullable String content,
                @NonNull List<Attachment> attachments,
                @NonNull String contentType
        ) {
            this.messageId = messageId;
            this.unread = unread;
            this.labelIds = labelIds;
            this.timestamp = timestamp;
            this.fromEmail = fromEmail;
            this.fromName = fromName;
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.prefix = prefix;
            this.subject = subject;
            this.types = types;
            this.content = content;
            this.attachments = attachments;
            this.contentType = contentType;
        }

        @NonNull
        public static Builder createMessage() {
            return new Builder();
        }

        public static class Builder {

            long messageId = NO_MESSAGE_ID;

            boolean unread = true;

            @NonNull
            List<String> labelIds = new ArrayList<>();

            @Nullable
            Date timestamp;

            @Nullable
            String fromEmail;

            @Nullable
            String fromName;

            @NonNull
            List<String> to = new ArrayList<>();

            @NonNull
            List<String> cc = new ArrayList<>();

            @NonNull
            List<String> bcc = new ArrayList<>();

            @Nullable
            String prefix;

            @Nullable
            String subject;

            @Nullable
            Type[] types;

            @Nullable
            String content;

            @NonNull
            String contentType = DEFAULT_CONTENT_TYPE;

            @NonNull
            List<Attachment> attachments = new ArrayList<>();

            @NonNull
            public Builder messageId(long messageId) {
                this.messageId = messageId;
                return this;
            }

            @NonNull
            public Builder unread(boolean unread) {
                this.unread = unread;
                return this;
            }

            @NonNull
            public Builder label(@NonNull String labelId) {
                this.labelIds.add(labelId);
                return this;
            }

            @NonNull
            public Builder timestamp(@NonNull Date timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            @NonNull
            public Builder from(@NonNull String email, @NonNull String name) {
                this.fromEmail = email;
                this.fromName = name;
                return this;
            }

            @NonNull
            public Builder addTo(@NonNull String email) {
                to.add(email);
                return this;
            }

            @NonNull
            public Builder addCc(@NonNull String email) {
                cc.add(email);
                return this;
            }

            @NonNull
            public Builder addBcc(@NonNull String email) {
                bcc.add(email);
                return this;
            }

            @NonNull
            public Builder subject(@NonNull String subject) {
                this.subject = subject;
                return this;
            }

            @NonNull
            public Builder subjectWithPrefix(@NonNull String prefix, @NonNull String subject) {
                this.prefix = prefix;
                this.subject = subject;
                return this;
            }

            @NonNull
            public Builder types(@NonNull Type... types) {
                this.types = types;
                return this;
            }

            @NonNull
            public Builder content(@NonNull String content) {
                this.content = content;
                return this;
            }

            @NonNull
            public Builder contentType(@NonNull String contentType) {
                this.contentType = contentType;
                return this;
            }

            @NonNull
            public Builder addAttachment(@NonNull Attachment.Builder builder) {
                this.attachments.add(builder.build());
                return this;
            }

            @NonNull
            public Message build() {
                return new Message(
                        messageId,
                        unread,
                        labelIds,
                        timestamp,
                        fromEmail,
                        fromName,
                        to,
                        cc,
                        bcc,
                        prefix,
                        subject,
                        types,
                        content,
                        attachments,
                        contentType
                );
            }
        }
    }

    public static class Label {

        @NonNull
        final String labelId;

        @Nullable
        private final String name;

        private final int type;

        Label(@NonNull String labelId, @Nullable String name, int type) {
            this.labelId = labelId;
            this.name = name;
            this.type = type;
        }

        @NonNull
        public static Label.Builder createLabel() {
            return new Label.Builder();
        }

        public static class Builder {

            @Nullable
            String labelId;

            @Nullable
            String name;

            private int type = USER;

            @NonNull
            public Builder labelId(@NonNull String labelId) {
                this.labelId = labelId;
                return this;
            }

            @NonNull
            public Builder name(@NonNull String name) {
                this.name = name;
                return this;
            }

            @NonNull
            public Builder type(int type) {
                this.type = type;
                return this;
            }

            @NonNull
            public Label build() {
                if (labelId == null) {
                    throw new IllegalStateException("labelId is required");
                }

                return new Label(labelId, name, type);
            }
        }
    }

    public static class Attachment {

        @Nullable
        private String name;

        private long size;

        @NonNull
        final String hid;

        @Nullable
        final String mimeType;

        private boolean isDisk;

        @Nullable
        private String downloadUrl;

        public Attachment(
                @Nullable String name,
                long size,
                @NonNull String hid,
                @Nullable String mimeType,
                boolean isDisk,
                @Nullable String downloadUrl
        ) {
            this.name = name;
            this.size = size;
            this.hid = hid;
            this.mimeType = mimeType;
            this.isDisk = isDisk;
            this.downloadUrl = downloadUrl;
        }

        @NonNull
        public static Builder createAttachment() {
            return new Builder();
        }

        @NonNull
        public static Builder createTextAttachment(@NonNull String name, @NonNull String content) {
            return new Builder()
                    .setName(name)
                    .setSize(content.length())
                    .setHid("")
                    .setMimeType("text/plain")
                    .setDownloadUrl("");
        }

        public static class Builder {

            @Nullable
            private String name;

            private long size = -1;

            @Nullable
            private String hid;

            @Nullable
            private String mimeType;

            private boolean isDisk;

            @Nullable
            private String downloadUrl;

            @NonNull
            public Builder setName(@NonNull String name) {
                this.name = name;
                return this;
            }

            @NonNull
            public Builder setSize(long size) {
                this.size = size;
                return this;
            }

            @NonNull
            public Builder setHid(@NonNull String hid) {
                this.hid = hid;
                return this;
            }

            @NonNull
            public Builder setMimeType(@Nullable String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            @NonNull
            public Builder setIsDisk(boolean isDisk) {
                this.isDisk = isDisk;
                return this;
            }

            @NonNull
            public Builder setDownloadUrl(@Nullable String downloadUrl) {
                this.downloadUrl = downloadUrl;
                return this;
            }

            @NonNull
            public Attachment build() {
                if (hid == null) {
                    throw new IllegalStateException();
                }

                return new Attachment(name, size, hid, mimeType, isDisk, downloadUrl);
            }
        }
    }


    public enum MessageType {
        /**
         * Default read message.
         */
        READ,
        /**
         * Default unread message.
         */
        UNREAD,
        /**
         * Fully customizable message. You could edit all parameters.
         */
        CUSTOM
    }
}
