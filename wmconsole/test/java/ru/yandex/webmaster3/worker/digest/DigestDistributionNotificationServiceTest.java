package ru.yandex.webmaster3.worker.digest;

import NWebmaster.proto.digest.Digest;
import com.google.common.collect.Range;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.digest.graphics.draw.ChartSettings;
import ru.yandex.webmaster3.core.digest.graphics.draw.ComputeBordersUtil;
import ru.yandex.webmaster3.core.digest.graphics.draw.data.PointLong;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.digest.DigestMessage;
import ru.yandex.webmaster3.storage.turbo.service.autoparser.TurboAutoparserInfoService;
import ru.yandex.webmaster3.storage.user.dao.PersonalInfoCacheYDao;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;
import ru.yandex.webmaster3.storage.util.clickhouse2.ClickhouseException;
import ru.yandex.webmaster3.worker.digest.blog.YaBlogsApiService;
import ru.yandex.webmaster3.worker.digest.graphics.draw.BuildClicksGraphicsUtil;
import ru.yandex.webmaster3.worker.digest.html.DigestData;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.mockito.Mockito.mock;

/**
 * Created by ifilippov5 on 09.10.17.
 */
@Ignore
public class DigestDistributionNotificationServiceTest {

    private ChartSettings chartSettings = buildChartSettings();
    private DigestNotificationService digestNotificationService = new DigestNotificationService();
    private YaBlogsApiService yaBlogsApiService = new YaBlogsApiServiceMock();

    private ChartSettings buildChartSettings() {
        ChartSettings chartSettings = new ChartSettings();
        chartSettings.setChartWidth(480);
        chartSettings.setChartHeight(180);
        chartSettings.setBackgroundColor("#fff");
        chartSettings.setGridColor("#e1e1e1");
        chartSettings.setUndertextColor("rgba(255,255,255,0.5)");
        chartSettings.setPastSeriesColor("#999999");
        chartSettings.setCurrentSeriesColor("#0044BB");
        chartSettings.setLabelFontSize("10px");
        chartSettings.setLabelFontFamily("Arial");
        chartSettings.setLabelHeight(12);
        chartSettings.setLabelRx(3);
        chartSettings.setLabelRy(15);
        chartSettings.setStrokeWidth(2);
        return chartSettings;
    }

    private static final List<TestData> data = new ArrayList<>();
    static {
        data.add(TestData.create("http:lexic-on.ru:80", "2017-11-03", LanguageEnum.RU, "lexic-on.ru.html", "lexic-on.ru", "lexic-on.ru_svg.html"));
        data.add(TestData.create("https:vk.com:443", "2017-10-01", LanguageEnum.RU, "vk.com.html", "vk.com.ru", "vk.com_svg.html"));
        data.add(TestData.create("https:lenta.ru:443", "2017-10-01", LanguageEnum.RU, "lenta.ru.html", "lenta.ru", "lenta.ru_svg.html"));
        data.add(TestData.create("http:00help.ru:80", "2017-10-01", LanguageEnum.RU, "00help.ru.html", "00help.ru", null));
        data.add(TestData.create("http:afy.ru:80", "2017-10-01", LanguageEnum.RU, "afy.ru.html", "afy.ru", null));
        data.add(TestData.create("https:yellowbag.kz:443", "2017-10-01", LanguageEnum.RU, "yellowbag.kz.html", "yellowbag.kz", null));
        data.add(TestData.create("https:lifejournal-on.ru:80", "2019-05-20", LanguageEnum.RU, "lifejournal.ru.html", "lifejournal.ru", null));
        data.add(TestData.create("http:lexic-on.ru:80", "2017-11-03", LanguageEnum.EN, "lexic-on.ru_en.html", "lexic-on.ru", "lexic-on.ru_svg.html"));
    }

    @Test
    public void test() throws ClickhouseException, IOException {
        PersonalInfoCacheYDao personalInfoCacheYDao = new PersonalInfoCacheYDaoWrapper();
        digestNotificationService.setChartSettings(chartSettings);
        digestNotificationService.setPersonalInfoCacheYDao(personalInfoCacheYDao);
        digestNotificationService.setServiceBaseUrl("https://webmaster.test.yandex.ru");
        digestNotificationService.setYaBlogsApiService(yaBlogsApiService);
        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        digestNotificationService.setTurboAutoparserInfoService(turboAutoparserInfoService);
        for (TestData request : data) {
            System.out.println("Testing " + request.hostId + " " +request.lang);
            InputStream is = getClass().getClassLoader().getResourceAsStream(request.pathToBinary);
            DigestMessage message = new DigestMessage(
                    1,
                    request.hostId,
                    "",
                    request.lang,
                    NotificationType.DIGEST,
                    Digest.DigestWeeklyReportMessage.parseFrom(ByteStreams.toByteArray(is)),
                    request.date
            );

            DigestData renderData = digestNotificationService.createData(message);
            String page = digestNotificationService.buildHtml(message, renderData, EnumSet.of(DigestAttachType.CLICKS_DYNAMICS));
            is = getClass().getClassLoader().getResourceAsStream(request.htmlToCompare);
            String referencePage = IOUtils.toString(is);
            Assert.assertEquals(referencePage, page);

            if (request.pathToSvg != null) {
                Digest.DigestWeeklyReportMessage digest = message.getDigest();
                Digest.AllQueriesIndicators allQueries = digest.getAllQueries();

                byte[] clickDynamicsImage = BuildClicksGraphicsUtil.process(allQueries.getOldClicksByPeriodList(), allQueries.getNewClicksByPeriodList(), chartSettings);
                is = getClass().getClassLoader().getResourceAsStream(request.pathToSvg);
                String svg = IOUtils.toString(is);
                //TODO: видимо, png кодируется по-разному в разных окружениях. Предлагается проверять только png
                //Assert.assertEquals(svg, new String(clickDynamicsImage));
            }
        }
    }

