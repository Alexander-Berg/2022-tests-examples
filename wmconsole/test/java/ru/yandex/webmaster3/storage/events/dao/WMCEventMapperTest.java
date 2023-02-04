package ru.yandex.webmaster3.storage.events.dao;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.metrika.counters.CounterBindingStateEnum;
import ru.yandex.webmaster3.core.metrika.counters.CounterRequestTypeEnum;
import ru.yandex.webmaster3.core.turbo.model.advertising.AdvertisingPlacement;
import ru.yandex.webmaster3.core.turbo.model.app.TurboAppSettings;
import ru.yandex.webmaster3.core.turbo.model.autoparser.AutoparserToggleState;
import ru.yandex.webmaster3.core.turbo.model.commerce.TurboPlusSettings;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.abt.model.Experiment;
import ru.yandex.webmaster3.storage.events.data.WMCEvent;
import ru.yandex.webmaster3.storage.events.data.WMCEventContent;
import ru.yandex.webmaster3.storage.events.data.WMCEventType;
import ru.yandex.webmaster3.storage.events.data.events.*;
import ru.yandex.webmaster3.storage.importanturls.data.ImportantUrlChange;
import ru.yandex.webmaster3.storage.user.message.content.MessageContent;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * @author Oleg Bazdyrev
 */
public class WMCEventMapperTest {

    @Test
    public void testDeserialize1() {
        String serialized = "{\"payload\":{\"hostId\":\"http:lenta.ru:80\",\"notificationType\":\"URL_SEARCH_STATUS_CHANGE\"," +
                "\"critical\":false,\"messageContent\":{\"hostId\":\"http:lenta.ru:80\",\"relativeUrl\":\"/\",\"oldSearchUrlStatus\":" +
                "{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\",\"httpCode\":301," +
                "\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\",\"relCanonicalTarget\":\"\"}," +
                "\"newSearchUrlStatus\":{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\"," +
                "\"httpCode\":301,\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\"," +
                "\"relCanonicalTarget\":\"\"},\"type\":\"URL_SEARCH_STATUS_CHANGE\",\"url\":\"http://lenta.ru/\",\"requiresHost\":true}}," +
                "\"payloadType\":\"USER_HOST_MESSAGE\"}";
        WMCEventContent object = WMCEventMapper.deserialize(WMCEventType.RETRANSLATE_TO_USERS, serialized);
        Assert.assertEquals(RetranslateToUsersEvent.class, object.getClass());
        var retranslateEvent = (RetranslateToUsersEvent) object;
        UserHostMessageEvent event = (UserHostMessageEvent) retranslateEvent.getPayload();
        MessageContent content = event.getMessageContent();
        Assert.assertEquals(ImportantUrlChange.SearchUrlStatusChange.class, content.getClass());
        List excludedUsers = retranslateEvent.getExcludedUsers();
        Assert.assertEquals(excludedUsers.size(), 0);
        Assert.assertNull(retranslateEvent.getExperiment());
    }

    @Test
    public void testDeserialize2() {
        String serialized = "{\"payload\":{\"hostId\":\"http:lenta.ru:80\",\"notificationType\":\"URL_SEARCH_STATUS_CHANGE\"," +
                "\"critical\":false,\"messageContent\":{\"hostId\":\"http:lenta.ru:80\",\"relativeUrl\":\"/\",\"oldSearchUrlStatus\":" +
                "{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\",\"httpCode\":301," +
                "\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\",\"relCanonicalTarget\":\"\"}," +
                "\"newSearchUrlStatus\":{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\"," +
                "\"httpCode\":301,\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\"," +
                "\"relCanonicalTarget\":\"\"},\"type\":\"URL_SEARCH_STATUS_CHANGE\",\"url\":\"http://lenta.ru/\",\"requiresHost\":true}}," +
                "\"payloadType\":\"USER_HOST_MESSAGE\",\"excludedUsers\":[5,123]}";
        WMCEventContent object = WMCEventMapper.deserialize(WMCEventType.RETRANSLATE_TO_USERS, serialized);
        Assert.assertEquals(RetranslateToUsersEvent.class, object.getClass());
        var retranslateEvent = (RetranslateToUsersEvent) object;
        UserHostMessageEvent event = (UserHostMessageEvent) retranslateEvent.getPayload();
        MessageContent content = event.getMessageContent();
        Assert.assertEquals(ImportantUrlChange.SearchUrlStatusChange.class, content.getClass());
        List excludedUsers = retranslateEvent.getExcludedUsers();
        Assert.assertEquals(excludedUsers.size(), 2);
        Assert.assertEquals(excludedUsers.get(0), 5L);
        Assert.assertEquals(excludedUsers.get(1), 123L);
        Assert.assertNull(retranslateEvent.getExperiment());
    }

