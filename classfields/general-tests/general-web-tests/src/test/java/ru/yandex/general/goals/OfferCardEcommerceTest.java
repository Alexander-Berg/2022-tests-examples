package ru.yandex.general.goals;

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
import ru.yandex.general.beans.metrics.EcommerceRequestBody;
import ru.yandex.general.beans.metrics.Product;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.metrics.EcommerceEvent.ecommerceEvent;
import static ru.yandex.general.beans.metrics.EcommerceRequestBody.ecommerceRequestBody;
import static ru.yandex.general.beans.metrics.EventAction.eventAction;
import static ru.yandex.general.beans.metrics.Product.product;
import static ru.yandex.general.beans.metrics.Ym.ym;
import static ru.yandex.general.consts.GeneralFeatures.CARD_VIEW_DETAIL;
import static ru.yandex.general.consts.GeneralFeatures.ECOMMERCE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PHONE_SHOW_PURCHASE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.REZUME_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(ECOMMERCE_FEATURE)
@DisplayName("События электронной коммерции на карточке оффера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardEcommerceTest {

    private static final long PRICE = 32000;
    private static final String ID = "123456";
    private static final String TITLE = "Название оффера";
    private static final String CATEGORY = "Электроника/Телефоны и умные часы/Мобильные телефоны";
    private static final String CATEGORY_WORK = "Работа/Резюме и предложения услуг/Без специальной подготовки";
    private static final String SALLARY_PRICE = "51000";
    private static final String JSONPATHS_TO_IGNORE = "__ym.ecommerce[0].purchase.actionField";

    private EcommerceRequestBody ecommerceRequestBody;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public MockCard card;

    @Parameterized.Parameter(2)
    public Product product;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Событие электронной коммерции с карточки оффера. цена указана",
                        mockCard(BASIC_CARD).setPrice(PRICE),
                        product().setPrice(PRICE).setCategory(CATEGORY)
                },
                {"Событие электронной коммерции с карточки оффера. цена - «Даром»",
                        mockCard(BASIC_CARD).setFreePrice(),
                        product().setPrice("").setCategory(CATEGORY)

                },
                {"Событие электронной коммерции с карточки оффера. цена не указана",
                        mockCard(BASIC_CARD).setUnsetPrice(),
                        product().setPrice("").setCategory(CATEGORY)
                },
                {"Событие электронной коммерции с карточки оффера. зарплата",
                        mockCard(REZUME_CARD).setSallaryPrice(SALLARY_PRICE),
                        product().setPrice(SALLARY_PRICE).setCategory(CATEGORY_WORK)
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CARD).path(ID).path(SLASH);

        mockRule.graphqlStub(mockResponse()
                .setCard(card.setId(ID)
                        .setTitle(TITLE)
                        .build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        product.setId(ID).setName(TITLE);

        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.open();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_VIEW_DETAIL)
    @DisplayName("Отправка события с карточки, при открытии с разными типами price")
    public void shouldSeeDetailEcommerceEventOnCardView() {
        ecommerceRequestBody = ecommerceRequestBody().setYm(ym().setEcommerce(asList(
                ecommerceEvent().setDetail(eventAction().setProducts(asList(product))))));

        goalsSteps.withPageUrl(urlSteps.getCurrentUrl())
                .withBody(ecommerceRequestBody)
                .withEcommerceDetail()
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PHONE_SHOW_PURCHASE)
    @DisplayName("Отправка события с карточки, при открытии номера телефона, с разными типами price")
    public void shouldSeePurchaseEcommerceEventOnPhoneShow() {
        goalsSteps.clearHar();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        ecommerceRequestBody = ecommerceRequestBody().setYm(ym().setEcommerce(asList(
                ecommerceEvent().setPurchase(eventAction().setProducts(asList(product))))));

        goalsSteps.withPageUrl(urlSteps.getCurrentUrl())
                .withBody(ecommerceRequestBody)
                .withEcommercePurchase()
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withCount(1)
                .shouldExist();
    }

}