    @Test
    public void testRoundCoordinate1() {
        List<PointLong> points = new ArrayList<>();
        points.add(new PointLong(0, 0));
        points.add(new PointLong(1, 0));
        points.add(new PointLong(2, 0));
        points.add(new PointLong(5, 0));
        ComputeBordersUtil.FrameBoundaries frame =
                ComputeBordersUtil.computeBorders(points, 3);

        List<Long> expectedAxisYDivisions = new ArrayList<Long>(Arrays.asList(0L, 1L, 2L, 3L));
        ComputeBordersUtil.FrameBoundaries expectedFrame = new ComputeBordersUtil.FrameBoundaries(0, 5, 0, 3, expectedAxisYDivisions);
        Assert.assertEquals(expectedFrame.getLeft(), frame.getLeft());
        Assert.assertEquals(expectedFrame.getRight(), frame.getRight());
        Assert.assertEquals(expectedFrame.getBottom(), frame.getBottom());
        Assert.assertEquals(expectedFrame.getTop(), frame.getTop());
        Assert.assertEquals(expectedFrame.getAxisYDivisions(), frame.getAxisYDivisions());
    }

    @Test
    public void testRoundCoordinate2() {
        List<PointLong> points = new ArrayList<>();
        points.add(new PointLong(0, 1));
        points.add(new PointLong(1, 3));
        points.add(new PointLong(2, 3));
        points.add(new PointLong(5, 9));
        ComputeBordersUtil.FrameBoundaries frame =
                ComputeBordersUtil.computeBorders(points, 3);

        List<Long> expectedAxisYDivisions = new ArrayList<Long>(Arrays.asList(0L, 4L, 8L, 12L));
        ComputeBordersUtil.FrameBoundaries expectedFrame = new ComputeBordersUtil.FrameBoundaries(0, 5, 0, 12, expectedAxisYDivisions);
        Assert.assertEquals(expectedFrame.getLeft(), frame.getLeft());
        Assert.assertEquals(expectedFrame.getRight(), frame.getRight());
        Assert.assertEquals(expectedFrame.getBottom(), frame.getBottom());
        Assert.assertEquals(expectedFrame.getTop(), frame.getTop());
        Assert.assertEquals(expectedFrame.getAxisYDivisions(), frame.getAxisYDivisions());
    }

    @Test
    public void testRoundCoordinate3() {
        List<PointLong> points = new ArrayList<>();
        points.add(new PointLong(0, 28));
        points.add(new PointLong(1, 113));
        points.add(new PointLong(2, 267));
        points.add(new PointLong(5, 936));
        ComputeBordersUtil.FrameBoundaries frame =
                ComputeBordersUtil.computeBorders(points, 3);

        List<Long> expectedAxisYDivisions = new ArrayList<Long>(Arrays.asList(25L, 350L, 675L, 1000L));
        ComputeBordersUtil.FrameBoundaries expectedFrame = new ComputeBordersUtil.FrameBoundaries(0, 5, 25, 1000, expectedAxisYDivisions);
        Assert.assertEquals(expectedFrame.getLeft(), frame.getLeft());
        Assert.assertEquals(expectedFrame.getRight(), frame.getRight());
        Assert.assertEquals(expectedFrame.getBottom(), frame.getBottom());
        Assert.assertEquals(expectedFrame.getTop(), frame.getTop());
        Assert.assertEquals(expectedFrame.getAxisYDivisions(), frame.getAxisYDivisions());
    }

    public static class PersonalInfoCacheYDaoWrapper extends PersonalInfoCacheYDao {
        public List<String> getLogins(Collection<Long> uids) {
            return Collections.emptyList();
        }
    }

    public static class TestData {
        private final WebmasterHostId hostId;
        private final LocalDate date;
        private final LanguageEnum lang;
        private final String htmlToCompare;
        private final String pathToBinary;
        private final String pathToSvg;

        public TestData(WebmasterHostId hostId, LocalDate date, LanguageEnum lang, String fileToCompare, String pathToBinary, String pathToSvg) {
            this.hostId = hostId;
            this.date = date;
            this.lang = lang;
            this.htmlToCompare = fileToCompare;
            this.pathToBinary = pathToBinary;
            this.pathToSvg = pathToSvg;
        }

        public static TestData create(String hostId, String date, LanguageEnum lang, String fileToCompare, String pathToBinary, String pathToSvg) {
             return new TestData(IdUtils.stringToHostId(hostId), new LocalDate(date), lang, fileToCompare, pathToBinary, pathToSvg);
        }
    }

    private class YaBlogsApiServiceMock extends YaBlogsApiService {

        public List<DigestData.BlogPost> getBlogPosts(Range<LocalDate> datesInterval) {
            if (datesInterval.upperEndpoint().equals(new LocalDate("2017-11-03"))) {
                return Arrays.asList(
                        new DigestData.BlogPost(
                                "esche-bolshe-mesta-dlya-raboty-v-vebmastere",
                                "Еще больше места для работы в Вебмастере!",
                                DateTime.parse("2017-11-03T10:20:09.014Z").toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE)
                        )
                );
            } else {
                return Collections.emptyList();
            }
        }
    }
}
