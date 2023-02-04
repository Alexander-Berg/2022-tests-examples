package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.Attributes.Typename.SELECT;
import static ru.yandex.general.consts.Attributes.createAttribute;
import static ru.yandex.general.consts.BaseConstants.Condition.NEW;
import static ru.yandex.general.consts.BaseConstants.Condition.USED;
import static ru.yandex.general.consts.GeneralFeatures.PRODUCT_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.REZUME_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(SEO_FEATURE)
@Feature(PRODUCT_SEO_MARK)
@DisplayName("Разметка Shema.org «Product» на карточке товара")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoProductOfferShemaOrgMarkingTest {

    private static final long PRICE = 3520;
    private static final String TITLE = "Название товара";
    private static final String CANONICAL_LINK = "/rabota/offer/88918384664903681/";
    private static final String DESCRIPTION = "Описание товара";
    private static final String USED_CONDITION = "https://schema.org/UsedCondition";
    private static final String NEW_CONDITION = "https://schema.org/NewCondition";
    private static final String IN_STOCK = "https://schema.org/InStock";
    private static final String IMAGE_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/af6807fe8f1796887c7e6907389a38f9";
    private static final String REZUME_IMAGE_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/1b2af371afcbb0dc10c6467de881b673";
    private static final String FIRST_DIMENSION = "1556x1556";
    private static final String SECOND_DIMENSION = "1556x876";
    private static final String THIRD_DIMENSION = "824x620";
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
        mockCard = mockCard(BASIC_CARD).setId("88918384664903681")
                .setCanonicalLink(CANONICAL_LINK)
                .setTitle(TITLE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Shema.org разметка «Product» для карточки товара с описанием, брендом, ценой, состоянием б/у")
    public void shouldSeeCardShemaOrgProductMarkingWithAllFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setPrice(PRICE).setDescription(DESCRIPTION).setCondition(USED)
                        .setAttributes(
                                createAttribute(SELECT).setName("Производитель").setId(PHONE_MANUFACTURER_ID)
                                        .withSelectValue(APPLE)).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();


        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.getItempropContent("name")).as("Тайтл").isEqualTo("Название товара купить в Москве | Объявления о продаже в категории Электроника на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo("Название товара по цене 3 520 ₽ в Москве. Описание товара");
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("availability")).as("Наличие").isEqualTo(IN_STOCK);
            s.assertThat(jSoupSteps.getItempropContent("price")).as("Цена").isEqualTo(String.valueOf(PRICE));
            s.assertThat(jSoupSteps.getItempropContent("itemCondition")).as("Состояние").isEqualTo(USED_CONDITION);
            s.assertThat(jSoupSteps.select("div[itemprop='brand'] meta[itemprop='name']").attr("content")).as("Бренд").isEqualTo(APPLE);
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(0).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, FIRST_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(1).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, SECOND_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(2).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, THIRD_DIMENSION));
        });

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Shema.org разметка «Product» для карточки товара, даром, без бренда, без описания, новое")
    public void shouldSeeCardShemaOrgProductMarkingWithMinimumFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setFreePrice().setCondition(NEW).removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.getItempropContent("name")).as("Тайтл").isEqualTo("Название товара купить в Москве | Объявления о продаже в категории Электроника на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo("Название товара бесплатно в Москве. ");
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("availability")).as("Наличие").isEqualTo(IN_STOCK);
            s.assertThat(jSoupSteps.getItempropContent("price")).as("Цена").isEqualTo("0");
            s.assertThat(jSoupSteps.getItempropContent("itemCondition")).as("Состояние").isEqualTo(NEW_CONDITION);
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(0).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, FIRST_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(1).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, SECOND_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(2).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, THIRD_DIMENSION));
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Shema.org разметка «Product» для карточки товара, цена не указана")
    public void shouldSeeCardShemaOrgProductMarkingWithoutPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setUnsetPrice().setCondition(NEW).removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.getItempropContent("name")).as("Тайтл").isEqualTo("Название товара купить в Москве | Объявления о продаже в категории Электроника на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo("Название товара по цене Не указана в Москве. ");
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("availability")).as("Наличие").isEqualTo(IN_STOCK);
            s.assertThat(jSoupSteps.getItempropContent("price")).as("Цена").isEqualTo("0");
            s.assertThat(jSoupSteps.getItempropContent("itemCondition")).as("Состояние").isEqualTo(NEW_CONDITION);
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(0).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, FIRST_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(1).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, SECOND_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(2).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", IMAGE_URL, THIRD_DIMENSION));
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Shema.org разметка «Product» для карточки резюме, без цены")
    public void shouldSeeCardShemaOrgProductMarkingRezumeNoPrice() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(REZUME_CARD).setId("88918384664903681")
                        .setDescription(DESCRIPTION)
                        .setCanonicalLink(CANONICAL_LINK)
                        .setTitle(TITLE).setSallaryPrice("120000").removeDescription().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.getItempropContent("name")).as("Тайтл").isEqualTo("Резюме Название товара в Москве | резюме в отрасли Без специальной подготовки в Москве на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo("Резюме Название товара в Москве. ");
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("availability")).as("Наличие").isEqualTo(IN_STOCK);
            s.assertThat(jSoupSteps.getItempropContent("price")).as("Цена").isEqualTo("120000");
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(0).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", REZUME_IMAGE_URL, FIRST_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(1).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", REZUME_IMAGE_URL, SECOND_DIMENSION));
            s.assertThat(jSoupSteps.select("link[itemprop='image']").get(2).attr("href")).as("Ссылка").isEqualTo(format("%s/%s", REZUME_IMAGE_URL, THIRD_DIMENSION));
        });
    }

}
