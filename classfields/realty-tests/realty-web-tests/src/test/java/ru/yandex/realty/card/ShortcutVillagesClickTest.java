package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.TARAS;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.element.Shortcuts.GENPLAN;
import static ru.yandex.realty.element.Shortcuts.LOCATION;
import static ru.yandex.realty.element.Shortcuts.PROGRESS;

@DisplayName("Карточка оффера. Шорткаты")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ShortcutVillagesClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferPageSteps offerPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .path("/bereg-stolicy-1834243/").ignoreParam("redirect_from_rgid").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KANTEMIROV)
    @DisplayName("Карточка коттеджного поселка. клик по шорткату - «Расположение»")
    public void shouldSeeScrollAndSelectVillages() {
        offerPageSteps.findShortcut(LOCATION).click();
        waitSomething(2, SECONDS);
        urlSteps.fragment("infrastructure").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class})
    @Owner(KANTEMIROV)
    @DisplayName("Карточка коттеджного поселка. клик по шорткату - «Генплан»")
    public void shouldSeeGallery() {
        offerPageSteps.findShortcut(GENPLAN).click();
        offerPageSteps.onVillageSitePage().showPhoneButtonGallery().should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TARAS)
    @DisplayName("Карточка коттеджного поселка. клик по шорткату - «Ход строительства»")
    public void shouldSeeScrollToVillageCardProgress() {
        offerPageSteps.findShortcut(PROGRESS).click();
        waitSomething(2, SECONDS);
        urlSteps.fragment("progress").shouldNotDiffWithWebDriverUrl();
    }
}
