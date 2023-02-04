package ru.yandex.webmaster3.core.turbo.xml;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import ru.yandex.webmaster3.core.util.XmlUtil;

import java.io.ByteArrayInputStream;

/**
 * Created by Oleg Bazdyrev on 30/08/2018.
 */
public class TurboXmlReaderTest {

    private static final String XML = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<rss>\n" +
            "   <channel>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-1</url>\n" +
            "           <turbo:content>some content 1</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-2</url>\n" +
            "           <turbo:content>some content 2 </turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-3</url>\n" +
            "           <turbo:content>some content 3</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-4</url>\n" +
            "           <turbo:content>some content 4</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-5</url>\n" +
            "           <turbo:content><![CDATA[some content 5]]></turbo:content>\n" +
            "       </item>\n" +
            "   </channel>\n" +
            "</rss>";

    private static final String YML = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<yml_catalog date=\"2017-02-05 17:22\">\n" +
            "   <shop>\n" +
            "       <currencies>\n" +
            "           <currency id=\"RUR\" rate=\"1\"/>\n" +
            "           <currency id=\"USD\" rate=\"60\"/>\n" +
            "       </currencies>" +
            "       <offers>\n" +
            "           <offer id=\"1\">\n" +
            "               <url>offer-url-1</url>\n" +
            "               <name>offer N1</name>\n" +
            "           </offer>\n" +
            "           <offer id=\"2\">\n" +
            "               <url>offer-url-2</url>\n" +
            "               <name>offer N2</name>\n" +
            "           </offer>\n" +
            "           <offer id=\"3\">\n" +
            "               <url>offer-url-3</url>\n" +
            "               <name>offer N3</name>\n" +
            "           </offer>\n" +
            "           <offer id=\"4\">\n" +
            "               <url>offer-url-4</url>\n" +
            "               <name>offer N4</name>\n" +
            "           </offer>\n" +
            "           <offer id=\"5\">\n" +
            "               <url>offer-url-5</url>\n" +
            "               <name>offer N5</name>\n" +
            "           </offer>\n" +
            "       </offers>\n" +
            "       <categories>\n" +
            "           <category id=\"1\">Бытовая техника</category>\n" +
            "           <category id=\"10\" parentId=\"1\">Мелкая техника для кухни</category>\n" +
            "           <category id=\"101\" parentId=\"10\">Сэндвичницы и приборы для выпечки</category>\n" +
            "           <category id=\"102\" parentId=\"10\">Мороженицы</category>\n" +
            "       </categories>" +
            "   </shop>\n" +
            "</yml_catalog>";

    private static final String XML_BAD = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<rss>\n" +
            "   <channel>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-1</url>\n" +
            "           <turbo:content>some content 1</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-2</url>\n" +
            "           <turbo:content async>some content 2 </turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-3</url>\n" +
            "           <turbo:content>some content 3</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-4</url>\n" +
            "           <turbo:content>some content 4</turbo:content>\n" +
            "       </item>\n" +
            "       <item turbo=\"true\">\n" +
            "           <url>some-url-5</url>\n" +
            "           <turbo:content><![CDATA[some content 5]]></turbo:content>\n" +
            "       </item>\n" +
            "   </channel>\n" +
            "</rss>";

