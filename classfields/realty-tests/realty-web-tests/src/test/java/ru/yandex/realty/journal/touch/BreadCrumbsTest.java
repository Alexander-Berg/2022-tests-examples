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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATEGORY;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BreadCrumbsTest {

    private static final String NOVOSTROYKI = "/novostroyki/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(JOURNAL);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Хлебные крошки. Со страницы поста")
    public void shouldSeeBreadCrumbsFromPostPage() {
        urlSteps.path(POST_PAGE).path("/prosrochka-v-novostroyke-chto-delat/").open();
        basePageSteps.onJournalPage().breadcrumb("Новостройки").click();
        urlSteps.testing().path(JOURNAL).path(CATEGORY).path(NOVOSTROYKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Хлебные крошки. Со страницы листинга")
    public void shouldSeeBreadCrumbsFromListingPage() {
        urlSteps.path(CATEGORY).path(NOVOSTROYKI).open();
        basePageSteps.onJournalPage().breadcrumb("Журнал").click();
        urlSteps.testing().path(JOURNAL).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Хлебные крошки. «Главная»")
    public void shouldSeeBreadCrumbsToMainPage() {
        urlSteps.setSpbCookie();
        urlSteps.path(CATEGORY).path("/vtorichnoe-zhilyo/").open();
        basePageSteps.onJournalPage().breadcrumb("Главная").click();
        basePageSteps.onMobileMainPage().searchFilters().region().value().should(hasText("Санкт-Петербург"));
        urlSteps.testing().path("/").shouldNotDiffWithWebDriverUrl();
    }
}
