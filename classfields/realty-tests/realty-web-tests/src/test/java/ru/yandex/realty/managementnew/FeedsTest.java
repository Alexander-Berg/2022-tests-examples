package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mock.GetUserFeedsTemplate;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.nio.file.Path;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FEEDS;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.mock.GetUserFeedsTemplate.getUserFeedsTemplate;

@Tag(JURICS)
@Link("https://st.yandex-team.ru/VERTISTEST-1589")
@DisplayName("Фиды")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedsTest {

    private Path filePath;
    private String expectedFileName;
    private String actualFileName;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Rule
    public TemporaryFolder files = new TemporaryFolder();

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameters(name = "{index} формат {0}")
    public static Collection<String> testParams() {
        return asList("xml", "tsv", "xls");
    }

    @Before
    public void before() {
        GetUserFeedsTemplate template = getUserFeedsTemplate();
        String partnerId = template.getPartnerId();

        expectedFileName = format("feed_stats_%s.%s", partnerId, type);

        String uid = apiSteps.createRealty3JuridicalAccount(account).getId();

        mockRuleConfigurable
                .getUserFeeds(uid, template.build())
                .getUserFeedsOffersExport(uid, template.getPartnerId(),
                        getResourceAsString(format("mock/managementnew/feeds/%sformat.%s", type, type)))
                .createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW).path(FEEDS).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем что нужный файл скачался")
    public void shouldSeeDownloadedFile() {
        basePageSteps.onManagementNewPage().feeds().downloadLink(type).click();
        filePath = files.getRoot().toPath().resolve(expectedFileName);
        actualFileName = retrofitApiSteps
                .downloadReport(filePath, basePageSteps.session(), expectedFileName).getDownloadedFileName();
        assertThat(actualFileName).isEqualTo(expectedFileName);
    }
}
