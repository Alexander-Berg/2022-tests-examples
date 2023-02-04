package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(LISTING_FEATURE)
@DisplayName("Ссылки в списке категорий сбоку на авто/недвижимость")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingSideCategoriesExternalLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String linkName;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Авто", "https://auto.ru/cars/all/?from=classified&utm_content=category_listing&utm_source=yandex_ads"},
                {"Недвижимость", "https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/kvartira/?from=classified&utm_content=category_listing&utm_source=yandex_ads"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в списке категорий сбоку на авто/недвижимость")
    public void shouldSeeSideCategoriesExternalLinks() {
        urlSteps.testing().path(MOSKVA).open();

        basePageSteps.onListingPage().sidebarCategories().link(linkName).should(hasAttribute(HREF, link));
    }

}
