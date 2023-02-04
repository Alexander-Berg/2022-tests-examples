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

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.developer.office.OfficeResponse.kazanOffice;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;
import static ru.yandex.realty.page.DeveloperPage.OFFICES;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Телефон на сниппете оффиса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapSnippetOfficePhoneButtonTest {

    private static final String PHONE = "+7 (800) 555-35-35";
    private static final String PHONE_IN_BUTTON = "+7 800 555 35 35";

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
    @DisplayName("Телефон на сниппете офиса")
    public void shouldSeePhoneOfficeSnipet() {
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID,
                mockEnhancedDeveloper().setOffices(
                        kazanOffice().setPhones(asList(PHONE))).build()).createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
        basePageSteps.onDeveloperPage().map().waitUntil(isDisplayed());
        basePageSteps.scrollElementToCenter(basePageSteps.onDeveloperPage().button(OFFICES));
        basePageSteps.onDeveloperPage().button(OFFICES).click();
        basePageSteps.onDeveloperPage().map().pins().get(0).hover();
        basePageSteps.onDeveloperPage().officeSnippet().button().click();

        basePageSteps.onDeveloperPage().officeSnippet().button().should(hasText(PHONE_IN_BUTTON));
    }

}
