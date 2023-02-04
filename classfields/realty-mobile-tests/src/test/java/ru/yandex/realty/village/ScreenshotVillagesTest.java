package ru.yandex.realty.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ScreenshotVillagesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот страницы")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeScreenshot() {
        urlSteps.testing().villageCardMobile().open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onVillageCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
