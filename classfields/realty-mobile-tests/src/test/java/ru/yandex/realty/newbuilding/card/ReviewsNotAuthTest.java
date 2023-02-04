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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mobile.element.newbuilding.ReviewsBlock.ADD_REVIEW;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ReviewsNotAuthTest {
    private static final String OTZYVY = "Отзывы";

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
    @DisplayName("Видим блок авторицазии после нажатия на кнопку «Написать отзыв»")
    public void shouldSeeAuthBlockAfterClick() {
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().accordionBlock(OTZYVY));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().accordionBlock(OTZYVY));
        basePageSteps.onNewBuildingCardPage().reviewsBlock().button(ADD_REVIEW).click();
        basePageSteps.refreshUntil(() -> urlSteps.getCurrentUrl(),
                containsString("https://passport-test.yandex.ru/auth"));
    }
}
