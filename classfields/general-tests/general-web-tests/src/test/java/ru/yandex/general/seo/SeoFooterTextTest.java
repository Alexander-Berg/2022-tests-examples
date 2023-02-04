package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mock.MockSearch;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.FOOTER;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.LISTING_VAKANCII_RAZRABOTCHIK_TEMPLATE;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.mock.MockSearch.listingRezumeResponse;
import static ru.yandex.general.mock.MockSearch.mockSearch;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(SEO_FEATURE)
@Feature(FOOTER)
@DisplayName("Сео текст в футере")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoFooterTextTest {

    private static final String FOOTER_SEO_TEXT_LOCATOR = "footer span[class*='seoText']";

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public String region;

    @Parameterized.Parameter(2)
    public MockSearch mockSearch;

    @Parameterized.Parameter(3)
    public String footerText;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Категория товаров с ценой «от 0 до 40000»", MOSKVA,
                        listingCategoryResponse().addOffers(12).setSellersCount(5).setPriceStatictics(0, 40000),
                        "12 объявлений о продаже электроники в Москве. Быстрый поиск " +
                                "вещей с удобной сортировкой и фильтрацией по вашим требованиям на Яндекс.Объявлениях." +
                                " Электроника по цене от 0 рублей от 5 продавцов"},
                {"Категория товаров с ценой «от 0 до 0»", SANKT_PETERBURG,
                        listingCategoryResponse().addOffers(5).setSellersCount(1).setPriceStatictics(0, 0),
                        "5 объявлений о продаже электроники в Санкт-Петербурге. Быстрый поиск вещей с удобной сортировкой " +
                                "и фильтрацией по вашим требованиям на Яндекс.Объявлениях. Электроника по цене от " +
                                "0 рублей от 1 продавца"},
                {"Категория товаров с ценой «от 1000 до 1250000»", ROSSIYA,
                        listingCategoryResponse().addOffers(21).setSellersCount(121).setPriceStatictics(1000, 1250000),
                        "21 объявление о продаже электроники в России. Быстрый поиск вещей с удобной сортировкой и " +
                                "фильтрацией по вашим требованиям на Яндекс.Объявлениях. Электроника по цене от" +
                                " 1 000 рублей от 121 продавца"},
                {"Категория товаров с ценой «от 0 до 0»", MOSKVA,
                        listingCategoryResponse().addOffers(5).setSellersCount(1).setPriceStatictics(0, 0),
                        "5 объявлений о продаже электроники в Москве. Быстрый поиск вещей с удобной сортировкой " +
                                "и фильтрацией по вашим требованиям на Яндекс.Объявлениях. Электроника по цене от " +
                                "0 рублей от 1 продавца"},
                {"Промежуточная категория в «Резюме»", MOSKVA,
                        listingRezumeResponse().addOffers(13).setSellersCount(5).setPriceStatictics(500, 40000),
                        "13 объявлений - резюме и предложения услуг в Москве. Быстрый поиск сотрудников, вакансий и " +
                                "резюме с удобной сортировкой и фильтрацией по вашим требованиям на Яндекс.Объявлениях." +
                                " Резюме и предложения услуг - в разделе работа."},
                {"Конечная категория в «Вакансии»", MOSKVA,
                        mockSearch(LISTING_VAKANCII_RAZRABOTCHIK_TEMPLATE).addOffers(2).setSellersCount(2).setPriceStatictics(0, 40000),
                        "2 объявления - разработчик в Москве. Быстрый поиск сотрудников, вакансий и резюме с удобной" +
                                " сортировкой и фильтрацией по вашим требованиям на Яндекс.Объявлениях. " +
                                "Разработчик - в разделе работа."}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сео футер")
    public void shouldSeeSeoFooter() {
        mockRule.graphqlStub(mockResponse()
                .setSearch(mockSearch.build())
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(region).path(NOUTBUKI).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        String actualSeoText = jSoupSteps.select(FOOTER_SEO_TEXT_LOCATOR).text();
        Assert.assertThat("Текст сео футера на странице соответствует", actualSeoText, equalTo(footerText));
    }

}
