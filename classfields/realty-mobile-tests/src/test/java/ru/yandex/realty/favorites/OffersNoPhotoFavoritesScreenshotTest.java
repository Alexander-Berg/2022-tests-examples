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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockOffer.*;
import static ru.yandex.realty.mock.UserOfferByIdV15MockResponse.userOfferByIdV15Template;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1355")
@Epic(RealtyFeatures.FAVORITES)
@Feature(OFFERS)
@DisplayName("Скриншот сниппета оффера без фото в избранном")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OffersNoPhotoFavoritesScreenshotTest {

    private static final String OFFER_ID = "123456";

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String mock;

    @Parameterized.Parameters(name = "Тип оффера «{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Продажа квартиры", SELL_APARTMENT},
                {"Продажа квартиры в новостройке вторичка", SELL_NEW_BUILDING_SECONDARY},
                {"Продажа дома", SELL_HOUSE},
                {"Продажа таунхауза", SELL_TOWN_HOUSE},
                {"Продажа дуплекса", SELL_DUPLEX},
                {"Продажа части дома", SELL_PART_HOUSE},
                {"Продажа гаража", SELL_GARAGE},
                {"Продажа комнаты", SELL_ROOM},
                {"Продажа новой вторички", SELL_NEW_SECONDARY},
                {"Продажа участка", SELL_LOT},
                {"Продажа коммерческой земли", SELL_COMMERCIAL},
                {"Продажа склада", SELL_COMMERCIAL_WAREHOUSE},
                {"Продажа офиса", SELL_COMMERCIAL_OFFICE},
                {"Продажа торгового помещения", SELL_COMMERCIAL_RETAIL},
                {"Продажа помещения свободного назначения", SELL_COMMERCIAL_FREE_PURPOSE},
                {"Продажа производственного помещения", SELL_COMMERCIAL_MANUFACTURING},
                {"Продажа общепита", SELL_COMMERCIAL_PUBLIC_CATERING},
                {"Продажа готового бизнеса", SELL_COMMERCIAL_BUSINESS},
                {"Аренда квартиры", RENT_APARTMENT},
                {"Посуточная аренда квартиры", RENT_BY_DAY},
                {"Аренда дома", RENT_HOUSE},
                {"Аренда комнаты", RENT_ROOM},
                {"Аренда гаража", RENT_GARAGE},
                {"Аренда коммерческого участка", RENT_COMMERCIAL},
                {"Аренда склада", RENT_COMMERCIAL_WAREHOUSE},
                {"Аренда офиса", RENT_COMMERCIAL_OFFICE},
                {"Аренда торгового помещения", RENT_COMMERCIAL_RETAIL},
                {"Аренда помещения свободного назначения", RENT_COMMERCIAL_FREE_PURPOSE},
                {"Аренда производственного помещения", RENT_COMMERCIAL_MANUFACTURING},
                {"Аренда общепита", RENT_COMMERCIAL_PUBLIC_CATERING},
                {"Аренда автосервиса", RENT_COMMERCIAL_AUTO_REPAIR},
                {"Аренда гостиницы", RENT_COMMERCIAL_HOTEL},
                {"Аренда готового бизнеса", RENT_COMMERCIAL_BUSINESS},
                {"Аренда юридического адреса", RENT_COMMERCIAL_LEGAL_ADDRESS}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот сниппета оффера без фото в избранном")
    public void shouldSeeNoPhotoScreenshot() {
        mockRuleConfigurable.favoritesStub(favoritesTemplate().addItem(OFFER_ID).build())
                .getOffersByIdStub(userOfferByIdV15Template().offers(asList(
                        mockOffer(mock).setOfferId(OFFER_ID).clearPhotos())).build()).createWithDefaults();
        urlSteps.testing().path(FAVORITES).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().offersList().get(FIRST));

        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().offersList().get(FIRST));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
