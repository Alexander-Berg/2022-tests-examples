package ru.auto.tests.amp.group;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ABOUT;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class GroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final String SALE_PATH = "/kia/optima/21342125/21342381/1076842087-f1e84/";
    public static final String DEALER_PATH = "avtogermes_kia_ryabinovaya_ul_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                "amp/SearchCarsGroupConfiguration",
                "desktop/SearchCarsEquipmentFiltersKiaOptima",
                "desktop/OfferCarsPhones").post();

        basePageSteps.setWindowHeight(1000);
        urlSteps.testing().path(AMP).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение группы")
    public void shouldSeeGroup() {
        basePageSteps.onGroupPage().groupHeader().should(hasText("Новые Kia Optima IV Рестайлинг\n" +
                "от 1 169 400 ₽\nПоделиться\nО модели"));
        basePageSteps.onGroupPage().footer().should(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onGroupPage().getSale(0));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onGroupPage().getSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickSnippet() {
        basePageSteps.onGroupPage().getSale(0).header().click();
        urlSteps.testing().path(AMP).path(CARS).path(NEW).path(GROUP).path(SALE_PATH)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Параметры»")
    public void shouldClickParamButton() {
        basePageSteps.onGroupPage().button("Все параметры").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH)
                .addParam("showAllParams", "true").ignoreParam("_gl").shouldNotSeeDiff();
        basePageSteps.onGroupPage().filtersPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Поделиться»")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVkButton() {
        basePageSteps.onGroupPage().shareButton().click();
        basePageSteps.onGroupPage().popup().button("ВКонтакте").should(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «О модели»")
    public void shouldClickAboutModelButton() {
        basePageSteps.onGroupPage().button("О модели").click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).path(ABOUT)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Контакты»")
    public void shouldClickContactsButton() {
        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALER_PATH)
                .addParam("from", "auto-snippet").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить»")
    public void shouldClickCallButton() {
        basePageSteps.onGroupPage().getSale(0).callButton().click();
        basePageSteps.onGroupPage().getSale(0).callButton().waitUntil(hasText("+7 (916) 039-84-27"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подгрузка предложений")
    public void shouldLoadMoreOffers() {
        mockRule.with("amp/SearchCarsGroupConfigurationPage2").update();

        basePageSteps.scrollAndClick(basePageSteps.onGroupPage().footer());
        basePageSteps.onGroupPage().showMoreButton().click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onGroupPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}
