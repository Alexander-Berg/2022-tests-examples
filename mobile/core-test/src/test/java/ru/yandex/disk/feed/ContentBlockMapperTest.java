package ru.yandex.disk.feed;

import org.json.JSONObject;
import org.junit.Test;
import ru.yandex.disk.test.AndroidTestCase2;

import static org.hamcrest.Matchers.equalTo;

public class ContentBlockMapperTest extends AndroidTestCase2 {

    @Test
    public void shouldCreateFromJson() throws Exception {
        final String dataString = "{\"files_count\":\"5\","
                + "\"mtill\":\"1481044731000\","
                + "\"media_type\":\"image\","
                + "\"mfrom\":\"1480958331000\","
                + "\"modifier_uid\":\"157603762\","
                + "\"mtime\":\"1481204046932\","
                + "\"folder_id\":\"157603762:4582e9bf6f970052752230759da9504a7137741d27f4ea2738f4e2ad6513ce29\","
                + "\"type\":\"content_block\","
                + "\"group_key\":\"157603762:4582e9bf6f970052752230759da9504a7137741d27f4ea2738f4e2ad6513ce29:image:157603762\","
                + "\"collection_id\":\"index\","
                + "\"block-id\":\"12345\","
                + "\"order\":\"2280\","
                + "\"area\":\"photounlim\"}";
        final JSONObject jsonObject = new JSONObject(dataString);
        final ContentBlock block = ContentBlockMapper.create(FeedBlock.DataSource.FEED, jsonObject);

        assertNotNull(block);
        assertThat(block.getMediaType(), equalTo("image"));
        assertThat(block.getModifierUid(), equalTo("157603762"));
        assertThat(block.getFolderId(), equalTo("157603762:4582e9bf6f970052752230759da9504a7137741d27f4ea2738f4e2ad6513ce29"));
        assertThat(block.getType(), equalTo("content_block"));
        assertThat(block.getOrder(), equalTo(0));
        assertThat(block.getRemoteId(), equalTo("12345"));
        assertThat(block.getDate(), equalTo(1481204046932L));
        assertThat(block.getDateFrom(), equalTo(1480958331000L));
        assertThat(block.getDateTill(), equalTo(1481044731000L));
        assertThat(block.getArea(), equalTo("photounlim"));
    }
}
