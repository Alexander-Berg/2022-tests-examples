package com.yandex.mail.react;

import android.text.util.Rfc822Token;

import com.yandex.mail.react.entity.Attachment;
import com.yandex.mail.react.entity.Avatar;
import com.yandex.mail.react.entity.Fields;
import com.yandex.mail.react.entity.ReactMessage;
import com.yandex.mail.react.entity.Recipient;
import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlin.Triple;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(UnitTestRunner.class)
public final class ReactMailUtilTest {

    @Test
    public void divideMessagesForAddUpdateAndRemove() {
        final ReactMessage
                message1 = ReactTestFactory.buildMessageWithIdWithoutBody(1),
                message2 = ReactTestFactory.buildMessageWithIdWithoutBody(2),
                message3 = ReactTestFactory.buildMessageWithIdWithoutBody(3);

        Map<Long, ReactMessage> oldMessages = new LinkedHashMap<>(2);
        oldMessages.put(message1.messageId(), message1);
        oldMessages.put(message2.messageId(), message2);

        List<ReactMessage> newMessages = new ArrayList<>(1);
        newMessages.add(message1.toBuilder().body("new body").build());
        newMessages.add(message3);

        // 1 should be updated (collapsed state of the message should be persisted)
        // 2 should be removed
        // 3 should be added
        Triple<List<ReactMessage>, List<ReactMessage>, List<ReactMessage>> triplet =
                ReactMailUtil.addUpdateOrRemoveMessages(
                        oldMessages,
                        newMessages,
                        emptyList()
                );

        List<ReactMessage> messagesToAdd = triplet.getFirst();
        List<ReactMessage> messagesToUpdate = triplet.getSecond();
        List<ReactMessage> messagesIdsToRemove = triplet.getThird();

        assertThat(messagesIdsToRemove).hasSize(1);
        assertThat(messagesIdsToRemove.get(0).messageId()).isEqualTo(message2.messageId());

        assertThat(messagesToUpdate).hasSize(1);
        assertThat(messagesToUpdate.get(0).messageId()).isEqualTo(message1.messageId());

        assertThat(messagesToAdd).hasSize(1);
        assertThat(messagesToAdd.get(0).messageId()).isEqualTo(message3.messageId());
    }

    @Test
    public void urlToAvatarFileShouldReturnNullIfAvatarIsNull() {
        assertThat(ReactMailUtil.urlToAvatarFile(null)).isNull();
    }

    @Test
    public void urlToAvatarFileShouldReturnNullIfAvatarImageIsNull() {
        assertThat(ReactMailUtil.urlToAvatarFile(new Avatar.Builder().monogram("only monogram").build())).isNull();
    }

    @Test
    public void urlToAvatarFileShouldReturnUrlToAvatarImage() {
        Avatar avatar = new Avatar.Builder().monogram("only monogram").imageUrl("weeee").build();
        assertThat(ReactMailUtil.urlToAvatarFile(avatar)).isEqualTo("weeee");
    }

    @Test
    public void urlsToAttachmentsPreviewsShouldReturnEmptyIfNoAttachments() {
        assertThat(ReactMailUtil.urlsToAttachmentsPreviews(emptyList())).isEmpty();
    }

    @Test
    public void urlsToAttachmentsPreviewsShouldReturnUrlToAttachmentPreview() {
        List<Attachment> attachments = singletonList(
                new Attachment.Builder()
                        .hid("1")
                        .name("2")
                        .size("3 Kb")
                        .previewUrl("lol")
                        .supportsPreview(false)
                        .hasThumbnail(false)
                        .type("type")
                        .extension("ext 1")
                        .disk(false)
                        .build()
        );
        Collection<String> urlsToAttachmentsPreviews = ReactMailUtil.urlsToAttachmentsPreviews(attachments);

        assertThat(urlsToAttachmentsPreviews).hasSize(1);
        assertThat(urlsToAttachmentsPreviews.iterator().next()).isEqualTo("lol");
    }

    @Test
    public void urlsToAvatarsFilesFromToCcBccShouldReturnEmptyIfToCcBccNull() {
        assertThat(ReactMailUtil.urlsToAvatarsFilesFromToCcBcc(null)).isEmpty();
    }

