package ru.yandex.realty.core.xbi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.ObjectExtractor;
import ru.yandex.common.serialization.parser.xbi.XbiFactory;
import ru.yandex.common.serialization.parser.xbi.XmlUtils;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.text.Charsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * @author aherman
 *
 */
public class XbiObjectReaderTest {
    private XbiFactory xbiFactory;

    @Test
    public void testReader() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        File file = new File("../realty-common/test-data/simple.xml");
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                new FileInputStream(file));

        ObjectExtractor<Simple> extractor = xbiFactory.getExtractor(Simple.class);

        reader.nextTag();
        assertEquals("root", reader.getLocalName());

        reader.nextTag();
        simpleTest(reader, extractor);

        assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
        assertEquals("root", reader.getLocalName());
    }

    private void simpleTest(XMLStreamReader streamReader, ObjectExtractor<Simple> extractor) throws
            XMLStreamException
    {
        assertEquals("simple", streamReader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, streamReader.getEventType());
        Simple simple = extractor.extract(streamReader);
        assertSimple(simple, "s1.f1", 12, 1.3f, 1.4, Cf.list("s1.f5.1", "s1.f5.2", "s1.f5.3"));
        assertA(simple.field6, 161, "s1.f6.f6");
        assertEquals(XMLStreamConstants.END_ELEMENT, streamReader.getEventType());
        streamReader.nextTag();

        assertEquals("simple", streamReader.getLocalName());
        assertEquals(XMLStreamConstants.START_ELEMENT, streamReader.getEventType());
        simple = extractor.extract(streamReader);
        assertSimple(simple, "s2.f1", 22, 2.3f, 2.4, Cf.list("s2.f5.1", "s2.f5.2"));
        assertA(simple.field6, 261, "s2.f6.f6");
        assertEquals(XMLStreamConstants.END_ELEMENT, streamReader.getEventType());
        streamReader.nextTag();
    }

    private void assertSimple(Simple simple,
            String field1,
            int field2,
            float field3,
            double field4,
            List<String> field5) {
        assertEquals(field1, simple.field1);
        assertEquals(field2, simple.field2);
        assertEquals(field3, simple.field3, 0.0001f);
        assertEquals(field4, simple.field4, 0.0001);
        assertEquals(field5, simple.field5);
    }

    private void assertA(A a,
            int field1,
            String field6) {
        assertEquals(field1, a.field1);
        assertEquals(field6, a.field6);
    }

    @Test
    public void testReader2() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        File file = new File("../realty-common/test-data/simple2.xml");
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                new FileInputStream(file));

        ObjectExtractor<Simple2> extractor = xbiFactory.getExtractor(Simple2.class);

        reader.nextTag();
        assertEquals("root", reader.getLocalName());

        reader.nextTag();
        Simple2 simple2 = extractor.extract(reader);
        assertEquals(1, simple2.fInt);
        assertEquals(Integer.valueOf(2), simple2.fInteger);
        assertEquals(3.0f, simple2.fFl, 0.0001f);
        assertEquals(Float.valueOf(4.0f), simple2.fFloat);
        assertEquals(5.0, simple2.fDb, 0.0001);
        assertEquals(Double.valueOf(6.0), simple2.fDouble);
        assertTrue(simple2.fBool);
        assertFalse(simple2.fBoolean);
        assertEquals("s", simple2.fString);
        assertEquals(7L, simple2.getFL());
        assertEquals(Long.valueOf(8L), simple2.getFLong());
    }

    @Ignore
    @Test
    public void testPerformance() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        File file = new File("test-data/big-simple.xml.gz");
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                new GZIPInputStream(new FileInputStream(file)));

        ObjectExtractor<Simple> extractor = xbiFactory.getExtractor(Simple.class);

        reader.nextTag();
        assertEquals("root", reader.getLocalName());

        reader.nextTag();
        int count = 0;
        long start = System.currentTimeMillis();
        String localName = reader.getLocalName();
        while (!(reader.getEventType() == XMLStreamConstants.END_ELEMENT && "root".equals(localName)))
        {
            simpleTest(reader, extractor);
            count += 2;
            if ((count % 16536) == 0) {
                System.out.println("Count: " + count);
            }
            localName = reader.getLocalName();
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Duration: " + duration);
        System.out.println("Items read: " + count);
        System.out.println(String.format("Items per sec: %.2f", count * 1000.0 / duration));
    }

    @Test
    public void testText() throws Exception {
        String xml = "<a><b id=\"10\">ccc</b><b id=\"20\">ddd</b></a>";
        XMLStreamReader reader = XmlUtils.createReader(xml, Charsets.UTF_8);
        ObjectExtractor<B> bExtractor = xbiFactory.getExtractor(B.class);
        reader.nextTag();

        reader.nextTag();
        B b = bExtractor.extract(reader);
        assertNotNull(b);
        assertEquals(10, b.id);
        assertEquals("ccc", b.text);

        reader.nextTag();
        b = bExtractor.extract(reader);
        assertNotNull(b);
        assertEquals(20, b.id);
        assertEquals("ddd", b.text);

        reader.close();
    }

    public static class B {
        int id;
        String text;

        public void setId(int id) {this.id = id;}
        public void setText(String text) {this.text = text;}
    }

    @Before
    public void setUp() throws Exception {
        xbiFactory = new XbiFactory();
        xbiFactory.registerDefaultExtractors();
    }
}
