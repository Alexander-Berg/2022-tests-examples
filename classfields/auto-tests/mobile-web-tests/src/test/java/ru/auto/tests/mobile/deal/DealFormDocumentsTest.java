package ru.auto.tests.mobile.deal;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Личная информация»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealFormDocumentsTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";

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

    @Parameterized.Parameter
    public String getDealMockFirst;

    @Parameterized.Parameter(1)
    public String getDealMockSecond;

    @Parameterized.Parameter(2)
    public String updateDealMock;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {
                        "desktop-lk/SafeDealDealGetWithOffer",
                        "desktop-lk/SafeDealDealGetDocumentsCheck",
                        "desktop/SafeDealDealUpdateDocuments"
                },
                {
                        "desktop-lk/SafeDealDealGetWithOfferForBuyer",
                        "desktop-lk/SafeDealDealGetBuyerDocumentsCheck",
                        "desktop/SafeDealDealUpdateBuyerDocuments"
                }
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop/SuggestionsApiRSSuggestFioForSafeDeal",
                "desktop/SuggestionsApiRSSuggestAddressLeoTolstoy",
                "desktop/SuggestionsApiRSSuggestAddressNovosibirsk",
                "desktop/SuggestionsApiRSSuggestFms",
                "desktop/SuggestionsApiRSSuggestParty",
                getDealMockFirst,
                updateDealMock).post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заполняем блок с документами под продавцом")
    public void shouldFillDocumentsBlock() {
        basePageSteps.onDealPage().section("Личная информация").input("Фамилия", "Иванов");
        basePageSteps.onDealPage().section("Личная информация").input("Имя", "Иван");
        basePageSteps.onDealPage().section("Личная информация").input("Отчество", "Аркадиевич");
        basePageSteps.onDealPage().section("Личная информация").input("Серия и номер паспорта", "1111111111");
        basePageSteps.onDealPage().section("Личная информация").input("Дата выдачи", "21.01.2004");
        basePageSteps.onDealPage().section("Личная информация").input("Код подразделения", "111111");
        basePageSteps.onDealPage().section("Личная информация").input("Кем выдан", "Химки");
        basePageSteps.onDealPage().geoSuggest().getItem(0).click();
        basePageSteps.onDealPage().section("Личная информация").input("Дата рождения", "31.01.1983");
        basePageSteps.onDealPage().section("Личная информация").input("Место рождения", "новосибирск");
        basePageSteps.onDealPage().geoSuggest().getItem(0).click();
        basePageSteps.onDealPage().section("Личная информация").input("Адрес регистрации", "Льва Толстого, 16");
        basePageSteps.onDealPage().geoSuggest().getItem(0).click();

        mockRule.overwriteStub(7, getDealMockSecond);

        basePageSteps.onDealPage().section("Личная информация").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().section("Личная информация").stepCheck().should(hasText("Личная информация\n" +
                "Проверяем паспортные данные. Это может занять несколько минут"));
    }
}
