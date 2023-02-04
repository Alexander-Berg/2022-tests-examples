package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.ldJson.BreadcrumbList;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.ldJson.BreadcrumbItem.breadcrumbItem;
import static ru.yandex.general.beans.ldJson.BreadcrumbList.breadcrumbList;
import static ru.yandex.general.consts.GeneralFeatures.BREADCRUMBS_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.KOMPUTERI;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOBILNIE_TELEFONI;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.TELEFONY_I_UMNYE_CHASY;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.finalCategoryListingResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.page.BasePage.BREADCRUMB_LIST;

@Epic(SEO_FEATURE)
@Feature(BREADCRUMBS_SEO_MARK)
@DisplayName("LD-JSON разметка «BreadcrumbsList» на листингах и карточке")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoBreadcrumbsListLdJsonTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «BreadcrumbsList» на листинге конечной категории")
    public void shouldSeeBreadcrumbsListLdJsonFinalCategoryListing() {
        mockRule.graphqlStub(mockResponse().setSearch(finalCategoryListingResponse().addOffers(5).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        jSoupSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        BreadcrumbList expectedBreadcrumbsList = breadcrumbList(
                breadcrumbItem().setPosition(1).setName("Все объявления")
                        .setItem(jSoupSteps.realProduction().path(ROSSIYA).toString()),
                breadcrumbItem().setPosition(2).setName("Москва")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).toString()),
                breadcrumbItem().setPosition(3).setName("Компьютерная техника")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).toString()),
                breadcrumbItem().setPosition(4).setName("Компьютеры")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI).toString()),
                breadcrumbItem().setPosition(5).setName("Ноутбуки — 5 объявлений")
                        .setItem(jSoupSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).toString()));

        String actualBreadcrumbsList = jSoupSteps.getLdJsonMark(BREADCRUMB_LIST);

        assertThatJson(actualBreadcrumbsList).isEqualTo(expectedBreadcrumbsList.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «BreadcrumbsList» на листинге родительской категории")
    public void shouldSeeBreadcrumbsListLdJsonParentCategoryListing() {
        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().addOffers(12).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        jSoupSteps.testing().path(MOSKVA).path(ELEKTRONIKA)
                .setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        BreadcrumbList expectedBreadcrumbsList = breadcrumbList(
                breadcrumbItem().setPosition(1).setName("Все объявления")
                        .setItem(jSoupSteps.realProduction().path(ROSSIYA).toString()),
                breadcrumbItem().setPosition(2).setName("Москва")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).toString()),
                breadcrumbItem().setPosition(3).setName("Электроника — 12 объявлений")
                        .setItem(jSoupSteps.testing().path(MOSKVA).path(ELEKTRONIKA).toString()));

        String actualBreadcrumbsList = jSoupSteps.getLdJsonMark(BREADCRUMB_LIST);

        assertThatJson(actualBreadcrumbsList).isEqualTo(expectedBreadcrumbsList.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка «BreadcrumbsList» на карточке оффера")
    public void shouldSeeBreadcrumbsListLdJsonOfferCard() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        jSoupSteps.testing().path(CARD).path(ID)
                .setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        BreadcrumbList expectedBreadcrumbsList = breadcrumbList(
                breadcrumbItem().setPosition(1).setName("Все объявления")
                        .setItem(jSoupSteps.realProduction().path(ROSSIYA).toString()),
                breadcrumbItem().setPosition(2).setName("Москва")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).toString()),
                breadcrumbItem().setPosition(3).setName("Электроника")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).path(ELEKTRONIKA).toString()),
                breadcrumbItem().setPosition(4).setName("Телефоны и умные часы")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).path(ELEKTRONIKA).path(TELEFONY_I_UMNYE_CHASY).toString()),
                breadcrumbItem().setPosition(5).setName("Мобильные телефоны")
                        .setItem(jSoupSteps.realProduction().path(MOSKVA).path(ELEKTRONIKA).path(MOBILNIE_TELEFONI).toString()));

        String actualBreadcrumbsList = jSoupSteps.getLdJsonMark(BREADCRUMB_LIST);

        assertThatJson(actualBreadcrumbsList).isEqualTo(expectedBreadcrumbsList.toString());
    }

}