    @Test
    public void testDeserialize3() {
        String serialized = "{\"payload\":{\"hostId\":\"http:lenta.ru:80\",\"notificationType\":\"URL_SEARCH_STATUS_CHANGE\"," +
                "\"critical\":false,\"messageContent\":{\"hostId\":\"http:lenta.ru:80\",\"relativeUrl\":\"/\",\"oldSearchUrlStatus\":" +
                "{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\",\"httpCode\":301," +
                "\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\",\"relCanonicalTarget\":\"\"}," +
                "\"newSearchUrlStatus\":{\"status\":\"NOT_MAIN_MIRROR\",\"region\":\"RUS\",\"addTime\":1484325080000,\"beautyUrl\":\"\"," +
                "\"httpCode\":301,\"mainHost\":\"\",\"mainPath\":\"\",\"mainRegion\":\"\",\"redirectTarget\":\"https://lenta.ru/\"," +
                "\"relCanonicalTarget\":\"\"},\"type\":\"URL_SEARCH_STATUS_CHANGE\",\"url\":\"http://lenta.ru/\",\"requiresHost\":true}}," +
                "\"payloadType\":\"USER_HOST_MESSAGE\", " +
                "\"experiment\":\"TEST_HOST_FIRST\", " +
                "\"excludedUsers\":[5,123]}";
        WMCEventContent object = WMCEventMapper.deserialize(WMCEventType.RETRANSLATE_TO_USERS, serialized);
        Assert.assertEquals(RetranslateToUsersEvent.class, object.getClass());
        var retranslateEvent = (RetranslateToUsersEvent) object;
        UserHostMessageEvent event = (UserHostMessageEvent) retranslateEvent.getPayload();
        MessageContent content = event.getMessageContent();
        Assert.assertEquals(ImportantUrlChange.SearchUrlStatusChange.class, content.getClass());
        List excludedUsers = retranslateEvent.getExcludedUsers();
        Assert.assertEquals(excludedUsers.size(), 2);
        Assert.assertEquals(excludedUsers.get(0), 5L);
        Assert.assertEquals(excludedUsers.get(1), 123L);

        Assert.assertEquals(Experiment.TEST_HOST_FIRST, retranslateEvent.getExperiment());
    }


    @Test
    public void testDeserializeMetrika() {
        String serialized = "{\"metrikaEvent\":\"0\"," +
                "\"domain\":\"lenta.ru\"," +
                "\"userId\":\"123\"," +
                "\"userLogin\":\"userName\"," +
                "\"counterId\":\"12345\"}";
        WMCEventContent object = WMCEventMapper.deserialize(WMCEventType.METRIKA_COUNTER_BINDING, serialized);
        Assert.assertEquals(MetrikaCounterBindingEvent.class, object.getClass());
        MetrikaCounterBindingEvent event = (MetrikaCounterBindingEvent) object;
        Assert.assertEquals(event.getCounterId(), 12345L);
        Assert.assertEquals(event.getInitiatorId(), 123L);
        Assert.assertEquals(event.getDomain(), "lenta.ru");
        Assert.assertEquals(event.getInitiatorLogin(), "userName");
        Assert.assertEquals(event.getMetrikaEvent(), CounterRequestTypeEnum.CREATE);
    }

