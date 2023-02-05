package com.yandex.mail.network.response;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.storage.MessageStatus;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class ThreadMetaJsonTest extends GsonTest {

    @SuppressWarnings("checkstyle:NumericLiteralNeedsUnderscore")
    @Test
    public void fromJson() throws IOException {
        ThreadMeta thread = gson.fromJson(THREAD_META_RESPONSE_STRING, ThreadMeta.class);
        assertThat(thread.getMid()).isEqualTo(157344511981256773L);
        assertThat(thread.getFid()).isEqualTo(1L);
        assertThat(thread.getTid()).isEqualTo(157344511981256769L);
        assertThat(thread.getLid()).containsOnly("5");
        assertThat(thread.getStatus()).containsOnly(MessageStatus.Status.UNREAD.getId());
        assertThat(thread.getThreadCount()).isEqualTo(2);
        assertThat(thread.getScn()).isEqualTo(60L);
        assertThat(thread.getUtc_timestamp()).isEqualTo(1450357412L);
        assertThat(thread.getHasAttach()).isFalse();
        assertThat(thread.getSubjPrefix()).isEqualTo("");
        assertThat(thread.getSubjText()).isEqualTo("another test");

        final String recipientString =
                " {\n" +
                "      \"email\":\"testvs1@yandex.ru\",\n" +
                "      \"name\":\"Stanislav Voronchihin\",\n" +
                "      \"type\":2\n" +
                " }";
        RecipientJson recipient = gson.fromJson(recipientString, RecipientJson.class);

        assertThat(thread.getFrom()).isEqualToComparingFieldByField(recipient);
        assertThat(thread.getFirstLine()).isEqualTo("Fhhfhhgg");
        assertThat(thread.getTypes()).isEmpty();
    }

    private final String THREAD_META_RESPONSE_STRING =
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
            "    \"threadCount\":\"2\",\n" +
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
