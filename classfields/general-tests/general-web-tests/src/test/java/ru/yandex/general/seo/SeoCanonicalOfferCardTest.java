package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import static org.hamcrest.Matchers.is;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.step.JSoupSteps.CANONICAL_LOCATOR;

@Epic(SEO_FEATURE)
@Feature("Canonical на страницах сервиса")
@DisplayName("Сео тесты на canonical на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoCanonicalOfferCardTest {

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем canonical на карточке оффера")
    public void shouldSeeOfferCardCanonical() {
        String offerCardUrl = jSoupSteps.getActualOfferCardUrl();
        jSoupSteps.uri(offerCardUrl).setDesktopUserAgent().get();
        String actualCanonical = jSoupSteps.select(CANONICAL_LOCATOR).attr(HREF);

        Assert.assertThat("Canonical на карточке соответствует", actualCanonical,
                is(jSoupSteps.testing().uri(offerCardUrl).toString()));
    }

}
