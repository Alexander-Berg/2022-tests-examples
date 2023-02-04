package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.spec;

import junit.framework.TestCase;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.TextMicrodata;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 8/6/11
 * Time: 12:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class MicrodataFilterTest extends TestCase {

    @Test
    public void testFits() throws Exception {
        final MicrodataFilter filter = new MicrodataFilter(Cf.list("fn", "org", "url", "adr.country"));
        final ComplexMicrodata ok = new ComplexMicrodata("11");
        ok.addPropValue("fn", new TextMicrodata("sdasd",null, "sdasd"));
        ok.addPropValue("org", new TextMicrodata("sdasd",null, "sdasd"));
        assertEquals(filter.fits(ok), false);
        ok.addPropValue("url", new TextMicrodata("sdasd",null, "sdasd"));
        assertEquals(filter.fits(ok), false);
        final ComplexMicrodata adr = new ComplexMicrodata("!!!!");
        adr.addPropValue("country", new TextMicrodata("asdads",null, "asdads"));
        ok.addPropValue("adr", adr);
        assertEquals(filter.fits(ok), true);
    }
}
