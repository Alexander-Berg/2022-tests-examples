package ru.yandex.general.search;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_VALUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@Epic(SEARCH_FEATURE)
@Feature("Опечатки")
@DisplayName("Опечатки в поисковом запросе")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchMisspellTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String withMisspel;

    @Parameterized.Parameter(1)
    public String withoutMisspel;

    @Parameterized.Parameters(name = "{index}. Поиск по слову {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"акамулятор", "аккумулятор"},
                {"yjen,er", "ноутбук"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, withMisspel).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("h1 для исправленного поискового запроса")
    public void shouldSeeMisspellH1() {
        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", withoutMisspel)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Значение в поисковом инпуте для исправленного поискового запроса")
    public void shouldSeeMisspellInSearchInput() {
        basePageSteps.onListingPage().searchBar().input().should(hasValue(withoutMisspel));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Предложение поискать с опечаткой")
    public void shouldSeeSearchMisspellText() {
        basePageSteps.onListingPage().misspellText().should(hasText(format("Искать «%s»", withMisspel)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поиск с опечаткой")
    public void shouldSeeSearchMisspell() {
        basePageSteps.onListingPage().misspellText().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Москве", withMisspel)));

        urlSteps.queryParam(LOCKED_FIELDS, TEXT_VALUE).shouldNotDiffWithWebDriverUrl();
    }

}