    @Test
    public void testTurboSettings() {
        String s = "{\"domain\":\"khaliullin.info\",\"turboHostSettings\":{\"header\":{\"title\":\"Тайтл намбер фор\",\"type\":\"HORIZONTAL\",\"logoInfo\":{\"logoId\":\"2a00000162d96040d759907c3d7e52533862-5003\",\"logoUrls\":{\"SQUARE\":\"https://avatars.mdst.yandex.net/get-turbo/5003/2a00000162d96040d759907c3d7e52533862/logo_square_x10\",\"HORIZONTAL\":\"https://avatars.mdst.yandex.net/get-turbo/5003/2a00000162d96040d759907c3d7e52533862/logo_horizontal_s_x10\"},\"svg\":false}},\"advertisingSettings\":{\"CONTENT\":[{\"type\":\"YANDEX\",\"value\":\"R-A-229598-1\",\"data\":{\"id\":\"R-A-229598-1\"},\"displayType\":\"ID\"},{\"type\":\"ADFOX\",\"alias\":\"yandex_ad_2\",\"value\":\"<![CDATA[<script src=\\\"https://yastatic.net/pcode/adfox/loader.js\\\" crossorigin=\\\"anonymous\\\"></script><div id=\\\"adfox_150036761555038701\\\"></div><script>    window.Ya.adfoxCode.create({        ownerId: 257673,        containerId: 'adfox_150036761555038701',        params: {            pp: 'g',            ps: 'cmic',            p2: 'fqem'        }    });</script>]]>\",\"data\":{\"height\":\"300\",\"ownerId\":\"257673\",\"params\":{\"p2\":\"fqem\",\"pp\":\"g\",\"ps\":\"cmic\"},\"type\":\"AdFox\",\"width\":\"300\"},\"displayType\":\"CODE\"},{\"type\":\"YANDEX\",\"alias\":\"R-A-229598-1\",\"value\":\"R-A-229598-1\",\"data\":{\"id\":\"R-A-229598-1\"},\"displayType\":\"ID\"}]},\"analyticsSettings\":[{\"type\":\"YANDEX\",\"value\":\"123123\",\"data\":{\"id\":\"123123\"},\"displayType\":\"ID\"}],\"accessSettings\":{\"login\":\"alalaыdsf\",\"password\":\"12345dsf\"},\"menuSettings\":[{\"label\":\"Item 1\",\"url\":\"http://example.com/menu-item-1\"},{\"label\":\"Item 2\",\"url\":\"http://example.com/menu-item-2\"},{\"label\":\"Item 3\",\"url\":\"http://example.com/menu-item-3\"}],\"feedbackSettings\":{\"stick\":\"LEFT\",\"buttons\":[{\"type\":\"CALL\",\"url\":\"tel:123\"},{\"type\":\"MAIL\",\"url\":\"mailto:vas@khaliullin.info\"},{\"type\":\"CALLBACK\",\"sendTo\":\"vasya@khaliullin.info\"},{\"type\":\"CHAT\"}]},\"css\":\"TODO\",\"autoRelated\":false,\"eCommerceSettings\":{\"cartUrl\":\"http://khaliullin.info/order?id={offer_id}\",\"cartUrlEnabled\":true,\"checkoutEmail\":\"\",\"checkoutEmailEnabled\":false,\"infoSections\":[{\"title\":\"Section 1\",\"value\":\"Text for section 1\"},{\"title\":\"Section 2\",\"value\":\"Text for section 2\"}],\"parsedAccordion\":[{\"value\":[{\"content\":\"Text for section 1\",\"content_type\":\"paragraph\"}],\"title\":\"Section 1\"},{\"value\":[{\"content\":\"Text for section 2\",\"content_type\":\"paragraph\"}],\"title\":\"Section 2\"}]},\"userAgreement\":{\"agreementCompany\":\"Khaliullin and partners LTD\",\"agreementLink\":\"https://example.com\"}},\"autoparserToggleState\":\"ABSENT\",\"feedSettingsList\":[{\"domain\":\"khaliullin.info\",\"type\":\"YML\",\"url\":\"http://khaliullin.info/rss/empty.xml\",\"active\":false,\"state\":\"DOWNLOADING\",\"addDate\":1537191422332,\"validateDate\":1538473111382}]}";
        TurboSettingsChangeEvent event = (TurboSettingsChangeEvent) WMCEventMapper.deserialize(WMCEventType.TURBO_SETTINGS_CHANGE, s);
        Assert.assertEquals(1, event.getTurboHostSettings().getAdvertisingSettings().size());
        Assert.assertEquals(3, event.getTurboHostSettings().getAdvertisingSettings().get(AdvertisingPlacement.CONTENT).size());
    }