    @Test
    public void testNoRestrictions() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(XML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, null, 0);
        String result = documentToString(document);
        Assert.assertEquals("No changes to document", XML, result);
    }

    @Test
    public void testRestrictByItemUrl_1() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(XML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, "some-url-4", 0);
        String result = documentToString(document);
        Assert.assertEquals("Only 1 item remains",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<rss>\n" +
                        "   <channel>\n" +
                        "       <item turbo=\"true\">\n" +
                        "           <url>some-url-4</url>\n" +
                        "           <turbo:content>some content 4</turbo:content>\n" +
                        "       </item>\n" +
                        "   </channel>\n" +
                        "</rss>", result);
    }

    @Test
    public void testRestrictByItemUrl_2() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(XML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, "some-url-5", 0);
        String result = documentToString(document);
        Assert.assertEquals("Only 1 item remains",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<rss>\n" +
                        "   <channel>\n" +
                        "       <item turbo=\"true\">\n" +
                        "           <url>some-url-5</url>\n" +
                        "           <turbo:content><![CDATA[some content 5]]></turbo:content>\n" +
                        "       </item>\n" +
                        "   </channel>\n" +
                        "</rss>", result);
    }

    @Test
    public void testRestrictByItemCount() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(XML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 3, null, 0);
        String result = documentToString(document);
        Assert.assertEquals("Only 1 item remains",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<rss>\n" +
                        "   <channel>\n" +
                        "       <item turbo=\"true\">\n" +
                        "           <url>some-url-1</url>\n" +
                        "           <turbo:content>some content 1</turbo:content>\n" +
                        "       </item>\n" +
                        "       <item turbo=\"true\">\n" +
                        "           <url>some-url-2</url>\n" +
                        "           <turbo:content>some content 2 </turbo:content>\n" +
                        "       </item>\n" +
                        "       <item turbo=\"true\">\n" +
                        "           <url>some-url-3</url>\n" +
                        "           <turbo:content>some content 3</turbo:content>\n" +
                        "       </item>\n" +
                        "   </channel>\n" +
                        "</rss>", result);
    }

    @Test
    public void testYmlNoRestrictions() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(YML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, null, 0);
        String result = documentToString(document);
        Assert.assertEquals("No changes to document", YML, result);
    }

    @Test
    public void testYmlRestrictByItemUrl() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(YML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, "offer-url-2", 0);
        String result = documentToString(document);
        Assert.assertEquals("Only 1 item remains", "" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<yml_catalog date=\"2017-02-05 17:22\">\n" +
                        "   <shop>\n" +
                        "       <currencies>\n" +
                        "           <currency id=\"RUR\" rate=\"1\"/>\n" +
                        "           <currency id=\"USD\" rate=\"60\"/>\n" +
                        "       </currencies>" +
                        "       <offers>\n" +
                        "           <offer id=\"2\">\n" +
                        "               <url>offer-url-2</url>\n" +
                        "               <name>offer N2</name>\n" +
                        "           </offer>\n" +
                        "       </offers>\n" +
                        "       <categories>\n" +
                        "           <category id=\"1\">Бытовая техника</category>\n" +
                        "           <category id=\"10\" parentId=\"1\">Мелкая техника для кухни</category>\n" +
                        "           <category id=\"101\" parentId=\"10\">Сэндвичницы и приборы для выпечки</category>\n" +
                        "           <category id=\"102\" parentId=\"10\">Мороженицы</category>\n" +
                        "       </categories>" +
                        "   </shop>\n" +
                        "</yml_catalog>"
                , result);
    }

    @Test
    public void testYmlRestrictByItemCount() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(YML.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 3, null, 0);
        String result = documentToString(document);
        Assert.assertEquals("Only 1 item remains", "" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<yml_catalog date=\"2017-02-05 17:22\">\n" +
                        "   <shop>\n" +
                        "       <currencies>\n" +
                        "           <currency id=\"RUR\" rate=\"1\"/>\n" +
                        "           <currency id=\"USD\" rate=\"60\"/>\n" +
                        "       </currencies>" +
                        "       <offers>\n" +
                        "           <offer id=\"1\">\n" +
                        "               <url>offer-url-1</url>\n" +
                        "               <name>offer N1</name>\n" +
                        "           </offer>\n" +
                        "           <offer id=\"2\">\n" +
                        "               <url>offer-url-2</url>\n" +
                        "               <name>offer N2</name>\n" +
                        "           </offer>\n" +
                        "           <offer id=\"3\">\n" +
                        "               <url>offer-url-3</url>\n" +
                        "               <name>offer N3</name>\n" +
                        "           </offer>\n" +
                        "       </offers>\n" +
                        "       <categories>\n" +
                        "           <category id=\"1\">Бытовая техника</category>\n" +
                        "           <category id=\"10\" parentId=\"1\">Мелкая техника для кухни</category>\n" +
                        "           <category id=\"101\" parentId=\"10\">Сэндвичницы и приборы для выпечки</category>\n" +
                        "           <category id=\"102\" parentId=\"10\">Мороженицы</category>\n" +
                        "       </categories>" +
                        "   </shop>\n" +
                        "</yml_catalog>"
                , result);
    }

    @Test(expected = TurboXMLReader.TurboFeedIsEmptyException.class)
    public void testEmptyFeed() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes());
        TurboXMLReader.parseTurboFeed(bais, 0, null, 0);
    }

    @Test(expected = TurboXMLReader.TurboFeedIsTooBigException.class)
    public void testTooBigFeed() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(YML.getBytes());
        TurboXMLReader.parseTurboFeed(bais, 0, null, 100);
    }

    @Test
    public void testBadXml() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(XML_BAD.getBytes());
        try {
            TurboXMLReader.parseTurboFeed(bais, 0, null, 0);
        } catch (SAXParseException e) {
            System.out.println("line=" + e.getLineNumber() + "  column = " + e.getColumnNumber());
            System.out.println(e.getSystemId());
            System.out.println(e.getPublicId());
        }
    }

    private static final String YML_WITH_ENTITIES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<yml_catalog date=\"2019-09-17 12:41\">\n" +
            "<shop>\n" +
            "<name>Художественный магазин \"Коллаж\"</name>\n" +
            "<company>Коллаж</company>\n" +
            "<url>https://artkollage.ru</url>\n" +
            "<platform>1C-Bitrix</platform>\n" +
            "<currencies>\n" +
            "<currency id=\"RUB\" rate=\"1\"/>\n" +
            "</currencies>\n" +
            "<categories>\n" +
            "<category id=\"2332\">Бумага</category>\n" +
            "<category id=\"2333\" parentId=\"2332\">Акварельная (листами)</category>\n" +
            "</categories>\n" +
            "<offers><offer available=\"true\" id=\"135469\">\n" +
            "<url>https://artkollage.ru/bumaga_listy_albomy_sketchbuki_i_t_d_/akvarelnaya_listami/bumaga_dlya_akvareli_tsellyuloza_150gr_m2_srednee_zerno_kholst_allegretto_hahnemuhle_50kh65sm?r1=yandext&amp;r2=</url>\n" +
            "<price>96</price>\n" +
            "<currencyId>RUB</currencyId>\n" +
            "<categoryId>2333</categoryId>\n" +
            "<picture>https://artkollage.ru/upload/iblock/382/3820f2f9d565c8a2f0ff99de8adf7db7.jpg</picture>\n" +
            "<vendor/>\n" +
            "<name>Бумага для акварели целлюлоза 150гр/м2 среднее зерно холст Allegretto Hahnemuhle 50х65см</name>\n" +
            "<description>Бумага для акварели целлюлоза 150гр/м2 среднее зерно холст Allegretto Hahnemuhle 50х65см</description>\n" +
            "<country_of_origin>Германия</country_of_origin>\n" +
            "</offer>\n" +
            "</offers>\n" +
            "</shop>\n" +
            "</yml_catalog>";

    @Test
    public void testYmlWithEntities() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(YML_WITH_ENTITIES.getBytes());
        Document document = TurboXMLReader.parseTurboFeed(bais, 0, "https://artkollage.ru/bumaga_listy_albomy_sketchbuki_i_t_d_/akvarelnaya_listami/bumaga_dlya_akvareli_tsellyuloza_150gr_m2_srednee_zerno_kholst_allegretto_hahnemuhle_50kh65sm?r1=yandext&r2=", 0);
        String result = documentToString(document);
        Assert.assertEquals(YML_WITH_ENTITIES, result); // no changes
    }

    public static String documentToString(Document document) {
        return new String(XmlUtil.serializeDocument(document));
    }

}
