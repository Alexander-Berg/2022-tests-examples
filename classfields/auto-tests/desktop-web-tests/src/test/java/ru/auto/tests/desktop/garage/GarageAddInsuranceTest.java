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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.InsuranceForm.NAME_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.NUMBER_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.PHONE_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.SERIAL_AND_NUMBER_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.TYPE_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.VALID_FROM_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.VALID_TO_FIELD;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Страхование")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageAddInsuranceTest {

    private static final String CARD_ID = "/1146321503/";

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
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/GarageUserCardVin").post();

        urlSteps.testing().path(GARAGE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Добавление страховки «ОСАГО»")
    public void shouldAddInsuranceOsago() {
        mockRule.with("desktop/GarageUserCardAddInsuranceOsago").update();

        basePageSteps.onGarageCardPage().button("Добавить страховку").waitUntil(isDisplayed()).click();

        basePageSteps.onGarageCardPage().popup().waitUntil(isDisplayed());
        basePageSteps.onGarageCardPage().popup().selectItem(TYPE_FIELD, "ОСАГО");
        basePageSteps.onGarageCardPage().popup().input(NAME_FIELD, "Рога и копыта");
        basePageSteps.onGarageCardPage().popup().input(SERIAL_AND_NUMBER_FIELD, "XXX111");
        basePageSteps.onGarageCardPage().popup().input(PHONE_FIELD, "+71112221111");
        basePageSteps.onGarageCardPage().popup().input(VALID_FROM_FIELD, "01.01.2020");
        basePageSteps.onGarageCardPage().popup().input(VALID_TO_FIELD, "31.12.2020");

        mockRule.overwriteStub(2, "desktop/GarageUserCardOsago");

        basePageSteps.onGarageCardPage().popup().button("Сохранить").click();

        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nОСАГО [\\w ]+(день|дня|дней)+" +
                "\\nНазвание страховой компании\\nРога и копыта\\nСерия и номер\\nXXX 111\\nПри ДТП звонить" +
                "\\n\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2050" +
                "\nДобавить страховку")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Добавление страховки «КАСКО»")
    public void shouldAddInsuranceKasko() {
        mockRule.with("desktop/GarageUserCardAddInsuranceKasko").update();

        basePageSteps.onGarageCardPage().button("Добавить страховку").waitUntil(isDisplayed()).click();

        basePageSteps.onGarageCardPage().popup().waitUntil(isDisplayed());
        basePageSteps.onGarageCardPage().popup().selectItem(TYPE_FIELD, "КАСКО");
        basePageSteps.onGarageCardPage().popup().input(NAME_FIELD, "Рога и копыта");
        basePageSteps.onGarageCardPage().popup().input(NUMBER_FIELD, "XXX111");
        basePageSteps.onGarageCardPage().popup().input(PHONE_FIELD, "+71112221111");
        basePageSteps.onGarageCardPage().popup().input(VALID_FROM_FIELD, "01.01.2020");
        basePageSteps.onGarageCardPage().popup().input(VALID_TO_FIELD, "31.12.2020");

        mockRule.overwriteStub(2, "desktop/GarageUserCardKasko");

        basePageSteps.onGarageCardPage().popup().button("Сохранить").click();

        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nКАСКО [\\w ]+(день|дня|дней)+" +
                "\\nНазвание страховой компании\\nРога и копыта\\nНомер\\nXXX111\\nПри ДТП звонить\\n\\+71112221111" +
                "\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2050\\nДобавить страховку")));
    }
}