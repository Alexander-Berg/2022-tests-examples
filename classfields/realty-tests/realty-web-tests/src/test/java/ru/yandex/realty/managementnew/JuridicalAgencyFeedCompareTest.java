package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.common.IsElementDisplayedMatcher.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FEEDS;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.RealtyTags.JURICS;

@Tag(JURICS)
@DisplayName("Агентство. Скриншоты.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class JuridicalAgencyFeedCompareTest {

    private static final String EDIT_FEED = "Редактировать фид";
    private static final String SEND_TO_VERIFY = "Отправить на проверку";
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
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравниваем скрины добавленого фида")
    public void shouldSeeFeed() {
        basePageSteps.getDriver().manage().window().fullscreen();
        urlSteps.testing().path(MANAGEMENT_NEW).path(FEEDS).open();
        String feedUrl = format("http://%s.com", getRandomString(12));
        managementSteps.onManagementNewPage().input("Укажите понятное название фида", getRandomString());
        managementSteps.onManagementNewPage().input("http://example.com", feedUrl);
        managementSteps.onManagementNewPage().button(SEND_TO_VERIFY).click();
        managementSteps.onManagementNewPage().link(EDIT_FEED).waitUntil(isDisplayed()).click();
        managementSteps.onManagementNewPage().button(SEND_TO_VERIFY).click();
        managementSteps.onManagementNewPage().link(EDIT_FEED).waitUntil(isDisplayed());
        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().feeds());
        urlSteps.setProductionHost().open();
        managementSteps.onManagementNewPage().link(EDIT_FEED).waitUntil(isDisplayed()).click();
        managementSteps.onManagementNewPage().button(SEND_TO_VERIFY).click();
        managementSteps.onManagementNewPage().link(EDIT_FEED).waitUntil(isDisplayed());
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().feeds());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
