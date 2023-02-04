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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mock.MockSearch;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.FOOTER;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.LISTING_VAKANCII_RAZRABOTCHIK_TEMPLATE;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.mock.MockSearch.mockSearch;

@Epic(SEO_FEATURE)
@Feature(FOOTER)
@DisplayName("Нет сео текста в футере на пустой выдаче")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoNoFooterTextTest {

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
    public MockSearch mockSearch;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Категория товаров",
                        listingCategoryResponse().setSellersCount(0).setNullPriceStatictics()},
                {"Конечная категория в «Вакансии»",
                        mockSearch(LISTING_VAKANCII_RAZRABOTCHIK_TEMPLATE).setSellersCount(0).setNullPriceStatictics()}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет сео текста в футере на пустой выдаче")
    public void shouldNotSeeSeoTextFooter() {
        mockRule.graphqlStub(mockResponse()
                .setSearch(mockSearch.build())
                .setRegionsTemplate()
                .setCategoriesTemplate()
                .build()).withDefaults().create();
        jSoupSteps.testing().path(MOSKVA).path(NOUTBUKI).setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        Assert.assertThat("Текст сео футера на странице соответствует", jSoupSteps.select(FOOTER_SEO_TEXT_LOCATOR).text(),
                equalTo(""));
    }

}
