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
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockGarageCard.EX_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.page.GarageAddPage.ADD_TO_GARAGE;
import static ru.auto.tests.desktop.page.GarageAddPage.PUT_INTO_GARAGE;
import static ru.auto.tests.desktop.page.GarageAddPage.PUT_VIN;
import static ru.auto.tests.desktop.page.GarageCardPage.MY_EX;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;


@DisplayName("Добавляем бывшую в гараж")
@Feature(AutoruFeatures.GARAGE)
@Story(AutoruFeatures.EX_CAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageAddExCarTest {

    private static final String VIN = "WVWZZZ16ZBM121912";
    private static final String GARAGE_CARD_ID = "1146321503";

    private static final String MY_EX_CAR_BLOCK_TEXT = "Моя бывшая\nПоставьте и узнайте, как она там без вас\n" +
            "Введите VIN\nПоставить\nVolkswagen Jetta\nГод\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет\nСерый\nVIN\n" +
            "WVWZZZ16*BM*****2\nДобавить в гараж";

    private static final String VIN_POPUP_TEXT = "Где смотреть\nVIN вы найдёте в строке «Идентификационный номер» " +
            "в вашем Свидетельстве о регистрации транспортного средства (СТС).";

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
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest")
        ).create();

        urlSteps.testing().path(GARAGE).path(ADD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по кнопке «?» в блоке добавления бывшего авто")
    public void shouldClickQuestionButtonExCar() {
        basePageSteps.onGarageAddPage().myExCarBlock().questionButton().waitUntil(isDisplayed()).click();

        basePageSteps.onGarageAddPage().popup().should(hasText(VIN_POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление бывшего авто по VIN")
    public void shouldAddVinExCar() {
        mockRule.setStubs(
                stub("desktop/GarageUserVehicleInfoVin"),
                stub("desktop/GarageUserCardIdentifierVin"),
                stub("desktop/GarageUserCardsVinPost"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setCardType(EX_CAR).getBody())
        ).update();

        basePageSteps.onGarageAddPage().myExCarBlock().input(PUT_VIN, VIN);
        basePageSteps.onGarageAddPage().myExCarBlock().button(PUT_INTO_GARAGE).click();
        urlSteps.shouldNotSeeDiff();

        basePageSteps.onGarageAddPage().myExCarBlock().waitUntil(hasText(MY_EX_CAR_BLOCK_TEXT));
        basePageSteps.onGarageAddPage().myExCarBlock().button(ADD_TO_GARAGE).click();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().gallery().badge(MY_EX).should(isDisplayed());
    }

}
