package ru.yandex.webmaster3.viewer.http.turbo.host;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.WebmasterCommonErrorType;
import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ActionResponse.ErrorResponse;
import ru.yandex.webmaster3.core.turbo.model.TurboHostHeader;
import ru.yandex.webmaster3.core.turbo.model.TurboHostHeaderType;
import ru.yandex.webmaster3.core.turbo.model.TurboHostSettings;
import ru.yandex.webmaster3.core.turbo.model.TurboHostSettingsBlock;
import ru.yandex.webmaster3.core.turbo.model.advertising.AdvertisingSettings;
import ru.yandex.webmaster3.core.turbo.model.analytics.AnalyticsSettings.GoogleAnalyticsSettings;
import ru.yandex.webmaster3.core.turbo.model.commerce.CartUrlType;
import ru.yandex.webmaster3.core.turbo.model.commerce.TurboCommerceInfoSection;
import ru.yandex.webmaster3.core.turbo.model.commerce.TurboCommerceSettings.TurboCommerceSettingsBuilder;
import ru.yandex.webmaster3.core.turbo.model.menu.TurboMenuItem;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.abt.ExperimentMapperService;
import ru.yandex.webmaster3.storage.abt.dao.AbtHostExperimentYDao;
import ru.yandex.webmaster3.storage.turbo.dao.TurboDomainsStateCHDao;
import ru.yandex.webmaster3.storage.turbo.service.preview.TurboFeedPreviewService;
import ru.yandex.webmaster3.storage.turbo.service.settings.SetTurboSettingsResponse.ErrorCode;
import ru.yandex.webmaster3.storage.turbo.service.settings.TurboSettingsMergerService;
import ru.yandex.webmaster3.storage.turbo.service.settings.TurboSettingsService;
import ru.yandex.webmaster3.storage.turbo.service.validation.TurboParserService;

import static ru.yandex.webmaster3.core.turbo.model.advertising.AdvertisingNetworkType.YANDEX;
import static ru.yandex.webmaster3.core.turbo.model.advertising.AdvertisingPlacement.MANUAL;

