package ru.yandex.webmaster3.storage.turbo.service.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.turbo.adv.model.response.ExtendedAttributeInfoResponse;
import ru.yandex.webmaster3.core.turbo.model.TurboHostSettingsBlock;
import ru.yandex.webmaster3.core.turbo.model.advertising.AdvertisingPlacement;
import ru.yandex.webmaster3.core.turbo.model.commerce.TurboCommerceSettings.TurboCommerceSettingsBuilder;
import ru.yandex.webmaster3.core.turbo.model.feedback.TurboFeedbackButton;
import ru.yandex.webmaster3.core.turbo.model.feedback.TurboFeedbackButtonType;
import ru.yandex.webmaster3.core.turbo.model.feedback.TurboFeedbackSettings;
import ru.yandex.webmaster3.core.turbo.model.feedback.TurboFeedbackSettings.Stick;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.host.service.HostOwnerService;
import ru.yandex.webmaster3.storage.util.JsonDBMapping;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Created by Oleg Bazdyrev on 26/09/2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class TurboParserServiceTest {
    private static final String RECORD = "{\"data\" : {\n" +
            "      \"type\" : \"context_on_site_rtb\",\n" +
            "      \"id\" : \"R-A-144467-31\",\n" +
            "      \"attributes\" : {\n" +
            "         \"site_version\" : \"%s\",\n" +
            "         \"page_id\" : 144467,\n" +
            "         \"design_templates\" : [\n" +
            "            {\n" +
            "               \"page_id\" : 144467,\n" +
            "               \"block_id\" : 31,\n" +
            "               \"design_settings\" : {\n" +
            "                  \"name\" : \"%s\"" +
            "               },\n" +
            "               \"is_custom_format_direct\" : false,\n" +
            "               \"caption\" : \"Исходный дизайн\",\n" +
            "               \"filter_tags\" : null,\n" +
            "               \"id\" : 1090888,\n" +
            "               \"type\" : \"tga\"\n" +
            "            },\n" +
            "            {\n" +
            "               \"page_id\" : 144467,\n" +
            "               \"block_id\" : 31,\n" +
            "               \"design_settings\" : {\n" +
            "                  \"filterSizes\" : true,\n" +
            "                  \"horizontalAlign\" : false\n" +
            "               },\n" +
            "               \"is_custom_format_direct\" : null,\n" +
            "               \"caption\" : \"General media design\",\n" +
            "               \"filter_tags\" : null,\n" +
            "               \"id\" : 5986496,\n" +
            "               \"type\" : \"media\"\n" +
            "            }],\n" +
            "         \"show_video\" : false,\n" +
            "         \"dsp_blocks\" : [\"%s\"]\n" +
            "      }}}";
    private static final WebmasterHostId EXAMPLE_1 = IdUtils.stringToHostId("http:example1.com:80");
    private static final WebmasterHostId EXAMPLE_2 = IdUtils.stringToHostId("http:example2.com:80");
    private static final WebmasterHostId HOST_ID = IdUtils.stringToHostId("http:example.com:80");

    @InjectMocks
    private TurboParserService turboParserService;
    @Mock
    private HostOwnerService hostOwnerService;

    @Before
    public void init() {
        Mockito.when(hostOwnerService.isSameOwner(Mockito.any(), Mockito.any())).thenReturn(true);
    }

    @Test
    public void testValidateFeedbackSettings_phoneNumber() throws Exception {
        TurboFeedbackSettings settings = turboParserService.validateFeedbackSettings(
                EXAMPLE_1, new TurboFeedbackSettings(Stick.LEFT, Arrays.asList(
                        new TurboFeedbackButton(TurboFeedbackButtonType.CALL, "88002000600", null, null, null)
                )));
        Assert.assertEquals(1, settings.getButtons().size());
        Assert.assertEquals("tel:88002000600", settings.getButtons().get(0).getUrl());

        settings = turboParserService.validateFeedbackSettings(
                EXAMPLE_1, new TurboFeedbackSettings(Stick.LEFT, Arrays.asList(
                        new TurboFeedbackButton(TurboFeedbackButtonType.CALL, "tel:vasya-pupkin", null, null, null)
                )));
        Assert.assertEquals(1, settings.getButtons().size());
        Assert.assertEquals("tel:vasya-pupkin", settings.getButtons().get(0).getUrl());
    }

    @Test
    public void testValidateFeedbackSettings_chat() throws Exception {
        TurboFeedbackSettings settings = turboParserService.validateFeedbackSettings(
                EXAMPLE_1, new TurboFeedbackSettings(Stick.LEFT, Arrays.asList(
                        new TurboFeedbackButton(TurboFeedbackButtonType.CHAT, null, null, null, null)
                )));
        Assert.assertEquals(1, settings.getButtons().size());
        Assert.assertEquals(TurboFeedbackButtonType.CHAT, settings.getButtons().get(0).getType());

        turboParserService.validateFeedbackSettings(
                EXAMPLE_2, new TurboFeedbackSettings(Stick.LEFT, Arrays.asList(
                        new TurboFeedbackButton(TurboFeedbackButtonType.CHAT, null, null, null, null)
                )));

    }

    @Test
    public void testCartUrlDomain() throws Exception {
        turboParserService.validateCommerceSettings(HOST_ID, new TurboCommerceSettingsBuilder()
                .setCartUrl("http://example.com/buy?id={offer_id}").setCartUrlEnabled(true).build(), null,
                EnumSet.allOf(TurboHostSettingsBlock.class));
        turboParserService.validateCommerceSettings(HOST_ID, new TurboCommerceSettingsBuilder()
                .setCartUrl("http://www.example.com/buy?id={offer_id}").setCartUrlEnabled(true).build(), null,
                EnumSet.allOf(TurboHostSettingsBlock.class));
        turboParserService.validateCommerceSettings(HOST_ID, new TurboCommerceSettingsBuilder()
                .setCartUrl("https://www.example.com/buy?id={offer_id}").setCartUrlEnabled(true).build(), null,
                EnumSet.allOf(TurboHostSettingsBlock.class));
    }

    @Test(expected = URISyntaxException.class)
    public void testCheckScheme() throws Exception {
        Assert.assertEquals("tg:1234", TurboParserService.checkScheme("tg:1234"));
        Assert.assertEquals("fb-messenger:vasya.pupkin", TurboParserService.checkScheme("fb-messenger:vasya.pupkin"));
        Assert.assertEquals("http://vasya.com", TurboParserService.checkScheme("vasya.com"));
        Assert.assertEquals("error", TurboParserService.checkScheme("unknown-protocol:unknown"));
    }

    @Test
    public void testEmail() throws Exception {
        //unnecessary stub: Mockito.when(hostOwnerService.isSameOwner(Mockito.eq(EXAMPLE_1), Mockito.eq(EXAMPLE_1))).thenReturn(true);
        try {
            turboParserService.validateEmail("bred", 0);
            Assert.assertTrue(false);
        } catch (TurboParserException e) {
            Assert.assertEquals(TurboParserException.ErrorCode.INVALID_FEEDBACK_EMAIL, e.getCode());
        }

        Assert.assertEquals("test@example1.com", turboParserService.validateEmail("mailto:test@example1.com", 0));
        try {
            turboParserService.validateEmail("info@http://www.example1.com", 0);
            Assert.assertTrue(false);
        } catch (TurboParserException e) {
            Assert.assertEquals(TurboParserException.ErrorCode.INVALID_FEEDBACK_EMAIL, e.getCode());
        }

        Assert.assertEquals("test@example1.com", turboParserService.validateEmail("mailto:test@example1.com", 0));
        try {
            turboParserService.validateEmail("info@https://example1.com", 0);
            Assert.assertTrue(false);
        } catch (TurboParserException e) {
            Assert.assertEquals(TurboParserException.ErrorCode.INVALID_FEEDBACK_EMAIL, e.getCode());
        }
    }

    @Test(expected = TurboParserException.class)
    public void testPartnerBlockValidation_AsideRightInvalid() throws Exception {
        final ExtendedAttributeInfoResponse blockInfo = JsonDBMapping.OM.readValue(String.format(RECORD,"desktop","posterVertical","100%x180"), ExtendedAttributeInfoResponse.class);
        TurboParserService.validatePartnerBlockInfo(blockInfo, AdvertisingPlacement.ASIDE_RIGHT, true);
    } //R-A-144467-31

    @Test(expected = TurboParserException.class)
    public void testPartnerBlockValidation_AsideLeftInvalid() throws Exception {
        final ExtendedAttributeInfoResponse blockInfo = JsonDBMapping.OM.readValue(String.format(RECORD,"desktop","240x400","300x600"), ExtendedAttributeInfoResponse.class);
        TurboParserService.validatePartnerBlockInfo(blockInfo, AdvertisingPlacement.ASIDE_LEFT, true);
    } //R-A-144467-31

    @Test
    public void testPartnerBlockValidation_ContentValid() throws Exception {
        final ExtendedAttributeInfoResponse blockInfo = JsonDBMapping.OM.readValue(String.format(RECORD,"desktop","posterHorizontal","728x90"), ExtendedAttributeInfoResponse.class);
        TurboParserService.validatePartnerBlockInfo(blockInfo, AdvertisingPlacement.CONTENT, true);

    } //R-A-144467-31

    @Test
    public void testPartnerBlockValidation_TopValid() throws Exception {
        final ExtendedAttributeInfoResponse blockInfo = JsonDBMapping.OM.readValue(String.format(RECORD,"turbo","modernAdaptive","320x50"), ExtendedAttributeInfoResponse.class);
        TurboParserService.validatePartnerBlockInfo(blockInfo, AdvertisingPlacement.TOP, false);
    } //R-A-144467-31


    @Test
    public void testDspBlockParsing(){
        TurboParserService.MediaSize mediaSize = TurboParserService.splitValue("100x200");
        Assert.assertEquals("100x200",mediaSize.getVal());
        Assert.assertEquals(100L,mediaSize.getHeight());
        Assert.assertEquals(200L,mediaSize.getWidth());
        mediaSize = TurboParserService.splitValue("100%x200%");
        Assert.assertEquals("100%x200%",mediaSize.getVal());
        Assert.assertEquals(0L,mediaSize.getHeight());
        Assert.assertEquals(0L,mediaSize.getWidth());

        mediaSize = TurboParserService.splitValue("100%x200");
        Assert.assertEquals("100%x200",mediaSize.getVal());
        Assert.assertEquals(0L,mediaSize.getHeight());
        Assert.assertEquals(200L,mediaSize.getWidth());
    }

}
