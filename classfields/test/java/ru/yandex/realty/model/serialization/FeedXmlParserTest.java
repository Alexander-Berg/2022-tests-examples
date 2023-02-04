package ru.yandex.realty.model.serialization;

import com.google.protobuf.StringValue;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.XbiFactory;
import ru.yandex.common.serialization.parser.xbi.XmlUtils;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.realty.model.offer.OfferCampaignType;
import ru.yandex.realty.model.raw.*;
import ru.yandex.realty.model.serialization.extractor.RawOfferXbiFactory;
import ru.yandex.realty.proto.WeekDay;
import ru.yandex.realty.proto.offer.VirtualTour;
import ru.yandex.realty.proto.offer.vos.Offer;
import ru.yandex.realty.proto.unified.offer.images.Image;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 * @author aherman
 */
public class FeedXmlParserTest {

    public XMLStreamReader getReader(String file) throws XMLStreamException {
        String base = "./feed/";

        return XmlUtils.createReader(
                getClass().getClassLoader().getResourceAsStream(base + file)
        );
    }

    private static final String TEST_XML = "test.xml";

    private static final String TEST_XML1 = "test1.xml";

    private static final String TEST_XML2 = "test2.xml";

    private static final String TEST_XML4 = "test4.xml";

    private final static String TEST_COMMERCIAL_FEED = "commercial_feed.xml";

    private final static String TEST_XML_5 = "test5.xml";

    private static final String TEST_CDATA_IMAGE = "cdata_image.xml";

    private static final String TEXT_XML_TEMP_PRICE = "test_temp_price.xml";


    @Test
    public void testParseSimpleRawOffer() throws Exception {
        XMLStreamReader reader = getReader(TEST_XML);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl rawOffer = parser.next();

        Assert.assertEquals("http://www.cian.ru/showphoto.php?id_flat=1645197", rawOffer.getUrl());
        Assert.assertEquals(1, rawOffer.getPrice().size());

        RawPrice rawPrice = rawOffer.getPrice().get(0);
        Assert.assertEquals(4100f, 1E-6, rawPrice.getValue());
        Assert.assertEquals("RUR", rawPrice.getCurrency());

        Assert.assertNotNull(rawOffer.getImage());
        Assert.assertEquals(7, rawOffer.getImage().size());
        Assert.assertEquals("plan 3d", rawOffer.getImage().get(0).getTag());
        Assert.assertNull(rawOffer.getImage().get(1).getTag());

        Assert.assertEquals(2, rawOffer.getRoomSpace().size());
        Assert.assertEquals(20, rawOffer.getRoomSpace().get(0).getValue(), 0.01);
        Assert.assertEquals(16, rawOffer.getRoomSpace().get(1).getValue(), 0.01);
        Assert.assertEquals("кв.м", rawOffer.getRoomSpace().get(0).getUnit());
        Assert.assertEquals("кв.м", rawOffer.getRoomSpace().get(1).getUnit());

        Assert.assertFalse(parser.hasNext());

        Assert.assertEquals("123456_some_cadastral_number", rawOffer.getCadastralNumber());
        Assert.assertEquals("143", rawOffer.getLocation().getApartment());

        List<WeekDay> allDays = CollectionFactory.unmodifiableList(
                WeekDay.MONDAY,
                WeekDay.TUESDAY,
                WeekDay.WEDNESDAY,
                WeekDay.THURSDAY,
                WeekDay.FRIDAY,
                WeekDay.SATURDAY,
                WeekDay.SUNDAY
        );

        Offer.VasSchedule schedule =
                Offer.VasSchedule.newBuilder()
                        .addAllSchedule(allDays)
                        .build()
        ;
        RawVas raisingWithoutSchedule = new RawVasImpl(OfferCampaignType.RAISE, Instant.parse("2012-10-09T12:20:34+04:00"), Instant.parse("2012-10-10T12:20:34+04:00"), Offer.VasSchedule.getDefaultInstance());
        RawVas raisingWithSchedule = new RawVasImpl(OfferCampaignType.RAISE, Instant.parse("2012-10-09T12:20:34+04:00"), Instant.parse("2012-10-10T12:20:34+04:00"), schedule);

        Assert.assertTrue(rawOffer.getVas().contains(raisingWithoutSchedule));
        Assert.assertTrue(rawOffer.getVas().contains(raisingWithSchedule));
        Assert.assertTrue(rawOffer.getVas().contains(new RawVasImpl(OfferCampaignType.PREMIUM, null, null, Offer.VasSchedule.getDefaultInstance())));

        Assert.assertEquals(Long.valueOf(4242L), rawOffer.getYandexHouseId());

        Assert.assertEquals(2, rawOffer.getVirtualTours().size());
        Assert.assertEquals(VirtualTour
                .newBuilder()
                .setProvider(VirtualTour.Provider.MATTERPORT)
                .setEnabled(true)
                .setMatterportTour(VirtualTour.MatterportTour
                        .newBuilder()
                        .setModelUrl(StringValue.of("https://tour1.ya"))
                        .setPreviewUrl(Image.newBuilder().setOrigin("https://tour1.ya?preview").build())
                        .build())
                .build(),
                rawOffer.getVirtualTours().get(0));
        Assert.assertEquals(VirtualTour
                        .newBuilder()
                        .setProvider(VirtualTour.Provider.IFRAME)
                        .setEnabled(true)
                        .setIframeTour(VirtualTour.IframeTour
                                .newBuilder()
                                .setModelUrl(StringValue.of("https://tour2.ya"))
                                .build())
                        .build(),
                rawOffer.getVirtualTours().get(1));

    }

