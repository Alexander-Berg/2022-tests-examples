package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.events.CategoryChosen;
import ru.yandex.general.beans.events.Context;
import ru.yandex.general.beans.events.Event;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.beans.events.CategoryChosen.categoryChosen;
import static ru.yandex.general.beans.events.CategorySelectedFromPrediction.categorySelectedFromPrediction;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_CATEGORY_SELECT;
import static ru.yandex.general.consts.Events.CATEGORY_CHOSEN;
import static ru.yandex.general.consts.Events.PAGE_ADD_FORM;
import static ru.yandex.general.consts.FormConstants.Categories.SADOVII_INVENTAR;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_CATEGORY_CHOSEN;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ADD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_CATEGORY_CHOSEN)
@DisplayName("Отправка событий «categoryChosen»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormCategoryChoseEventTest {

    private static final String REGION_ID = "213";
    private static final String CATEGORY_ID = "komplekti-akustiki_dJ0Ivt";
    private static final String CATEGORY_NAME = "Комплекты акустики";
    private static final String CATEGORY_PATH = "/komplekti-akustiki/";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId", "eventInfo.categoryChosen.draftId", "eventInfo.categoryChosen.predictionId"};

    private Event event = event().setPortalRegionId(REGION_ID).setTrafficSource(trafficSource());
    private Context context = context().setBlock(BLOCK_CATEGORY_SELECT).setPage(PAGE_ADD_FORM);
    private CategoryChosen categoryChosen;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        offerAddSteps.withCategory(UMNIE_KOLONKI);
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «categoryChosen» при выборе категории из саджеста")
    public void shouldSeeCategoryChosenEvent() {
        offerAddSteps.fillToDescriptionStep();

        categoryChosen = categoryChosen().setCategoryId("umnie-kolonki_mQdWwr")
                .setCategorySelectedFromPrediction(categorySelectedFromPrediction()
                        .setCategoryId("umnie-kolonki_mQdWwr")
                        .setCategoryRank(1));

        event.setEventInfo(eventInfo().setCategoryChosen(categoryChosen))
                .setContext(context.setReferer(urlSteps.testing().path(ADD).path("/umnie-kolonki/").toString()));

        eventSteps.withEventType(CATEGORY_CHOSEN)
                .singleEventWithParams(event)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «categoryChosen» при выборе категории из второго саджеста")
    public void shouldSeeCategoryChosenEventFromSecondSuggest() {
        offerAddSteps.fillToCategoryStep();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().categorySelect().spanLink(CATEGORY_NAME).click();

        categoryChosen = categoryChosen().setCategoryId(CATEGORY_ID)
                .setCategorySelectedFromPrediction(categorySelectedFromPrediction()
                        .setCategoryId(CATEGORY_ID)
                        .setCategoryRank(1));

        event.setEventInfo(eventInfo().setCategoryChosen(categoryChosen))
                .setContext(context.setReferer(urlSteps.testing().path(ADD)
                        .path(CATEGORY_PATH).toString()));

        eventSteps.withEventType(CATEGORY_CHOSEN)
                .singleEventWithParams(event)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «categoryChosen» при выборе категории из попапа ручного выбора")
    public void shouldSeeCategoryChosenEventFromManualPopup() {
        offerAddSteps.fillToCategoryStep();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().modal().link("Работа").click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().modal().link("Резюме").click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().modal().link("Продажи").click();

        categoryChosen = categoryChosen().setCategoryId("rezume-prodazhi_cGJ7IQ");

        event.setEventInfo(eventInfo().setCategoryChosen(categoryChosen))
                .setContext(context.setReferer(urlSteps.toString()));

        eventSteps.withEventType(CATEGORY_CHOSEN)
                .singleEventWithParams(event)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «categoryChosen» при выборе категории с доп. выбором")
    public void shouldSeeCategoryChosenEventFromWithChoose() {
        offerAddSteps.withCategory(SADOVII_INVENTAR);
        offerAddSteps.fillToDescriptionStep();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onFormPage().modal().link("Лопаты и движки для снега").click();

        categoryChosen = categoryChosen().setCategoryId("lopati-i-dvizhki-dlya-snega_cDSqRj")
                .setCategorySelectedFromPrediction(categorySelectedFromPrediction()
                        .setCategoryId("sadoviy-inventar-i-instrumenti_cT3sJA")
                        .setCategoryRank(1));

        event.setEventInfo(eventInfo().setCategoryChosen(categoryChosen))
                .setContext(context.setReferer(urlSteps.testing().path(FORM).toString()));

        eventSteps.withEventType(CATEGORY_CHOSEN)
                .singleEventWithParams(event)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
