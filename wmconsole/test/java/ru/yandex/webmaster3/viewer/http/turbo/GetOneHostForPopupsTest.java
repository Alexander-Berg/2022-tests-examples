package ru.yandex.webmaster3.viewer.http.turbo;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.data.WebmasterUser;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.turbo.model.autoparser.AutoparserToggleState;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;
import ru.yandex.webmaster3.storage.abt.AbtService;
import ru.yandex.webmaster3.storage.abt.model.Experiment;
import ru.yandex.webmaster3.storage.turbo.service.autoparser.TurboAutoparserInfoService;
import ru.yandex.webmaster3.storage.user.service.UserHostsService;
import ru.yandex.webmaster3.storage.user.settings.FrontendUserHostSettingsKey;
import ru.yandex.webmaster3.storage.user.settings.FrontendUserHostSettingsYDao;
import ru.yandex.webmaster3.storage.user.settings.FrontendUserSettingsKey;
import ru.yandex.webmaster3.storage.user.settings.FrontendUserSettingsYDao;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author: ishalaru
 * DATE: 30.09.2019
 */
public class
GetOneHostForPopupsTest {
    private static final WebmasterHostId HOST_ID_1 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "02.ru", 80);
    private static final WebmasterHostId HOST_ID_2 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "01.ru", 80);
    private static final WebmasterHostId HOST_ID_3 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "00.ru", 80);
    private static final WebmasterHostId HOST_ID_4 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "04.ru", 80);

    @Test
    public void emptyAnswerTest() {
        UserHostsService userHostsService = mock(UserHostsService.class);
        var userVerifiedHosts = List.of(new UserVerifiedHost(HOST_ID_3, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_2, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN));
        when(userHostsService.getVerifiedHosts(new WebmasterUser(1))).thenReturn(userVerifiedHosts);
        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        FrontendUserHostSettingsYDao frontendUserHostSettingsYDao = mock(FrontendUserHostSettingsYDao.class);
        AbtService abtService = mock(AbtService.class);
        GetOneHostForPopups getOneHostForPopups = new GetOneHostForPopups(userHostsService, abtService, turboAutoparserInfoService, frontendUserHostSettingsYDao);
        final GetOneHostForPopups.Request request = new GetOneHostForPopups.Request();
        request.setWebmasterUser(new WebmasterUser(1L));
        request.setUserId(1L);
        var process = (GetOneHostForPopups.Response.NormalResponse) getOneHostForPopups.process(request);
        Assert.assertEquals("Count answers", 0, process.getResult().size());
    }


    @Test
    public void fullAnswerTest() {
        UserHostsService userHostsService = mock(UserHostsService.class);
        var userVerifiedHosts = List.of(new UserVerifiedHost(HOST_ID_3, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_2, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_1, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN)
        );
        when(userHostsService.getVerifiedHosts(new WebmasterUser(1))).thenReturn(userVerifiedHosts);

        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        when(turboAutoparserInfoService.getAutoparseCheckBoxState("00.ru")).thenReturn(AutoparserToggleState.OFF);
        FrontendUserHostSettingsYDao frontendUserHostSettingsYDao = mock(FrontendUserHostSettingsYDao.class);

        List<WebmasterHostId> hostIds = List.of(HOST_ID_1, HOST_ID_2, HOST_ID_3);
        AbtService abtService = mock(AbtService.class);
        when(abtService.getHostsExperiments(argThat(t -> t.containsAll(hostIds))))
                .thenReturn(Map.of(
                        HOST_ID_1, Map.of(Experiment.TURBO_SHOP_TEASER.getName(), "TEST"),
                        HOST_ID_2, Map.of(Experiment.TURBO_YML_TEASER.getName(), "TEST")
                ));

        GetOneHostForPopups getOneHostForPopups = new GetOneHostForPopups(userHostsService, abtService, turboAutoparserInfoService, frontendUserHostSettingsYDao);
        final GetOneHostForPopups.Request request = new GetOneHostForPopups.Request();
        request.setWebmasterUser(new WebmasterUser(1L));
        request.setUserId(1L);
        var process = (GetOneHostForPopups.Response.NormalResponse) getOneHostForPopups.process(request);
        Assert.assertEquals("Count answers", 1, process.getResult().size());
    }

    @Test
    public void fullAnswerTestWithFiltrationByUserSettings() {
        UserHostsService userHostsService = mock(UserHostsService.class);
        var userVerifiedHosts = List.of(new UserVerifiedHost(HOST_ID_3, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_2, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_1, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN)
        );
        when(userHostsService.getVerifiedHosts(new WebmasterUser(1))).thenReturn(userVerifiedHosts);

        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        when(turboAutoparserInfoService.getAutoparseCheckBoxState("00.ru")).thenReturn(AutoparserToggleState.OFF);
        FrontendUserHostSettingsYDao frontendUserHostSettingsYDao = mock(FrontendUserHostSettingsYDao.class);
        FrontendUserSettingsYDao frontendUserSettingsYDao = mock(FrontendUserSettingsYDao.class);
        when(frontendUserSettingsYDao.getValues(anyLong(),anyList())).thenReturn(
                Map.of(FrontendUserSettingsKey.TURBO_YML_TEASER, "TestValue",
                        FrontendUserSettingsKey.TURBO_SHOP_TEASER, "TestValueShop"));

        List<WebmasterHostId> hostIds = List.of(HOST_ID_1, HOST_ID_2, HOST_ID_3);
        AbtService abtService = mock(AbtService.class);
        when(abtService.getHostsExperiments(argThat(t -> t.containsAll(hostIds))))
                .thenReturn(Map.of(
                        HOST_ID_1, Map.of(Experiment.TURBO_SHOP_TEASER.getName(), "TEST"),
                        HOST_ID_2, Map.of(Experiment.TURBO_YML_TEASER.getName(), "TEST")
                ));

        GetOneHostForPopups getOneHostForPopups = new GetOneHostForPopups(userHostsService, abtService, turboAutoparserInfoService, frontendUserHostSettingsYDao);
        final GetOneHostForPopups.Request request = new GetOneHostForPopups.Request();
        request.setWebmasterUser(new WebmasterUser(1L));
        request.setUserId(1L);
        var process = (GetOneHostForPopups.Response.NormalResponse) getOneHostForPopups.process(request);
        Assert.assertEquals("Count answers", 1, process.getResult().size());
    }

    @Test
    public void fullAnswerFilteredByFrontParametersTest() {
        UserHostsService userHostsService = mock(UserHostsService.class);
        var userVerifiedHosts = List.of(new UserVerifiedHost(HOST_ID_3, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_2, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_1, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN)
        );
        when(userHostsService.getVerifiedHosts(new WebmasterUser(1))).thenReturn(userVerifiedHosts);

        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        when(turboAutoparserInfoService.getAutoparseCheckBoxState("00.ru")).thenReturn(AutoparserToggleState.OFF);
        FrontendUserHostSettingsYDao frontendUserHostSettingsYDao = mock(FrontendUserHostSettingsYDao.class);
        when(frontendUserHostSettingsYDao.getValues(1L)).thenReturn(
                Map.of(HOST_ID_2, Map.of(FrontendUserHostSettingsKey.TURBO_YML_TEASER, "TestValue"),
                        HOST_ID_1, Map.of(FrontendUserHostSettingsKey.TURBO_SHOP_TEASER, "TestValueShop"))
        );

        List<WebmasterHostId> hostIds = List.of(HOST_ID_1, HOST_ID_2, HOST_ID_3);
        AbtService abtService = mock(AbtService.class);
        when(abtService.getHostsExperiments(argThat(t -> t.containsAll(hostIds))))
                .thenReturn(Map.of(
                        HOST_ID_1, Map.of(Experiment.TURBO_SHOP_TEASER.getName(), "TEST"),
                        HOST_ID_2, Map.of(Experiment.TURBO_YML_TEASER.getName(), "TEST")
                ));

        GetOneHostForPopups getOneHostForPopups = new GetOneHostForPopups(userHostsService, abtService, turboAutoparserInfoService, frontendUserHostSettingsYDao);
        final GetOneHostForPopups.Request request = new GetOneHostForPopups.Request();
        request.setWebmasterUser(new WebmasterUser(1L));
        request.setUserId(1L);
        var process = (GetOneHostForPopups.Response.NormalResponse) getOneHostForPopups.process(request);
        Assert.assertEquals("Count answers", 1, process.getResult().size());
    }

    @Test
    public void hostWithIncorrectAnswer() {
        UserHostsService userHostsService = mock(UserHostsService.class);
        var userVerifiedHosts = List.of(new UserVerifiedHost(HOST_ID_3, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_2, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_1, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN),
                new UserVerifiedHost(HOST_ID_4, DateTime.now(), DateTime.now().plusDays(1), 123, VerificationType.UNKNOWN)
        );
        when(userHostsService.getVerifiedHosts(new WebmasterUser(1))).thenReturn(userVerifiedHosts);

        TurboAutoparserInfoService turboAutoparserInfoService = mock(TurboAutoparserInfoService.class);
        when(turboAutoparserInfoService.getAutoparseCheckBoxState("00.ru")).thenReturn(AutoparserToggleState.OFF);
        FrontendUserHostSettingsYDao frontendUserHostSettingsYDao = mock(FrontendUserHostSettingsYDao.class);
        when(frontendUserHostSettingsYDao.getValues(1L)).thenReturn(
                Map.of(HOST_ID_2, Map.of(FrontendUserHostSettingsKey.TURBO_YML_TEASER, "TestValue"),
                        HOST_ID_1, Map.of(FrontendUserHostSettingsKey.TURBO_SHOP_TEASER, "TestValueShop"),
                        HOST_ID_4, Map.of(FrontendUserHostSettingsKey.TURBO_YML_TEASER, "TestValue"))
        );

        List<WebmasterHostId> hostIds = List.of(HOST_ID_1, HOST_ID_2, HOST_ID_3, HOST_ID_4);
        AbtService abtService = mock(AbtService.class);
        when(abtService.getHostsExperiments(argThat(t -> t.containsAll(hostIds))))
                .thenReturn(Map.of(
                        HOST_ID_1, Map.of(Experiment.TURBO_SHOP_TEASER.getName(), "TEST"),
                        HOST_ID_2, Map.of(Experiment.TURBO_YML_TEASER.getName(), "TEST"),
                        HOST_ID_4, Map.of(Experiment.TURBO_YML_TEASER.getName(), "TURBO_YML_TEST",Experiment.TURBO_SHOP_TEASER.getName(),"TURBO_SHOT_TEST")
                ));

        GetOneHostForPopups getOneHostForPopups = new GetOneHostForPopups(userHostsService, abtService, turboAutoparserInfoService, frontendUserHostSettingsYDao);
        final GetOneHostForPopups.Request request = new GetOneHostForPopups.Request();
        request.setWebmasterUser(new WebmasterUser(1L));
        request.setUserId(1L);
        var process = (GetOneHostForPopups.Response.NormalResponse) getOneHostForPopups.process(request);
        Assert.assertEquals("Count answers", 1, process.getResult().size());
        Assert.assertNull(process.getResult().get(GetOneHostForPopups.PopupType.TURBO_YML_TEASER));
        Assert.assertNotNull(process.getResult().get(GetOneHostForPopups.PopupType.TURBO_AUTOPARSER_POPUP));
    }

}