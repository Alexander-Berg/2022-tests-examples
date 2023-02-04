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
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;
import static ru.yandex.realty.page.DocumentsPage.DOCUMENTS_DOWNLOAD;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница документов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DownloadDocumentsPageTest {

    private static final String EXPECTED_NAME = "kuplya-prodazha_dogovor_peredatochnyj-akt_raspiska";

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
    @DisplayName("Клик «Скачать шаблоны»")
    public void shouldSeeDownload() {
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().link(DOCUMENTS_DOWNLOAD).click();
        String actualFileName = retrofitApiSteps
                .downloadReport(files.getRoot().toPath().resolve(EXPECTED_NAME), basePageSteps.session(),
                        EXPECTED_NAME + ".zip").getDownloadedFileName();
        assertThat(actualFileName).isEqualTo(EXPECTED_NAME);
    }
}
