package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.DiskItem;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;

public class GetFileListMethodTest extends WebdavClientReadMethodTestCase {

    @Override
    protected void invokeMethod() throws Exception {
        client.getFileList(asList("/disk/1", "/disk/2"));
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        prepareToReturnContentFromFile(207, "GetFileListResponse.xml");
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("PROPFIND", "/", null, request);
        final String requestBody = extractEntity(request);
        assertThat(requestBody, equalTo("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n" +
                "<d:propfind xmlns:d=\"DAV:\" xmlns:e=\"urn:yandex:disk:meta\">\n" +
                "  <d:prop>\n" +
                "    <d:resourcetype />\n" +
                "    <d:displayname />\n" +
                "    <d:getcontentlength />\n" +
                "    <d:getlastmodified />\n" +
                "    <d:getetag />\n" +
                "    <d:getcontenttype />\n" +
                "    <e:alias_enabled />\n" +
                "    <e:visible />\n" +
                "    <e:shared />\n" +
                "    <e:readonly />\n" +
                "    <e:public_url />\n" +
                "    <e:etime />\n" +
                "    <e:mediatype />\n" +
                "    <e:mpfs_file_id />\n" +
                "    <e:mpfs_resource_id />\n" +
                "    <e:photoslice_album_type />\n" +
                "    <e:albums_exclusions />\n" +
                "    <e:width />\n" +
                "    <e:height />\n" +
                "    <e:beauty />\n" +
                "    <e:video_duration_millis />\n" +
                "    <e:hasthumbnail />\n" +
                "  </d:prop>\n" +
                "<multiple><resource>/disk/1</resource><resource>/disk/2</resource></multiple></d:propfind>"));
    }

    @Test
    public void shouldParseResponse() throws Exception {
        prepareGoodResponse();
        final List<? extends DiskItem> fileList = client.getFileList(asList("/disk/1", "/disk/1"));
        assertThat(fileList.size(), equalTo(2));

    }

    @Override
    protected int getGoodCode() {
        return 207;
    }
}
