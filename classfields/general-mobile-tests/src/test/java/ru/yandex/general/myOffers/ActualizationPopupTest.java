package ru.yandex.general.myOffers;

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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.element.ActualizationBlock.IS_ACTUAL;
import static ru.yandex.general.mobile.element.ActualizationBlock.OFFER_WILL_RAISE;
import static ru.yandex.general.mobile.element.Image.SRC;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.ACTUALIZE;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.REZUME_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature("Актуальность оффера")
@DisplayName("Попап актуализации")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class ActualizationPopupTest {

    private static final String IS_OFFER_ACTUAL = "Ваше объявление ещё актуально?";
    private static final long PRICE = 31500;
    private static final String SALLARY = "121000";
    private static final String TITLE = "Название оффера";
    private static final String PHOTO = "https://avatars.mdst.yandex.net/get-o-yandex/65675/7a9759667ae07ff3eeb5fa26cd02e2c4/";
    private static final String YES = "Да";
    private static final String NO = "Нет";

    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер с указанной ценой в попапе актуализации")
    public void shouldSeeOfferPriceInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setPrice(PRICE).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().price().should(hasText(formatPrice(PRICE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер с ценой даром в попапе актуализации")
    public void shouldSeeOfferFreePriceInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setFreePrice().setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().price().should(hasText("Даром"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер с не указанной ценой в попапе актуализации")
    public void shouldSeeOfferUnsetPriceInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setUnsetPrice().setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().price().should(hasText("Не указана"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер с указанной зарплатой в попапе актуализации")
    public void shouldSeeOfferSallaryInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(REZUME_SNIPPET).setSallaryPrice(SALLARY).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().price().should(hasText(formatPrice(SALLARY)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер с не указанной зарплатой в попапе актуализации")
    public void shouldSeeOfferUnsetSallaryInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(REZUME_SNIPPET).setUnsetPrice().setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().price().should(hasText("Не указана"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл оффера в попапе актуализации")
    public void shouldSeeOfferTitleInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setTitle(TITLE).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().title().should(hasText(TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фото оффера в попапе актуализации")
    public void shouldSeeOfferPhotoInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).addPhoto(1).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().image().should(
                hasAttribute(SRC, containsString(PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Каунтер фото оффера в попапе актуализации")
    public void shouldSeeOfferPhotoCounterInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).addPhoto(20).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().photoCounter().should(hasText("20"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет фото оффера в попапе актуализации")
    public void shouldSeeOfferNoPhotoInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).removePhotos().setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).offer().noPhoto().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливается кука при показе попапа актуализации")
    public void shouldSeeInExpiredDialogWasShownCookieActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).waitUntil(isDisplayed());

        basePageSteps.shouldSeeCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается попап актуализации с кукой «classified_expired_dialog_was_shown»")
    public void shouldSeeNoActualizationPopupWithExpiredDialogWasShownCookie() {
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается попап актуализации без офферов требующих актуализацию")
    public void shouldSeeNoActualizationPopupWithoutExpiredOffers() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, false))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Да» в попапе актуализации")
    public void shouldClickYesInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).button(YES).click();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().actualizationBlock().text().should(hasText(OFFER_WILL_RAISE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Нет» в попапе актуализации")
    public void shouldClickNoInActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).button(NO).click();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().popup("Укажите причину снятия").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап актуализации по крестику")
    public void shouldCloseActualizationPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).closeFloatPopup().click();
        basePageSteps.onMyOffersPage().popup(IS_OFFER_ACTUAL).should(not(isDisplayed()));
    }

}
