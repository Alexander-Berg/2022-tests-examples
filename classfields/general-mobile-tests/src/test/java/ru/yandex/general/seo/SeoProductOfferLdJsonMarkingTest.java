package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.ldJson.Brand.brand;
import static ru.yandex.general.beans.ldJson.Offers.offers;
import static ru.yandex.general.beans.ldJson.ProductOffer.productOffer;
import static ru.yandex.general.consts.Attributes.Typename.SELECT;
import static ru.yandex.general.consts.Attributes.createAttribute;
import static ru.yandex.general.consts.BaseConstants.Condition.NEW;
import static ru.yandex.general.consts.BaseConstants.Condition.USED;
import static ru.yandex.general.consts.GeneralFeatures.PRODUCT_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mobile.page.BasePage.JOB_POSTING;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.REZUME_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.BasePage.PRODUCT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(SEO_FEATURE)
@Feature(PRODUCT_SEO_MARK)
@DisplayName("Разметка LD-JSON «Product» на карточке товара")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoProductOfferLdJsonMarkingTest {

    private static final long PRICE = 3520;
    private static final String TITLE = "Название товара";
    private static final String CANONICAL_LINK = "/rabota/offer/88918384664903681/";
    private static final String DESCRIPTION = "Описание товара";
    private static final String USED_CONDITION = "https://schema.org/UsedCondition";
    private static final String NEW_CONDITION = "https://schema.org/NewCondition";
    private static final String IMAGE_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/af6807fe8f1796887c7e6907389a38f9";
    private static final String REZUME_IMAGE_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/1b2af371afcbb0dc10c6467de881b673";
    private static final String FIRST_DIMENSION = "778x778";
    private static final String SECOND_DIMENSION = "778x586";
    private static final String THIRD_DIMENSION = "778x438";
    private static final String PHONE_MANUFACTURER_ID = "proizvoditel-mobilnogo-telefona_454ghb";
    private static final String APPLE = "Apple";

    private MockCard mockCard;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Before
    public void before() {
        jSoupSteps.testing().path(CANONICAL_LINK);

        mockCard = mockCard(BASIC_CARD).setId("88918384664903681")
                .setCanonicalLink(CANONICAL_LINK)
                .setTitle(TITLE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «Product» для карточки товара с описанием, брендом, ценой, состоянием б/у")
    public void shouldSeeCardLdJsonProductMarkingWithAllFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setPrice(PRICE).setDescription(DESCRIPTION).setCondition(USED)
                        .setAttributes(
                                createAttribute(SELECT).setName("Производитель").setId(PHONE_MANUFACTURER_ID)
                                        .withSelectValue(APPLE)).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        String actualProduct =  jSoupSteps.getLdJsonMark(PRODUCT);
        String expectedProduct = productOffer().setBrand(brand().setName(APPLE))
                .setName(TITLE)
                .setDescription(DESCRIPTION)
                .setOffers(offers()
                        .setUrl(jSoupSteps.toString())
                        .setPrice(PRICE)
                        .setItemCondition(USED_CONDITION))
                .setImage(asList(
                        format("%s/%s", IMAGE_URL, FIRST_DIMENSION),
                        format("%s/%s", IMAGE_URL, SECOND_DIMENSION),
                        format("%s/%s", IMAGE_URL, THIRD_DIMENSION))).toString();

        assertThatJson(actualProduct).isEqualTo(expectedProduct);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «Product» для карточки товара, даром, без бренда, без описания, новое")
    public void shouldSeeCardLdJsonProductMarkingWithMinimumFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setFreePrice().setCondition(NEW).removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        String actualProduct =  jSoupSteps.getLdJsonMark(PRODUCT);
        String expectedProduct = productOffer().setBrand(brand())
                .setName(TITLE)
                .setDescription("")
                .setOffers(offers()
                        .setUrl(jSoupSteps.toString())
                        .setPrice(0)
                        .setItemCondition(NEW_CONDITION))
                .setImage(asList(
                        format("%s/%s", IMAGE_URL, FIRST_DIMENSION),
                        format("%s/%s", IMAGE_URL, SECOND_DIMENSION),
                        format("%s/%s", IMAGE_URL, THIRD_DIMENSION))).toString();

        assertThatJson(actualProduct).isEqualTo(expectedProduct);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «Product» для карточки товара, цена не указана")
    public void shouldSeeCardLdJsonProductMarkingWithoutPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setUnsetPrice().setCondition(NEW).removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        String actualProduct =  jSoupSteps.getLdJsonMark(PRODUCT);
        String expectedProduct = productOffer().setBrand(brand())
                .setName(TITLE)
                .setDescription("")
                .setOffers(offers()
                        .setUrl(jSoupSteps.toString())
                        .setItemCondition(NEW_CONDITION)
                        .setPrice(0))
                .setImage(asList(
                        format("%s/%s", IMAGE_URL, FIRST_DIMENSION),
                        format("%s/%s", IMAGE_URL, SECOND_DIMENSION),
                        format("%s/%s", IMAGE_URL, THIRD_DIMENSION))).toString();

        assertThatJson(actualProduct).isEqualTo(expectedProduct);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «Product» для карточки резюме, без цены")
    public void shouldSeeCardLdJsonProductMarkingRezumeNoPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(REZUME_CARD).setId("88918384664903681")
                        .setCanonicalLink(CANONICAL_LINK)
                        .removeDescription()
                        .setTitle(TITLE).setSallaryPrice("120000").removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        String actualProduct =  jSoupSteps.getLdJsonMark(PRODUCT);
        String expectedProduct = productOffer().setBrand(brand())
                .setName(TITLE)
                .setDescription("")
                .setOffers(offers()
                        .setUrl(jSoupSteps.toString())
                        .setPrice("120000"))
                .setImage(asList(
                        format("%s/%s", REZUME_IMAGE_URL, FIRST_DIMENSION),
                        format("%s/%s", REZUME_IMAGE_URL, SECOND_DIMENSION),
                        format("%s/%s", REZUME_IMAGE_URL, THIRD_DIMENSION))).toString();

        assertThatJson(actualProduct).isEqualTo(expectedProduct);
    }

}