/**
 * Created by Oleg Bazdyrev on 05/09/2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class SetTurboSettingsActionTest {

    private static final WebmasterHostId HOST_ID = IdUtils.stringToHostId("http:example.com:80");
    private static final String DOMAIN = "example.com";

    private SetTurboSettingsAction setTurboSettingsAction;
    @InjectMocks
    private TurboSettingsMergerService turboSettingsMergerService;
    @Mock
    private AbtHostExperimentYDao abtHostExperimentYDao;
    @Mock
    private TurboSettingsService turboSettingsService;
    @Mock
    private TurboParserService turboParserService;
    @Mock
    private TurboFeedPreviewService turboFeedPreviewService;
    @Mock
    private TurboDomainsStateCHDao turboDomainsStateCHDao;
    @Mock
    private ExperimentMapperService experimentMapperService;

    private SetTurboSettingsRequest request = new SetTurboSettingsRequest();

    @Before
    public void init() throws Exception {
        setTurboSettingsAction = new SetTurboSettingsAction(turboFeedPreviewService, turboSettingsMergerService, turboSettingsService);
        request.setConvertedHostId(HOST_ID);
        Mockito.when(turboParserService.validateCommerceSettings(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet()))
                .thenCallRealMethod();
        Mockito.when(turboParserService.validateCheckoutSettings(Mockito.any(), Mockito.any())).thenCallRealMethod();
        Mockito.doNothing().when(abtHostExperimentYDao).delete(Mockito.any(), Mockito.any());
        Mockito.when(turboParserService.validateInfoSections(Mockito.any())).thenCallRealMethod();
        Mockito.when(turboSettingsService.getSettings(Mockito.eq(DOMAIN))).thenReturn(
                new TurboHostSettings.TurboHostSettingsBuilder().build());
//        Mockito.when(turboDomainsStateCHDao.getDomainState(Mockito.anyString(), Mockito.any())).thenReturn(TurboDomainsStateService.TurboDomainState.builder().marketFeeds(Collections.emptyList()).build());
//        Mockito.when(experimentMapperService.experimentIsActual(Mockito.anyString())).thenReturn(false);
        /*Mockito.when(turboSettingsService.getDesktopSettings(Mockito.eq(DOMAIN))).thenReturn(
                new TurboDesktopSettings.TurboDesktopSettingsBuilder().build());*/
    }

    @Test
    public void testHeaderNotSelectedResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader(null, null, null))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.HEADER));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__HEADER_NOT_SELECTED, response.getCode());
    }

    @Test
    public void testTooManyMenuItemsException() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setMenuSettings(Stream.generate(() -> createMenuItem("label", "http://some.url")).limit(220).collect(Collectors.toList()))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.MENU));
        ErrorResponse response = execute();
        Assert.assertEquals(WebmasterCommonErrorType.REQUEST__ILLEGAL_PARAMETER_VALUE, response.getCode());
    }

    @Test
    public void testEmptyMenuLabelResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setMenuSettings(Collections.singletonList(createMenuItem("", "http://some.url")))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.MENU));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__INVALID_MENU_LABEL, response.getCode());
    }

    @Test
    public void testEmptyMenuUrlResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setMenuSettings(Collections.singletonList(createMenuItem("Label", "vasya")))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.MENU));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__INVALID_MENU_URL, response.getCode());
    }

    @Test
    public void testLogoNotSetResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.SQUARE, null))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.HEADER));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__LOGO_NOT_SET, response.getCode());
    }

    @Test
    public void testDuplicateAliasResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setAdvertisingSettings(Arrays.asList(new AdvertisingSettings(YANDEX, MANUAL, "ad1", "id1", null),
                        new AdvertisingSettings(YANDEX, MANUAL, "ad1", "id2", null)))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.ADVERTISING));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__DUPLICATE_ALIAS, response.getCode());
    }

    @Test
    public void testDuplicateAnalyticsResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setAnalyticsSettings(Arrays.asList(new GoogleAnalyticsSettings("id1", null, null, null, null, null)
                        , new GoogleAnalyticsSettings("id1", null, null, null, null, null)))
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.ANALYTICS));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__DUPLICATE_ANALYTICS_CODE, response.getCode());
    }

    @Test
    public void testCartUrlFromOtherDomainResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setCommerceSettings(new TurboCommerceSettingsBuilder()
                        .setCartUrl("http://vasya.com/buy?id={offer_id}").setCartUrlType(CartUrlType.CUSTOM)
                        .setCartUrlEnabled(true).build())
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.CHECKOUT));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__INVALID_CART_URL, response.getCode());
    }

    @Test
    public void testCartUrlWithoutOfferIdResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setCommerceSettings(new TurboCommerceSettingsBuilder()
                        .setCartUrl("http://example.com/buy?id={ofid}").setCartUrlType(CartUrlType.CUSTOM)
                        .setCartUrlEnabled(true).build())
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.CHECKOUT));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__INVALID_CART_URL, response.getCode());
    }

    @Test
    public void testCartUrlOnlyOfferUrlResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setCommerceSettings(new TurboCommerceSettingsBuilder()
                        .setCartUrl("{offer_url}").setCartUrlType(CartUrlType.CUSTOM).setCartUrlEnabled(true).build())
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.CHECKOUT));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__INVALID_CART_URL, response.getCode());
    }

    @Test
    public void testTooManyInfoSectionsResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setCommerceSettings(new TurboCommerceSettingsBuilder()
                        .setCartUrl("http://example.com/buy?id={offer_id}").setCartUrlType(CartUrlType.CUSTOM)
                        .setInfoSections(Stream.generate(() -> new TurboCommerceInfoSection("title", "value")).limit(100L).collect(Collectors.toList()))
                        .setCartUrlEnabled(true).build())
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.INFOSECTIONS));
        ErrorResponse response = execute();
        Assert.assertEquals(WebmasterCommonErrorType.REQUEST__ILLEGAL_PARAMETER_VALUE, response.getCode());
    }

    @Test
    public void testDuplicateInfoSectionsResponse() throws Exception {
        request.setSettings(TurboHostSettings.builder()
                .setHeader(new TurboHostHeader("MyTitle", TurboHostHeaderType.NOLOGO, null))
                .setCommerceSettings(new TurboCommerceSettingsBuilder()
                        .setCartUrl("http://example.com/buy?id={offer_id}").setCartUrlType(CartUrlType.CUSTOM)
                        .setCartUrlEnabled(true)
                        .setInfoSections(Arrays.asList(new TurboCommerceInfoSection("title 1", "value 1"),
                                new TurboCommerceInfoSection("title 1", "value 2")))
                        .build())
                .build()
        );
        request.setSettingsBlocks(Arrays.asList(TurboHostSettingsBlock.INFOSECTIONS));
        ErrorResponse response = execute();
        Assert.assertEquals(ErrorCode.SET_TURBO_SETTINGS__DUPLICATE_COMMERCE_SECTION, response.getCode());
    }

    private <T extends ActionResponse> T execute() {
        try {
            return (T) setTurboSettingsAction.process(request);
        } catch (WebmasterException e) {
            return (T) e.getError();
        }
    }

    private static TurboMenuItem createMenuItem(String label, String url) {
        return new TurboMenuItem(null, label, url, null, null);
    }

}
