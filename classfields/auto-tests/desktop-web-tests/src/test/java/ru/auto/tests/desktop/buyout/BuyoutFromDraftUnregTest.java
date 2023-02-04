package ru.auto.tests.desktop.buyout;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.BUYOUT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.unAuthUserDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.page.BuyoutPage.ESTIMATE_CAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Создание заявки из черновика под незарегом")
@Feature(BUYOUT)
@Story("Создание заявки из черновика")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BuyoutFromDraftUnregTest {

    static private final String APPLY_DRAFT_TEXT = "1 748 450 — 2 237 400 ₽\nKia Optima IV, 2017\n100 000 км · 2.4 AT (188 л.с.) · Белый\nОценить другой автомобиль\nБесплатный осмотр\nНаш эксперт приедет на осмотр в удобное для вас место, а после — сможем назвать точную цену\nМосква и Подмосковье\nВ пределах 20 км от МКАД в будни с 9:00 до 21:00.\nНомер телефона\nЗаписаться\nПринимаю правила и даю согласие на обработку персональных данных";

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
                stub("desktop/SessionUnauth")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Создание заявки из черновика под незарегом")
    public void shouldCreateApplicationFromDraft() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(unAuthUserDraftExample().getBody()),
                stub("desktop/UserDraftCarsC2bApplicationInfo")
        ).update();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().floatingBuyoutButton().click();

        basePageSteps.onBuyoutPage().popup().should(hasText(APPLY_DRAFT_TEXT));
    }
}
