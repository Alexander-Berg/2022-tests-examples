package ru.yandex.qe.dispenser.ws.sanity;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

public final class AdminkaSanityTest extends AcceptanceTestBase {
    @Disabled
    @Test
    public void adminkaMustBeHtmlPage() {
        final Response response = createLocalClient()
                .path("/admin")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + AMOSOV_F)
                .get();
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatus());
        Assertions.assertEquals("text/html;charset=utf-8", response.getMediaType().toString());
        Assertions.assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    @Disabled
    @Test
    public void adminkaEditMustBeHtmlPage() {
        final Response response = createLocalClient()
                .path("/admin/edit")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + AMOSOV_F)
                .get();
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatus());
        Assertions.assertEquals("text/html;charset=utf-8", response.getMediaType().toString());
        Assertions.assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    @Disabled
    @Test
    public void quotaTableMustBeHtmlPage() {
        final Response response = createLocalClient()
                .path("/table")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + AMOSOV_F)
                .get();
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatus());
        Assertions.assertEquals("text/html;charset=utf-8", response.getMediaType().toString());
        Assertions.assertNull(response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }
}
