package ru.yandex.general.offerCard;

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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.consts.QueryParams.ROOT_CATEGORY_ID_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.element.Image.SRC;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.REZUME_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.general.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок похожих снизу")
@DisplayName("Блок похожих снизу, проверка полей сниппета")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardBottomSimilarBlockItemTest {

    private static final String TITLE = "Название оффера";
    private static final long PRICE = 12459;
    private static final String SALLARY = "119500";
    private static final String URL = "/card/421495195122/?root_category_id=elektronika_UhWUEm";
    private static final String SNIPPET_ID = "421495195122";
    private static final String ROOT_CATEGORY_ID_VALUE = "elektronika_UhWUEm";
    private static final String PHOTO = "https://avatars.mdst.yandex.net/get-o-yandex/65675/9a8cfa211a2c56004de15adea346688f/";
    private static final String CARD_ID = "12345";

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
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(CARD_ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemTitle() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setTitle(TITLE))).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().title().should(hasText(TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPrice(PRICE))).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().price().should(hasText(formatPrice(PRICE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена даром оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemFreePrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setFreePrice())).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().price().should(hasText("Даром"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена не указана оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemUnsetPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setUnsetPrice())).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().price().should(hasText("Не указана"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зарплата оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemSallary() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(REZUME_SNIPPET).getMockSnippet().setSallaryPrice(SALLARY))).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().price().should(hasText(formatPrice(SALLARY)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemLink() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setCardlinkUrl(URL))).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().link().should(hasAttribute(HREF, urlSteps.testing()
                .path(CARD).path(SNIPPET_ID).path(SLASH).queryParam(ROOT_CATEGORY_ID_PARAM, ROOT_CATEGORY_ID_VALUE)
                .toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фото оффера из блока похожих снизу")
    public void shouldSeeSimilarCardItemPhotoPreview() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().addPhoto(1))).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().image().should(
                hasAttribute(SRC, containsString(PHOTO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оффер без фото из блока похожих снизу, отображается заглушка")
    public void shouldSeeSimilarCardItemWithoutPhoto() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().removePhotos())).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();

        basePageSteps.onOfferCardPage().firstSnippet().dummyImg().should(isDisplayed());
    }

}
