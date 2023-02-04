package ru.yandex.webmaster3.worker.addurl;

import com.datastax.driver.core.utils.UUIDs;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.addurl.RecrawlState;
import ru.yandex.webmaster3.core.addurl.UrlForRecrawl;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;

import java.util.UUID;

/**
 * @author aherman
 */
public class UpdateUrlStatePeriodicTaskTest {
    @Test
    public void changeStateStale() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusDays(3).minusMinutes(1),
                now.minusDays(3).minusMinutes(1),
                RecrawlState.IN_PROGRESS
        );

        Assert.assertEquals(RecrawlState.STALE, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, null, resend, stale));

        UrlForRecrawl expected = new UrlForRecrawl(
                hostId, urlId, "/1",
                waitingUrl.getAddDate(),
                now,
                RecrawlState.STALE);
        Assert.assertEquals(expected, waitingUrl.changeState(RecrawlState.STALE, now));
    }

    @Test
    public void changeStateResend() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).minusMinutes(1),
                now.minusHours(1).minusMinutes(1),
                RecrawlState.NEW
        );

        Assert.assertEquals(RecrawlState.NEW, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, null, resend, stale));
    }

    @Test
    public void changeStateIgnore1() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).minusMinutes(1),
                now.minusHours(1).minusMinutes(1),
                RecrawlState.IN_PROGRESS);

        Assert.assertEquals(null, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, null, resend, stale));
    }

    @Test
    public void changeStateIgnore2() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).plusMinutes(1),
                now.minusHours(1).plusMinutes(1),
                RecrawlState.NEW
        );

        Assert.assertEquals(null, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, null, resend, stale));
    }

    @Test
    public void changeStateIgnore3() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusMinutes(10),
                now.minusMinutes(10),
                RecrawlState.NEW
        );

        RecrawlResult result = new RecrawlResult(hostId, "/1", now.minusMinutes(20), null, true);

        Assert.assertEquals(null, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, result, resend, stale));
    }

    @Test
    public void changeStateProcessed1() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).minusMinutes(1),
                now.minusHours(1).minusMinutes(1),
                RecrawlState.IN_PROGRESS
        );

        RecrawlResult result = new RecrawlResult(hostId, "/1", now.minusMinutes(10), null, true);
        Assert.assertEquals(RecrawlState.PROCESSED, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, result, resend, stale));

        UrlForRecrawl expected = new UrlForRecrawl(
                hostId, urlId, "/1",
                waitingUrl.getAddDate(),
                now.minusMinutes(10),
                RecrawlState.PROCESSED);
        Assert.assertEquals(expected, waitingUrl.changeState(RecrawlState.PROCESSED, result.getProcessingTime()));
    }

    @Test
    public void changeStateProcessed2() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).minusMinutes(1),
                now.minusHours(1).minusMinutes(1),
                RecrawlState.NEW
        );

        RecrawlResult result = new RecrawlResult(hostId, "/1", now.minusMinutes(10), null, true);
        Assert.assertEquals(RecrawlState.PROCESSED, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, result, resend, stale));

        UrlForRecrawl expected = new UrlForRecrawl(
                hostId, urlId, "/1",
                waitingUrl.getAddDate(),
                result.getProcessingTime(),
                RecrawlState.PROCESSED);
        Assert.assertEquals(expected, waitingUrl.changeState(RecrawlState.PROCESSED, result.getProcessingTime()));
    }

    @Test
    public void changeStateFailed() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        UUID urlId = UUIDs.timeBased();
        DateTime now = DateTime.now();
        DateTime resend = now.minusHours(1);
        DateTime stale = now.minusDays(3);

        UrlForRecrawl waitingUrl = new UrlForRecrawl(
                hostId, urlId, "/1",
                now.minusHours(1).minusMinutes(1),
                now.minusHours(1).minusMinutes(1),
                RecrawlState.NEW);

        RecrawlResult result = new RecrawlResult(hostId, "/1", now.minusMinutes(10), null, false);
        Assert.assertEquals(RecrawlState.STALE, UpdateUrlStatePeriodicTask.computeNewState(waitingUrl, result, resend, stale));
    }

}
