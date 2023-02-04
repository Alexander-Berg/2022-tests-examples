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

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SHARE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Публичная карточка гаража")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageShareCardTest {

    private static final String VIN_CARD_ID = "/1146321503/";

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
                "desktop/GarageCardVin",
                "desktop/GarageUserCardsVinPost").post();

        urlSteps.testing().path(GARAGE).path(SHARE).path(VIN_CARD_ID).open();
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
    @DisplayName("Клик по кнопке «Добавить новый автомобиль»")
    public void shouldClickAddButton() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onGarageCardPage().leftColumn().button("Добавьте свой автомобиль в Гараж").click();
        urlSteps.testing().path(GARAGE).path(ADD).shouldNotSeeDiff();
    }
}