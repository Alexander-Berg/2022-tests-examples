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
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.REZUME_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(MY_OFFERS_FEATURE)
@Feature("Отображение стоимости")
@DisplayName("Отображение стоимости")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SnippetPriceTest {

    private static final long PRICE = 31500;
    private static final String SALLARY = "121000";
    private static final String EDIT_FORM_LINK = "/form/86050276474376192/";

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
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена")
    public void shouldSeePrice() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions().setPrice(PRICE))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().price().should(hasText(formatPrice(PRICE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Даром")
    public void shouldSeeFreePrice() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions().setFreePrice())).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().price().should(hasText("Даром"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена не указана")
    public void shouldSeeUnsetPrice() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions()
                        .setEditFormLink(EDIT_FORM_LINK)
                        .setUnsetPrice())).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().price().should(hasText("Указать стоимость"));
        basePageSteps.onMyOffersPage().snippetFirst().price().link().should(
                hasAttribute(HREF, urlSteps.testing().path(EDIT_FORM_LINK).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зарплата")
    public void shouldSeeSallary() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(REZUME_SNIPPET).setActiveOfferAvaliableActions().setSallaryPrice(SALLARY))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().price().should(hasText(formatPrice(SALLARY)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зарплата не указана")
    public void shouldSeeNoSallary() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(REZUME_SNIPPET).setActiveOfferAvaliableActions()
                        .setEditFormLink(EDIT_FORM_LINK)
                        .setUnsetPrice())).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().price().should(hasText("Указать стоимость"));
        basePageSteps.onMyOffersPage().snippetFirst().price().link().should(
                hasAttribute(HREF, urlSteps.testing().path(EDIT_FORM_LINK).toString()));
    }

}
