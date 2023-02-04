package ru.yandex.realty.listing.newbuilding;

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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;

@DisplayName("Листинг новостроек. Верхний баннер")
@Feature(NEWBUILDING)
@Link("https://st.yandex-team.ru/VERTISTEST-1948")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class TopBannerTest {

    private static final int LISTING_SITE_SIZE = 12;
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.onNewBuildingPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Баннер остается после применения фильтров")
    public void shouldSeeTopListingBanner() {
        basePageSteps.onNewBuildingPage().topListingBanner().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingPage().filters().checkButton("Студия");
        basePageSteps.onNewBuildingPage().loader().waitUntil(not(isDisplayed()));
        basePageSteps.onNewBuildingPage().topListingBanner().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Баннер остается после скролла ко второй странице")
    public void shouldSeeTopListingBannerAfterScrolling() {
        basePageSteps.onNewBuildingPage().topListingBanner().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingPage().snippetElements().waitUntil(hasSize(LISTING_SITE_SIZE));
        basePageSteps.scrollingUntil(() -> basePageSteps.onNewBuildingPage().snippetElements(),
                hasSize(greaterThan(LISTING_SITE_SIZE)));
        basePageSteps.onNewBuildingPage().topListingBanner().should(isDisplayed());
    }
}