    @Test
    public void testTurboSettings_v2() {
        String s = "{\"domain\":\"khaliullin.info\",\"turboHostSettings\":{\"header\":{\"title\":\"Тайтл намбер фор\",\"type\":\"HORIZONTAL\"," +
                "\"logoInfo\":{\"logoId\":\"2a00000162d96040d759907c3d7e52533862-5003\",\"logoUrls\":" +
                "{\"SQUARE\":\"https://avatars.mdst.yandex.net/get-turbo/5003/2a00000162d96040d759907c3d7e52533862/logo_square_x10\"," +
                "\"HORIZONTAL\":\"https://avatars.mdst.yandex.net/get-turbo/5003/2a00000162d96040d759907c3d7e52533862/logo_horizontal_s_x10\"},\"svg\":false}}," +
                "\"advertisingSettings\":[{\"type\":\"YANDEX\",\"value\":\"R-A-229598-1\",\"data\":{\"id\":\"R-A-229598-1\"},\"displayType\":\"ID\"}," +
                "{\"type\":\"ADFOX\",\"alias\":\"yandex_ad_2\",\"value\":\"<![CDATA[<script src=\\\"https://yastatic.net/pcode/adfox/loader.js\\\" crossorigin=\\\"anonymous\\\"></script><div id=\\\"adfox_150036761555038701\\\"></div><script>    window.Ya.adfoxCode.create({        ownerId: 257673,        containerId: 'adfox_150036761555038701',        params: {            pp: 'g',            ps: 'cmic',            p2: 'fqem'        }    });</script>]]>\"," +
                "\"data\":{\"height\":\"300\",\"ownerId\":\"257673\",\"params\":{\"p2\":\"fqem\",\"pp\":\"g\",\"ps\":\"cmic\"},\"type\":\"AdFox\",\"width\":\"300\"}," +
                "\"displayType\":\"CODE\"},{\"type\":\"YANDEX\",\"alias\":\"R-A-229598-1\",\"value\":\"R-A-229598-1\",\"data\":{\"id\":\"R-A-229598-1\"}," +
                "\"displayType\":\"ID\"}],\"analyticsSettings\":[{\"type\":\"YANDEX\",\"value\":\"123123\",\"data\":{\"id\":\"123123\"},\"displayType\":\"ID\"}]," +
                "\"accessSettings\":{\"login\":\"alalaыdsf\",\"password\":\"12345dsf\"},\"menuSettings\":[{\"label\":\"Item 1\"," +
                "\"url\":\"http://example.com/menu-item-1\"},{\"label\":\"Item 2\",\"url\":\"http://example.com/menu-item-2\"}," +
                "{\"label\":\"Item 3\",\"url\":\"http://example.com/menu-item-3\"}]," +
                "\"feedbackSettings\":{\"stick\":\"LEFT\",\"buttons\":[{\"type\":\"CALL\",\"url\":\"tel:123\"}," +
                "{\"type\":\"MAIL\",\"url\":\"mailto:vas@khaliullin.info\"},{\"type\":\"CALLBACK\",\"sendTo\":\"vasya@khaliullin.info\"}," +
                "{\"type\":\"CHAT\"}]},\"css\":\"TODO\",\"autoRelated\":false,\"eCommerceSettings\":{\"cartUrl\":\"http://khaliullin.info/order?id={offer_id}\",\"cartUrlEnabled\":true,\"checkoutEmail\":\"\",\"checkoutEmailEnabled\":false,\"infoSections\":[{\"title\":\"Section 1\",\"value\":\"Text for section 1\"},{\"title\":\"Section 2\",\"value\":\"Text for section 2\"}],\"parsedAccordion\":[{\"value\":[{\"content\":\"Text for section 1\",\"content_type\":\"paragraph\"}],\"title\":\"Section 1\"},{\"value\":[{\"content\":\"Text for section 2\",\"content_type\":\"paragraph\"}],\"title\":\"Section 2\"}]},\"userAgreement\":{\"agreementCompany\":\"Khaliullin and partners LTD\",\"agreementLink\":\"https://example.com\"}},\"autoparserToggleState\":\"ABSENT\",\"feedSettingsList\":[{\"domain\":\"khaliullin.info\",\"type\":\"YML\",\"url\":\"http://khaliullin.info/rss/empty.xml\",\"active\":false,\"state\":\"DOWNLOADING\",\"addDate\":1537191422332,\"validateDate\":1538473111382}]}";
        TurboSettingsChangeEvent event = (TurboSettingsChangeEvent) WMCEventMapper.deserialize(WMCEventType.TURBO_SETTINGS_CHANGE, s);
        Assert.assertEquals(2, event.getTurboHostSettings().getAdvertisingSettings().size());
    }

