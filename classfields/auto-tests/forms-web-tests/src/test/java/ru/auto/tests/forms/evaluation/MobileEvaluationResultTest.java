package ru.auto.tests.forms.evaluation;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EVALUATION;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Оценка авто - результат оценки")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MobileEvaluationResultTest {

    private static final String MARK = "hyundai";
    private static final String MODEL = "solaris";
    private static final String SHARE_TEXT = "Смотрите правильную цену от Авто.ру на Hyundai Solaris 2020 года";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() throws IOException {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "forms/ReferenceCatalogCarsSuggest",
                "forms/TradeInIsAvailable",
                "forms/SearchCars",
                "forms/SearchCarsPriceFrom460000",
                "forms/SearchCarsPriceFrom360000",
                "forms/SearchCarsHistogram",
                "forms/StatsPredict",
                "desktop/ProxyPublicApi").post();

        formsSteps.createEvaluationCarsForm();
        urlSteps.testing().path(CARS).path(EVALUATION).open();
        formsSteps.fillForm(formsSteps.getEvaluateReason().getBlock());
        formsSteps.submitForm();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class, Screenshot.class})
    @DisplayName("Отображение результата оценки авто")
    public void shouldSeeEvaluationResult() throws IOException {
        formsSteps.setWindowMaxHeight();
        urlSteps.hideDevtoolsBranch();

        formsSteps.onFormsEvaluationPage().evaluationResult().waitUntil(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsEvaluationPage().content());

        urlSteps.setProduction().testing().path(CARS).path(EVALUATION).open();
        formsSteps.fillForm(formsSteps.getEvaluateReason().getBlock());
        formsSteps.submitForm();
        formsSteps.onFormsEvaluationPage().evaluationResult().waitUntil(isDisplayed());
        urlSteps.hideDevtoolsBranch();

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsEvaluationPage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Оценить другой автомобиль»")
    public void shouldClickEvaluateOtherAutoButton() {
        formsSteps.onFormsEvaluationPage().evaluationResult().button("Оценить другой автомобиль")
                .waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(EVALUATION).shouldNotSeeDiff();
        formsSteps.onFormsEvaluationPage().unfoldedBlock(formsSteps.getMark().getBlock()).waitUntil(isDisplayed());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Отображение подсказок")
    public void shouldSeeHelp() {
        formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Дилеру в трейд-ин").helpIcon().click();
        formsSteps.onFormsEvaluationPage().popup().waitUntil(isDisplayed()).should(hasText("Цена окончательная?\n" +
                "Финальная цена зависит от технического состояния автомобиля. " +
                "Если машина в порядке, то цена дилера не будет сильно отличаться от оценочной. " +
                "Точную цену выкупа дилер назовет после осмотра."));
        formsSteps.onFormsEvaluationPage().activePopupCloser().click();
        formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Продажа на Авто.ру").helpIcon().click();
        formsSteps.onFormsEvaluationPage().popup().waitUntil(isDisplayed()).should(hasText("Средняя цена\n" +
                "40 похожих объявлений\nот 516 000 ₽ до 586 000 ₽\n" +
                "Рассчитывается на основании цен автомобилей с похожими характеристиками, продаваемых на Авто.ру " +
                "Мы учитываем: марку, модель, поколение, комплектацию, пробег, год выпуска, тип и объём двигателя, " +
                "количество владельцев."));
        formsSteps.onFormsEvaluationPage().popup().button("40\u00a0похожих объявлений").click();
        urlSteps.switchToNextTab();
        urlSteps.mobileURI().path(MOSKVA).path(CARS).path("/hyundai/solaris/2020-year/20162370/20162385/20162567/")
                .path(ALL).addParam("geo_radius", "200").addParam("from", "evaluation")
                .addParam("customs_state_group", "CLEARED")
                .addParam("seller_group", "PRIVATE").shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Продать на Авто.ру»")
    public void shouldClickSellButton() {
        formsSteps.onFormsEvaluationPage().evaluationResult().snippet("Продажа на Авто.ру")
                .button("Продать на Авто.ру").waitUntil(isDisplayed()).click();
        urlSteps.mobileURI().path(PROMO).path(FROM_WEB_TO_APP).addParam("from", "evaluation")
                .shouldNotSeeDiff();
        formsSteps.onPromoPage().content().should(anyOf(hasText("Разместите объявление в пару касаний\n" +
                "Установить сейчас\nСпасибо, не сейчас"), hasText("Разместить объявление в пару касаний\nПродолжить в приложении")));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        formsSteps.onFormsEvaluationPage().evaluationResult().getSale(0).click();
        urlSteps.switchToNextTab();
        urlSteps.mobileURI().path(CARS).path(USED).path(SALE).path(MARK).path(MODEL).path("/1114848283-bebe3d0d/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Смотреть все объявления»")
    public void shouldClickAllSalesButton() {
        formsSteps.onFormsEvaluationPage().evaluationResult().button("Смотреть все объявления")
                .waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.mobileURI().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path("/2020-year/20162370/20162385/20162567/")
                .path(ALL).addParam("price_to", "560000").addParam("geo_radius", "200").addParam("from", "evaluation")
                .addParam("price_from", "460000").addParam("customs_state_group", "CLEARED")
                .addParam("seller_group", "PRIVATE").shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Переключение списков")
    public void shouldSwitchLists() {
        formsSteps.onFormsEvaluationPage().evaluationResult().hover();
        String firstSaleTitle = formsSteps.onFormsEvaluationPage().evaluationResult().getSale(0).getText();
        formsSteps.onFormsEvaluationPage().evaluationResult().radioButtonContains("15").click();
        formsSteps.onFormsEvaluationPage().evaluationResult().getSale(0).waitUntil(not(hasText(firstSaleTitle)));
    }
}
