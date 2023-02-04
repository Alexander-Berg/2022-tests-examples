package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.validators;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.webmaster3.core.semantic.schema_org_information_extractor.UtilityTest;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.exceptions.MicrodataValidatorException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 4/10/12
 * Time: 3:29 AM
 */
@Ignore
public class SchemaOrgValidatorTest {

    DFSVisitorMicrodataValidatorWrapper validator;
    SchemaValidator schemaOrgValidator;

    @Before
    public void setUp() throws Exception {
        schemaOrgValidator = new SchemaValidator(UtilityTest.getSchemaClassMap());
        schemaOrgValidator.setVocab("schema.org");
        validator = new DFSVisitorMicrodataValidatorWrapper();
        validator.setValidator(schemaOrgValidator);
    }

    @Test
    public void testValidate() throws Exception {

        JSONArray array = new JSONArray("[\"a\",\"b\"]");
        System.out.println(array.toString());

        long ml = Long.parseLong("123123");
        System.out.println(ml);
        for(int i = 0; i< array.length(); i++){
            System.out.println(array.getString(i));
        }
        List<Microdata> mds = MicrodataUtils.extractMD("<div itemscope=\"itemscope\" itemtype=\"http://schema.org/Organization\">\n" +
                        "<meta itemprop=\"name\" content=\"Some Text\"/>\n" +
                        "<meta itemprop=\"telephone\" content=\"Some Text\"/>\n" +
                        "<div>\n" +
                        "<address itemprop=\"address\" itemscope=\"itemscope\" itemtype=\"http://schema.org/PostalAddress\">\n" +
                        "<div>\n" +
                        "<span itemprop=\"addressCountry\">Украина</span>\n" +
                        "<span itemprop=\"addressRegion\">Some Text</span>\n" +
                        "<span itemprop=\"addressLocality\">Some Text</span>\n" +
                        "<span itemprop=\"streetAddress\">Some Text</span>\n" +
                        "<span itemprop=\"postalCode\">Some Text</span>\n" +
                        "</div>\n" +
                        "</address>\n" +
                        "</div>\n" +
                        "</div>",
                "http://schema.org/Organization", true, false);
        List<MicrodataValidatorException> exs = validator.validate(mds.get(0));
        for (final MicrodataValidatorException ex : exs) {
            System.out.printf("Ex tag - %s message - %s", ex.getKey(), ex.message);
        }
    }


}
