package ru.yandex.webmaster3.worker.addurl;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.storage.util.yt.YtPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ifilippov5 on 07.11.17.
 */
public class UploadAddUrlLogsPeriodicTaskTest {

    @Test
    public void findDatesToProcessTest() {
        List<TestData> testCases = Arrays.asList(loadTestCase1(), loadTestCase2(), loadTestCase3(), loadTestCase4(), loadTestCase5());

        testCases.forEach(data -> Assert.assertEquals(data.expectedDates,
                UploadAddUrlLogsPeriodicTask.findDatesToProcess(data.ytPaths, data.now)));
    }

    private TestData loadTestCase1() {
        List<DateTime> expectedDates = new ArrayList<>();

        List<YtPath> paths = new ArrayList<>();
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171101"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171102"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171103"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171104"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171105"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171106"));

        return new TestData(
            new DateTime("2017-11-07T19:31:01"),
            expectedDates,
            paths
        );
    }

    private TestData loadTestCase2() {
        List<DateTime> expectedDates = new ArrayList<>();
        expectedDates.add(new DateTime("2017-11-06T00:00:00"));

        List<YtPath> paths = new ArrayList<>();
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171101"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171102"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171103"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171104"));
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171105"));

        return new TestData(
                new DateTime("2017-11-07T19:31:01"),
                expectedDates,
                paths
        );
    }

    private TestData loadTestCase3() {
        List<DateTime> expectedDates = new ArrayList<>();
        expectedDates.add(new DateTime("2017-11-06T00:00:00"));
        expectedDates.add(new DateTime("2017-11-05T00:00:00"));
        expectedDates.add(new DateTime("2017-11-04T00:00:00"));

        List<YtPath> paths = new ArrayList<>();
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171101"));

        return new TestData(
                new DateTime("2017-11-07T19:31:01"),
                expectedDates,
                paths
        );
    }

    private TestData loadTestCase4() {
        List<DateTime> expectedDates = new ArrayList<>();

        List<YtPath> paths = new ArrayList<>();
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171107"));

        return new TestData(
                new DateTime("2017-11-07T19:31:01"),
                expectedDates,
                paths
        );
    }

    private TestData loadTestCase5() {
        List<DateTime> expectedDates = new ArrayList<>();
        expectedDates.add(new DateTime("2017-11-06T00:00:00"));
        expectedDates.add(new DateTime("2017-11-05T00:00:00"));

        List<YtPath> paths = new ArrayList<>();
        paths.add(YtPath.fromString("arnold://home/webmaster/prod/export/antispam/addurl_events_log.20171104"));

        return new TestData(
                new DateTime("2017-11-07T19:31:01"),
                expectedDates,
                paths
        );
    }

    private static class TestData {
        private DateTime now;
        private List<DateTime> expectedDates;
        private List<YtPath> ytPaths;

        TestData(DateTime now, List<DateTime> expectedDates, List<YtPath> ytPaths) {
            this.now = now;
            this.expectedDates = expectedDates;
            this.ytPaths = ytPaths;
        }
    }
}
