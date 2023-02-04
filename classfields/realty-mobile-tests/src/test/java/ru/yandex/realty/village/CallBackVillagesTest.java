package ru.yandex.realty.village;

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
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class CallBackVillagesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(395, 920);
        urlSteps.testing().villageCardMobile().open();
        basePageSteps.scrollToElement(basePageSteps.onVillageCardPage().villageDev());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот подсказки в поле «Обратный звонок»")
    public void shouldSeeHint() {
        basePageSteps.onVillageCardPage().villageDev().hint().click();
        basePageSteps.onVillageCardPage().popupVisible().waitUntil(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().popupVisible());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrollToElement(basePageSteps.onVillageCardPage().villageDev());
        basePageSteps.onVillageCardPage().villageDev().hint().click();
        basePageSteps.onVillageCardPage().popupVisible().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().popupVisible());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
