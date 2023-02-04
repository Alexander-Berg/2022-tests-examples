package ru.yandex.realty.filters.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Фильтры коммерческой недвижимости")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BuyCommercialFiltersDisabledCheckboxTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> testParameters() {
        return asList(
                "Земельный участок",
                "Юридический адрес");
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        user.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. При выборе чекбокса, остальные становятся неактивными")
    public void shouldSeeDisabledCheckboxes() {
        user.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        user.onMainPage().filters().selectPopup().item("Коммерческую недвижимость").click();
        user.onMainPage().filters().button("Тип").click();
        user.onMainPage().filters().selectPopup().item(label).click();
        shouldSeeDisabledSheckBoxes(user.onMainPage().filters().selectPopup().items(),
                user.onMainPage().filters().selectPopup().items().size());

    }

    @Step("Всего {size} чекбоксов, неактивных на 1 меньше")
    private void shouldSeeDisabledSheckBoxes(ElementsCollection<AtlasWebElement> checkBoxes, int size) {
        assertThat(checkBoxes.stream()
                .filter(c -> c.getAttribute("class").contains("Menu__item_disabled"))
                .collect(Collectors.toList()).size()).isEqualTo(size - 1);
    }
}
