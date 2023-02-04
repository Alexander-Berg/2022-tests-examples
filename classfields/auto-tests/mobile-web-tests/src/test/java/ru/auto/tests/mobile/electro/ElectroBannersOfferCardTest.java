package ru.auto.tests.mobile.electro;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.GASOLINE;
import static ru.auto.tests.desktop.consts.QueryParams.HYBRID;
import static ru.auto.tests.desktop.element.card.Benefits.ELECTROCAR;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Баннеры на карточке")
@DisplayName("Баннеры на карточке")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroBannersOfferCardTest {

    private static final String SALE_ID = "1114782187-3302e085";
    private static final String ELECTRO_POPUP = "Электромобили\nУ таких автомобилей нет транспортного " +
            "налога, а электричество дешевле бензина\nДавайте посмотрим";
    private static final String LETS_SEE = "Давайте посмотрим";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап «Электромобили» в карточке электромобиля")
    public void shouldSeeElectroBannerOnCard() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).setEngineType(QueryParams.ELECTRO)
                                .getResponse()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH).open();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit(ELECTROCAR));
        basePageSteps.onCardPage().popup().waitUntil(hasText(ELECTRO_POPUP));
        basePageSteps.onCardPage().popup().button(LETS_SEE).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап «Электромобили» в карточке гибридного авто")
    public void shouldSeeElectroBannerOnCardHybrid() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).setEngineType(HYBRID).getResponse()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH).open();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().benefits().benefit(ELECTROCAR));
        basePageSteps.onCardPage().popup().waitUntil(hasText(ELECTRO_POPUP));
        basePageSteps.onCardPage().popup().button(LETS_SEE).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет преимущества «Электромобиль» в карточке бензинового авто")
    public void shouldNotSeeElectroBenefitOnBenzineCard() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).setEngineType(GASOLINE).getResponse()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH).open();

        basePageSteps.onCardPage().benefits().benefit(ELECTROCAR).should(not(isDisplayed()));
    }

}
