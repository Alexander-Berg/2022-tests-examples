package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.validators;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.exceptions.MicrodataValidatorException;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.exceptions.MicrodataValidatorExceptionsSerializer;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.exceptions.RuMicrodataValidatorExceptionSerializer;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 12/6/11
 * Time: 2:59 PM
 */
public class YaOrgTelMicrodataValidatorTest {

    List<Microdata> mds;
    YaOrgTelMicrodataValidator validator = new YaOrgTelMicrodataValidator(true);
    MicrodataValidatorExceptionsSerializer ser = new RuMicrodataValidatorExceptionSerializer();

    @Before
    public void setUp() throws Exception {
        mds = MicrodataUtils.extractMD("    <div itemscope itemtype=\"http://schema.org/Organization\">\n" +
                "      <span itemprop=\"name\">Google.org (GOOG)</span>\n" + "\n" + "    Contact Details:\n" +
                "      <div itemprop=\"address\" itemscope itemtype=\"http://schema.org/PostalAddress\">\n" +
                "        Main address:\n" + "          <span itemprop=\"streetAddress\">38 avenue de l'Opera</span>\n" +
                "          <span itemprop=\"postalCode\">F-75002</span>\n" +
                "          <span itemprop=\"addressLocality\">Paris, France</span>\n" + "        ,\n" +
                "      </div>\n" + "        Fax:<span itemprop=\"faxNumber\">( 33 1) 42 68 53 01 </span>,\n" +
                "        E-mail: <span itemprop=\"email\">secretariat(at)google.org</span>\n" + "\n" +
                "    Members:\n" +
                "    - National Scientific Members in 100 countries and territories: Country1, Country2, ...\n" +
                "    - Scientific Union Members, 30 organizations listed in this Yearbook:\n" +
                "      <span itemprop=\"members\" itemscope itemtype=\"http://schema.org/Organization\">\n" +
                "        Member1\n" + "      </span>,\n" +
                "      <span itemprop=\"members\" itemscope itemtype=\"http://schema.org/Organization\">\n" +
                "        Member2\n" + "      </span>,\n" + "\n" + "    History:\n" + "    </div>", "http://localhost",
                true, false);

    }

    @Test
    public void testValidate() throws Exception {
        for (Microdata md : mds) {
            List<MicrodataValidatorException> exs = validator.validate(md);
            for (MicrodataValidatorException ex : exs) {
                System.out.println(ex.toString());
            }
        }
    }
}
