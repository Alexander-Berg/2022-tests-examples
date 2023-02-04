package ru.auto.tests.desktopcompare;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение 2-х моделей")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ModelVsModelTest {

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
                "desktop-compare/SearchCarsBreadcrumbsHyundaiSonataKiaRio",
                "desktop-compare/UserCompareCarsModelsShowWithoutSave",
                "desktop-compare/UserCompareCarsModelsShowWithoutSaveByConfiguration",
                "desktop-compare/SearchCarsHyundaiSonataHyundaiRio",
                "desktop-compare/ReviewsAutoCarsRatingHyundaiSonata",
                "desktop-compare/ReviewsAutoCarsRatingKiaRio",
                "desktop-compare/ReviewsAutoCarsCounterHyundaiSonata",
                "desktop-compare/ReviewsAutoCarsCounterKiaRio",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(COMPARE_CARS).path("/hyundai-sonata-vs-kia-rio/").open();
    }


    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по названию модели")
    @Category({Regression.class, Testing.class})
    public void shouldClickModel() {
        basePageSteps.onCompareCarsPage().firstModelHead().button("Hyundai Sonata").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CATALOG).path(CARS).path("/hyundai/sonata/21719392/21719435/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «N предложений»")
    @Category({Regression.class, Testing.class})
    public void shouldClickOffersButton() {
        basePageSteps.onCompareCarsPage().firstModelOffers().should(hasText("от 1 740 000 ₽\n40 предложений"));
        basePageSteps.onCompareCarsPage().firstModelOffers().button("40\u00a0предложений").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path("/hyundai/sonata/21719392/21719435/21719502/").path(ALL)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по чекбоксу «Показать только отличия»")
    @Category({Regression.class, Testing.class})
    public void shouldClickShowOnlyDiffCheckbox() {
        basePageSteps.onCompareCarsPage().row("Тип двигателя").should(hasText("Тип двигателя\nБензин\nБензин"));
        basePageSteps.onCompareCarsPage().checkbox("Показать только отличия").click();
        basePageSteps.onCompareCarsPage().row("Тип двигателя").should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отзывов")
    @Category({Regression.class, Testing.class})
    public void shouldSeeReviews() {
        basePageSteps.onCompareCarsPage().firstModelReviews().should(hasText("4,4\nпо 700 отзывам"));
        basePageSteps.onCompareCarsPage().firstModelReviews().button("по\u00a0700\u00a0отзывам").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(REVIEWS).path(CARS).path("/hyundai/sonata/21719392/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение похожих предложений")
    @Category({Regression.class, Testing.class})
    public void shouldSeeRelatedOffers() {
        basePageSteps.setWideWindowSize();
        basePageSteps.onCompareCarsPage().verticalRelated().itemsList().should(hasSize(4))
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onCompareCarsPage().verticalRelated().getItem(0).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/rio/22646507/22646653/1102646114-b51ed0f0/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другого поколения")
    @Category({Regression.class, Testing.class})
    public void shouldSelectOtherGeneration() {
        basePageSteps.onCompareCarsPage().row("Объем двигателя").should(hasText("Объем двигателя\n2.5 л\n1.6 л"));
        basePageSteps.onCompareCarsPage()
                .selectItem("Поколение: 2019 – н.в. VIII (DN8)",
                        "Поколение: 2017 – 2019 VII (LF) Рестайлинг");
        basePageSteps.onCompareCarsPage().row("Объем двигателя").should(hasText("Объем двигателя\n2.0 л\n1.6 л"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другого двигателя")
    @Category({Regression.class, Testing.class})
    public void shouldSelectOtherEngine() {
        basePageSteps.onCompareCarsPage().row("Объем двигателя").should(hasText("Объем двигателя\n2.5 л\n1.6 л"));
        basePageSteps.onCompareCarsPage()
                .selectItem("Двигатель: 2.5 AT (180 л.с.)\u00a0бензин",
                        "Двигатель: 1.6 AT (183 л.с.)\u00a0бензин");
        basePageSteps.onCompareCarsPage().row("Объем двигателя").should(hasText("Объем двигателя\n1.6 л\n1.6 л"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой комплектации")
    @Category({Regression.class, Testing.class})
    public void shouldSelectOtherComplectation() {
        basePageSteps.onCompareCarsPage().row("Датчик дождя").icon()
                .should(hasClass(containsString("ComparisonCell__icon_minus")));
        basePageSteps.onCompareCarsPage()
                .selectItem("Комплектация: Online",
                        "Комплектация: Business");
        basePageSteps.onCompareCarsPage().row("Датчик дождя").icon()
                .should(hasClass(containsString("ComparisonCell__icon_checked")));
    }

    @Test
    @Owner(TIMONDL)
    @DisplayName("Выбор другого кузова")
    @Category({Regression.class, Testing.class})
    public void shouldSelectOtherBody() {
        basePageSteps.onCompareCarsPage().secondModelPriceBlock()
                .should(hasText("от 940 900 ₽\n54 предложения"));

        basePageSteps.onCompareCarsPage().secondModelHead()
                .selectItem("Кузов: Седан", "Кузов: Хэтчбек 5 дв. X");

        basePageSteps.onCompareCarsPage().secondModelPriceBlock()
                .should(hasText("от 1 345 000 ₽\n1 предложение"));
    }
}
