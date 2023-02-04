package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("ЛК. Ошибки при продлении")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class RenewalErrorOfferTest {

    private static final String PLACEMENT_WARNING = "Не получается списать средства за повторную публикацию " +
            "объявления, чтобы объявление снова появилось на сервисе ";
    private static final String PROMOTION_ERROR = "Ошибка применения опции «Продвижение», подключить опцию";
    private static final String OFFER_ID = "77777777777777777";

    private String uid;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Before
    public void openWallet() {
        uid = apiSteps.createVos2Account(account, OWNER).getId();
    }

    @Test
    @Link("https://st.yandex-team.ru/VERTISTEST-1505")
    @Owner(KANTEMIROV)
    @DisplayName("Размещение. Видим ошибку")
    public void shouldSeePlacementWarning() {
        mockRuleConfigurable.create(
                getResourceAsString("mock/managementnew/RenewalErrorOfferTest/shouldSeePlacementWarning.json")
                        .replaceAll("#UID#", uid).replaceAll("#OFFERID#", OFFER_ID)).setMockritsaCookie();
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerMessage()
                .should(hasText(containsString(PLACEMENT_WARNING)));
    }

    @Test
    @Link("https://st.yandex-team.ru/VERTISTEST-1505")
    @Owner(KANTEMIROV)
    @DisplayName("Продвижение. Видим ошибку")
    public void shouldSeePromotionError() {
        mockRuleConfigurable.create(
                getResourceAsString("mock/managementnew/RenewalErrorOfferTest/shouldSeePromotionError.json")
                        .replaceAll("#UID#", uid).replaceAll("#OFFERID#", OFFER_ID)).setMockritsaCookie();
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerMessage()
                .should(hasText(containsString(PROMOTION_ERROR)));
    }
}
