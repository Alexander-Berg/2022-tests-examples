package ru.auto.tests.mobile.credits;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.anyOf;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ACTIVE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Блок кредитов на карточке под зарегом")
@Feature(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveCreditBlockOfferPageTest {

    private static final String SALE_ID1 = "/1076842087-f1e84/";
    private static final String SALE_ID2 = "/1103691301-d02bfdba/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/OfferCarsUsedUser2"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("mobile/SharkCreditApplicationActiveWithOffers"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive"),
                stub("desktop/SharkCreditProductListByCreditApplication"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/SharkCreditProductCalculator"),
                stub("desktop/ProxyPublicApi")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NIKOVCHARENKO)
    @DisplayName("Отображение активной заявки (авто из заявки)")
    public void shouldSeeApplication() {
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID2).open();

        basePageSteps.onCardPage().cardCreditBlock().should(anyOf(
                hasText("Заявка на этот автомобиль отправлена\nКредит " +
                        "на 630 000 ₽\nMitsubishi Outlander\n4 заявки на рассмотрении"),
                hasText("Заявка на этот автомобиль отправлена\nКредит на 630 000 ₽\nMitsubishi Outlander" +
                        "\n4 заявки на рассмотрении\nРазыгрываем полное погашение автокредита")));

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardCreditClaimSnippet());

        urlSteps.testing().path(MY).path(CREDITS).path(ACTIVE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NIKOVCHARENKO)
    @DisplayName("Отображение активной заявки (авто не из заявки)")
    public void shouldSeeApplicationOnAnotherAuto() {
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID1).open();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardCreditBlock());

        basePageSteps.onCardPage().cardCreditBlock().should(hasText("Этот автомобиль в кредит\nБанки уже рассматривают " +
                "вашу заявку. Авто можно заменить, и мы отправим его вместе с новыми заявками.\nПервый взнос\n" +
                "70 000 ₽\nПлатеж\n12 100 ₽ / мес.\nСумма кредита\nСрок кредита\n5 лет\nЗаменить на этот автомобиль"));

        basePageSteps.setWindowMaxHeight();
        basePageSteps.scrollUp(80);
        basePageSteps.onCardPage().cardCreditBlock().button("заявку").click();

        urlSteps.testing().path(MY).path(CREDITS).path(ACTIVE).shouldNotSeeDiff();
    }
}
