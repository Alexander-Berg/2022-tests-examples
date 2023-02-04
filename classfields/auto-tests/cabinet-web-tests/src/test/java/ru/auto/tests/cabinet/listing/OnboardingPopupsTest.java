package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.REMOVED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.SalesFiltersBlock.ALL_PARAMETERS;
import static ru.auto.tests.desktop.element.cabinet.SalesFiltersBlock.MINIMIZE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.page.cabinet.CabinetOffersPage.ARCHIVE;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.ARCHIVE_POPUP;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.GO_TO_ARCHIVE;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.GROUP_OPERATIONS_POPUP;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.I_KNOW;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.OFFER_FILTER_POPUP;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.OK_CLEARLY;
import static ru.auto.tests.desktop.page.cabinet.OnboardingPopup.SEE_ALL_PARAMETERS;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_ARCHIVE;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_FILTERS;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_MASS_ACTION;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@Story("Онбординг")
@DisplayName("Проверка онбординг попапов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class OnboardingPopupsTest {

    private static final String DATE_IN_PAST = "2022-02-01T12%3A24%3A02%2B03%3A00";
    private static final int MINIMIZED_FILTERS_COUNT = 11;
    private static final int MAXIMIZED_FILTERS_COUNT = 15;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/UserOffersAll"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/DesktopSidebarGet"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/UserOffersAllCount")
        ).create();

        cookieSteps.deleteCookie(IS_SHOWING_ONBOARDING_ARCHIVE);
        cookieSteps.deleteCookie(IS_SHOWING_ONBOARDING_MASS_ACTION);
        cookieSteps.deleteCookie(IS_SHOWING_ONBOARDING_FILTERS);

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).open();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа онбординга «Архив»")
    public void shouldSeeArchiveOnboardingPopup() {
        steps.onCabinetOffersPage().onboardingPopup().should(hasText(ARCHIVE_POPUP));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход в архив из попапа онбординга, проверяем ссылку и куки")
    public void shouldGoToArchiveFromOnboardingPopup() {
        mockRule.overwriteStub(1, stub("cabinet/UserOffersAllRemoved"));
        steps.onCabinetOffersPage().onboardingPopup().button(GO_TO_ARCHIVE).click();
        steps.onCabinetOffersPage().activeTab().waitUntil(hasText(ARCHIVE));

        urlSteps.addParam(STATUS, REMOVED).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_ARCHIVE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап онбординга «Архив», проверяем ссылку и куки")
    public void shouldCloseArchiveOnboardingPopup() {
        steps.onCabinetOffersPage().onboardingPopup().close().click();
        steps.onCabinetOffersPage().onboardingPopup().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_ARCHIVE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа онбординга «Групповые операции»")
    public void shouldSeeGroupOperationsOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().should(hasText(GROUP_OPERATIONS_POPUP));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем «Я в курсе» в попапе онбординга «Групповые операции»")
    public void shouldClickIKnowGroupOperationsOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().button(I_KNOW).click();
        steps.onCabinetOffersPage().onboardingPopup().waitUntil(not(isDisplayed()));

        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_MASS_ACTION);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем «Ок, понятно» в попапе онбординга «Групповые операции»")
    public void shouldClickOkGroupOperationsOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().button(OK_CLEARLY).click();
        steps.onCabinetOffersPage().onboardingPopup().waitUntil(not(isDisplayed()));

        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_MASS_ACTION);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа онбординга «Фильтр объявлений»")
    public void shouldSeeOffersFilterOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_MASS_ACTION, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().should(hasText(OFFER_FILTER_POPUP));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем «Смотреть все параметры» в попапе онбординга «Фильтр объявлений»")
    public void shouldClickSeeAllParametersOffersFilterOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_MASS_ACTION, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().salesFiltersBlock().button(ALL_PARAMETERS).should(isDisplayed());
        steps.onCabinetOffersPage().salesFiltersBlock().filtersList().should(hasSize(MINIMIZED_FILTERS_COUNT));

        steps.onCabinetOffersPage().onboardingPopup().button(SEE_ALL_PARAMETERS).click();
        steps.onCabinetOffersPage().onboardingPopup().waitUntil(not(isDisplayed()));

        steps.onCabinetOffersPage().salesFiltersBlock().button(MINIMIZE).should(isDisplayed());
        steps.onCabinetOffersPage().salesFiltersBlock().filtersList().should(hasSize(MAXIMIZED_FILTERS_COUNT));
        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_FILTERS);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап онбординга «Фильтр объявлений»")
    public void shouldCloseOffersFilterOnboardingPopup() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_MASS_ACTION, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().close().click();
        steps.onCabinetOffersPage().onboardingPopup().waitUntil(not(isDisplayed()));

        steps.onCabinetOffersPage().salesFiltersBlock().button(ALL_PARAMETERS).should(isDisplayed());
        steps.onCabinetOffersPage().salesFiltersBlock().filtersList().should(hasSize(MINIMIZED_FILTERS_COUNT));
        cookieSteps.shouldSeeCookie(IS_SHOWING_ONBOARDING_FILTERS);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается попап онбординга со всеми куками")
    public void shouldNotSeeOnboardingPopupWithAllCookies() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_ARCHIVE, DATE_IN_PAST);
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_MASS_ACTION, DATE_IN_PAST);
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_FILTERS, DATE_IN_PAST);
        steps.refresh();

        steps.onCabinetOffersPage().onboardingPopup().should(not(isDisplayed()));
    }

}
