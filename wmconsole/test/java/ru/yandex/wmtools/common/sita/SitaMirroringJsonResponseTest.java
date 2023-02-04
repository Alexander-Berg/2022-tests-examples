package ru.yandex.wmtools.common.sita;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * User: azakharov
 * Date: 18.03.14
 * Time: 17:34
 */
public class SitaMirroringJsonResponseTest {

    private final SitaMirroringActionStatusEnum actionOk = SitaMirroringActionStatusEnum.OK;
    private final SitaMirroringHostStatusEnum hostOk = SitaMirroringHostStatusEnum.OK;

    @Test
    public void testRerankResponse01() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_rerank_resp01.json");
        try {
            SitaMirroringResponse response = SitaMirroringJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1));
            Assert.assertNotNull("response is null", response);
            Assert.assertEquals("response.status is not OK", actionOk, response.getStatus());
            Assert.assertNotNull("response.hostResults is null", response.getHostResults());
            Assert.assertEquals("bad response.hostResults.size", 2, response.getHostResults().size());
            Assert.assertEquals("bad response.hostResults[0].status", hostOk, response.getHostResults().get(0).getStatus());
            Assert.assertEquals("bad response.hostResults[0].host", new URI("http://yandex.ru"), response.getHostResults().get(0).getHost());
            Assert.assertEquals("bad response.hostResults[0].newMainMirror", new URI("http://www.yandex.ru"), response.getHostResults().get(0).getNewMainMirror());
            Assert.assertEquals("bad response.hostResults[1].status", hostOk, response.getHostResults().get(1).getStatus());
            Assert.assertEquals("bad response.hostResults[1].host", new URI("http://www.yandex.ru"), response.getHostResults().get(1).getHost());
            Assert.assertEquals("bad response.hostResults[1].newMainMirror", new URI("http://www.yandex.ru"), response.getHostResults().get(1).getNewMainMirror());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test(expected = SitaException.class)
    public void testRerankResponse02() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_rerank_resp02.json");
        try {
            SitaMirroringResponse response = SitaMirroringJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1));
            Assert.assertNotNull("response is null", response);
            Assert.assertEquals("response.status is not OK", actionOk, response.getStatus());
            Assert.assertNull("response.hostResults is not null", response.getHostResults());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test(expected = SitaException.class)
    public void testRerankResponse03() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_rerank_resp03.json");
        try {
            SitaMirroringResponse response = SitaMirroringJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1));
            Assert.assertNotNull("response is null", response);
            Assert.assertEquals("response.status is not OK", actionOk, response.getStatus());
            Assert.assertNull("response.hostResults is not null", response.getHostResults());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test
    public void testStickResponse01() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_stick_resp01.json");
        try {
            SitaMirroringResponse response = SitaMirroringJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1));
            Assert.assertNotNull("response is null", response);
            Assert.assertEquals("response.status is not OK", actionOk, response.getStatus());
            Assert.assertNotNull("response.hostResults is null", response.getHostResults());
            Assert.assertEquals("bad response.hostResults.size", 2, response.getHostResults().size());
            Assert.assertEquals("bad response.hostResults[0].status", hostOk, response.getHostResults().get(0).getStatus());
            Assert.assertEquals("bad response.hostResults[0].host", new URI("http://www.yandex.ru"), response.getHostResults().get(0).getHost());
            Assert.assertEquals("bad response.hostResults[0].newMainMirror", new URI("http://www.yandex.ru"), response.getHostResults().get(0).getNewMainMirror());
            Assert.assertEquals("bad response.hostResults[1].status", hostOk, response.getHostResults().get(1).getStatus());
            Assert.assertEquals("bad response.hostResults[1].host", new URI("http://yandex.ru"), response.getHostResults().get(1).getHost());
            Assert.assertEquals("bad response.hostResults[1].newMainMirror", new URI("http://www.yandex.ru"), response.getHostResults().get(1).getNewMainMirror());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test
    public void testUnstickResponse01() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_unstick_resp01.json");
        try {
            SitaMirroringResponse response = SitaMirroringJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1));
            Assert.assertNotNull("response is null", response);
            Assert.assertEquals("response.status is not OK", actionOk, response.getStatus());
            Assert.assertNotNull("response.hostResults is null", response.getHostResults());
            Assert.assertEquals("bad response.hostResults.size", 2, response.getHostResults().size());
            Assert.assertEquals("bad response.hostResults[0].status", hostOk, response.getHostResults().get(0).getStatus());
            Assert.assertEquals("bad response.hostResults[0].host", new URI("http://yandeh.ru"), response.getHostResults().get(0).getHost());
            Assert.assertEquals("bad response.hostResults[0].newMainMirror", new URI("http://yandeh.ru"), response.getHostResults().get(0).getNewMainMirror());
            Assert.assertEquals("bad response.hostResults[1].status", hostOk, response.getHostResults().get(1).getStatus());
            Assert.assertEquals("bad response.hostResults[1].host", new URI("http://wandex.ru"), response.getHostResults().get(1).getHost());
            Assert.assertEquals("bad response.hostResults[1].newMainMirror", new URI("http://wandex.ru"), response.getHostResults().get(1).getNewMainMirror());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }
}
