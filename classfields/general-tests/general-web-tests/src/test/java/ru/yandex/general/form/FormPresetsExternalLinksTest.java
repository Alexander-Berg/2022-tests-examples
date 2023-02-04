package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(ADD_FORM_FEATURE)
@DisplayName("Ссылки в пресетах формы на авто/недвижимость")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FormPresetsExternalLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String linkName;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Авто и мото", "https://auto.ru/cars/used/add/?from=classified&utm_content=form&utm_source=yandex_ads"},
                {"Недвижимость", "https://realty.yandex.ru/management-new/add/?from=classified&utm_content=form&utm_source=yandex_ads"}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в пресетах формы на авто/недвижимость")
    public void shouldSeePresetLink() {
        urlSteps.testing().path(FORM).open();
        offerAddSteps.onFormPage().sectionLink(linkName).should(hasAttribute(HREF, link));
    }

}
