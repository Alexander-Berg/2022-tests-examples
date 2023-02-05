package com.yandex.mail.react.entity;

import android.text.util.Rfc822Token;

import com.yandex.mail.network.response.GsonTest;
import com.yandex.mail.react.ReactTestFactory;
import com.yandex.mail.react.entity.ReactMessage.Action;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import kotlin.collections.CollectionsKt;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static kotlin.collections.CollectionsKt.listOf;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class ReactMessageTest extends GsonTest {

    @Test
    public void equals_hashcode_verify() {
        assertThat(ReactTestFactory.buildMessageWithIdWithoutBody(1)).isNotEqualTo(ReactTestFactory.buildMessageWithIdWithoutBody(2));
    }

    @Test
    public void toJson_allPropertiesFilled() {
        ReactMessage reactMessage = new ReactMessage.Builder()
                .messageId(1L)
                .folderId(2L)
                .firstLine("Test first line")
                .body("Test body")
                .typeMask(4096)
                .bodyLoadingError(new RuntimeException())
                .folderName("Test folder name")
                .time("Test time")
                .timestamp(3L)
                .rawLabels("Test raw labels")
                .labels(
                        asList(
                                new ReactLabel("4", "Test color 1", "Test label 1"),
                                new ReactLabel("5", "Test color 2", "Test label 2")
                        )
                )
                .hasAttachments(true)
                .attachments(
                        asList(
                                new Attachment.Builder()
                                        .name("Test attachment 1")
                                        .hid("at1")
                                        .previewUrl("atUrl1")
                                        .size("size1")
                                        .type("type1")
                                        .supportsPreview(false)
                                        .hasThumbnail(false)
                                        .extension("ext1")
                                        .disk(false)
                                        .build(),
                                new Attachment.Builder()
                                        .name("Test attachment 2")
                                        .hid("at2")
                                        .previewUrl("atUrl2")
                                        .size("size2")
                                        .type("type2")
                                        .extension("ext2")
                                        .supportsPreview(false)
                                        .hasThumbnail(false)
                                        .disk(false)
                                        .build()
                        )
                )
                .toCcBcc(
                        new Fields(
                                asList(
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc1", "toCcBcc2", "toCcBcc3"),
                                                new Avatar.Builder().monogram("av1").imageUrl("av1Url").build()
                                        ),
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc4", "toCcBcc5", "toCcBcc6"),
                                                new Avatar.Builder().monogram("av2").imageUrl("av2Url").build()
                                        )
                                ),
                                asList(
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc19", "toCcBcc20", "toCcBcc21"),
                                                new Avatar.Builder().monogram("av7").imageUrl("av7Url").build()
                                        ),
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc22", "toCcBcc23", "toCcBcc24"),
                                                new Avatar.Builder().monogram("av8").imageUrl("av8Url").build()
                                        )
                                ),
                                asList(
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc7", "toCcBcc8", "toCcBcc9"),
                                                new Avatar.Builder().monogram("av3").imageUrl("av3Url").build()
                                        ),
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc10", "toCcBcc11", "toCcBcc12"),
                                                new Avatar.Builder().monogram("av4").imageUrl("av4Url").build()
                                        )
                                ),
                                asList(
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc13", "toCcBcc14", "toCcBcc15"),
                                                new Avatar.Builder().monogram("av5").imageUrl("av5Url").build()
                                        ),
                                        Recipient.create(
                                                new Rfc822Token("toCcBcc16", "toCcBcc17", "toCcBcc18"),
                                                new Avatar.Builder().monogram("av6").imageUrl("av6Url").build()
                                        )
                                )
                        )
                )
                .read(true)
                .collapsed(true)
                .draft(true)
                .important(true)
                .participantsCount(6)
                .controls(listOf(Action.COMPOSE, Action.REPLY, Action.REPLY_ALL, Action.MORE, Action.MOVE_TO_TRASH))
                .build();

        String reactMessageAsJson = gson.toJson(reactMessage);

        assertJsonEquals(
                "{\"mid\":\"1\"," +
                "\"firstline\":\"Test first line\"," +
                "\"body\":\"Test body\"," +
                "\"typeMask\":4096," +
                "\"folderName\":\"Test folder name\"," +
                "\"time\":\"Test time\"," +
                "\"timestamp\":3," +
                "\"labels\":[" +
                "{\"lid\":\"4\",\"color\":\"Test color 1\",\"name\":\"Test label 1\"}," +
                "{\"lid\":\"5\",\"color\":\"Test color 2\",\"name\":\"Test label 2\"}" +
                "]," +
                "\"attachments\":[" +
                "{" +
                "\"hasThumbnail\":false,\"name\":\"Test attachment 1\",\"type\":\"type1\"," +
                "\"preview\":\"atUrl1\",\"hid\":\"at1\",\"size\":\"size1\",\"ext\":\"ext1\",\"disk\":false" +
                "}," +
                "{" +
                "\"hasThumbnail\":false,\"name\":\"Test attachment 2\",\"type\":\"type2\"," +
                "\"preview\":\"atUrl2\",\"hid\":\"at2\",\"size\":\"size2\",\"ext\":\"ext2\",\"disk\":false" +
                "}" +
                "]," +
                "\"fields\":" +
                "{\"to\":[" +
                "{\"email\":\"toCcBcc2\",\"name\":\"toCcBcc1\",\"avatar\":{\"monogram\":\"av1\",\"image\":\"av1Url\",\"type\":\"image\"}}," +
                "{\"email\":\"toCcBcc5\",\"name\":\"toCcBcc4\",\"avatar\":{\"monogram\":\"av2\",\"image\":\"av2Url\",\"type\":\"image\"}}" +
                "]," +
                "\"from\":[" +
                "{\"email\":\"toCcBcc20\",\"name\":\"toCcBcc19\",\"avatar\":{\"monogram\":\"av7\",\"image\":\"av7Url\",\"type\":\"image\"}}," +
                "{\"email\":\"toCcBcc23\",\"name\":\"toCcBcc22\",\"avatar\":{\"monogram\":\"av8\",\"image\":\"av8Url\",\"type\":\"image\"}}" +
                "]," +
                "\"cc\":[" +
                "{\"email\":\"toCcBcc8\",\"name\":\"toCcBcc7\",\"avatar\":{\"monogram\":\"av3\",\"image\":\"av3Url\",\"type\":\"image\"}}," +
                "{\"email\":\"toCcBcc11\",\"name\":\"toCcBcc10\",\"avatar\":{\"monogram\":\"av4\",\"image\":\"av4Url\",\"type\":\"image\"}}" +
                "]," +
                "\"hidden\":[" +
                "{\"email\":\"toCcBcc14\",\"name\":\"toCcBcc13\",\"avatar\":{\"monogram\":\"av5\",\"image\":\"av5Url\",\"type\":\"image\"}}," +
                "{\"email\":\"toCcBcc17\",\"name\":\"toCcBcc16\",\"avatar\":{\"monogram\":\"av6\",\"image\":\"av6Url\",\"type\":\"image\"}}" +
                "]}," +
                "\"read\":true," +
                "\"isCollapsed\":true," +
                "\"isDraft\":true," +
                "\"isImportant\":true," +
                "\"isPhishing\":false," +
                "\"participantsCount\":6," +
                "\"controls\":[\"compose\",\"reply\",\"reply-all\",\"more\",\"trash\"]" +
                "}",
                reactMessageAsJson
        );
    }

    @Test
    public void toJson_ignoreNullBody() {
        ReactMessage reactMessage = new ReactMessage.Builder()
                .messageId(1L)
                .folderId(2L)
                .typeMask(4096)
                .firstLine("Test first line")
                .bodyLoadingError(new RuntimeException())
                .folderName("Test folder name")
                .time("Test time")
                .timestamp(3L)
                .rawLabels("Test raw labels")
                .labels(emptyList())
                .hasAttachments(true)
                .attachments(emptyList())
                .toCcBcc(new Fields(emptyList(), emptyList(), emptyList(), emptyList()))
                .read(true)
                .collapsed(true)
                .draft(true)
                .important(true)
                .participantsCount(6)
                .controls(CollectionsKt.emptyList())
                .build();

        String reactMessageAsJson = gson.toJson(reactMessage);

        assertJsonEquals(
                "{\"mid\":\"1\"," +
                "\"firstline\":\"Test first line\"," +
                "\"folderName\":\"Test folder name\"," +
                "\"typeMask\":4096," +
                "\"time\":\"Test time\"," +
                "\"timestamp\":3," +
                "\"labels\":[]," +
                "\"attachments\":[]," +
                "\"fields\":" +
                "{\"to\":[]," +
                "\"from\":[]," +
                "\"cc\":[]," +
                "\"hidden\":[]}," +
                "\"read\":true," +
                "\"isCollapsed\":true," +
                "\"isDraft\":true," +
                "\"isImportant\":true," +
                "\"isPhishing\":false," +
                "\"participantsCount\":6," +
                "\"controls\":[]" +
                "}",
                reactMessageAsJson
        );
    }
}
