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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;

@Tag(JURICS)
@DisplayName("Тесты на блок mos.ru")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MosRuJuricsTest {

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

    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static List<String> getData() {
        return asList("AGENCY", "AGENT");
    }

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccountWithType(account, type).getId();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Нет блока mos.ru в форме добавления")
    public void shouldNotSeeMosRuOnOfferAddPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        managementSteps.onOfferAddPage().dealType().selectButton(SELL);
        managementSteps.onOfferAddPage().offerType().selectButton(FLAT);
        managementSteps.onOfferAddPage().mosruBLock().should(not(exists()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Нет сообщения о привязанном аккаунте mos.ru на форме добавления")
    public void shouldNotSeeMosRuOnContactsPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().mosruBLock().should(not(exists()));
    }
}