    @Test
    public void testGenerationDate() throws Exception {
        XMLStreamReader reader = getReader(TEST_XML1);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl rawOffer = parser.next();

        Assert.assertEquals("http://nk.bududoma.ru/kvartiryi/1-7/56-100/124.39848.html", rawOffer.getUrl());
        Assert.assertEquals(1, rawOffer.getPrice().size());
    }

    @Test
    public void testParseFullRawOffer() throws Exception {
        RawOfferImpl expected = MockRawOfferBuilder.createMockRawOfferOld();

        XMLStreamReader reader = getReader(TEST_XML2);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        MockUtils.assertEquals(expected, actual);
    }

    @Test
    public void testParseSaleAgentPhoto() throws Exception {
        RawOfferImpl expected = MockRawOfferBuilder.createMockRawOfferOldWithAgentPhoto();

        XMLStreamReader reader = getReader(TEST_XML4);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        MockUtils.assertEquals(expected, actual);
    }

    @Test
    public void testCommercialFeed() throws Exception {

        XMLStreamReader reader = getReader(TEST_COMMERCIAL_FEED);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        RawOfferImpl first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(false);
        MockUtils.assertEquals(first, actual);
    }

    @Test
    public void testDealStatus() throws Exception {
        XMLStreamReader reader = getReader(TEST_XML_5);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        Assert.assertEquals(actual.getDealStatus(), "primary sale of secondary");
    }

    @Test
    public void testRedirectPhones() throws Exception {
        XMLStreamReader reader = getReader(TEST_XML_5);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        Assert.assertEquals(actual.getSalesAgent().getRedirectPhones(), true);
    }

