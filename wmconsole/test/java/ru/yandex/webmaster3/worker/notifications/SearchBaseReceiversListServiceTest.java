package ru.yandex.webmaster3.worker.notifications;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.searchbase.SearchBaseDates;
import ru.yandex.webmaster3.core.searchbase.SearchBaseUpdateInfo;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.notifications.NotificationRecListId;
import ru.yandex.webmaster3.storage.notifications.dao.PreparedGlobalMessageInfo;
import ru.yandex.webmaster3.storage.notifications.dao.SearchBaseNotificationListCHDao;
import ru.yandex.webmaster3.storage.searchurl.history.dao.SiteStructureCHDao;
import ru.yandex.webmaster3.storage.searchurl.history.data.SearchUrlStat;
import ru.yandex.webmaster3.storage.searchurl.offline.data.SearchBaseImportInfo;
import ru.yandex.webmaster3.storage.searchurl.offline.data.SearchBaseImportStageEnum;
import ru.yandex.webmaster3.storage.searchurl.offline.data.SearchBaseImportTaskType;
import ru.yandex.webmaster3.storage.searchurl.samples.dao.SearchUrlEmailSamplesCHDao;
import ru.yandex.webmaster3.storage.searchurl.samples.data.RawSearchUrlEventSample;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventType;
import ru.yandex.webmaster3.storage.user.UserPersonalInfo;
import ru.yandex.webmaster3.storage.user.dao.UserNotificationChannelsYDao;
import ru.yandex.webmaster3.storage.user.dao.UserNotificationEmailYDao;
import ru.yandex.webmaster3.storage.user.dao.UserNotificationHostSettingsYDao;
import ru.yandex.webmaster3.storage.user.notification.HostNotificationSetting;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;
import ru.yandex.webmaster3.storage.user.service.UserPersonalInfoService;
import ru.yandex.webmaster3.storage.util.clickhouse2.ClickhouseHost;
import ru.yandex.webmaster3.storage.util.yt.YtPath;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static ru.yandex.webmaster3.storage.notifications.NotificationChannel.EMAIL;
import static ru.yandex.webmaster3.storage.notifications.NotificationChannel.SERVICE;
import static ru.yandex.webmaster3.storage.user.notification.HostNotificationMode.ENABLED;
import static ru.yandex.webmaster3.storage.user.notification.NotificationType.SEARCH_BASE_UPDATE;

