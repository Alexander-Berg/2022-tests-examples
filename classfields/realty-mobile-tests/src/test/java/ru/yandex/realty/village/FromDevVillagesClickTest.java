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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FromDevVillagesClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().villageCardMobile().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на первый в списке карточек новостроек от застройщика")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeFromDeveloperSiteClick() {
        urlSteps.testing().villageCardMobile().open();
        String href = basePageSteps.onVillageCardPage().fromDevVillages().waitUntil(hasSize(greaterThan(0))).get(FIRST)
                .villageLink().getAttribute("href");
        basePageSteps.onVillageCardPage().fromDevVillages().waitUntil(hasSize(greaterThan(0))).get(FIRST)
                .villageLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(href).shouldNotDiffWithWebDriverUrl();
    }
}
