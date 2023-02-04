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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class StickyActionsNewBuildingCardTest {

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

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка позвонить отображена")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeCallButton() {
        basePageSteps.onNewBuildingCardPage().stickyActions().link(CALL).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Время работы отображено")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeWorkTime() {
        basePageSteps.onNewBuildingCardPage().stickyActions().workTime().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Реклама возле кнопки позвонить")
    @Category({Regression.class, Mobile.class})
    public void shouldSeePromoNearCallButton() {
        basePageSteps.onNewBuildingCardPage().stickyActions().hint().click();
        basePageSteps.onNewBuildingCardPage().popupVisible().waitUntil(isDisplayed());
        basePageSteps.onNewBuildingCardPage().popupVisible().should(hasText(containsString("Реклама")));
    }
}
