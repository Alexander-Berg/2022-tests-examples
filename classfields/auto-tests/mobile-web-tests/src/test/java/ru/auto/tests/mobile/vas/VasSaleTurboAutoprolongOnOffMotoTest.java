package ru.auto.tests.mobile.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Включение/выключение автопродления VAS на карточке")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasSaleTurboAutoprolongOnOffMotoTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferMotoUsedUserOwnerWithServices",
                "desktop/SessionAuthUser",
                "desktop/UserWithTiedCard",
                "desktop/UserOffersMotoProductPackageTurboProlongablePut",
                "desktop/UserOffersMotoProductPackageTurboProlongableDelete").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение автопродления")
    public void shouldOnOffAutoprolong() {
        mockRule.overwriteStub(0, "desktop/OfferMotoUsedUserOwnerWithAutoprolongServices");

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().activeService("Турбо-продажа")
                .autoprolongTogglerInactive().waitUntil(isDisplayed()));
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Автопродление включено"));

        mockRule.overwriteStub(0, "desktop/OfferMotoUsedUserOwnerWithServices");

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().activeService("Турбо-продажа")
                .autoprolongTogglerActive().waitUntil(isDisplayed()));
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Автопродление отключено"));
        basePageSteps.onCardPage().vas().activeService("Турбо-продажа").autoprolongTogglerInactive()
                .waitUntil(isDisplayed());
    }
}
