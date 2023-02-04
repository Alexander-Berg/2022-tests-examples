package ru.auto.tests.cabinet.autobidder;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.ACTIVE_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.SUSPENDED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.ACTIVE;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.PAUSED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Period.period;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getISO8601Date;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Отображение свернутой кампании")
@DisplayName("Отображение свернутой кампании")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderRowTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_AUTOBIDDER, DATE_IN_PAST);

        mockRule.setStubs(
                stub("cabinet/SessionDirectDealerAristos"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGetClientAristos"),
                stub("cabinet/DesktopClientsGetAristos"),
                stub("cabinet/DealerTariff/AllTariffs"));

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Название кампании")
    public void shouldSeeTitle() {
        String id = String.valueOf(getRandomShortInt());
        String name = getRandomString();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setId(id)
                                                .setName(name)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).title().should(hasText(
                format("#%s: %s", id, name)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Описание кампании")
    public void shouldSeeDescriprion() {
        String description = getRandomString();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setDescription(description)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).description().should(hasText(description));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Срок кампании, кампания еще не началась")
    public void shouldSeePeriodCampaignNotStarted() {
        int daysToStart = getRandomShortInt();
        int campaignPeriod = getRandomBetween(5, 20);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setPeriod(
                                                period().setFrom(getISO8601Date(daysToStart))
                                                        .setTo(getISO8601Date(daysToStart + campaignPeriod))
                                        )
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).period().should(hasText(matchesRegex(format(
                "Ещё %s (день|дня|дней)\\n%s", campaignPeriod + 1, formatPeriod(daysToStart, campaignPeriod)))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Срок кампании, кампания в процессе")
    public void shouldSeePeriodCampaignInProcess() {
        int daysToStart = -getRandomShortInt();
        int campaignPeriod = getRandomBetween(10, 20);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setPeriod(
                                                period().setFrom(getISO8601Date(daysToStart))
                                                        .setTo(getISO8601Date(daysToStart + campaignPeriod)))
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).period().should(hasText(matchesRegex(format(
                "Ещё %s (день|дня|дней) из %s\\n%s",
                campaignPeriod + daysToStart + 1, campaignPeriod + 1, formatPeriod(daysToStart, campaignPeriod)))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Статус кампании «Активная»")
    public void shouldSeeCampaignStatusActive() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setStatus(ACTIVE)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).status().should(hasText(ACTIVE_CAMPAIGN));
        steps.onAutobidderPage().rows().get(0).pause().should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).play().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Статус кампании «Приостановлена»")
    public void shouldSeeCampaignStatusPaused() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        getBaseCampaign().setStatus(PAUSED)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).status().should(hasText(SUSPENDED));
        steps.onAutobidderPage().rows().get(0).play().should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).pause().should(not(isDisplayed()));
    }


    private static String formatPeriod(int daysToStart, int campaignPeriod) {
        final String DAY_MONTH_YEAR_PATTERN = "d MMMM y";
        final String DAY_MONTH_PATTERN = "d MMMM";
        final String DAY_PATTERN = "d";

        String startDatePattern;
        String endDatePattern;

        LocalDate startDate = LocalDate.parse(LocalDate.now().toString()).plusDays(daysToStart);
        LocalDate endDate = LocalDate.parse(LocalDate.now().toString()).plusDays(daysToStart + campaignPeriod);

        if (startDate.getYear() != endDate.getYear()) {
            startDatePattern = DAY_MONTH_YEAR_PATTERN;
            endDatePattern = DAY_MONTH_YEAR_PATTERN;
        } else if (startDate.getMonth() != endDate.getMonth()) {
            startDatePattern = DAY_MONTH_PATTERN;
            endDatePattern = DAY_MONTH_PATTERN;
        } else {
            startDatePattern = DAY_PATTERN;
            endDatePattern = DAY_MONTH_PATTERN;
        }

        return format("%s — %s",
                startDate.format(getFormatter(startDatePattern)), endDate.format(getFormatter(endDatePattern)));
    }

    private static DateTimeFormatter getFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withLocale(new Locale("ru"));
    }

}
