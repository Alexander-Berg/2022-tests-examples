package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.EDIT_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Забаненное объявление - Кнопка «Редактировать»")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BannedSaleEditButtonTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";

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

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Редактировать» должна быть на странице и кликаться")
    public void shouldClickEditButton() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsBannedHigherPrice").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID).open();

        basePageSteps.onCardPage().bannedMessage().editButton().should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(OFFER_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопки «Редактировать» не должно быть")
    public void shouldNotSeeEditButton() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsBannedDoNotExist").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID).open();

        basePageSteps.onCardPage().bannedMessage().editButton().should(not(exists()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должна появляться нотифайка про модерацию")
    public void shouldSeeNotifyAboutModeration() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsBannedHigherPrice").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID).addParam(PAGE_FROM, EDIT_PAGE)
                .open();
        basePageSteps.onCardPage().notifier().waitUntil("Нотифайка не появилась", isDisplayed(), 5)
                .should(hasText("Спасибо. Модераторы проверят изменения и активируют объявление, если всё будет верно."));
    }
}
