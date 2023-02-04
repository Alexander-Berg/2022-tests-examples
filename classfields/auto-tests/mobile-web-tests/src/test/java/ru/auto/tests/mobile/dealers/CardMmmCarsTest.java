package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.SORT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - легковые, выбор марки/модели/поколения")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardMmmCarsTest {

    private static final String DEALER = "/avilon_mercedes_benz_moskva_vozdvizhenka/";

    private static final String MARK = "Audi";
    private static final String MODEL = "A3";
    private static final String GENERATION = "III (8V) Рестайлинг";
    private static final String GENERATION_CODE = "20785010";

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/Salon"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("mobile/SearchCarsAllDealerId"),
                stub("desktop/SearchCarsCountDealerId")).create();

        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(MARK).click();

        mockRule.overwriteStub(3, stub("desktop/SearchCarsAllDealerIdAudi"));

        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();

        urlSteps.path(MARK.toLowerCase()).path(SLASH).ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().footer().hover()); // без этого листинг не обновляется
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title().should(hasText(startsWith(MARK))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор марки из списка всех")
    public void shouldSelectMarkFromAll() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().allMark(MARK).hover();
        basePageSteps.onDealerCardPage().mmmPopup().allMark(MARK).click();
        basePageSteps.showApplyFiltersButton();

        mockRule.overwriteStub(3, stub("desktop/SearchCarsAllDealerIdAudi"));

        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();

        urlSteps.path(MARK.toLowerCase()).path(SLASH).ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().footer().hover()); // без этого листинг не обновляется
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title().should(hasText(startsWith(MARK))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор модели из списка популярных")
    public void shouldSelectModelFromPopular() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3")).update();

        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(MARK).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().popularModel(MODEL).hover();

        mockRule.overwriteStub(3, stub("desktop/SearchCarsAllDealerIdAudi_A3"));

        basePageSteps.onDealerCardPage().mmmPopup().popularModel(MODEL).name().click();

        urlSteps.path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH).ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().footer().hover()); // без этого листинг не обновляется
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title()
                .should(hasText(startsWith(format("%s %s", MARK, MODEL)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор модели из списка всех")
    public void shouldSelectModelFromAll() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3")).update();

        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(MARK).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().allModel(MODEL).hover();
        mockRule.overwriteStub(3, stub("desktop/SearchCarsAllDealerIdAudi_A3"));
        basePageSteps.onDealerCardPage().mmmPopup().allModel(MODEL).name().hover().click();

        urlSteps.path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH).ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().footer().hover()); // без этого листинг не обновляется
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title()
                .should(hasText(startsWith(format("%s %s", MARK, MODEL)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsAudi_A3"),
                stub("desktop/SearchCarsAllDealerIdAudi_A3_NoRid"),
                stub("desktop/SearchCarsBreadcrumbsAudi_A3_20785010")).update();

        urlSteps.path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().filters().mmm().button("Поколение").click();

        mockRule.overwriteStub(6, stub("desktop/SearchCarsAllDealerIdAudi_A3_20785010"));

        basePageSteps.onDealerCardPage().mmmPopup().generation(GENERATION).click();

        urlSteps.path(GENERATION_CODE).path(SLASH).ignoreParam(SORT).shouldNotSeeDiff();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().footer().hover()); // без этого листинг не обновляется
        basePageSteps.onDealerCardPage().salesList().forEach(sale -> sale.title()
                .should(hasText(startsWith(format("%s %s %s", MARK, MODEL, GENERATION)))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldCloseMmmPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().mmmPopup().waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().mmmPopup().closeButton().click();
        basePageSteps.onDealerCardPage().mmmPopup().waitUntil(not(isDisplayed()));
    }
}
