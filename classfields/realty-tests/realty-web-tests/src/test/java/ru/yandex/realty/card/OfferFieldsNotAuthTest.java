package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.PREMIUM;
import static ru.yandex.realty.mock.MockOffer.PROMOTED;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL_WAREHOUSE;
import static ru.yandex.realty.mock.MockOffer.RENT_GARAGE;
import static ru.yandex.realty.mock.MockOffer.RENT_HOUSE;
import static ru.yandex.realty.mock.MockOffer.RENT_ROOM;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.SELL_GARAGE;
import static ru.yandex.realty.mock.MockOffer.SELL_HOUSE;
import static ru.yandex.realty.mock.MockOffer.SELL_LOT;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_BUILDING_SECONDARY;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_SECONDARY;
import static ru.yandex.realty.mock.MockOffer.SELL_ROOM;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Проверка карточки оффера со стороны покупателя. Не залогинен")
@Feature(OFFERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferFieldsNotAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public MockOffer offer;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", mockOffer(SELL_APARTMENT)},
                {"Купить новостроечную вторичку", mockOffer(SELL_NEW_BUILDING_SECONDARY).setExtImages()},
                {"Купить новую вторичку", mockOffer(SELL_NEW_SECONDARY).setPredictions()},
                {"Купить комнату", mockOffer(SELL_ROOM).setService(PROMOTED)},
                {"Купить дом", mockOffer(SELL_HOUSE).setService(RAISED)},
                {"Купить участок", mockOffer(SELL_LOT)},
                {"Купить гараж", mockOffer(SELL_GARAGE)},
                {"Купить коммерческую", mockOffer(SELL_COMMERCIAL).setService(TURBOSALE).setExtImages()},
                {"Снять квартиру", mockOffer(RENT_APARTMENT).setService(PREMIUM).setService(PROMOTED).setExtImages()},
                {"Снять комнату", mockOffer(RENT_ROOM).setPredictions()},
                {"Снять дом", mockOffer(RENT_HOUSE)},
                {"Снять гараж", mockOffer(RENT_GARAGE)},
                {"Снять коммерческую", mockOffer(RENT_COMMERCIAL_WAREHOUSE).setService(PREMIUM)}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .blogPostsStub()
                .createWithDefaults();
        compareSteps.resize(1600, 7000);
        basePageSteps.disableAd();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
    }


    //    @Ignore("Нужно поправить, мешает журнал и какие-то сдвиги на 1 пксл")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скрин оффера")
    public void shouldSeeBaseInfo() {
        Screenshot testing = compareSteps.takeScreenshot(user.onOfferCardPage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(user.onOfferCardPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
