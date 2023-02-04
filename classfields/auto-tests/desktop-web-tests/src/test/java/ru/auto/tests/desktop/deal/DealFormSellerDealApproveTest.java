package ru.auto.tests.desktop.deal;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Личная встреча и подтверждение сделки»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealFormSellerDealApproveTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";
    private final static String DKP_FIRST_PAGE_URL = "https://images.mds-proxy.test.avto.ru/get-palma/65905/" +
            "2a0000017c6f6989ca7fb058c30b52ad0c01/image?dynamic-watermark=%D0%91%D0%B5%D0%B7%D0%BE%D0%BF%D0%B0%D1%81%" +
            "D0%BD%D0%B0%D1%8F%20%D1%81%D0%B4%D0%B5%D0%BB%D0%BA%D0%B0&ts=1633958560&sign=781f7910d86af47cb50fe0e9efe84" +
            "b62d49770a4a317b836f8670f5d10f22888";
    private final static String DKP_SECOND_PAGE_URL = "https://images.mds-proxy.test.avto.ru/get-palma/65905/" +
            "2a0000017c6f698a540662d9d0160b4b32f7/image?dynamic-watermark=%D0%91%D0%B5%D0%B7%D0%BE%D0%BF%D0%B0%D1%81" +
            "%D0%BD%D0%B0%D1%8F%20%D1%81%D0%B4%D0%B5%D0%BB%D0%BA%D0%B0&ts=1633958560&sign=da9c73059a505d535bbd62015" +
            "bcc1d22d4c771d4e7bcd72f8cf1fc6aa9c59126";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/SafeDealDealGetWithDealApproveDocuments",
                "desktop/SafeDealDealUpdateApproveDeal").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны быть фоточки ДКП с двух сторон под продавцом")
    public void shouldSeeDKPPagesPhotosBySeller() {
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").dkpFirstPagePhoto().dkpLink()
                .should(hasAttribute("href", DKP_FIRST_PAGE_URL));
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").dkpSecondPagePhoto().dkpLink()
                .should(hasAttribute("href", DKP_SECOND_PAGE_URL));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подтверждаем всю сделку под продавцом")
    public void shouldApproveDealBySeller() {
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").button("Подтвердить").click();

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithApprovedDealDocuments");
        mockRule.overwriteStub(3, "desktop/SafeDealDealUpdateRequestCode");

        basePageSteps.onDealPage().notifier().should(hasText("Документы подтверждены"));

        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").button("Запросить код").click();
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").input("Код из смс", "123456");

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithApprovedSeller");
        mockRule.overwriteStub(3, "desktop/SafeDealDealUpdateEnterCode");
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").stepWaitingDescription()
                .should(hasText("Подтверждение сделки\nПожалуйста, подождите, пока покупатель подтвердит сделку в " +
                        "своем личном кабинете и сверьтесь с дальнейшими шагами"));
    }
}