    @Test
    public void urlsToAvatarsFilesFromToCcBccShouldReturnEmptyIfToCcBccAreEmpty() {
        Fields toCcBcc = Fields.EMPTY_FIELDS;
        assertThat(ReactMailUtil.urlsToAvatarsFilesFromToCcBcc(toCcBcc)).isEmpty();
    }

    @Test
    public void urlsToAvatarsFilesFromToCcBccShouldReturnEmptyIfToCcBccHasNoAvatarsWithImages() {
        Avatar avatar1 = new Avatar.Builder().monogram("a1").build();
        Avatar avatar2 = new Avatar.Builder().monogram("a2").build();
        Avatar avatar3 = new Avatar.Builder().monogram("a3").build();
        Avatar avatar4 = new Avatar.Builder().monogram("a4").build();

        Fields toCcBcc = new Fields(
                singletonList(Recipient.create(new Rfc822Token("Somebody1", "s1@ya.ru", null), avatar1)),
                singletonList(Recipient.create(new Rfc822Token("Somebody4", "s4@ya.ru", null), avatar4)),
                singletonList(Recipient.create(new Rfc822Token("Somebody2", "s2@ya.ru", null), avatar2)),
                singletonList(Recipient.create(new Rfc822Token("Somebody3", "s3@ya.ru", null), avatar3))
        );

        assertThat(ReactMailUtil.urlsToAvatarsFilesFromToCcBcc(toCcBcc)).isEmpty();
    }

    @Test
    public void urlsToAvatarsFilesFromToCcBccShouldReturnAvatarsToCcBcc() {
        Avatar avatar1 = new Avatar.Builder().monogram("a1").imageUrl("1").build();
        Avatar avatar2 = new Avatar.Builder().monogram("a2").imageUrl("2").build();
        Avatar avatar3 = new Avatar.Builder().monogram("a3").imageUrl("3").build();
        Avatar avatar4 = new Avatar.Builder().monogram("a4").imageUrl("4").build();

        Fields toCcBcc = new Fields(
                singletonList(Recipient.create(new Rfc822Token("Somebody1", "s1@ya.ru", null), avatar1)),
                singletonList(Recipient.create(new Rfc822Token("Somebody4", "s4@ya.ru", null), avatar4)),
                singletonList(Recipient.create(new Rfc822Token("Somebody2", "s2@ya.ru", null), avatar2)),
                singletonList(Recipient.create(new Rfc822Token("Somebody3", "s3@ya.ru", null), avatar3))
        );

        assertThat(ReactMailUtil.urlsToAvatarsFilesFromToCcBcc(toCcBcc)).containsOnly("1", "2", "3", "4");
    }

    @Test
    public void collectUrisToMessageResourcesShouldReturnCollectAllUris() {
        Recipient to = Recipient.create(
                new Rfc822Token("r1", "r1@ya.ru", null),
                new Avatar.Builder()
                        .monogram("a1")
                        .imageUrl("r1Avatar")
                        .build()
        );

        Recipient cc = Recipient.create(
                new Rfc822Token("r2", "r2@ya.ru", null),
                new Avatar.Builder()
                        .monogram("a2")
                        .imageUrl("r2Avatar")
                        .build()
        );

        Recipient bcc = Recipient.create(
                new Rfc822Token("r3", "r3@ya.ru", null),
                new Avatar.Builder()
                        .monogram("a2")
                        .imageUrl("r3Avatar")
                        .build()
        );

        Recipient from = Recipient.create(
                new Rfc822Token("r4", "r4@ya.ru", null),
                new Avatar.Builder()
                        .monogram("a2")
                        .imageUrl("r4Avatar")
                        .build()
        );

        ReactMessage message = ReactTestFactory.buildMessageWithIdWithoutBody(1)
                .toBuilder()
                .attachments(singletonList(
                        new Attachment.Builder()
                                .hid("hid")
                                .name("name")
                                .size("3 Kb")
                                .previewUrl("attachUrl")
                                .type("type")
                                .supportsPreview(false)
                                .hasThumbnail(false)
                                .extension("ext")
                                .disk(false)
                                .build())
                )
                .toCcBcc(new Fields(
                        singletonList(to),
                        singletonList(from),
                        singletonList(cc),
                        singletonList(bcc)
                         )
                )
                .build();

        assertThat(ReactMailUtil.collectUrisToMessageResources(message))
                .containsOnly(
                        "attachUrl",
                        "r1Avatar",
                        "r2Avatar",
                        "r3Avatar",
                        "r4Avatar"
                );
    }
}