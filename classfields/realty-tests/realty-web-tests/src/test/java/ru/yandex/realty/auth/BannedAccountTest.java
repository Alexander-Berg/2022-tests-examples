package ru.yandex.realty.auth;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;

/**
 * Created by ivanvan on 18.08.17.
 */
@DisplayName("Форма добавления объявления. Забаненный пользователь")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BannedAccountTest {

    private static final String LOGIN = "marbya7";
    private static final String PASSWORD = "vfif19";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openManagementPage() {
        passportSteps.login(LOGIN, PASSWORD);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
    }

    @Test
    @Owner(IVANVAN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Появляется предупреждение при попытке публикации")
    public void shouldNotPublishFromBannedAccount() {
        Screenshot testing = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage().errorField());

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        Screenshot production = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage().errorField());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
