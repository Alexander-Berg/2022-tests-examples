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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExpandableSectionTest {

    private static final String ARIA_HIDDEN = "aria-hidden";
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private static final String LOCATION = "Расположение";

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
    @DisplayName("Сворачиваем и разворчиваем карточку")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillageCardWithFoldedCards() {
        basePageSteps.scrollToElement(basePageSteps.onVillageCardPage().cardSection(LOCATION));
        basePageSteps.onVillageCardPage().cardSection(LOCATION).content().waitUntil(hasAttribute(ARIA_HIDDEN, FALSE));
        basePageSteps.onVillageCardPage().cardSection(LOCATION).arrowIcon().click();
        basePageSteps.onVillageCardPage().cardSection(LOCATION).content().should(hasAttribute(ARIA_HIDDEN, TRUE));
        basePageSteps.onVillageCardPage().cardSection(LOCATION).arrowIcon().click();
        basePageSteps.onVillageCardPage().cardSection(LOCATION).content().should(hasAttribute(ARIA_HIDDEN, FALSE));
    }
}
