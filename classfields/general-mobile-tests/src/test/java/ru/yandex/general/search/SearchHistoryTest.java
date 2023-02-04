package ru.yandex.general.search;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature("История поисков")
@DisplayName("История поисков")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SearchHistoryTest {

    private static final String TELEVIZOR = "Телевизор";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Один поиск в истории поисков")
    public void shouldSeeOneSearchInHistory() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TELEVIZOR);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.waitSomething(5, TimeUnit.SECONDS);
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().openSearch().click();

        basePageSteps.onListingPage().wrapper().suggestList().should(hasSize(1)).should(hasItem(hasText(TELEVIZOR)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет поиска в истории поисков")
    public void shouldNotSeeSearchInHistory() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();

        basePageSteps.onListingPage().wrapper().suggestList().should(not(isDisplayed()));
    }

}
