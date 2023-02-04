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

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка Коттеджного посёлка")
@Feature(VILLAGE_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class StickyActionsVillagesTest {

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
    @DisplayName("Кнопка позвонить отображена")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesCallButton() {
        basePageSteps.onVillageCardPage().stickyActions().link(CALL).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Время работы отображено")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesWorkTime() {
        basePageSteps.onVillageCardPage().stickyActions().workTime().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Реклама возле кнопки позвонить")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeVillagesPromoNearCallButton() {
        basePageSteps.onVillageCardPage().stickyActions().hint().click();
        basePageSteps.onVillageCardPage().popupVisible().waitUntil(isDisplayed());
        basePageSteps.onVillageCardPage().popupVisible().should(hasText(containsString("Реклама")));
    }
}
