package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class SafeDealSaleCarsTest {

    private static final String DEAL_ID_PATH = "/79ca542e-7709-4b6c-9f62-e432fd8b1e8f/";

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsActiveWithSafeDeal"),
                stub("desktop/SafeDealDealListForOffer"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение блока с безопасной сделкой")
    public void shouldSeeSafeDealBlock() {
        basePageSteps.onLkSalesNewPage().getSale(0).safeDealBlock().should(hasText("Безопасная сделка\nИмя покупателя\n" +
                "Частное лицо\nВаш статус\nЗаполнение паспортных данных\nСтатус покупателя\nЗаполнение паспортных " +
                "данных\nУзнайте подробнее о вашей сделке\nПерейти к сделке"));
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по ссылке перехода к сделке")
    public void shouldClickDealInfoLink() {
        basePageSteps.onLkSalesNewPage().getSale(0).safeDealBlock().button("Перейти к сделке").click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_PATH).shouldNotSeeDiff();
    }

}
