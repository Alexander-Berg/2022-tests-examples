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
import ru.yandex.general.beans.card.Address;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import static java.lang.String.format;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.consts.GeneralFeatures.JOB_POSTING_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mock.MockCard.VACANCY_CARD;
import static ru.yandex.general.mock.MockCard.getCreateDateTime;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(SEO_FEATURE)
@Feature(JOB_POSTING_SEO_MARK)
@DisplayName("Разметка Schema.org на карточке вакансии")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoJobPostingSchemaOrgCardMarkingTest {

    private static final String ADDRESS_NAME = "Павелецкая площадь, 2с1";
    private static final String DISTRICT_NAME = "Замоскворечье";
    private static final String SALLARY = "120000";
    private static final String SELLER_NAME = "Дон Валера";
    private static final String TITLE = "Название вакансии";
    private static final String CANONICAL_LINK = "/rabota/offer/88918384664903681/";
    private static final String DESCRIPTION = "Описание вакансии";

    private static final Address ADDRESS = Address.address()
            .setAddress(addressText().setAddress(ADDRESS_NAME))
            .setGeoPoint(geoPoint().setLatitude("55.730423").setLongitude("37.634796"));
    private static final Address DISTRICT = Address.address()
            .setGeoPoint(geoPoint().setLatitude("55.734157").setLongitude("37.63429"))
            .setDistrict(district().setId("117067").setName(DISTRICT_NAME).setIsEnriched(false));

    private String createDateTime = getCreateDateTime();

    private MockCard mockCard;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Before
    public void before() {
        mockCard = mockCard(VACANCY_CARD).setId("88918384664903681")
                .setCreateDateTime(createDateTime)
                .setCanonicalLink(CANONICAL_LINK)
                .setSellerName(SELLER_NAME)
                .setTitle(TITLE)
                .setCanonicalLink(CANONICAL_LINK);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Schema.org разметка на карточке вакансии, есть зарплата и описание, указан адрес")
    public void shouldSeeCardSchemaOrgMarkingWithAllFields() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setSallaryPrice(SALLARY)
                        .setAddresses(ADDRESS)
                        .setDescription(DESCRIPTION).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopRobotUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("datePosted")).as("Дата создания").isEqualTo(createDateTime);
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo(format("Вакансия %s в Москве. %s", TITLE, DESCRIPTION));
            s.assertThat(jSoupSteps.getItempropContent("title")).as("Тайтл").isEqualTo("Вакансия Название вакансии в Москве | вакансии в отрасли IT, интернет, связь, телеком в Москве на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.select("div[itemprop='hiringOrganization'] meta[itemprop='name']").attr("content")).as("Имя продавца").isEqualTo(SELLER_NAME);
            s.assertThat(jSoupSteps.select("div[itemprop='address'] meta[itemprop='streetAddress']").attr("content")).as("Адрес").isEqualTo(ADDRESS_NAME);
            s.assertThat(jSoupSteps.select("div[itemprop='baseSalary'] meta[itemprop='value']").attr("content")).as("Зарплата").isEqualTo(SALLARY);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Schema.org разметка на карточке вакансии, нет зарплаты и описания, указан район")
    public void shouldSeeCardSchemaOrgMarkingWithDistrictWithoutSallaryAndDescription() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setUnsetPrice()
                        .setAddresses(DISTRICT).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(CANONICAL_LINK).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.select("link[itemprop='url']").attr("href")).as("Ссылка").isEqualTo(jSoupSteps.toString());
            s.assertThat(jSoupSteps.getItempropContent("datePosted")).as("Дата создания").isEqualTo(createDateTime);
            s.assertThat(jSoupSteps.getItempropContent("description")).as("Описание").isEqualTo(format("Вакансия %s в Москве. ", TITLE));
            s.assertThat(jSoupSteps.getItempropContent("title")).as("Тайтл").isEqualTo("Вакансия Название вакансии в Москве | вакансии в отрасли IT, интернет, связь, телеком в Москве на Яндекс.Объявлениях");
            s.assertThat(jSoupSteps.select("div[itemprop='hiringOrganization'] meta[itemprop='name']").attr("content")).as("Имя продавца").isEqualTo(SELLER_NAME);
            s.assertThat(jSoupSteps.select("div[itemprop='address'] meta[itemprop='addressLocality']").attr("content")).as("Адрес").isEqualTo(DISTRICT_NAME);
            s.assertThat(jSoupSteps.select("div[itemprop='baseSalary'] meta[itemprop='value']").attr("content")).as("Зарплата").isEqualTo("0");
        });
    }

}
