package ru.yandex.realty.developer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockDeveloper;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. СЕО.")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoDeveloperTest {

    private static final String TITLE_WITH_OFFERS = "Новостройки от 4D в Тюменской области – 5 жилых комплексов " +
            "от застройщика 4D от 1450000 руб. на Яндекс.Недвижимости";
    private static final String TITLE_WITHOUT_OFFERS = "Новостройки от 4D в Тюменской области – 5 жилых комплексов " +
            "от застройщика 4D на Яндекс.Недвижимости";
    private static final String DESCRIPTION_WITH_OFFERS = "✅ Купить квартиру в Тюменской области в новостройках от " +
            "застройщика 4D на Яндекс.Недвижимости. Свежие объявления в 5 жилых комплексах от застройщика 4D";
    private static final String DESCRIPTION_WITHOUT_OFFERS = "✅ Купить квартиру в Тюменской области в новостройках от " +
            "застройщика 4D на Яндекс.Недвижимости.";
    private static final String H1 = "4D\\nв Тюменской области";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockDeveloper developer;

    @Parameterized.Parameter(2)
    public String title;

    @Parameterized.Parameter(3)
    public String description;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Застройщик с офферами", mockEnhancedDeveloper(), TITLE_WITH_OFFERS,
                        DESCRIPTION_WITH_OFFERS},
                {"Застройщик без офферов", mockEnhancedDeveloper().removeOfferStatistic(),
                        TITLE_WITHOUT_OFFERS, DESCRIPTION_WITHOUT_OFFERS}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID, developer.build()).createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сео тайтл")
    public void shouldSeeSeoTitle() {
        assertThat("Тайтл соответствует", basePageSteps.getDriver().getTitle(), findPattern(title));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сео описание")
    public void shouldSeeSeoDescription() {
        basePageSteps.onDeveloperPage().seoDescription().should(hasAttribute("content", findPattern(description)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("H1")
    public void shouldSeeH1() {
        basePageSteps.onDeveloperPage().h1().should(hasText(findPattern(H1)));
    }
}
