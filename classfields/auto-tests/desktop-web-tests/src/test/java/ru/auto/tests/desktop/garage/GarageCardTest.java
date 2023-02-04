package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.component.WithButton.CHANGE;
import static ru.auto.tests.desktop.component.WithButton.SAVE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SHARE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.HIMKI;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.element.garage.CardForm.CARD_REGION_PLACEHOLDER;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_COLOR;
import static ru.auto.tests.desktop.element.garage.CardForm.COMPLECTATION;
import static ru.auto.tests.desktop.element.garage.CardForm.DATE_OF_PURCHASE;
import static ru.auto.tests.desktop.element.garage.CardForm.MILEAGE;
import static ru.auto.tests.desktop.element.garage.CardForm.REGION_FIELD;
import static ru.auto.tests.desktop.element.garage.CardForm.YEAR;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.ADD_NEW_CAR;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.DELETE_FROM_GARAGE;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.SALE;
import static ru.auto.tests.desktop.element.garage.CardLeftColumn.WATCH_PUBLIC_PROFILE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.page.GarageCardPage.ALL_PARAMETERS;
import static ru.auto.tests.desktop.page.GaragePage.ADD_CAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Карточка своего автомобиля")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageCardTest {

    private static final String VIN_CARD_ID = "/1146321503/";
    private static final String MARK = "volkswagen/";
    private static final String MODEL = "jetta/";
    private static final String GEN = "7355324/";
    private static final String DEFAULT_CITY = "Москва";
    private static final String MAIN_REGION = "Москва и Московская область";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin"),
                stub("desktop/ReferenceCatalogCarsSuggestVolkswagenJetta")
        ).create();

        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID)
                .addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Редирект с главной на карточку")
    public void shouldRedirectToCard() {
        urlSteps.testing().path(GARAGE).open();
        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение карточки")
    public void shouldSeeCard() {
        basePageSteps.onGarageCardPage().header().should(isDisplayed());
        basePageSteps.onGarageCardPage().leftColumn().should(isDisplayed());
        basePageSteps.onGarageCardPage().recalls().should(isDisplayed());
        basePageSteps.onGarageCardPage().footer().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Посмотреть публичный профиль»")
    public void shouldClickSeePublicProfileUrl() {
        mockRule.setStubs(stub("desktop/GarageCardVin")).update();

        basePageSteps.onGarageCardPage().leftColumn().button(WATCH_PUBLIC_PROFILE).click();
        urlSteps.testing().path(GARAGE).path(SHARE).path(VIN_CARD_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Продать»")
    public void shouldClickSellButton() {
        mockRule.setStubs(stub("desktop/UserDraftCarsEmpty"),
                stub("desktop/UserDraftCarsPut")).update();

        basePageSteps.onGarageCardPage().leftColumn().button(SALE).click();
        urlSteps.testing().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Добавить новый автомобиль»")
    public void shouldClickAddButton() {
        mockRule.setStubs(stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")).update();

        basePageSteps.onGarageCardPage().leftColumn().button(ADD_NEW_CAR).click();
        urlSteps.testing().path(GARAGE).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Удалить из гаража»")
    public void shouldClickDeleteButton() {
        mockRule.delete();
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/GarageUserCardDelete"),
                stub("desktop/GarageUserCardsPostEmpty")).create();

        basePageSteps.onGarageCardPage().leftColumn().button(DELETE_FROM_GARAGE).click();
        basePageSteps.acceptAlert();
        urlSteps.testing().path(GARAGE).shouldNotSeeDiff();
        basePageSteps.onGaragePage().addBlock().button(ADD_CAR).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Подписка/отписка на отзывные кампании")
    public void shouldSubscribeToRecalls() {
        mockRule.setStubs(stub("desktop/RecallsUserCardsSubscriptionPut"),
                stub("desktop/RecallsUserCardsSubscriptionDelete")).update();

        basePageSteps.onGarageCardPage().recalls().button("Отписаться").click();
        basePageSteps.onGarageCardPage().notifier().waitUntil(hasText("Подписка отключена"));
        basePageSteps.onGarageCardPage().recalls().status().should(hasText("Подписаться на обновления"));
        basePageSteps.onGarageCardPage().recalls().button("Подписаться на обновления").click();
        basePageSteps.onGarageCardPage().notifier().waitUntil(hasText("Подписка добавлена"));
        basePageSteps.onGarageCardPage().recalls().status().should(hasText("Вы подписаны на обновления\nОтписаться"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Все параметры» и редактирование")
    public void shouldClickAllParamsButtonAndEdit() {
        mockRule.setStubs(stub("desktop/GarageUserCardPut"),
                stub("desktop/GeoSuggest")).update();

        basePageSteps.onGarageCardPage().button(ALL_PARAMETERS).click();
        basePageSteps.onGarageCardPage().button(CHANGE).waitUntil(not(isDisplayed()));
        basePageSteps.onGarageCardPage().form().waitUntil(isDisplayed());

        basePageSteps.onGarageCardPage().form().block(format("%s%s", REGION_FIELD, DEFAULT_CITY)).click();
        basePageSteps.onGarageCardPage().form().input(CARD_REGION_PLACEHOLDER, HIMKI);
        basePageSteps.onGarageCardPage().form().geoSuggest().region(format("%s%s", HIMKI,
                MAIN_REGION)).waitUntil(isDisplayed()).click();
        basePageSteps.onGarageCardPage().form().unfoldedBlock(COMPLECTATION)
                .radioButton("Comfortline (36 опций)").click();
        basePageSteps.onGarageCardPage().form().unfoldedBlock(CHOOSE_COLOR).color("EE1D19").click();
        basePageSteps.onGarageCardPage().form().unfoldedBlock(MILEAGE)
                .input(MILEAGE, "100000");
        basePageSteps.onGarageCardPage().form().unfoldedBlock(DATE_OF_PURCHASE)
                .input(YEAR, "2021");
        basePageSteps.onGarageCardPage().form().button(SAVE).click();
        basePageSteps.onGarageCardPage().form().waitUntil(not(isDisplayed()));

        basePageSteps.onGarageCardPage().button(ALL_PARAMETERS).should(isDisplayed());
    }
}
