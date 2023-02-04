package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.AddOfferNavigateModal.CHROME;
import static ru.auto.tests.desktop.mobile.element.AddOfferNavigateModalItem.CONTINUE;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Модалка выбора апп/браузер при подаче оффера")
@Epic(BETA_POFFER)
@Feature("Модалка выбора апп/браузер при подаче оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class AddOfferNavigateModalTest {

    private static final String MODAL_TEXT = "В приложении — удобнее\nAuto.ru\nОткрыть\nChrome\nПродолжить";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.testing().path(SLASH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст модалки выбора апп/браузер при подаче оффера")
    public void shouldSeeModalText() {
        basePageSteps.onMainPage().fabAddSale().waitUntil(isDisplayed()).click();

        basePageSteps.onMainPage().addOfferNavigateModal().should(hasText(MODAL_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим в поффер из модалки с «Разместить бесплатно» на главной")
    public void shouldGoToPofferFromFabAddSale() {
        basePageSteps.onMainPage().fabAddSale().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().item(CHROME).button(CONTINUE).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().waitUntil(not(isDisplayed()), 2);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
        basePageSteps.onPofferPage().markBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим в поффер из модалки с «Добавить объявление» в сайдбаре")
    public void shouldGoToPofferFromSidebar() {
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().sidebar().button("Добавить объявление").waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().item(CHROME).button(CONTINUE).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().waitUntil(not(isDisplayed()), 2);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
        basePageSteps.onPofferPage().markBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим в поффер с «Добавить объявление» в ЛК")
    public void shouldGoToPofferFromLk() {
        urlSteps.testing().path(MY).open();
        basePageSteps.onLkPage().stub().button("Добавить объявление").click();
        basePageSteps.onMainPage().addOfferNavigateModal().item(CHROME).button(CONTINUE).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().waitUntil(not(isDisplayed()), 2);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
        basePageSteps.onPofferPage().markBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем модалку выбора апп/браузер при подаче оффера")
    public void shouldCloseModal() {
        basePageSteps.onMainPage().fabAddSale().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().addOfferNavigateModal().closeIcon().waitUntil(isDisplayed()).click();

        basePageSteps.onMainPage().addOfferNavigateModal().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
