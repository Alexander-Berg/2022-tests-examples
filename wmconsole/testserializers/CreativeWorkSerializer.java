package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers;

import org.apache.commons.lang3.StringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONException;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.yandex.webmaster3.core.security.tvm.TVMTokenService;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.EntitySerializer;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.FrontEnd;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data.CreativeWork;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data.StructuredDataTransformers;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;

/**
 * Created by aleksart on 08.07.14.
 */
public class CreativeWorkSerializer implements EntitySerializer {
    public String serialize(Entity e) throws JSONException {
        if(e.getTag().equals("CreativeWorkOrgEntity")){
            //return EntityToClearJsonSerializer.toClearJson(e).toString(4);
            String old = null;
            try {
                old = testWork(e.getUrl());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            String newStr = prettyFormat(XML.toString(EntityToClearJsonSerializer.toClearJson(e), "creative-work"), 0);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = null;
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e1) {
                e1.printStackTrace();
            }

            Document doc = null;
            try {
                doc = db.parse(new InputSource(new StringReader(newStr)));
            } catch (SAXException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Element element = (Element) doc.getElementsByTagName("creative-work").item(0);

            // Adds a new attribute. If an attribute with that name is already present
            // in the element, its value is changed to be that of the value parameter
            element.setAttribute("type-id", "25");

            try {
                newStr = prettyPrint(doc);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\nnew:\n"+newStr+"\nold:\n");
            int maxLen = Integer.MAX_VALUE;

            int dist = StringUtils.getLevenshteinDistance(old, newStr);

            sb.append(old+"\n");
            try {
                sb.append("difference:\n" + assertXMLEquals(newStr,old) + '\n');
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                maxLen = assertXMLEquals(newStr,old).length();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if(maxLen == 0){
                sb.append("OK\n");
            }
            else{
                sb.append("Bad\n");
            }
            return sb.toString();
        }
        return "";
    }
    public static String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    public String testWork(String urlSource) throws Exception {
        final URL url = new URL(urlSource);
        final String content = (new FrontEnd()).downloadWithTimeout(url);
        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        for (final Microdata data : result) {
            final CreativeWork org =
                    StructuredDataTransformers.fromSchemaOrgCreativeWork((ComplexMicrodata) data, url.toString(),"eng");
            if (org != null) {
                final StringBuilder sb = new StringBuilder();
                org.toXml().toXml(sb);
                return sb.toString();
            }
        }
        return "";
    }
    public static String prettyPrint(Document xml) throws Exception {
        TransformerFactory tff = TransformerFactory.newInstance();
        tff.setAttribute("indent-number", 0);
        Transformer tf = tff.newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(xml), new StreamResult(out));
        return out.toString();
    }
    public static String assertXMLEquals(String expectedXML, String actualXML) throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML));
        return "Differences found: " + diff.toString();
    }
}