    @Test
    public void testTurboSettings_v3() {
        String s = "{\"domain\":\"sk35.ru\",\"turboHostSettings\":{\"header\":{\"type\":\"NOLOGO\"},\"advertisingSettings\":[],\"analyticsSettings\":[{\"type\":\"YANDEX\",\"value\":\"40856894\",\"data\":{\"id\":\"40856894\"},\"displayType\":\"ID\"}],\"accessSettings\":{\"login\":\"\",\"password\":\"\"},\"menuSettings\":[{\"label\":\"�.ома из б�.�.�.а\",\"url\":\"https://sk35.ru/doma_iz_brusa/\"},{\"label\":\"С�.�.б�. из б�.�.�.а\",\"url\":\"https://sk35.ru/srub_iz_brusa/\"},{\"label\":\"�.���.ка�.н�.е дома\",\"url\":\"https://sk35.ru/karkasnye_doma/\"},{\"label\":\"�.ани из б�.�.�.а\",\"url\":\"https://sk35.ru/bani_iz_brusa/\"},{\"label\":\"Узна�.�. �.ен�.\",\"url\":\"https://sk35.ru/kalkulyator/\"}],\"feedbackSettings\":{\"stick\":\"RIGHT\"},\"autoRelated\":false,\"userAgreement\":{\"agreementCompany\":\"\",\"agreementLink\":\"\"}},\"autoparserToggleState\":\"ABSENT\",\"feedSettingsList\":[{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/rss1.html\",\"active\":false,\"state\":\"DOWNLOADING\",\"addDate\":1549044751337,\"validateDate\":1549044754137},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/domasrub.html\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1548439911590,\"validateDate\":1548439978437},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/rss_karkasnye_doma.xml\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1549622323640,\"validateDate\":1549622328775},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/index.php?yandex_feed=news\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1548177597980,\"validateDate\":1548242147064},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/feed/yanturbo/\",\"active\":false,\"state\":\"DOWNLOADING\",\"addDate\":1548180780829,\"validateDate\":1548180784548},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/sk35.xml\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1549991969098,\"validateDate\":1549992787750},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/karkasnyedomak.html\",\"active\":false,\"state\":\"DELETED\",\"addDate\":1548617453091,\"validateDate\":1548617454835},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/sk35-1.xml\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1550146641053,\"validateDate\":1550146643544},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/rss2.xml\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1549045547356,\"validateDate\":1549701663625},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/srubizbrusa.html\",\"active\":true,\"state\":\"DOWNLOADING\",\"addDate\":1548248216158,\"validateDate\":1548444258969},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/yandex/news\",\"active\":false,\"state\":\"DELETED\",\"addDate\":1548177659149,\"validateDate\":1548177671924},{\"domain\":\"sk35.ru\",\"type\":\"RSS\",\"url\":\"https://sk35.ru/srubizbrusa1.html\",\"active\":false,\"state\":\"DOWNLOADING\",\"addDate\":1548612473566,\"validateDate\":1548612479961}]}";
        TurboSettingsChangeEvent event = (TurboSettingsChangeEvent) WMCEventMapper.deserialize(WMCEventType.TURBO_SETTINGS_CHANGE, s);
        Assert.assertEquals(0, event.getTurboHostSettings().getAdvertisingSettings().size());
    }

