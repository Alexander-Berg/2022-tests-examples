package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockSite;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.FAVORITE_TYPE_PARAM;
import static ru.yandex.realty.step.UrlSteps.SITE_URL_VALUE;

@Issue("VERTISTEST-1355")
@Epic(RealtyFeatures.FAVORITES)
@Feature(NEWBUILDING)
@DisplayName("Состояния новостройки в избранном")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NewbuildingStatesFavoritesTest {

    private static final int INT_ID = 123456;
    private static final String UNFINISHED = "UNFINISHED";

    private MockSite site;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Parameterized.Parameter
    public String stateName;

    @Parameterized.Parameter(1)
    public String state;

    @Parameterized.Parameter(2)
    public boolean finished;

    @Parameterized.Parameters(name = "Состояние «{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Строится, есть сданные", UNFINISHED, true},
                {"Строится", UNFINISHED, false},
                {"Сдан", "HAND_OVER", true},
                {"Стройка заморожена", "CONSTRUCTION_SUSPENDED", false}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.favoritesStub(favoritesTemplate().addItem(format("site_%s", INT_ID)).build());
        site = mockSite(SITE_TEMPLATE).setId(INT_ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Состояния новостройки в избранном")
    public void shouldSeeSiteState() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                site.setState(state).setFinishedApartments(finished))).buildSite())
                .createWithDefaults();
        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, SITE_URL_VALUE).open();

        basePageSteps.onFavoritesPage().offersList().get(FIRST).state().should(hasText(stateName));
    }

}
