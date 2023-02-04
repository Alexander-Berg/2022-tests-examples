package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Черновики")
@Feature(LK)
@Story("Черновик")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop-lk/UserDraftCars", "desktop/User").post();

        urlSteps.testing().path(MY).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение черновика")
    public void shouldSeeDraft() {
        basePageSteps.onLkPage().getSale(0).should(hasText("Subaru Forester\nЧерновик\nРедактировать\nУдалить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onLkPage().getSale(0).link().hover().click();
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam("action", "edit").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkPage().getSale(0).button("Редактировать").click();
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam("action", "edit").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Удалить»")
    public void shouldClickDeleteButton() {
        mockRule.with("desktop-lk/UserDraftCarsDelete").update();

        basePageSteps.onLkPage().getSale(0).button("Удалить").click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Черновик удалён"));
        basePageSteps.onLkPage().salesList().waitUntil(hasSize(0));
    }
}
