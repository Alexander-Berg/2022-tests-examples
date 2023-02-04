package ru.yandex.realty.journal.touch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATEGORY;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;
import static ru.yandex.realty.step.UrlSteps.PAGE_URL_PARAM;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PagerTest {

    private static final String ARENDA = "/arenda/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path(ARENDA);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пейджер. Правая стрелка")
    public void shouldSeeNextPage() {
        urlSteps.open();
        basePageSteps.onJournalPage().pagerNext().click();
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path(ARENDA).queryParam(PAGE_URL_PARAM, "2")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пейджер. Левая стрелка")
    public void shouldSeePrevPage() {
        urlSteps.queryParam(PAGE_URL_PARAM, "4").open();
        basePageSteps.onJournalPage().pagerPrev().click();
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path(ARENDA).queryParam(PAGE_URL_PARAM, "3")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пейджер. Номер страницы")
    public void shouldSeePageNumber() {
        String page = "4";
        urlSteps.queryParam(PAGE_URL_PARAM, "2").open();
        basePageSteps.onJournalPage().page(page).click();
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path(ARENDA).queryParam(PAGE_URL_PARAM, page)
                .shouldNotDiffWithWebDriverUrl();
    }
}
