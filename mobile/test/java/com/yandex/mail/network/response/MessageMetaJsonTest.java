package com.yandex.mail.network.response;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.storage.MessageStatus;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class MessageMetaJsonTest extends GsonTest {

    @SuppressWarnings("checkstyle:NumericLiteralNeedsUnderscore")
    @Test
    public void fromJson() throws IOException {
        MessageMetaJson message = gson.fromJson(MESSAGE_META_RESPONSE_STRING, MessageMetaJson.class);
        assertThat(message.getMid()).isEqualTo(157344511981256773L);
        assertThat(message.getFid()).isEqualTo(1L);
        assertThat(message.getTid()).isEqualTo(157344511981256769L);
        assertThat(message.getLid()).containsOnly("5");
        assertThat(message.getStatus()).containsOnly(MessageStatus.Status.UNREAD.getId());
        assertThat(message.getUtc_timestamp()).isEqualTo(1450357412L);
        assertThat(message.getHasAttach()).isFalse();
        assertThat(message.getSubjPrefix()).isEqualTo("");
        assertThat(message.getSubjText()).isEqualTo("another test");

        final String recipientString =
                " {\n" +
                "      \"email\":\"testvs1@yandex.ru\",\n" +
                "      \"name\":\"Stanislav Voronchihin\",\n" +
                "      \"type\":2\n" +
                " }";
        RecipientJson recipient = gson.fromJson(recipientString, RecipientJson.class);

        assertThat(message.getFrom()).isEqualToComparingFieldByField(recipient);
        assertThat(message.getFirstLine()).isEqualTo("Fhhfhhgg");
        assertThat(message.getTypes()).isEmpty();
    }

    private final String MESSAGE_META_RESPONSE_STRING =
            "{\n" +
            "    \"mid\":\"157344511981256773\",\n" +
            "    \"fid\":\"1\",\n" +
            "    \"tid\":\"157344511981256769\",\n" +
            "    \"lid\":[\n" +
            "        \"5\"\n" +
            "    ],\n" +
            "    \"modseq\":\"\",\n" +
            "    \"status\":[\n" +
            "        1\n" +
            "    ],\n" +
            "    \"scn\":\"60\",\n" +
            "    \"timestamp\":\"1450357412000\",\n" +
            "    \"utc_timestamp\":\"1450357412\",\n" +
            "    \"hasAttach\":false,\n" +
            "    \"subjEmpty\":false,\n" +
            "    \"subjPrefix\":\"\",\n" +
            "    \"subjText\":\"another test\",\n" +
            "    \"from\":{\n" +
            "        \"email\":\"testvs1@yandex.ru\",\n" +
            "        \"name\":\"Stanislav Voronchihin\",\n" +
            "        \"type\":2\n" +
            "    },\n" +
            "    \"firstLine\":\"Fhhfhhgg\",\n" +
            "    \"types\":[]\n" +
            "}";
}
