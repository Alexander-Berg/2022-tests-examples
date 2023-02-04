package ru.yandex.general.homepage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.beans.ajaxRequests.GetHomepage.getHomepage;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PAGING;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.step.AjaxProxySteps.GET_HOMEPAGE;

@Epic(HOMEPAGE_FEATURE)
@Feature(PAGING)
@DisplayName("Проверка пейджинга на главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class HomepagePagingTest {

    private static final int PXLS_TO_NEXT_PAGE = 3000;
    private static final String[] JSONPATHS_TO_IGNORE = {"pageToken"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.resize(1920, 1080);
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением второй страницы главной")
    public void shouldSeeGetHomepageSecondPage() {
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_HOMEPAGE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getHomepage().setPage(2).setLimit(30).setRegionId("213").toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса за получением третьей страницы главной")
    public void shouldSeeGetHomepageThirdPage() {
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        basePageSteps.slowScrolling(PXLS_TO_NEXT_PAGE);

        ajaxProxySteps.setAjaxHandler(GET_HOMEPAGE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getHomepage().setPage(3).setLimit(30).setRegionId("213").toString())
                .shouldExist();
    }

}
