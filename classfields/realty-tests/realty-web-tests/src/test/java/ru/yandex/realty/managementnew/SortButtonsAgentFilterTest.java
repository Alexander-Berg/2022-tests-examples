package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Фильтры агентского оффера.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SortButtonsAgentFilterTest {

    private static final String DATA = "Дата";
    private static final String DIRECTION = "direction";
    private static final String DESC = "DESC";
    private static final String SORT = "sort";
    private static final String UPDATE_TIME = "updateTime";
    private static final String ASC = "ASC";
    private static final String PRICE = "price";
    private static final String OBNOVLENIYA = "обновления";
    private static final String SOZDANIYA = "создания";
    private static final String CREATE_TIME = "createTime";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем на «Дата», проверяем что отобразилось в урле")
    public void shouldSeeSortDateDescInUrl() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().headerAgentOffers().sortLink(DATA).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, DESC).queryParam(SORT, UPDATE_TIME)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем на «Цена», проверяем что отобразилось в урле")
    public void shouldSeeSortPriceAscInUrl() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().headerAgentOffers().sortLink("Цена").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, ASC).queryParam(SORT, PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем на «Дата» два раза, проверяем что отобразилось в урле")
    public void shouldSeeSortDateAscInUrlAfterDoubleClick() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().headerAgentOffers().sortLink(DATA).waitUntil(isDisplayed()).click();
        managementSteps.onManagementNewPage().paranja().waitUntil(not(isDisplayed()));
        managementSteps.onManagementNewPage().headerAgentOffers().sortLink(DATA).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, ASC).queryParam(SORT, UPDATE_TIME)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем тип сортировки по «дате создания», проверяем что отобразилось в урле")
    public void shouldSeeCreateTimeSortInUrl() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, DESC).queryParam(SORT, UPDATE_TIME).open();
        managementSteps.onManagementNewPage().headerAgentOffers().select(OBNOVLENIYA, SOZDANIYA);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, DESC).queryParam(SORT, CREATE_TIME)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем тип сортировки по «дате обновления», проверяем что отобразилось в урле")
    public void shouldSeeUpdateTimeSortInUrl() {
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, ASC).queryParam(SORT, CREATE_TIME)
                .open();
        managementSteps.onManagementNewPage().headerAgentOffers().select(SOZDANIYA, OBNOVLENIYA);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam(DIRECTION, ASC).queryParam(SORT, UPDATE_TIME)
                .shouldNotDiffWithWebDriverUrl();
    }
}
