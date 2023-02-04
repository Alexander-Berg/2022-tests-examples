package ru.yandex.general.listing;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.ListingSnippet.SHOW_PHONE;
import static ru.yandex.general.element.ListingSnippet.WRITE;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature("Контакты на сниппете, листинг списком")
@DisplayName("Контакты на сниппете, листинг списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ListListingSnippetContactsTest {

    private static final String PHONE_CALL = "PhoneCall";
    private static final String CHAT = "Chat";
    private static final String ANY = "Any";
    private static final String PHONE = "+7 999 469-46-34";

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

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По ховеру отображаются кнопки «Показать телефон» и «Написать», с PreferContactWay = Any")
    public void shouldSeeShowPhoneAndWriteButtonsHover() {
        mockRule.graphqlStub(mockResponse().setSearch(
                        listingCategoryResponse().offers(asList(
                                mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(ANY))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().hover();

        basePageSteps.onListingPage().firstSnippet().button(SHOW_PHONE).should(isDisplayed());
        basePageSteps.onListingPage().firstSnippet().button(WRITE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По ховеру отображается «Показать телефон», с PreferContactWay = PhoneCall")
    public void shouldSeeShowPhoneButtonHover() {
        mockRule.graphqlStub(mockResponse().setSearch(
                        listingCategoryResponse().offers(asList(
                                mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(PHONE_CALL))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().hover();

        basePageSteps.onListingPage().firstSnippet().button(SHOW_PHONE).should(isDisplayed());
        basePageSteps.onListingPage().firstSnippet().button(WRITE).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По ховеру отображается «Написать», с PreferContactWay = Chat")
    public void shouldSeeWriteButtonHover() {
        mockRule.graphqlStub(mockResponse().setSearch(
                        listingCategoryResponse().offers(asList(
                                mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(CHAT))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().hover();

        basePageSteps.onListingPage().firstSnippet().button(WRITE).should(isDisplayed());
        basePageSteps.onListingPage().firstSnippet().button(SHOW_PHONE).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик на «Показать телефон», с PreferContactWay = Any")
    public void shouldSeeShowPhoneClickWithPreferContactWayAny() {
        mockRule.graphqlStub(mockResponse().setPhone(PHONE)
                .setSearch(listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(ANY))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().hover();
        basePageSteps.onListingPage().firstSnippet().button(SHOW_PHONE).click();

        basePageSteps.onListingPage().firstSnippet().phoneLink().should(hasAttribute(HREF, format("tel:%s", PHONE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик на «Показать телефон», с PreferContactWay = PhoneCall")
    public void shouldSeeShowPhoneClickWithPreferContactWayPhone() {
        mockRule.graphqlStub(mockResponse().setPhone(PHONE)
                .setSearch(listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(PHONE_CALL))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().firstSnippet().hover();
        basePageSteps.onListingPage().firstSnippet().button(SHOW_PHONE).click();

        basePageSteps.onListingPage().firstSnippet().phoneLink().should(hasAttribute(HREF, format("tel:%s", PHONE)));
    }

}
