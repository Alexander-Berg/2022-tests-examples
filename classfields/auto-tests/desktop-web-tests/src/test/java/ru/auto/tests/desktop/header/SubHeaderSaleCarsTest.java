package ru.auto.tests.desktop.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Feature(SALES)
@DisplayName("Карточка - подшапка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SubHeaderSaleCarsTest {

    private static final String SALE_ID = "1076842087";
    private static final String SALE_HASH = "f1e84";
    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GENERATION = "2307388";
    private static final String BODY = "2307389";
    private static final String MODIFICATION = "2307392";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(format("%s-%s", SALE_ID, SALE_HASH)).open();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Дилеры»")
    public void shouldClickDealersTab() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").update();

        basePageSteps.onCardPage().subHeader().button("Дилеры").click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Каталог»")
    public void shouldClickCatalogTab() {
        basePageSteps.onCardPage().subHeader().button("Каталог").click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path(BODY).path(SPECIFICATIONS).path(format("/%s__%s/", BODY, MODIFICATION)).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Отзывы»")
    public void shouldClickReviewsTab() {
        basePageSteps.onCardPage().subHeader().button("Отзывы").should(hasAttribute("href",
                urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/")
                        .toString())).click();
        urlSteps.shouldUrl(startsWith(urlSteps.testing().path(REVIEWS).toString()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Видео»")
    public void shouldClickVideoTab() {
        basePageSteps.onCardPage().subHeader().button("Видео").click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Статистика»")
    public void shouldClickStatisticsTab() {
        basePageSteps.onCardPage().subHeader().button("Статистика цен").click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path(BODY).path("/").addParam("sale_id", SALE_ID).addParam("sale_hash", SALE_HASH)
                .addParam("section", "used").shouldNotSeeDiff();
    }
}