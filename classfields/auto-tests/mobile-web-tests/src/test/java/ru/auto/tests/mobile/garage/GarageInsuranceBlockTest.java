package ru.auto.tests.mobile.garage;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Гараж")
@Story("Страхование")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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
        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nОСАГО до 31 декабря " +
                "2050\\nXXX 111\\n[\\w ]+ (день|дня|дней)+\\nКАСКО до 31 декабря 2049\\nXXX111\\n[\\w ]+ (день|дня|дней)" +
                "+\\nАрхив страховок\\nДобавить полис")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны видеть активные и неактивные страховки")
    public void shouldSeeActiveAndInactiveInsurance() {
        basePageSteps.onGarageCardPage().insurances().showArchive().click();
        basePageSteps.onGarageCardPage().insurances().should(hasText(matchesRegex("Страховки\\nОСАГО до 31 декабря " +
                "2050\\nXXX 111\\n[\\w ]+ (день|дня|дней)+\\nКАСКО до 31 декабря 2049\\nXXX111\\n[\\w ]+ (день|дня|дней)" +
                "+\\nОСАГО до 31 декабря 2020\\nXXX 222\\nистёк\\nДобавить полис")));
    }
}
