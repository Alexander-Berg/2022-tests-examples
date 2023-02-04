package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOGOVOR_KUPLI_PRODAZHI;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница шаблона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocTemplateDownloadTest {

    private static final String EXPECTED_NAME = "kuplya-prodazha_dogovor";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    public TemporaryFolder files = new TemporaryFolder();

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Скачать шаблон»")
    public void shouldSeeDownload() {
        urlSteps.testing().path(DOKUMENTY).path(DOGOVOR_KUPLI_PRODAZHI).open();
        basePageSteps.onDocumentsPage().link("Скачать шаблон").click();
        String actualFileName = retrofitApiSteps
                .downloadReport(files.getRoot().toPath().resolve(EXPECTED_NAME), basePageSteps.session(),
                        EXPECTED_NAME + ".docx")
                .getDownloadedFileName();
        assertThat(actualFileName).isEqualTo(EXPECTED_NAME);
    }
}
