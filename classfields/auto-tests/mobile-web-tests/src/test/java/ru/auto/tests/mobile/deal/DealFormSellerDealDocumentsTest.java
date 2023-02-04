package ru.auto.tests.mobile.deal;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка. Форма. Блок «Личная встреча и подтверждение сделки»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealFormSellerDealDocumentsTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";
    private final static String URL_TEMPLATE = "https://%s/download-deal-agreement/%s/";

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
                "desktop-lk/SafeDealDealGetWithDealDocuments").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Описание шага с документами сделки под продавцом")
    public void shouldSeeDealDocumentsStepDescriptionBySeller() {
        basePageSteps.onDealPage().section("Документы для\u00a0сделки").should(hasText("Документы для сделки\n" +
                "Проверьте корректность информации, указанной в договоре.\nПокупатель распечатает список необходимых " +
                "документов\nДоговор купли-продажи\nПредзаполненный договор\nПосмотреть\nПодробнее о личной встрече"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на ДКП")
    public void shouldSeeDownloadDKPLinkBySeller() {
        basePageSteps.onDealPage().section("Документы для\u00a0сделки").button("Посмотреть")
                .should(hasAttribute("href", format(URL_TEMPLATE, urlSteps.getConfig().getBaseDomain(), DEAL_ID)));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Описание шага с личной встречей и подтверждением сделки под продавцом")
    public void shouldSeeMeetingStepDescriptionBySeller() {
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").should(hasText("Личная встреча и " +
                "подтверждение сделки\nШаг 4 из 4.\n Подписанный договор\nПодождите, пока покупатель загрузит фото " +
                "подписанного договора купли-продажи\nСвязаться с покупателем"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открываем попап со списком проверки документов под продавцом")
    public void shouldSeePopupWithDocumentsChecklistBySeller() {
        basePageSteps.onDealPage().section("Документы для\u00a0сделки").button("Подробнее о личной встрече").click();
        basePageSteps.onDealPage().popup().should(isDisplayed()).should(hasText("Личная встреча\n" +
                "Внимательно ознакомьтесь с шагами, которые вам нужно будет пройти на встрече вместе с  покупателем\n" +
                "Покупатель распечатает и привезет документы на встречу\nСверьте паспортные данные покупателя с " +
                "данными в договоре\nУбедитесь, что покупатель подписал все 3 экземпляра документов\nПодпишите договор " +
                "купли-продажи автомобиля в 3-х экземплярах\nПоставьте подпись в ПТС в поле “подпись прежнего " +
                "собственника”\nПроверьте, что покупатель загрузил фото подписанного договора в карточку сделки\n" +
                "Подтвердите факт состоявшейся сделки кодом из СМС\nУбедитесь, что покупатель подтвердил сделку на " +
                "Авто.ру\nПередайте покупателю ключи от автомобиля, ПТС и СТС\nСвязаться с покупателем"));
    }
}
