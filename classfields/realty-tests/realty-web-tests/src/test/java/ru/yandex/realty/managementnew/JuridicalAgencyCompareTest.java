package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
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

import java.util.List;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DASHBOARD;
import static ru.yandex.realty.consts.Pages.FEEDS;
import static ru.yandex.realty.consts.Pages.FINANCES;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.PROMOCODES;
import static ru.yandex.realty.consts.Pages.SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;

@Tag(JURICS)
@DisplayName("Агентство. Скриншоты.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class JuridicalAgencyCompareTest {

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

    @Parameterized.Parameter
    public String tab;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static List<String> getData() {
        return asList("",
                FEEDS,
                DASHBOARD,
                FINANCES,
                PROMOCODES,
                SETTINGS);
    }

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравниваем скрины агентства с разных страниц")
    public void shouldSeeTab() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(tab).open();
        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().root());
        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().root());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
