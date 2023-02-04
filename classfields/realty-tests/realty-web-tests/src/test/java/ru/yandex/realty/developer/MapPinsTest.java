package ru.yandex.realty.developer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.DeveloperPage.OFFICE;
import static ru.yandex.realty.page.DeveloperPage.OFFICES;
import static ru.yandex.realty.page.DeveloperPage.SITE;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Пины на карте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapPinsTest {

    private static final int OFFICES_COUNT = 5;
    private static final int SITES_COUNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пины офисов продаж на карте")
    public void shouldSeeOffices() {
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID,
                mockEnhancedDeveloper().addOffices(OFFICES_COUNT).build())
                .createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
        basePageSteps.onDeveloperPage().map().waitUntil(isDisplayed());
        basePageSteps.scrollElementToCenter( basePageSteps.onDeveloperPage().button(OFFICES));
        basePageSteps.onDeveloperPage().button(OFFICES).click();
        basePageSteps.onDeveloperPage().map().pinsWithType(OFFICE).should(hasSize(OFFICES_COUNT));
        basePageSteps.onDeveloperPage().map().pinsWithType(SITE).should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пины новостроек на карте")
    public void shouldSeeSites() {
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID,
                mockEnhancedDeveloper().build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().addMoscowSites(SITES_COUNT).buildSite())
                .createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
        basePageSteps.onDeveloperPage().map().waitUntil(isDisplayed());

        basePageSteps.onDeveloperPage().map().pinsWithType(SITE).should(hasSize(SITES_COUNT));
        basePageSteps.onDeveloperPage().map().pinsWithType(OFFICE).should(hasSize(0));
    }

}
