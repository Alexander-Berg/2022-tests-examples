package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOS_BLOCK;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockGarageCard.EX_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockPromos.promos;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_PROMOS;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Нет блока «Акции и скидки» у бывшего авто")
@Epic(AutoruFeatures.GARAGE)
@Feature(PROMOS_BLOCK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageCardNoAllPromoBlockExCarTest {

    private static final String GARAGE_CARD_ID = getRandomId();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setCardType(EX_CAR).getBody()),
                stub().withGetDeepEquals(GARAGE_USER_PROMOS)
                        .withResponseBody(
                                promos().getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет блока «Акции и скидки» у бывшего авто")
    public void shouldSeeNoPromosExCar() {
        basePageSteps.onGarageCardPage().promos().should(not(isDisplayed()));
    }

}
