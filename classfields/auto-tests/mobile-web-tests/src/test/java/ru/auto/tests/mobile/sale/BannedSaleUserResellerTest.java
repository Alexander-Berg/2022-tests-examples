package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Забаненное объявление - причина UserReseller")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BannedSaleUserResellerTest {

    private static final String SALE_ID = "/1084833936-c925b2bb/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/OfferCarsBannedReseller").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение причины блокировки")
    public void shouldSeeOwnerPanelWithButtons() {
        basePageSteps.onCardPage().price().bannedBadge().should(isDisplayed()).should(hasText("Заблокировано"));
        basePageSteps.onCardPage().banReasons().should(hasText("Снято с продажи\nТеперь вы можете публиковаться только " +
                "платно\nМы заметили, что вы размещаете много объявлений и/или используете сразу несколько личных " +
                "кабинетов. Это похоже на коммерческую деятельность, приносящую прибыль, поэтому мы ограничили у вас " +
                "возможность публиковаться бесплатно.\nСтоимость каждого нового объявления будет на кнопке Разместить " +
                "и оплатить после того, как вы введете все данные автомобиля. Цена будет зависеть от марки, модели, " +
                "года выпуска авто и региона размещения."));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Активировать»")
    public void shouldClickActivateButton() {
        mockRule.with("desktop/UserOffersCarsActivateReseller").update();

        basePageSteps.onCardPage().ownerControls().button("Активировать").click();
        basePageSteps.onCardPage().vasPopup().waitUntil(isDisplayed());
    }
}
