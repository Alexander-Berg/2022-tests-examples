package ru.yandex.realty.newbuilding.card;

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
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExpandableNewBuildingCardTest {

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
    public String section;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Описание ЖК"},
                {"Ипотека и скидки"},
                {"Расположение"},
                {"Отзывы"}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сворачиваем и разворчиваем секции аккордеона")
    public void shouldSeeNewBuildingCardWithFoldedCards() {
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().cardSection(section));
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().cardSection(section));
        basePageSteps.onNewBuildingCardPage().cardSection(section).newbuildingContentHidden().should(exists());
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().cardSection(section));
        basePageSteps.onNewBuildingCardPage().cardSection(section).newbuildingContent().should(exists());
        basePageSteps.scrollElementToCenter(basePageSteps.onNewBuildingCardPage().cardSection(section));
        basePageSteps.moveCursorAndClick(basePageSteps.onNewBuildingCardPage().cardSection(section));
        basePageSteps.onNewBuildingCardPage().cardSection(section).newbuildingContentHidden().should(exists());

    }
}
