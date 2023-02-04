package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGES;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockVillage.VILLAGE_COTTAGE;
import static ru.yandex.realty.mock.MockVillage.mockVillage;
import static ru.yandex.realty.mock.VillageSearchResponse.villageSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.FAVORITE_TYPE_PARAM;
import static ru.yandex.realty.step.UrlSteps.VILLAGE_URL_VALUE;

@Issue("VERTISTEST-1355")
@Epic(RealtyFeatures.FAVORITES)
@Feature(VILLAGES)
@DisplayName("Класс КП в избранном")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VillagesClassFavoritesTest {

    private static final String STRING_ID = "123456";

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
    public String title;

    @Parameterized.Parameter(1)
    public String className;

    @Parameterized.Parameter(2)
    public String villageClass;

    @Parameterized.Parameters(name = "Класс «{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Эконом", "Эконом", "ECONOMY"},
                {"Комфорт", "Комфорт", "COMFORT"},
                {"Комфорт плюс", "Комфорт+", "COMFORT_PLUS"},
                {"Бизнес", "Бизнес", "BUSINESS"},
                {"Элитный", "Элитный", "ELITE"},
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Класс КП в избранном")
    public void shouldSeeVillageClass() {
        mockRuleConfigurable.favoritesStub(favoritesTemplate().addItem(format("village_%s", STRING_ID)).build())
                .villageSearchStub(villageSearchTemplate().villages(asList(
                        mockVillage(VILLAGE_COTTAGE).setId(STRING_ID).setVillageClass(villageClass))).build())
                .createWithDefaults();
        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, VILLAGE_URL_VALUE).open();

        basePageSteps.onFavoritesPage().offersList().get(FIRST).villageClass().should(
                hasText(containsString(className)));
    }

}
