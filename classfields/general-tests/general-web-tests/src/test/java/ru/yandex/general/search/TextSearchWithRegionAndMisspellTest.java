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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_VALUE;
import static ru.yandex.general.element.SearchBar.FIND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature("Текстовый поиск с регионом и опечаткой")
@DisplayName("Тесты поиска по тексту")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class TextSearchWithRegionAndMisspellTest {

    private static final String TEXT_WITH_MISSPELL = "красовки в санкт-питербурге";
    private static final String TEXT_WITHOUT_MISSPELL = "кроссовки в санкт-петербурге";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT_WITH_MISSPELL);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск с регионом + опечатка")
    public void shouldSeeRegionSearchWithMisspell() {
        urlSteps.queryParam(TEXT_PARAM, TEXT_WITH_MISSPELL).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(hasText("Регион поиска: Санкт-Петербург"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("h1 для исправленного поискового запроса")
    public void shouldSeeMisspellH1() {
        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Новосибирске", TEXT_WITHOUT_MISSPELL)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поиск с опечаткой")
    public void shouldSeeSearchMisspell() {
        basePageSteps.onListingPage().misspellText().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Новосибирске", TEXT_WITH_MISSPELL)));

        urlSteps.queryParam(TEXT_PARAM, TEXT_WITH_MISSPELL)
                .queryParam(LOCKED_FIELDS, TEXT_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем чипсину региона в текстовом поиске с регионом и опечаткой")
    public void shouldSeeRegionChipsReset() {
        basePageSteps.onListingPage().chips().reset().click();
        basePageSteps.wait500MS();

        urlSteps.queryParam(TEXT_PARAM, TEXT_WITH_MISSPELL).queryParam(LOCKED_FIELDS, "Region").shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
    }

}
