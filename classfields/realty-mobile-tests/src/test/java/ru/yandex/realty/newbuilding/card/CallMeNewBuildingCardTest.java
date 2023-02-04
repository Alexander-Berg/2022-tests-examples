package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL_ME;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOfferStatCallbackTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class CallMeNewBuildingCardTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOfferStatCallbackTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMockWithCallback().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm());

    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот подсказки в поле «Обратный звонок»")
    public void shouldSeeSubscribeOnPromo() {
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().hint());
        basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().hint().click();
        basePageSteps.onNewBuildingCardPage().popupVisible().waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().popupVisible());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm());
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().hint());
        basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().hint().click();
        basePageSteps.onNewBuildingCardPage().popupVisible().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().popupVisible());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем «Позвоните мне»")
    public void shouldSeeCallMePopup() {
        basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().inputPhone(getRandomPhone());
        basePageSteps.onNewBuildingCardPage().newbuildingCallbackForm().button(CALL_ME).click();
    }
}
