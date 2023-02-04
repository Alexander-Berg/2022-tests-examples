package ru.yandex.realty.showphone.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.mock.OfferPhonesResponse;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.element.map.Sidebar.SHOW_PHONE;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.mock.UserOfferByIdV15MockResponse.userOfferByIdV15Template;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_DASHES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Избранное - карта")
@Feature(MAP)
@Link("https://st.yandex-team.ru/VERTISTEST-1599")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class FavoriteMapShowPhoneTest {

    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";

    private MockOffer offer;
    private OfferPhonesResponse offersPhonesTemplate;
    private String offerId;

    @ProfsearchAccount
    @Inject
    private Account account;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        offerId = offer.getOfferId();
        passportSteps.login(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в избранном на карте")
    public void shouldSeePhoneNormalCaseFavMap() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .favoritesStub(favoritesTemplate().addItem(offerId).build())
                .getOffersByIdStub(userOfferByIdV15Template().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().favoriteSidebar().favoriteOffer().button(SHOW_PHONE).click();
        basePageSteps.onMapPage().showPhonePopup().phones()
                .should(hasSize(1)).get(0).should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_DASHES)));
    }

    @Ignore("Пока нет заглушки")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При 500-ке показ заглушки")
    public void shouldSeePhone500FavMap() {
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .favoritesStub(favoritesTemplate().addItem(offerId).build())
                .getOffersByIdStub(userOfferByIdV15Template().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerPhonesStub500(offer.getOfferId())
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().favoriteSidebar().favoriteOffer().button(SHOW_PHONE).click();
        basePageSteps.onMapPage().showPhonePopup().phoneError()
                .should(hasText("Не удалось получить номер,\nпопробуйте позже"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона с подменным шильдиком в листинге")
    public void shouldSeePhoneRedirectFavMap() {
        offer.addRedirectPhones(true);
        offersPhonesTemplate = offersPhonesTemplate().addPhoneWithRedirectId(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .favoritesStub(favoritesTemplate().addItem(offerId).build())
                .getOffersByIdStub(userOfferByIdV15Template().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().favoriteSidebar().favoriteOffer().button(SHOW_PHONE).click();
        basePageSteps.onMapPage().showPhonePopup().phoneProtectSign().should(isDisplayed());
        basePageSteps.onMapPage().showPhonePopup().phones()
                .should(hasSize(1)).get(0).should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_DASHES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов")
    public void shouldSeeTwoPhoneFavMap() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE).addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .favoritesStub(favoritesTemplate().addItem(offerId).build())
                .getOffersByIdStub(userOfferByIdV15Template().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().favoriteSidebar().favoriteOffer().button(SHOW_PHONE).click();
        basePageSteps.onMapPage().showPhonePopup().phones().should(hasSize(2));
        basePageSteps.onMapPage().showPhonePopup().phones().get(0)
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_DASHES)));
        basePageSteps.onMapPage().showPhonePopup().phones().get(1)
                .should(hasText(makePhoneFormatted(SECOND_TEST_PHONE, PHONE_PATTERN_DASHES)));
    }

}