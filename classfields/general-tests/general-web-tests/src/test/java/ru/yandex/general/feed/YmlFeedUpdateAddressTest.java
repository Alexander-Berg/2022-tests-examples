package ru.yandex.general.feed;

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
import ru.yandex.general.beans.toponyms.SuggestItem;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.mock.MockTaskInfo;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.FeedSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.UpdateUser.updateUser;
import static ru.yandex.general.beans.ajaxRequests.User.user;
import static ru.yandex.general.beans.card.Address.address;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.District.district;
import static ru.yandex.general.beans.card.GeoPoint.geoPoint;
import static ru.yandex.general.beans.card.MetroStation.metroStation;
import static ru.yandex.general.beans.feed.FatalError.fatalError;
import static ru.yandex.general.beans.feed.FeedTask.feedTask;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Notifications.CHANGES_SAVED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Button.ARIA_DISABLED;
import static ru.yandex.general.element.Button.FALSE;
import static ru.yandex.general.element.Button.TRUE;
import static ru.yandex.general.element.Popup.ADD;
import static ru.yandex.general.element.Popup.CANCEL;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTasks.getRandomDateTimeInPast;
import static ru.yandex.general.mock.MockToponyms.addressPaveleckaya;
import static ru.yandex.general.mock.MockToponyms.districtZamoskvorechye;
import static ru.yandex.general.mock.MockToponyms.mockToponyms;
import static ru.yandex.general.mock.MockToponyms.subwayParkKulturi;
import static ru.yandex.general.page.FeedPage.ADDRESS_INPUT;
import static ru.yandex.general.page.FeedPage.ADD_ADDRESS;
import static ru.yandex.general.page.FeedPage.FAILED;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_USER;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("YML фид")
@DisplayName("Добавляем адрес в YML фиде с ошибками")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class YmlFeedUpdateAddressTest {

    private static final String ADDRESS = "Павелецкая набережная, 2";
    private static final String DISTRICT = "Замоскворечье";
    private static final String METRO = "Парк Культуры";

    private MockResponse mockResponse = mockResponse()
            .setFeed(feedTemplate().build())
            .setTaskInfo(new MockTaskInfo(
                    feedTask().setTaskId(String.valueOf(getRandomIntInRange(1, 50)))
                            .setStatus(FAILED)
                            .setFinishedAt(getRandomDateTimeInPast())
                            .setFeedStatistics(null)
                            .setFatalErrors(asList(
                                    fatalError().setMessage("Нет адреса")
                                            .setDescription("<a href=\"#\" data-action=\"set-address\">" +
                                                    "Добавить адрес</a>")
                                            .setRequiredAction("SetAddress")))).build())
            .setFeedErrors(feedErrorsTemplate().build())
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    private SuggestItem suggestItem;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FeedSteps feedSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками - добавляем адрес")
    public void shouldSeeYmlFeedWithAddressErrorAddAddress() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();

        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(ADDRESS);
        feedSteps.onFeedPage().modal().menuItem(ADDRESS).click();
        feedSteps.onFeedPage().modal().button(ADD).click();
        feedSteps.onFeedPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(
                updateUser().setUser(user().setAddresses(asList(
                        address().setAddress(addressText().setAddress(suggestItem.getName())).setGeoPoint(
                                geoPoint().setLatitude(suggestItem.getPosition().getLatitude())
                                        .setLongitude(suggestItem.getPosition().getLongitude())))))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками - добавляем метро")
    public void shouldSeeYmlFeedWithAddressErrorAddSubway() {
        suggestItem = subwayParkKulturi();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();

        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(METRO);
        feedSteps.onFeedPage().modal().menuItem(METRO).click();
        feedSteps.onFeedPage().modal().button(ADD).click();
        feedSteps.onFeedPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(
                        updateUser().setUser(user().setAddresses(asList(
                                address().setMetroStation(
                                                metroStation().setId(suggestItem.getStationId())
                                                        .setLineIds(asList(suggestItem.getLine().getLineId()))
                                                        .setColors(asList(suggestItem.getLine().getColor()))
                                                        .setName(suggestItem.getName()))
                                        .setGeoPoint(geoPoint().setLatitude(suggestItem.getPosition().getLatitude())
                                                .setLongitude(suggestItem.getPosition().getLongitude()))))))
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками - добавляем район")
    public void shouldSeeYmlFeedWithAddressErrorAddDistrict() {
        suggestItem = districtZamoskvorechye();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();

        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(DISTRICT);
        feedSteps.onFeedPage().modal().menuItem(DISTRICT).click();
        feedSteps.onFeedPage().modal().button(ADD).click();
        feedSteps.onFeedPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(
                        updateUser().setUser(user().setAddresses(asList(
                                address().setDistrict(district().setId(suggestItem.getDistrictId())
                                                .setName(suggestItem.getName()))
                                        .setGeoPoint(geoPoint().setLatitude(suggestItem.getPosition().getLatitude())
                                                .setLongitude(suggestItem.getPosition().getLongitude()))))))
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, кнопка «Добавить» задизейблена без адреса")
    public void shouldSeeYmlFeedWithAddressErrorAddButtonDisabled() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, кнопка «Добавить» задизейблена с частично введенным адресом")
    public void shouldSeeYmlFeedWithAddressErrorAddButtonDisabledWithNotFullAddress() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys("Мо");
        feedSteps.onFeedPage().modal().spanLink("Добавить адрес").click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление адреса, модалка закрывается по кнопке «Отмена»")
    public void shouldSeeYmlFeedWithAddressErrorAddButtonCloseModal() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().button(CANCEL).click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление адреса, модалка закрывается по кнопке «Отмена» с введенным телефоном")
    public void shouldSeeYmlFeedWithAddressErrorAddButtonCloseModalWithPhone() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(ADDRESS);
        feedSteps.onFeedPage().modal().menuItem(ADDRESS).click();
        feedSteps.wait500MS();
        feedSteps.onFeedPage().modal().button(CANCEL).click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().should(not(isDisplayed()));
        ajaxProxySteps.setAjaxHandler(UPDATE_USER).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление адреса, очистка инпута адреса")
    public void shouldSeeYmlFeedWithAddressErrorClearAddressInput() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(ADDRESS);
        feedSteps.onFeedPage().modal().menuItem(ADDRESS).click();
        feedSteps.wait500MS();
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).clearInput().click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление адреса, кнопка «Добавить» дизейблится после очистки")
    public void shouldSeeYmlFeedWithAddressErrorAddButtonDisabledAfterClearAddressInput() {
        suggestItem = addressPaveleckaya();
        mockRule.graphqlStub(mockResponse.setToponyms(mockToponyms().setSuggest(suggestItem).build())
                .build()).withDefaults().create();
        urlSteps.open();

        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_ADDRESS).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).sendKeys(ADDRESS);
        feedSteps.onFeedPage().modal().menuItem(ADDRESS).click();
        feedSteps.onFeedPage().modal().button(ADD).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().modal().input(ADDRESS_INPUT).clearInput().click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

}