/**
 * Created by Oleg Bazdyrev on 03/05/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchBaseReceiversListServiceTest {

    @SuppressWarnings("unchecked")
    private static final Pair<Instant, Instant>[] SEARCH_BASE_DATES = new Pair[]{
            Pair.of(Instant.parse("2017-05-02T02:15:00"), Instant.parse("2017-04-28T16:16:46")),
            Pair.of(Instant.parse("2017-04-30T01:20:00"), Instant.parse("2017-04-26T08:39:40")),
            Pair.of(Instant.parse("2017-04-28T01:40:00"), Instant.parse("2017-04-23T21:32:06"))
    };

    private static final long USER_ID = 222855366L;

    @InjectMocks
    private SearchBaseReceiversListService service;
    @Mock
    private UserPersonalInfoService userPersonalInfoService;
    @Mock
    private UserNotificationChannelsYDao userNotificationChannelsYDao;
    @Mock
    private UserNotificationEmailYDao userNotificationEmailYDao;
    @Mock
    private UserNotificationHostSettingsYDao userNotificationHostSettingsYDao;
    @Mock
    private SearchUrlEmailSamplesCHDao searchUrlEmailSamplesCHDao;
    @Mock
    private SearchBaseNotificationListCHDao searchBaseNotificationListCHDao;
    @Mock
    private SiteStructureCHDao siteStructureCHDao;

    /**
     * Пока это еще бесполезный тест, надо будет дорабатывать
     */
    @Ignore
    @Test
    public void test_WMC_3973() throws Exception {
        SearchBaseImportInfo importInfo = new SearchBaseImportInfo(SearchBaseImportTaskType.SEARCH_URL_EMAIL_SAMPLES,
                Instant.parse("2017-04-28T16:16:46"), "webmaster3_searchurls", SearchBaseImportStageEnum.READY,
                "search_url_email_samples_20170430", 1, 1, "IVA", Collections.emptyList(), false,
                YtPath.fromString("arnold://home/webmaster/prod/sitetree/search/history.wmc.new_gone.acceptance"),
                null, null);
        // channels
        when(userNotificationChannelsYDao.getChannelsInfo(any())).thenReturn(
                ImmutableMap.of(USER_ID, new EnumMap<>(ImmutableMap.of(
                        NotificationType.MAIN_MIRROR_UPDATE, Collections.emptySet(),
                        SEARCH_BASE_UPDATE, Collections.emptySet())))
        );
        // host settings
        when(userNotificationHostSettingsYDao.listSettingsForUsers(any())).thenReturn(
                ImmutableMap.of(USER_ID, Lists.newArrayList(
                        new HostNotificationSetting(IdUtils.stringToHostId("http:alfabetseo.net:80"), SEARCH_BASE_UPDATE, SERVICE, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:alfabetseo.net:80"), SEARCH_BASE_UPDATE, EMAIL, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:laboratory-prod.ru:80"), SEARCH_BASE_UPDATE, SERVICE, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:laboratory-prod.ru:80"), SEARCH_BASE_UPDATE, EMAIL, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:luckyprojects.ru:80"), SEARCH_BASE_UPDATE, SERVICE, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:luckyprojects.ru:80"), SEARCH_BASE_UPDATE, EMAIL, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:peregorodka-office.com:80"), SEARCH_BASE_UPDATE, SERVICE, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:peregorodka-office.com:80"), SEARCH_BASE_UPDATE, EMAIL, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:www.m360.ru:80"), SEARCH_BASE_UPDATE, SERVICE, ENABLED),
                        new HostNotificationSetting(IdUtils.stringToHostId("http:www.m360.ru:80"), SEARCH_BASE_UPDATE, EMAIL, ENABLED)
                ))
        );
        // user emails (why guava ImmutableMap does not support null values???)
        Map<Long, String> emails = new HashMap<>();
        emails.put(USER_ID, null);
        when(userNotificationEmailYDao.getUserEmails(any())).thenReturn(emails);
        // personal infos
        when(userPersonalInfoService.getCachedUsersPersonalInfos(any())).thenReturn(
                new UserPersonalInfoService.CachedResponse(ImmutableMap.of(
                        222855366L, new UserPersonalInfo(222855366L, "alfabetseo.net", null, null)), Collections.emptySet(), Collections.emptySet())
        );
        // search url stats
        WebmasterHostId host1 = IdUtils.stringToHostId("http:alfabetseo.net:80");
        WebmasterHostId host2 = IdUtils.stringToHostId("http:peregorodka-office.com:80");
        DateTime dt1 = DateTime.parse("2017-04-28T16:15:00");
        DateTime dt2 = DateTime.parse("2017-04-28T10:10:00");
        when(siteStructureCHDao.getSearchUrlStats(any(), any())).thenReturn(
                ImmutableMap.of(host1, new SearchUrlStat(host1, dt1.toInstant(), 200, 150, 30),
                        host2, new SearchUrlStat(host2, dt2.toInstant(), 999, 666, 333))
        );
        // url samples
        when(searchUrlEmailSamplesCHDao.getSamples(any(), any(Collection.class))).thenReturn(
                ImmutableMap.of(
                        host1, Lists.newArrayList(
                                new RawSearchUrlEventSample("/page1", "Some new test page from alfabetseo", dt1, dt1, SearchUrlEventType.NEW, null),
                                new RawSearchUrlEventSample("/page2", "Some old test page from alfabetseo", dt1, dt1, SearchUrlEventType.GONE, null)),
                        host2, Lists.newArrayList(
                                new RawSearchUrlEventSample("/alpha", "Some new test page from peregorodka", dt2, dt2, SearchUrlEventType.NEW, null),
                                new RawSearchUrlEventSample("/beta", "Some old test page from peregorodka", dt2, dt2, SearchUrlEventType.GONE, null))
                )
        );

        NotificationRecListId recListId = new NotificationRecListId(UUIDs.timeBased(), UUIDs.timeBased());
        // должно получиться два сообщения
        List<PreparedGlobalMessageInfo> messages = new ArrayList<>();
        doAnswer(invocation -> {
            // самопальный кэптор для аргументов, т.к.
            // ru.yandex.webmaster3.core.util.concurrent.graph.GraphExecution.GraphThread.run() любит чистить буфер
            messages.addAll(invocation.getArgument(1));
            return null;
        }).when(searchBaseNotificationListCHDao).addRecords(eq(recListId), anyList(), any(ClickhouseHost.class));

        service.sendSearchBaseUpdateMessage(recListId, importInfo, DateTime.parse("2017-05-02T02:15:00"), true);

        Assert.assertEquals(2, messages.size());
    }

    private SearchBaseDates getSearchBaseDates() {
        return SearchBaseDates.fromBaseInfos(
                Arrays.stream(SEARCH_BASE_DATES)
                        .map(dates -> new SearchBaseUpdateInfo(dates.getLeft(), dates.getRight()))
                        .collect(Collectors.toList())
        );
    }

}
