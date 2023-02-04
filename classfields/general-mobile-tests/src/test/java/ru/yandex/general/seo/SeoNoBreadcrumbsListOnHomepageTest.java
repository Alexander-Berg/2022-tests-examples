package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.BREADCRUMBS_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.page.BasePage.BREADCRUMB_LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

@Epic(SEO_FEATURE)
@Feature(BREADCRUMBS_SEO_MARK)
@DisplayName("Нет разметки «BreadcrumbsList» на главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoNoBreadcrumbsListOnHomepageTest {

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет разметки «BreadcrumbsList» на главной")
    public void shouldNotSeeBreadcrumbsListOnHomepage() {
        jSoupSteps.testing().setMobileUserAgent().get();

        jSoupSteps.noLdJsonMark(BREADCRUMB_LIST);
    }

}
