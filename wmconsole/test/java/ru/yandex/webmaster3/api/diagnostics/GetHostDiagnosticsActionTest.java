package ru.yandex.webmaster3.api.diagnostics;

import java.util.Collections;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.api.diagnostics.action.GetHostDiagnosticsAction;
import ru.yandex.webmaster3.api.diagnostics.action.GetHostDiagnosticsRequest;
import ru.yandex.webmaster3.api.diagnostics.action.GetHostDiagnosticsResponse;
import ru.yandex.webmaster3.api.diagnostics.data.ApiProblemInfo;
import ru.yandex.webmaster3.api.diagnostics.data.ApiSiteProblemState;
import ru.yandex.webmaster3.api.diagnostics.data.ApiSiteProblemTypeEnum;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemState;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.checklist.data.AbstractProblemInfo;
import ru.yandex.webmaster3.storage.checklist.service.SiteProblemsService;
import ru.yandex.webmaster3.storage.host.service.MirrorService2;

import static org.mockito.Mockito.when;


/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class GetHostDiagnosticsActionTest {
    private WebmasterHostId HOST_ID = IdUtils.stringToHostId("https:lenta.ru:443");

    @Mock
    private MirrorService2 mirrorService2;

    @Mock
    private SiteProblemsService siteProblemsService;

    @InjectMocks
    GetHostDiagnosticsAction action = new GetHostDiagnosticsAction(mirrorService2, siteProblemsService);

    @Test
    @Ignore
    public void testGet() {
        when(mirrorService2.isMainMirror(HOST_ID))
                .thenReturn(true);

        DateTime now = DateTime.now();
        AbstractProblemInfo problemInfo = new AbstractProblemInfo(
                HOST_ID, null, SiteProblemTypeEnum.MORDA_ERROR, now, SiteProblemState.NOT_APPLICABLE, now, 0
        );

        when(siteProblemsService.listProblemsForHost(HOST_ID, null))
                .thenReturn(Collections.singletonList(problemInfo));

        GetHostDiagnosticsRequest request = createRequest();
        GetHostDiagnosticsResponse response = action.process(request);
        Assert.assertNotNull(response);

        // В API, в отличии от вьювера, мы отдаем все, что проверяем
        Map<ApiSiteProblemTypeEnum, ApiProblemInfo> problemsMap = response.getProblems();
        Assert.assertEquals(ApiSiteProblemTypeEnum.values().length, problemsMap.size());

        ApiProblemInfo responseProblemInfo = problemsMap.get(ApiSiteProblemTypeEnum.MAIN_PAGE_ERROR);
        Assert.assertTrue(responseProblemInfo.getLastStateUpdate().isPresent());
        Assert.assertEquals(now, responseProblemInfo.getLastStateUpdate().get());
        Assert.assertEquals(ApiSiteProblemState.NOT_APPLICABLE, responseProblemInfo.getState());
    }

    private GetHostDiagnosticsRequest createRequest() {
        GetHostDiagnosticsRequest request = new GetHostDiagnosticsRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        HostDiagnosticsLocator locator = new HostDiagnosticsLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
