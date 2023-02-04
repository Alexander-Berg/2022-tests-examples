package ru.auto.tests.desktop.complain;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.component.WithButton.SEND;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPLAIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Жалобы на карточке объявления")
@Feature(COMPLAIN)
@Story("Карточка оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ComplainTwoReasonsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String RESELLER = "Перекупщик или салон";
    private static final String RESELLER_INPUT = "Как вы об этом узнали?";
    private static final String RESELLER_TEXT = "reseller";
    private static final String PHOTO = "Фото не соответствует";
    private static final String PHOTO_INPUT = "Что не так с фото?";
    private static final String PHOTO_TEXT = "wrong photo";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/OfferCarsComplaintsResellerReason",
                "desktop/OfferCarsComplaintsWrongPhotoReason").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Жалоба на объявление с описанием - две причины")
    public void shouldClickComplainButtonWithTwoDescriptions() {
        basePageSteps.onCardPage().cardHeader().toolBar().complainButton().hover().click();
        basePageSteps.onCardPage().popup().checkbox(RESELLER).click();
        basePageSteps.onCardPage().popup().input(RESELLER_INPUT, RESELLER_TEXT);
        basePageSteps.onCardPage().popup().checkbox(PHOTO).click();
        basePageSteps.onCardPage().popup().input(PHOTO_INPUT, PHOTO_TEXT);
        basePageSteps.onCardPage().popup().button(SEND).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Спасибо, мы всё проверим"));
    }
}