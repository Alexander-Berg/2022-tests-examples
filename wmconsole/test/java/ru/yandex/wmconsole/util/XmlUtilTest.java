package ru.yandex.wmconsole.util;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import ru.yandex.common.util.xml.XmlConvertable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: Alexey Zakharov <azakharov@yandex-team.ru>
 * Date: 31.01.12
 */
public class XmlUtilTest {

    @XmlRootElement(name = "entity")
    public static class XmlAnnotatedEntity {
        
        @XmlAttribute(name = "req-attr", required = true)
        private Integer requiredAttribute;

        @XmlAttribute(name = "opt-attr", required = false)
        private Integer optionalAttribute;

        @XmlElement(name = "required-element", required = true)
        private String requiredElement;

        @XmlElement(name = "optional-element", required = false)
        private String optionalElement;

        public XmlAnnotatedEntity() {
            // for JAXB
            throw new UnsupportedOperationException("Don't use default constructor! It is only for JAXB");
        }
        
        public XmlAnnotatedEntity(Integer reqAttr, Integer optAttr, String reqElement, String optElement) {
            this.requiredAttribute = reqAttr;
            this.optionalAttribute = optAttr;
            this.requiredElement = reqElement;
            this.optionalElement = optElement;
        }

        public Integer getRequiredAttribute() {
            return requiredAttribute;
        }

        public String getRequiredElement() {
            return requiredElement;
        }

        public String getOptionalElement() {
            return optionalElement;
        }
    }

    @Test
    public void testXmlConvertableWithNullOptional() throws JAXBException {
        XmlAnnotatedEntity info = new XmlAnnotatedEntity(1, null, "hello", null);
        XmlConvertable xc = XmlUtil.xmlConvertable(info);
        StringBuilder sb = new StringBuilder();
        xc.toXml(sb);
        String result = sb.toString();
        assertFalse("xml contains preamble :" + result, result.contains("<?xml"));
        assertTrue("xml doesn't contain required-element :" + result, result.contains("<required-element>hello</required-element>"));
        assertTrue("xml doesn't contain required-attribute :" + result, result.contains("req-attr=\"1\""));
        assertFalse("xml contains optional element though it is null: " + result, result.contains("opt"));
    }

    @Test
    public void testXmlConvertableWithNotNullOptional() throws JAXBException {
        XmlAnnotatedEntity info = new XmlAnnotatedEntity(1, 42, "hello", "xyz");
        XmlConvertable xc = XmlUtil.xmlConvertable(info);
        StringBuilder sb = new StringBuilder();
        xc.toXml(sb);
        String result = sb.toString();
        assertFalse("xml contains preamble :" + result, result.contains("<?xml"));
        assertTrue("xml doesn't contain required-element :" + result, result.contains("<required-element>hello</required-element>"));
        assertTrue("xml doesn't contain required-attribute :" + result, result.contains("req-attr=\"1\""));
        assertTrue("xml doesn't contain optional element though it is not null: " + result, result.contains("<optional-element>xyz</optional-element>"));
        assertTrue("xml doesn't contain optional attribute though it is not null: " + result, result.contains("opt-attr=\"42\""));
    }
}
