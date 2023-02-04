package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockVillage;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
@DisplayName("Статусы КП в избранном")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class VillagesStatusFavoritesTest {

    private static final String STRING_ID = "123456";

    private MockVillage village;

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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        mockRuleConfigurable.favoritesStub(favoritesTemplate().addItem(format("village_%s", STRING_ID)).build());
        village = mockVillage(VILLAGE_COTTAGE).setId(STRING_ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Статус КП «Сдан»")
    public void shouldSeeVillageStatusHandOver() {
        mockRuleConfigurable.villageSearchStub(villageSearchTemplate().villages(asList(
                village.setDeliveryDatesStatus("HAND_OVER"))).build())
                .createWithDefaults();
        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, VILLAGE_URL_VALUE).open();

        basePageSteps.onFavoritesPage().offersList().get(FIRST).deliveryDates().should(hasText("Сдан"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cтатус КП «Стройка заморожена»")
    public void shouldSeeVillageStatusSuspended() {
        mockRuleConfigurable.villageSearchStub(villageSearchTemplate().villages(asList(
                village.setDeliveryDatesStatus("SUSPENDED"))).build())
                .createWithDefaults();
        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, VILLAGE_URL_VALUE).open();

        basePageSteps.onFavoritesPage().offersList().get(FIRST).deliveryDates().should(hasText("Стройка заморожена"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот статуса КП «Заморожен»")
    public void shouldSeeVillageStatusSuspendedScreenshot() {
        mockRuleConfigurable.villageSearchStub(villageSearchTemplate().villages(asList(
                village.setDeliveryDatesStatus("SUSPENDED"))).build())
                .createWithDefaults();
        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, VILLAGE_URL_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().offersList().get(FIRST));

        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().offersList().get(FIRST));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
