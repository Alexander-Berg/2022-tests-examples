package ru.yandex.realty.journal.desktop;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PassToListingTest {

    private static final String NEWBUILDING_BUTTON = "q1";
    private static final String OFFERS_BUTTON = "q2";
    private static final String HREF = "href";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path("/v3/").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в новостройки")
    public void shouldSeeNewBuildingPass() {
        basePageSteps.onJournalPage().link(NEWBUILDING_BUTTON).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход в квартиры")
    public void shouldSeeOffersPass() {
        basePageSteps.onJournalPage().link(OFFERS_BUTTON).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по карточке новостройки -> первой отображается та по которой кликнули")
    public void shouldSeeNewbuildingCardPass() {
        String href = basePageSteps.onJournalPage().newbuildingCards().waitUntil(hasSize(greaterThan(1))).get(1)
                .getAttribute(HREF);
        String id = href.substring(href.indexOf("=") + 1);
        basePageSteps.onJournalPage().newbuildingCards().get(1).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).queryParam("pinnedSiteId", id)
                .shouldNotDiffWithWebDriverUrl();
        basePageSteps.onNewBuildingPage().offer(FIRST).link().should(hasHref(containsString(id)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по карточке оффера -> первым отображается тот по которому кликнули")
    public void shouldSeeOfferCardPass() {
        String href = basePageSteps.onJournalPage().offerCards().waitUntil(hasSize(greaterThan(1))).get(1)
                .getAttribute(HREF);
        String id = href.substring(href.indexOf("=") + 1);
        basePageSteps.onJournalPage().offerCards().get(1).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).queryParam("pinnedOfferId", id)
                .shouldNotDiffWithWebDriverUrl();
        basePageSteps.onOffersSearchPage().offer(FIRST).offerLink().should(hasHref(containsString(id)));
    }
}
