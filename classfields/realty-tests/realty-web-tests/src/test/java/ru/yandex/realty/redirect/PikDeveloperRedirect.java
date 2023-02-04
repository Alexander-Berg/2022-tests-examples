package ru.yandex.realty.redirect;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtySeoModule;
import ru.yandex.realty.step.SeoTestSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.REDIRECTS;

@Feature(REDIRECTS)
@DisplayName("Редирект со страницы застройщика ПИК на промо страницу")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtySeoModule.class)
public class PikDeveloperRedirect {

    @Inject
    private SeoTestSteps seoTestSteps;

    @Ignore("Нет редиректа?")
    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редирект со страницы застройщика ПИК на промо страницу")
    public void shouldSeePikPromoUrl() {
        String actualUrl = seoTestSteps.getNetworkResponseUrl(
                format("%s/moskva_i_moskovskaya_oblast/zastroyschik/gruppa-kompanij-pik-52308/",
                        seoTestSteps.getTesting()));
        assertThat(actualUrl, equalTo(format("%s/pik/", seoTestSteps.getTesting())));
    }

}
