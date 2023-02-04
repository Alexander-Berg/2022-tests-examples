package ru.yandex.webmaster3.storage.checklist.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.curator.shaded.com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.webmaster3.core.checklist.data.MobileAuditResolution;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemContent;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemState;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemStorageType;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.host.service.HostOwnerService;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.WwwUtil;
import ru.yandex.webmaster3.storage.checklist.dao.ChecklistPageSamplesService;
import ru.yandex.webmaster3.storage.checklist.dao.RealTimeSiteProblemsYDao;
import ru.yandex.webmaster3.storage.checklist.dao.ValidateSiteProblemService;
import ru.yandex.webmaster3.storage.checklist.data.ProblemSignal;
import ru.yandex.webmaster3.storage.checklist.data.ProblemStateInfo;
import ru.yandex.webmaster3.storage.checklist.data.RealTimeSiteProblemInfo;
import ru.yandex.webmaster3.storage.checklist.data.SiteProblemInfo;
import ru.yandex.webmaster3.storage.host.service.MirrorService2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemState.ABSENT;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemState.PRESENT;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemStorageType.REAL_TIME;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemStorageType.REAL_TIME_DOMAIN;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.ERRORS_IN_SITEMAPS;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.FAVICON_ERROR;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.SSL_CERTIFICATE_ERROR;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.THREATS;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.TURBO_ERROR;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.TURBO_HOST_BAN;
import static ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum.TURBO_WARNING;
import static ru.yandex.webmaster3.core.util.IdUtils.stringToHostId;
import static ru.yandex.webmaster3.core.util.IdUtils.toDomainHostId;

