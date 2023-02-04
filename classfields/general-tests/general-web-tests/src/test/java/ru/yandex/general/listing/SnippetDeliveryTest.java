package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.step.BasePageSteps.LIST;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.SEND_BY_COURIER;
import static ru.yandex.general.mock.MockListingSnippet.SEND_WITHIN_RUSSIA;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature("Отображение доставки")
@DisplayName("Отображение доставки")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetDeliveryTest {

    private static final String SEND_BY_COURIER_BADGE = "Отправлю такси или курьером";
    private static final String SEND_FROM_MOSCOW = "Отправлю из Москвы";

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String listingType;

    @Parameterized.Parameters(name = "{index}. Тип листинга «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Плиткой", GRID},
                {"Списком", LIST}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, listingType);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается бейдж доставки курьером")
    public void shouldSeeCourierDeliveryBadge() {
        mockRule.graphqlStub(mockResponse().setSearch(
                listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setDelivery(SEND_BY_COURIER))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().snippetFirst().deliveryBadge().should(hasText(SEND_BY_COURIER_BADGE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается бейдж доставки по России")
    public void shouldSeeRussiaDeliveryBadge() {
        mockRule.graphqlStub(mockResponse().setSearch(
                listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setDelivery(SEND_WITHIN_RUSSIA))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().snippetFirst().deliveryBadge().should(hasText(SEND_FROM_MOSCOW));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается бейдж доставки")
    public void shouldNotSeeDeliveryBadge() {
        mockRule.graphqlStub(mockResponse().setSearch(
                listingCategoryResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setDelivery(null))).build())
                .build()).withDefaults().create();
        urlSteps.testing().path(ELEKTRONIKA).open();

        basePageSteps.onListingPage().snippetFirst().deliveryBadge().should(not(isDisplayed()));
    }

}
