package ru.yandex.webmaster3.core.turbo.model.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: ishalaru
 * DATE: 12.08.2019
 */
public class TurboCommentsSettingsTest {
    private static ObjectMapper OM = new ObjectMapper();

    @Test
    public void serializeTest() throws IOException {
        TurboCommentsSettings turboCommentsSettings =
                new TurboCommentsSettings("testCommentEndpoitn", "testAddComenntsEndpoint", TurboCommentsSettings.TURBO_COMMENT_STATUS.MANUAL);
        final String s = OM.writeValueAsString(turboCommentsSettings);
        final TurboCommentsSettings turboCommentsSettings1 = OM.readValue(s, TurboCommentsSettings.class);
        Assert.assertEquals("Compare object after deserialization.", turboCommentsSettings, turboCommentsSettings1);
    }

    @Test
    public void testBackwardCompatibility() throws IOException {
        String str = "{\"getCommentsEndpoint\":\"testCommentEndpoitn\",\"addCommentsEndpoint\":\"testAddComenntsEndpoint\"}";
        final TurboCommentsSettings turboCommentsSettings = OM.readValue(str, TurboCommentsSettings.class);
        Assert.assertEquals(turboCommentsSettings.getAddCommentsEndpoint(), "testAddComenntsEndpoint");
        Assert.assertEquals(turboCommentsSettings.getGetCommentsEndpoint(), "testCommentEndpoitn");
        Assert.assertEquals(turboCommentsSettings.getStatus(), TurboCommentsSettings.TURBO_COMMENT_STATUS.MANUAL);
    }

    @Test
    public void serializeAndDeserializeAuto() throws IOException {
        TurboCommentsSettings turboCommentsSettings =
                new TurboCommentsSettings("testCommentEndpoitn", "testAddComenntsEndpoint", TurboCommentsSettings.TURBO_COMMENT_STATUS.CMNT);
        final String s = OM.writeValueAsString(turboCommentsSettings);
        final TurboCommentsSettings turboCommentsSettings1 = OM.readValue(s, TurboCommentsSettings.class);
        Assert.assertEquals("Compare object after deserialization.", turboCommentsSettings, turboCommentsSettings1);
    }
}