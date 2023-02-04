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
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Гараж")
@Story("Страхование")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageInsuranceBlockTest {

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
                "desktop/GarageUserCardWithInsurances").post();

        urlSteps.testing().path(GARAGE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны видеть только активные страховки")
    public void shouldSeeOnlyActiveInsurance() {
        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nОСАГО [\\w ]+ (день|дней|дня)+" +
                "\\nНазвание страховой компании\\nРога и копыта\\nСерия и номер\\nXXX 111\\nПри ДТП звонить\\n" +
                "\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2050\\n" +
                "КАСКО [\\w ]+ (день|дней|дня)+\\nНазвание страховой компании\\nРога и копыта\\nНомер\\nXXX111\\nПри ДТП " +
                "звонить\\n\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2049\\n" +
                "Архив страховок\\nДобавить страховку")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны видеть активные и неактивные страховки")
    public void shouldSeeActiveAndInactiveInsurance() {
        basePageSteps.onGarageCardPage().insurances().showArchive().click();
        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nОСАГО [\\w ]+ (день|дней|дня)+" +
                "\\nНазвание страховой компании\\nРога и копыта\\nСерия и номер\\nXXX 111\\nПри ДТП звонить" +
                "\\n\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2050\\n" +
                "КАСКО [\\w ]+ (день|дней|дня)+\\nНазвание страховой компании\\nРога и копыта\\nНомер\\nXXX111\\nПри ДТП звонить" +
                "\\n\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2049\\nОСАГО истёк" +
                "\\nНазвание страховой компании\\nРога и копыта\\nСерия и номер\\nXXX 222\\nПри ДТП звонить\\n" +
                "\\+71112221111\\nНачало действия\\n1 января 2020\\nОкончание действия\\n31 декабря 2020" +
                "\nДобавить страховку")));
    }
}