    @Test
    public void testTurboSettings_v4() {
        TurboSettingsChangeEvent event = new TurboSettingsChangeEvent("domain", null, null, TurboAppSettings.builder().enabled(true).build(),
                TurboPlusSettings.builder().safeShopping(true).build(), AutoparserToggleState.ABSENT, null);
        String s = WMCEventMapper.serializeContent(new WMCEvent(UUID.randomUUID(), WMCEventType.TURBO_SETTINGS_CHANGE, event, 0L));
        TurboSettingsChangeEvent event2 = (TurboSettingsChangeEvent) WMCEventMapper.deserialize(WMCEventType.TURBO_SETTINGS_CHANGE, s);
    }

        @Test
    public void testDeserializeMetrikaChange() {
        String serialized = "{\"metrikaEvent\":\"0\"," +
                "\"domain\":\"lenta.ru\"," +
                "\"userId\":\"123\"," +
                "\"userLogin\":\"userName\"," +
                "\"prevState\":\"0\"," +
                "\"state\":\"3\"," +
                "\"counterId\":\"12345\"}";
        WMCEventContent object = WMCEventMapper.deserialize(WMCEventType.METRIKA_COUNTER_BINDING_STATE_CHANGE, serialized);
        Assert.assertEquals(MetrikaCounterBindingStateChangeEvent.class, object.getClass());
        MetrikaCounterBindingStateChangeEvent event = (MetrikaCounterBindingStateChangeEvent) object;
        Assert.assertEquals(event.getCounterId(), 12345L);
        Assert.assertEquals(event.getInitiatorId(), 123L);
        Assert.assertEquals(event.getDomain(), "lenta.ru");
        Assert.assertEquals(event.getInitiatorLogin(), "userName");
        Assert.assertEquals(event.getMetrikaEvent(), CounterRequestTypeEnum.CREATE);
        Assert.assertEquals(event.getPrevState(), CounterBindingStateEnum.NONE);
        Assert.assertEquals(event.getState(), CounterBindingStateEnum.APPROVED);
    }

    @Test
    public void testSerialize() {
        WebmasterHostId hostId = IdUtils.urlToHostId("lenta.ru");
        UserHostMessageEvent<MessageContent.TurboListingsAvailable> event = new UserHostMessageEvent<>(
                hostId, null, new MessageContent.TurboListingsAvailable(hostId), NotificationType.TURBO_LISTINGS_NEW, false);
        Assert.assertEquals("{\"payload\":{\"hostId\":\"http:lenta.ru:80\",\"notificationType\":\"TURBO_LISTINGS_NEW\",\"critical\":false,\"messageContent\":{\"hostId\":\"http:lenta.ru:80\",\"type\":\"TURBO_LISTINGS_AVAILABLE\",\"requiresHost\":true}},\"excludedUsers\":[],\"payloadType\":\"USER_HOST_MESSAGE\"}",
                WMCEventMapper.serializeContent(WMCEvent.create(new RetranslateToUsersEvent<>(event))));
    }
}
