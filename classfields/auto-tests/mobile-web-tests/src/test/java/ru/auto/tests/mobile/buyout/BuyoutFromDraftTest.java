package ru.auto.tests.mobile.buyout;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.BUYOUT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.unAuthUserDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.BuyoutPage.ESTIMATE_CAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Создание заявки из черновика или оффера")
@Feature(BUYOUT)
@Story("Создание заявки из черновика")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BuyoutFromDraftTest {

    private static final String ID = "1114782187-3302e085";
    static private final String APPLY_DRAFT_TEXT = "1 748 450 — 2 237 400 ₽\nKia Optima IV, 2017\n100 000 км · 2.4 AT (188 л.с.) · Белый\nОценить другой автомобиль\nБесплатный осмотр\nНаш эксперт приедет на осмотр в удобное для вас место, а после — сможем назвать точную цену\nМосква и Подмосковье\nВ пределах 20 км от МКАД в будни с 9:00 до 21:00.\nНомер телефона\nЗаписаться\nПринимаю правила и даю согласие на обработку персональных данных";
    static private final String APPLY_OFFER_TEXT = "890 054 — 1 110 519 ₽\nLADA (ВАЗ) 2121 (4x4), 2018\n3 221 км · Bronto 1.7 MT (83 л.с.) 4WD · Зелёный\nОценить другой автомобиль\nБесплатный осмотр\nНаш эксперт приедет на осмотр в удобное для вас место, а после — сможем назвать точную цену\nМосква и Подмосковье\nВ пределах 20 км от МКАД в будни с 9:00 до 21:00.\nНомер телефона\nЗаписаться\nПринимаю правила и даю согласие на обработку персональных данных";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Создание заявки из черновика под зарегом")
    public void shouldCreateApplicationFromDraft() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(unAuthUserDraftExample().getBody()),
                stub("desktop/UserDraftCarsC2bApplicationInfo")
        ).update();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().button(ESTIMATE_CAR).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(APPLY_DRAFT_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Создание заявки из оффера под зарегом")
    public void shouldCreateApplicationFromOffer() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                query().setStatus("ACTIVE"))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)).build()),
                stub("desktop/UserOffersCarsC2bCanApply")
        ).update();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().button(ESTIMATE_CAR).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(APPLY_OFFER_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Проверяем что для заявки оффер приоритетней драфта")
    public void shouldSeeOfferApplication() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(unAuthUserDraftExample().getBody()),
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                query().setStatus("ACTIVE"))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)).build()),
                stub("desktop/UserDraftCarsC2bApplicationInfo"),
                stub("desktop/UserOffersCarsC2bCanApply")
        ).update();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().button(ESTIMATE_CAR).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(APPLY_OFFER_TEXT));
    }
}