/**
 * Created by Oleg Bazdyrev on 2019-11-07.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteProblemsServiceTest {

    public static final WebmasterHostId HTTP_HOST_ID = stringToHostId("http:lenta.ru:80");
    public static final WebmasterHostId HTTPS_HOST_ID = stringToHostId("https:www.lenta.ru:443");
    public static final WebmasterHostId HTTP_HOST_VERIFIED = stringToHostId("http:m.host.com:80");
    public static final WebmasterHostId HTTPS_HOST_ID_NOT_VERIFIED = stringToHostId("https:host.com:443");
    public static final WebmasterHostId UNVERIFIED_HOST_ID = IdUtils.stringToHostId("http:unverified.com:80");
    public static final ProblemSignal TURBO_PROBLEM = new ProblemSignal(
            new SiteProblemContent.TurboInsufficientClicksShare(30), DateTime.now());
    public static final ProblemSignal NON_TURBO_PROBLEM = new ProblemSignal(new SiteProblemContent.NotMobileFriendly(MobileAuditResolution.FEW_UNIQUE_URLS, 1), DateTime.now());

    @Mock
    ChecklistPageSamplesService checklistPageSamplesService;
    @Mock
    MirrorService2 mirrorService2;
    @Mock
    RealTimeSiteProblemsYDao realTimeSiteProblemsYDao;
    @Mock
    SiteProblemsNotificationService siteProblemsNotificationService;
    @Mock
    SiteProblemStorageService siteProblemStorageService;
    @Mock
    ValidateSiteProblemService validateSiteProblemService;
    @InjectMocks
    SiteProblemsService service;

    SiteProblemStorageService siteProblemStorageServiceReal = new SiteProblemStorageService(new HostOwnerService(null) {
        @Override
        public @NotNull WebmasterHostId getHostOwner(WebmasterHostId hostId) {
            return IdUtils.urlToHostId(WwwUtil.cutWWWAndM(hostId)); // работает только на примерах выше!
        }
    });

    @Before
    public void init() {
        Mockito.when(mirrorService2.isMainMirror(Mockito.eq(HTTP_HOST_ID))).thenReturn(false);
        Mockito.when(mirrorService2.isMainMirror(Mockito.eq(HTTPS_HOST_ID))).thenReturn(true);
        Mockito.when(validateSiteProblemService.hostInWebmaster(Mockito.eq(HTTP_HOST_ID), any(SiteProblemTypeEnum.class))).thenReturn(true);
        Mockito.when(validateSiteProblemService.hostInWebmaster(Mockito.eq(HTTPS_HOST_ID), any(SiteProblemTypeEnum.class))).thenReturn(true);
        Mockito.when(validateSiteProblemService.hostInWebmaster(Mockito.eq(HTTPS_HOST_ID_NOT_VERIFIED), any(SiteProblemTypeEnum.class))).thenReturn(false);
        Mockito.when(validateSiteProblemService.hostInWebmaster(Mockito.eq(UNVERIFIED_HOST_ID), any(SiteProblemTypeEnum.class))).thenReturn(false);


        Mockito.when(siteProblemStorageService.toProblemHostId(Mockito.any(), Mockito.any(SiteProblemStorageType.class))).thenAnswer((Answer<WebmasterHostId>) invocation ->
                siteProblemStorageServiceReal.toProblemHostId(
                        invocation.getArgument(0, WebmasterHostId.class),
                        invocation.getArgument(1, SiteProblemStorageType.class))
        );
    }

    @Test
    public void fixHostIdTest() throws Exception {
        Assert.assertEquals(HTTP_HOST_ID, toDomainHostId(HTTP_HOST_ID));
        Assert.assertEquals(HTTP_HOST_ID, toDomainHostId(HTTPS_HOST_ID));
        Assert.assertEquals(HTTPS_HOST_ID, service.toProblemHostId(HTTPS_HOST_ID, REAL_TIME));
        Assert.assertEquals(HTTP_HOST_ID, service.toProblemHostId(HTTPS_HOST_ID, REAL_TIME_DOMAIN));
        Assert.assertEquals(HTTPS_HOST_ID, service.toProblemHostId(HTTPS_HOST_ID, FAVICON_ERROR));
    }

    @Test
    public void getRealTimeProblemInfoTest() throws Exception {
        //Mockito.when(realTimeSiteProblemsCDao.getProblemInfo(Mockito.eq(HTTPS_HOST_ID), Mockito.eq(TURBO_FEED_BAN)))
        //        .thenReturn(makeProblemInfo(HTTPS_HOST_ID, TURBO_FEED_BAN, PRESENT));
        Mockito.when(realTimeSiteProblemsYDao.getProblemInfo(Mockito.eq(HTTPS_HOST_ID), Mockito.eq(FAVICON_ERROR)))
                .thenReturn(null);
        Mockito.when(realTimeSiteProblemsYDao.getProblemInfo(Mockito.eq(HTTP_HOST_ID), Mockito.eq(TURBO_HOST_BAN)))
                .thenReturn(makeRealProblemInfo(HTTP_HOST_ID, TURBO_HOST_BAN, ABSENT));
        Mockito.when(realTimeSiteProblemsYDao.getProblemInfo(Mockito.eq(HTTP_HOST_ID), Mockito.eq(FAVICON_ERROR)))
                .thenReturn(makeRealProblemInfo(HTTP_HOST_ID, FAVICON_ERROR, PRESENT));

        Assert.assertEquals(ABSENT, service.getRealTimeProblemInfo(HTTPS_HOST_ID, TURBO_HOST_BAN).getState());
        Assert.assertEquals(ABSENT, service.getRealTimeProblemInfo(HTTP_HOST_ID, TURBO_HOST_BAN).getState());
        Assert.assertEquals(null, service.getRealTimeProblemInfo(HTTPS_HOST_ID, FAVICON_ERROR));
        Assert.assertEquals(PRESENT, service.getRealTimeProblemInfo(HTTP_HOST_ID, FAVICON_ERROR).getState());
    }

    @Test
    @Ignore("Not applicable for providers")
    public void listProblemsForHostTest1() throws Exception {
        var problemsList = Arrays.asList(
                makeRealProblemInfo(HTTPS_HOST_ID, FAVICON_ERROR, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_HOST_BAN, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_ERROR, PRESENT)
        );
        Mockito.when(realTimeSiteProblemsYDao.listSitesProblems(argThat(t -> t.containsAll(List.of(HTTP_HOST_ID, toDomainHostId(HTTP_HOST_ID))))))
                .thenReturn(Map.of(HTTP_HOST_ID, problemsList));

        var httpHostProblems = service.listProblemsForHost(HTTP_HOST_ID, UUIDs.timeBased()).stream()
                .filter(p -> p.getState() == PRESENT)
                .map(ProblemStateInfo::getProblemType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Sets.newHashSet(THREATS, ERRORS_IN_SITEMAPS, FAVICON_ERROR, TURBO_HOST_BAN, TURBO_ERROR),
                httpHostProblems);
    }

    @Test
    @Ignore("Not applicable for providers")
    public void listProblemsForHostTest2() throws Exception {
        var problemsList1 = Arrays.asList(
                makeRealProblemInfo(HTTPS_HOST_ID, FAVICON_ERROR, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_HOST_BAN, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_ERROR, PRESENT)
        );
        var problemsList2 = Arrays.asList(
                makeRealProblemInfo(HTTPS_HOST_ID, SSL_CERTIFICATE_ERROR, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_ERROR, PRESENT),
                makeRealProblemInfo(HTTPS_HOST_ID, TURBO_WARNING, PRESENT)
        );
        Mockito.when(realTimeSiteProblemsYDao.listSitesProblems(argThat(t -> t.containsAll(List.of(HTTPS_HOST_ID, toDomainHostId(HTTPS_HOST_ID))))))
                .thenReturn(Map.of(
                        HTTPS_HOST_ID, problemsList2,
                        toDomainHostId(HTTPS_HOST_ID), problemsList1
                ));

        var httpsHostProblems = service.listProblemsForHost(HTTPS_HOST_ID, UUIDs.timeBased()).stream()
                .filter(p -> p.getState() == PRESENT)
                .map(ProblemStateInfo::getProblemType)
                .collect(Collectors.toSet());
        Assert.assertEquals(Sets.newHashSet(THREATS, SSL_CERTIFICATE_ERROR, TURBO_HOST_BAN, TURBO_ERROR),
                httpsHostProblems);
    }

    @Test
    public void testUpdateRealTimeSiteProblem_turboNoCurrentHttps() throws Exception {
        service.updateRealTimeProblem(HTTPS_HOST_ID, TURBO_PROBLEM);

        ArgumentCaptor<RealTimeSiteProblemInfo> problemInfoCaptor = ArgumentCaptor.forClass(RealTimeSiteProblemInfo.class);
        Mockito.verify(realTimeSiteProblemsYDao).addProblem(problemInfoCaptor.capture());
        Mockito.verify(siteProblemsNotificationService).sendNotification(any(), any(), any(), any());

        Assert.assertEquals(HTTP_HOST_ID, problemInfoCaptor.getValue().getHostId());
    }

    @Test
    public void testUpdateRealTimeSiteProblem_nonTurboNoCurrentHttps() throws Exception {
        service.updateRealTimeProblem(HTTPS_HOST_ID, NON_TURBO_PROBLEM);

        ArgumentCaptor<RealTimeSiteProblemInfo> problemInfoCaptor = ArgumentCaptor.forClass(RealTimeSiteProblemInfo.class);
        Mockito.verify(realTimeSiteProblemsYDao).addProblem(problemInfoCaptor.capture());
        Mockito.verify(siteProblemsNotificationService).sendNotification(any(), any(), any(), any());

        Assert.assertEquals(HTTPS_HOST_ID, problemInfoCaptor.getValue().getHostId());
    }

    @Test
    public void testUpdateRealTimeSiteProblem_turboNoCurrentHttp() throws Exception {
        service.updateRealTimeProblem(HTTP_HOST_ID, TURBO_PROBLEM);

        ArgumentCaptor<RealTimeSiteProblemInfo> problemInfoCaptor = ArgumentCaptor.forClass(RealTimeSiteProblemInfo.class);
        Mockito.verify(realTimeSiteProblemsYDao).addProblem(problemInfoCaptor.capture());
        Mockito.verify(siteProblemsNotificationService).sendNotification(any(), any(), any(), any());


        Assert.assertEquals(HTTP_HOST_ID, problemInfoCaptor.getValue().getHostId());
    }

    @Test
    public void testUpdateRealTimeSiteProblem_nonTurboNoCurrentHttp() throws Exception {
        service.updateRealTimeProblem(HTTP_HOST_ID, NON_TURBO_PROBLEM);

        ArgumentCaptor<RealTimeSiteProblemInfo> problemInfoCaptor = ArgumentCaptor.forClass(RealTimeSiteProblemInfo.class);
        Mockito.verify(realTimeSiteProblemsYDao).addProblem(problemInfoCaptor.capture());
        Mockito.verify(siteProblemsNotificationService).sendNotification(any(), any(), any(), any());

        Assert.assertEquals(HTTP_HOST_ID, problemInfoCaptor.getValue().getHostId());
    }

    @Test
    public void testUpdateRealTimeSiteProblem_turboNotVerified() throws Exception {

        service.updateRealTimeProblem(UNVERIFIED_HOST_ID, TURBO_PROBLEM);
        Mockito.verify(realTimeSiteProblemsYDao, Mockito.never()).addProblem(any(RealTimeSiteProblemInfo.class));
    }

    @Test
    public void testUpdateRealTimeSiteProblem_notTurboUnverified() throws Exception {
        service.updateRealTimeProblem(HTTPS_HOST_ID_NOT_VERIFIED, NON_TURBO_PROBLEM);
        Mockito.verify(realTimeSiteProblemsYDao, Mockito.never()).addProblem(any(RealTimeSiteProblemInfo.class));
    }

    private static RealTimeSiteProblemInfo makeRealProblemInfo(WebmasterHostId hostId, SiteProblemTypeEnum type, SiteProblemState state) {
        return new RealTimeSiteProblemInfo(hostId, DateTime.now(), DateTime.now(), DateTime.now(), state, type, null, 0);
    }

    private static SiteProblemInfo makeSiteProblemInfo(WebmasterHostId hostiD, SiteProblemTypeEnum type, SiteProblemState state) {
        return new SiteProblemInfo(hostiD, UUIDs.timeBased(), DateTime.now(), DateTime.now(), DateTime.now(),
                state, 0, type, null);
    }
}
