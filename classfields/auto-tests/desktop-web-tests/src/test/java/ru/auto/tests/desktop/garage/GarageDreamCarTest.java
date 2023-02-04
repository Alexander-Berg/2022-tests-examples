package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.BODY_SEDAN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DREAMCAR;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SHARE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.GEO_RADIUS;
import static ru.auto.tests.desktop.consts.QueryParams.RID;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MARK;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MODEL;
import static ru.auto.tests.desktop.element.garage.CardForm.GENERATION;
import static ru.auto.tests.desktop.element.garage.CardForm.MARK;
import static ru.auto.tests.desktop.element.garage.CardForm.MODIFICATION;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.DELETE_FROM_GARAGE;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.WATCH_PUBLIC_PROFILE;
import static ru.auto.tests.desktop.mobile.page.MagPage.TEST_ARTICLE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.page.GarageCardPage.ALL_OFFERS;
import static ru.auto.tests.desktop.page.GarageCardPage.ALL_PARAMETERS;
import static ru.auto.tests.desktop.page.GaragePage.ADD_CAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка машины мечты")
@Epic(AutoruFeatures.GARAGE)
@Feature(AutoruFeatures.DREAM_CAR)
@Story("Карточка машины мечты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageDreamCarTest {

    private static final String CARD_ID = "/1146321503/";
    private static final String HP_228 = "2.0 AMT (228 л.с.) 2018 - 2020";
    private static final String HP_150 = "2.0 AMT (150 л.с.) 2016 - 2020";
    private static final String HREF = "href";
    private static final String ARTICLE_FROM_GARAGE = "/pervyy-test-audia3-novogo-pokoleniya/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/GarageUserCardDreamCarGetAudi_A3_23172670"),
                stub("reviews/ReviewsAutoListingCarsAudiA3"),
                stub("desktop/LentaGetFeed")).create();

        urlSteps.testing().path(GARAGE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по кнопке «Удалить из гаража»")
    public void shouldDelete() {
        mockRule.setStubs(stub("desktop/GarageUserCardDelete")).update();
        basePageSteps.onGarageCardPage().leftColumn().button(DELETE_FROM_GARAGE).click();
        basePageSteps.acceptAlert();

        urlSteps.testing().path(GARAGE).shouldNotSeeDiff();
        basePageSteps.onGaragePage().addBlock().button(ADD_CAR).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по офферу")
    public void shouldClickOffer() {
        basePageSteps.onGarageCardPage().offerCarousel().get(0).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/audi/").path("/a3/").path("/1106542077-1d2a8d02/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в «Все объявления»")
    public void shouldClickAllOffersUrl() {
        basePageSteps.onGarageCardPage().button(ALL_OFFERS).should(hasAttribute(HREF,
                urlSteps.testing().path(MOSKVA).path(CARS).path("/audi/a3/20785010/").path(ALL).path(BODY_SEDAN)
                        .addParam(RID, "213").addParam(GEO_RADIUS, "500").toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по отзыву из «Статьи и отзывы»")
    public void shouldClickReview() {
        basePageSteps.onGarageCardPage().articlesAndReviews().get(0).click();

        basePageSteps.switchToTab(1);
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(ARTICLE_FROM_GARAGE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в «Посмотреть публичный профиль»")
    public void shouldWatchPublicProfileUrl() {
        basePageSteps.onGarageCardPage().leftColumn().button(WATCH_PUBLIC_PROFILE).should(hasAttribute(HREF,
                urlSteps.testing().path(GARAGE).path(DREAMCAR).path(SHARE).path(CARD_ID).toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем модификацию авто мечты в гараже")
    public void shouldEditModification() {
        mockRule.setStubs(stub("desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010Sedan23172670")).update();
        basePageSteps.onGarageCardPage().button(ALL_PARAMETERS).click();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", MODIFICATION, HP_228)).click();

        mockRule.overwriteStub(1, stub("desktop/SearchCarsBreadcrumbsAudi_A3_20785010_2320785541_20786020"));
        mockRule.overwriteStub(4, stub("desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010Sedan20786020"));
        basePageSteps.onGarageCardPage().form().block(MODIFICATION).radioButton(HP_150).click();

        mockRule.setStubs(stub("desktop/GarageUserCardDreamCarPutAudi_A3_20786020")).update();
        mockRule.overwriteStub(2, stub("desktop/GarageUserCardDreamCarGetAudi_A3_20786020"));
        basePageSteps.onGarageCardPage().submitButton().click();

        basePageSteps.onGarageCardPage().vehicleInfoText("Двигатель").should(hasText("2,0 л / 150 л.с."));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем авто мечты в гараже на другую марку")
    public void shouldEditCar() {
        chooseKiaOptima();

        mockRule.overwriteStub(1, stub("desktop/SearchCarsBreadcrumbsKiaOptima21342050"));
        basePageSteps.onGarageCardPage().form().block(GENERATION).radioButton("2018 - 2020 IV Рестайлинг").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        mockRule.setStubs(stub("desktop/GarageUserCardDreamCarPutKia_Optima_21342050")).update();
        mockRule.overwriteStub(2, stub("desktop/GarageUserCardDreamCarGetKia_Optima_21342050"));
        basePageSteps.onGarageCardPage().submitButton().click();

        basePageSteps.onGarageCardPage().h1().should(hasText("Kia Optima"));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Сохранить» задизейблена до выбора поколения при редактировании")
    public void shouldSeeDisabledSaveButton() {
        chooseKiaOptima();

        basePageSteps.onGarageCardPage().submitButton().should(hasAttribute("disabled", "true"));
    }

    private void chooseKiaOptima() {
        mockRule.setStubs(stub("desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010Sedan23172670")).update();
        basePageSteps.onGarageCardPage().button(ALL_PARAMETERS).click();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", MARK, "Audi")).click();

        mockRule.overwriteStub(1, stub("desktop/SearchCarsBreadcrumbsKia"));
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MARK).item("Kia").waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        mockRule.overwriteStub(1, stub("desktop/SearchCarsBreadcrumbsKiaOptima"));
        mockRule.setStubs(stub("desktop/ReferenceCatalogCarsSuggestKiaOptima")).update();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MODEL).radioButton("Optima").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

}
