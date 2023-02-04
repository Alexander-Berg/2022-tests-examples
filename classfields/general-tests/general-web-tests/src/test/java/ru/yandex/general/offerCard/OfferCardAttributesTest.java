package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.consts.BaseConstants;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.Attributes.Typename.BOOLEAN;
import static ru.yandex.general.consts.Attributes.Typename.INPUT;
import static ru.yandex.general.consts.Attributes.Typename.MULTISELECT;
import static ru.yandex.general.consts.Attributes.Typename.SELECT;
import static ru.yandex.general.consts.Attributes.createAttribute;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.CONDITION;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Атрибуты")
@DisplayName("Отображение атрибутов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardAttributesTest {

    private static final String ID = "12345";

    private static final String SELECT_ID = "proizvoditel-mobilnogo-telefona_454ghb";
    private static final String SELECT_NAME = "Производитель";
    private static final String SELECT_VALUE = "Apple";
    private static final String BOOLEAN_ID = "vstroenniy-ekran_jDZ2bD";
    private static final String BOOLEAN_NAME = "Встроенный экран";
    private static final String MULTISELECT_NAME = "Тип питания";
    private static final String MULTISELECT_ID =  "tip-pitaniya_8_hCo8gj";
    private static final String MS_FIRST_VALUE = "от аккумулятора";
    private static final String MS_SECOND_VALUE = "от сети";
    private static final String INPUT_NAME = "Суммарная мощность";
    private static final String INPUT_ID = "summarnaya-moschnost_15558796_H7M8y9";
    private static final String INPUT_METRIC = "Вт";
    private static final String INPUT_VALUE_POSITIVE = "100";
    private static final String INPUT_VALUE_NEGATIVE = "-50";
    private static final String INPUT_VALUE_ZERO = "0";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута нового товара")
    public void shouldSeeNewConditionAttribute() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).removeAttributes()
                        .setCondition(BaseConstants.Condition.NEW)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(1));
        basePageSteps.onOfferCardPage().attributes().get(0).name().should(hasText(CONDITION));
        basePageSteps.onOfferCardPage().attributes().get(0).value().should(hasText("новый"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута б/у товара")
    public void shouldSeeUsedConditionAttribute() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).removeAttributes()
                        .setCondition(BaseConstants.Condition.USED)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(1));
        basePageSteps.onOfferCardPage().attributes().get(0).name().should(hasText(CONDITION));
        basePageSteps.onOfferCardPage().attributes().get(0).value().should(hasText("б/у"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «SELECT»")
    public void shouldSeeSelectAttribute() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(SELECT).setName(SELECT_NAME).setId(SELECT_ID)
                                .withSelectValue(SELECT_VALUE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(SELECT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(hasText(SELECT_VALUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «BOOLEAN» = true")
    public void shouldSeeBooleanTrueAttribute() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(BOOLEAN).setId(BOOLEAN_ID)
                                .setName(BOOLEAN_NAME).withBooleanValue(true))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(BOOLEAN_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(hasText("Да"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «BOOLEAN» = false")
    public void shouldSeeBooleanFalseAttribute() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(BOOLEAN).setId(BOOLEAN_ID)
                                .setName(BOOLEAN_NAME).withBooleanValue(false))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(BOOLEAN_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(hasText("Нет"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «MULTISELECT» с одним значением")
    public void shouldSeeMultiselectAttributeWithOneValue() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(MULTISELECT).setId(MULTISELECT_ID)
                                .setName(MULTISELECT_NAME).withMultiselectValue(MS_FIRST_VALUE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(MULTISELECT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(hasText(MS_FIRST_VALUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «MULTISELECT» с двумя значениями")
    public void shouldSeeMultiselectAttributeWithTwoValues() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(MULTISELECT).setId(MULTISELECT_ID)
                                .setName(MULTISELECT_NAME).withMultiselectValue(MS_FIRST_VALUE, MS_SECOND_VALUE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(MULTISELECT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(hasText(format("%s, %s", MS_FIRST_VALUE, MS_SECOND_VALUE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «INPUT» с положительным числом")
    public void shouldSeeInputAttributeWithPositiveValue() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(INPUT).setId(INPUT_ID)
                                .setName(INPUT_NAME).setMetric(INPUT_METRIC).withInputValue(INPUT_VALUE_POSITIVE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(INPUT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(
                hasText(format("%s %s", INPUT_VALUE_POSITIVE, INPUT_METRIC)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «INPUT» с отрицательным числом")
    public void shouldSeeInputAttributeWithNegativeValue() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(INPUT).setId(INPUT_ID)
                                .setName(INPUT_NAME).setMetric(INPUT_METRIC).withInputValue(INPUT_VALUE_NEGATIVE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(INPUT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(
                hasText(format("%s %s", INPUT_VALUE_NEGATIVE, INPUT_METRIC)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение атрибута типа «INPUT» с нулём")
    public void shouldSeeInputAttributeWithZeroValue() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setAttributes(
                        createAttribute(INPUT).setId(INPUT_ID)
                                .setName(INPUT_NAME).setMetric(INPUT_METRIC).withInputValue(INPUT_VALUE_ZERO))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().attributes().should(hasSize(2));
        basePageSteps.onOfferCardPage().attributes().get(1).name().should(hasText(INPUT_NAME));
        basePageSteps.onOfferCardPage().attributes().get(1).value().should(
                hasText(format("%s %s", INPUT_VALUE_ZERO, INPUT_METRIC)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение всех типов атрибутов на одной карточке для покупателя")
    public void shouldSeeAllTypesAttributesBuyer() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setCondition(BaseConstants.Condition.USED).setAttributes(
                        createAttribute(SELECT).setName(SELECT_NAME).setId(SELECT_ID).withSelectValue(SELECT_VALUE),
                        createAttribute(BOOLEAN).setId(BOOLEAN_ID).setName(BOOLEAN_NAME).withBooleanValue(true),
                        createAttribute(MULTISELECT).setId(MULTISELECT_ID)
                                .setName(MULTISELECT_NAME).withMultiselectValue(MS_FIRST_VALUE, MS_SECOND_VALUE),
                        createAttribute(INPUT).setId(INPUT_ID)
                                .setName(INPUT_NAME).setMetric(INPUT_METRIC).withInputValue(INPUT_VALUE_POSITIVE))
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(basePageSteps.onOfferCardPage().attributes().size())
                    .as("Кол-во атрибутов").isEqualTo(5);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(0).name().getText())
                    .as("Имя первого атрибута").isEqualTo(CONDITION);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(0).value().getText())
                    .as("Значение первого атрибута").isEqualTo("б/у");
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(1).name().getText())
                    .as("Имя второго атрибута").isEqualTo(SELECT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(1).value().getText())
                    .as("Значение второго атрибута").isEqualTo(SELECT_VALUE);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(2).name().getText())
                    .as("Имя третьего атрибута").isEqualTo(BOOLEAN_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(2).value().getText())
                    .as("Значение третьего атрибута").isEqualTo("Да");
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(3).name().getText())
                    .as("Имя четвертого атрибута").isEqualTo(MULTISELECT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(3).value().getText())
                    .as("Значение четвертого атрибута").isEqualTo(format("%s, %s", MS_FIRST_VALUE, MS_SECOND_VALUE));
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(4).name().getText())
                    .as("Имя пятого атрибута").isEqualTo(INPUT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(4).value().getText())
                    .as("Значение пятого атрибута").isEqualTo(format("%s %s", INPUT_VALUE_POSITIVE, INPUT_METRIC));
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение всех типов атрибутов на одной карточке для продавца")
    public void shouldSeeAllTypesAttributesOwner() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setCondition(BaseConstants.Condition.USED).setAttributes(
                        createAttribute(SELECT).setName(SELECT_NAME).setId(SELECT_ID).withSelectValue(SELECT_VALUE),
                        createAttribute(BOOLEAN).setId(BOOLEAN_ID).setName(BOOLEAN_NAME).withBooleanValue(true),
                        createAttribute(MULTISELECT).setId(MULTISELECT_ID)
                                .setName(MULTISELECT_NAME).withMultiselectValue(MS_FIRST_VALUE, MS_SECOND_VALUE),
                        createAttribute(INPUT).setId(INPUT_ID)
                                .setName(INPUT_NAME).setMetric(INPUT_METRIC).withInputValue(INPUT_VALUE_POSITIVE))
                        .setIsOwner(true).setStatisticsGraph(7).setVas().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(basePageSteps.onOfferCardPage().attributes().size())
                    .as("Кол-во атрибутов").isEqualTo(5);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(0).name().getText())
                    .as("Имя первого атрибута").isEqualTo(CONDITION);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(0).value().getText())
                    .as("Значение первого атрибута").isEqualTo("б/у");
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(1).name().getText())
                    .as("Имя второго атрибута").isEqualTo(SELECT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(1).value().getText())
                    .as("Значение второго атрибута").isEqualTo(SELECT_VALUE);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(2).name().getText())
                    .as("Имя третьего атрибута").isEqualTo(BOOLEAN_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(2).value().getText())
                    .as("Значение третьего атрибута").isEqualTo("Да");
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(3).name().getText())
                    .as("Имя четвертого атрибута").isEqualTo(MULTISELECT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(3).value().getText())
                    .as("Значение четвертого атрибута").isEqualTo(format("%s, %s", MS_FIRST_VALUE, MS_SECOND_VALUE));
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(4).name().getText())
                    .as("Имя пятого атрибута").isEqualTo(INPUT_NAME);
            s.assertThat(basePageSteps.onOfferCardPage().attributes().get(4).value().getText())
                    .as("Значение пятого атрибута").isEqualTo(format("%s %s", INPUT_VALUE_POSITIVE, INPUT_METRIC));
        });
    }

}
