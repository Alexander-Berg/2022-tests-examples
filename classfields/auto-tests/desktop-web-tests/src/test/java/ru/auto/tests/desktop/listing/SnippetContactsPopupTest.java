package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchOffersUsedQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffersCarsExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - просмотр контактов продавца")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetContactsPopupTest {

    private static final String SALE_ID = getRandomOfferId();

    private static final String CONTACTS_POPUP_TEXT = "Частное лицо\nЧастное лицо\n" +
            "+7 921 001-35-93\nКруглосуточно\nВнимание\nПриобретая ТС, никогда не отправляйте предоплату." +
            "Подробнее\nТверь\nMazda 6 II (GH) • 470 000 ₽\n2.0 л / 147 л.с. / Бензин\nмеханика\n" +
            "лифтбек\nпередний\nчёрный\nЗаметка об этом автомобиле (её увидите только вы)";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                getSearchOffersUsedQuery())
                        .withResponseBody(
                                searchOffersCarsExample().setId(SALE_ID).getBody()),
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, SALE_ID))
        ).create();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Testing.class})
    @DisplayName("Попап с контактами продавца, тип листинга «Карусель»")
    public void shouldSeeContactsPopupOnSnippetCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();
        basePageSteps.onListingPage().getCarouselSale(0).showPhonesButton().click();

        basePageSteps.onListingPage().contactsPopup().waitUntil(hasText(CONTACTS_POPUP_TEXT));
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Testing.class})
    @DisplayName("Попап с контактами продавца через «Показать телефон» в галерее, тип листинга «Карусель»")
    public void shouldSeeContactsPopupFromGalleryOnSnippetCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();
        basePageSteps.onListingPage().getCarouselSale(0).galleryPhoneButton().hover().click();

        basePageSteps.onListingPage().contactsPopup().waitUntil(hasText(CONTACTS_POPUP_TEXT));
    }

}
