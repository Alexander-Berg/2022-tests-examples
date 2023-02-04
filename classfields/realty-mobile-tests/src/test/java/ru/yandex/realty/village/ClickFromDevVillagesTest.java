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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ClickFromDevVillagesTest {

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
    @DisplayName("Ссылки в поселках от застройщика отображается")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeMoreFromDev() {
        basePageSteps.scrollToElement(basePageSteps.onVillageCardPage().showMoreFromDev());
        basePageSteps.onVillageCardPage().fromDevVillages().waitUntil(hasSize(greaterThan(0)))
                .forEach(o -> o.villageLink().should(hasHref(containsString("/kupit/kottedzhnye-poselki/"))));
    }
}
