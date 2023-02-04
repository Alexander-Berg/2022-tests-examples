package ru.yandex.wmtools.common.sita;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author aherman
 */
public class SitaRespons_WMCON5275_Test {
    @Test
    public void testNewSelo() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("WMCON_5275_new-selo.json");
        try {
            SitaUrlFetchResponse sitaUrlFetchResponse = SitaUrlFetchJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1), false);

            sitaUrlFetchResponse.getDocument();
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test
    public void testEcoriver() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("WMCON_5275_ecoriver66.json");
        try {
            SitaUrlFetchResponse sitaUrlFetchResponse = SitaUrlFetchJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1), false);

            sitaUrlFetchResponse.getDocument();
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test
    public void testAshaOhota() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("WMCON_5275_asha-ohota.hostei.json");
        try {
            SitaUrlFetchResponse sitaUrlFetchResponse = SitaUrlFetchJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1), false);

            sitaUrlFetchResponse.getDocument();
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }
}
