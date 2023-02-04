package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.JOB_POSTING_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.page.BasePage.JOB_POSTING;

@Epic(SEO_FEATURE)
@Feature(JOB_POSTING_SEO_MARK)
@DisplayName("Нет JobPosting разметки на карточках резюме и товара")
@RunWith(Parameterized.class)
@GuiceModules(GeneralRequestModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SeoNoJobPostingMarkInNoVacancyCardTest {

    private String offerCardUrl;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"На карточке «Резюме»", "/rabota/rezume/"},
                {"На карточке товара", ELEKTRONIKA}

        });
    }

    @Before
    public void before() {
        offerCardUrl = jSoupSteps.testing().path(MOSKVA).path(path).getOfferCardUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет ShemaOrg JobPosting разметки на карточках резюме и товара")
    public void shouldNotSeeJobPostingShemaOrgMark() {
        jSoupSteps.testing().path(offerCardUrl).setMobileUserAgent().get();

        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/JobPosting']").html(),
                equalTo(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет LdJson JobPosting разметки на карточках резюме и товара")
    public void shouldNotSeeJobPostingLdJsonMark() {
        jSoupSteps.testing().path(offerCardUrl).setMobileUserAgent().get();

        jSoupSteps.noLdJsonMark(JOB_POSTING);
    }

}