    @Test
    public void testVerifiedByYandex() throws XMLStreamException {
        InputStream testFeedInputStream = getClass().getClassLoader().getResourceAsStream("feed_REALTY_9184.xml");
        XMLStreamReader reader = XmlUtils.createReader(testFeedInputStream);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        Assert.assertEquals(true, actual.getVerified());

        List<RawImage> expected = new ArrayList<>(23);
        expected.add(new RawImageImpl("http://test_host.com/image.jpg"));
        expected.add(new RawImageImpl("http://test_host.com/image-window-view1", "window-view"));
        expected.add(new RawImageImpl("http://test_host.com/image-window-view2", "window-view"));
        expected.add(new RawImageImpl("http://test_host.com/image-lobby1", "lobby"));
        expected.add(new RawImageImpl("http://test_host.com/image-lobby2", "lobby"));
        expected.add(new RawImageImpl("http://test_host.com/image-corridor1", "corridor"));
        expected.add(new RawImageImpl("http://test_host.com/image-corridor2", "corridor"));
        expected.add(new RawImageImpl("http://test_host.com/image-street1", "street"));
        expected.add(new RawImageImpl("http://test_host.com/image-street2", "street"));
        expected.add(new RawImageImpl("http://test_host.com/image-building1", "building"));
        expected.add(new RawImageImpl("http://test_host.com/image-building2", "building"));
        expected.add(new RawImageImpl("http://test_host.com/image-entrance1", "entrance"));
        expected.add(new RawImageImpl("http://test_host.com/image-entrance2", "entrance"));
        expected.add(new RawImageImpl("http://test_host.com/image-balcony1", "balcony"));
        expected.add(new RawImageImpl("http://test_host.com/image-balcony2", "balcony"));
        expected.add(new RawImageImpl("http://test_host.com/image-room1", "room"));
        expected.add(new RawImageImpl("http://test_host.com/image-room2", "room"));
        expected.add(new RawImageImpl("http://test_host.com/image-kitchen1", "kitchen"));
        expected.add(new RawImageImpl("http://test_host.com/image-kitchen2", "kitchen"));
        expected.add(new RawImageImpl("http://test_host.com/image-tambour1", "tambour"));
        expected.add(new RawImageImpl("http://test_host.com/image-tambour2", "tambour"));
        expected.add(new RawImageImpl("http://test_host.com/image-bathroom1", "bathroom-unit"));
        expected.add(new RawImageImpl("http://test_host.com/image-bathroom2", "bathroom-unit"));

        List<RawImageImpl> actualImages = actual.getImage();
        Assert.assertEquals(expected.size(), actualImages.size());
        for (int i = 0; i < expected.size(); ++i) {
            Assert.assertEquals(expected.get(i).getText(), actualImages.get(i).getText());
            if (expected.get(i).getTag() == null) {
                Assert.assertNull(actualImages.get(i).getTag());
            } else {
                Assert.assertEquals(expected.get(i).getTag(), actualImages.get(i).getTag());
            }
        }

        Assert.assertEquals(1L, actual.getMortgageApprove().longValue());
    }

    @Test
    public void testNMarket() throws XMLStreamException {
        InputStream testFeedInputStream = getClass().getClassLoader().getResourceAsStream("feed_REALTY-12558.xml");
        XMLStreamReader reader = XmlUtils.createReader(testFeedInputStream);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl actual = parser.next();

        Assert.assertEquals("158", actual.getHouseInternalId());
        Assert.assertEquals("3", actual.getSectionName());
        RawDecorationImpl decoration = new RawDecorationImpl();
        decoration.setImage(new RawImageImpl("http://img.nmarket.pro/photo/pid/E308CA21-569C-4677-B481-738E3C18001A/?wpsid=9&v=1", "decoration"));
        decoration.setImage(new RawImageImpl("http://img.nmarket.pro/photo/pid/1B8E4D6D-C469-4FA7-9424-18A556C7F77A/?wpsid=9&v=1", "decoration"));
        Assert.assertEquals(decoration, actual.getDecoration());

    }

    @Test
    @Ignore
    public void testCdataImageRawOffer() throws Exception {
        XMLStreamReader reader = getReader(TEST_CDATA_IMAGE);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl rawOffer = parser.next();

        Assert.assertNotNull(rawOffer.getImage());
        Assert.assertEquals(1, rawOffer.getImage().size());
        Assert.assertEquals("http://stat.jcat.ru/images/orders/2018-02/15/2e2917c78e1140f4b5566b558b2253e2.jpg", rawOffer.getImage().get(0).getImage());
    }

    @Test
    public void testFeedIsBlacklistedForFlatPlanEnrichment() throws XMLStreamException{
        XMLStreamReader reader = getReader(TEST_XML);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl rawOffer = parser.next();

        Assert.assertTrue(rawOffer.isDisableFlatPlanGuess());
    }

    @Test
    public void testParseTemporaryPrice() throws XMLStreamException {
        XMLStreamReader reader = getReader(TEXT_XML_TEMP_PRICE);
        XbiFactory xbiFactory = RawOfferXbiFactory.newInstance();
        RawOffersFeedParser parser = new RawOffersFeedParser(reader, xbiFactory);
        RawOfferImpl rawOffer = parser.next();

        Assert.assertNotNull(rawOffer.getTemporaryPrice());
        Assert.assertEquals(40000F, rawOffer.getTemporaryPrice().getValue(), 0.000001);
        Assert.assertEquals("RUR", rawOffer.getTemporaryPrice().getCurrency());
        Assert.assertEquals(2, rawOffer.getTemporaryPrice().getDuration());
    }
}
