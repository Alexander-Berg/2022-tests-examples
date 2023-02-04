package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.transformer;

import junit.framework.TestCase;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.HCard;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.MicroformatsManager;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 07.10.13
 * Time: 18:04
 */
public class AllByteContextTest extends TestCase{
    public void testGetInfo() throws Exception {
        AllByteContext context = new AllByteContext(("<div itemscope=\"\" itemtype=\"http://schema.org/TheaterEvent\">\n" +
                "  <span itemprop=\"name\">Julius Caesar at Shakespeare's Globe</span>\n" +
                "  <div itemprop=\"location\" itemscope=\"\" itemtype=\"http://schema.org/PerformingArtsTheater\">\n" +
                "    <meta itemprop=\"name\" content=\"Shakespeare's Globe\"/>\n" +
                "    <link itemprop=\"sameAs\" href=\"http://www.shakespearesglobe.com/\"/>\n" +
                "    <meta itemprop=\"address\" content=\"London, UK\"/>\n" +
                "  </div>\n" +
                "  <div itemprop=\"offers\" itemscope=\"\" itemtype=\"http://schema.org/Offer\">\n" +
                "    <link itemprop=\"url\" href=\"/examples/ticket/0012301230123\"/>\n" +
                "  </div>\n" +
                "  <span itemprop=\"startDate\" content=\"2014-10-01T19:30\">Wed 01 October 2014 19:30</span>\n" +
                "  <div itemprop=\"workPerformed\" itemscope=\"\" itemtype=\"http://schema.org/CreativeWork\">\n" +
                "    <link itemprop=\"sameAs\" href=\"http://en.wikipedia.org/wiki/Julius_Caesar_(play)\"/>\n" +
                "    <link itemprop=\"sameAs\" href=\"http://worldcat.org/entity/work/id/1807288036\"/>\n" +
                "    <div itemprop=\"creator\" itemscope=\"\" itemtype=\"http://schema.org/Person\">\n" +
                "       <meta itemprop=\"name\" content=\"William Shakespeare\"/>\n" +
                "       <link itemprop=\"sameAs\" href=\"http://en.wikipedia.org/wiki/William_Shakespeare\"/>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</div>").getBytes("utf-8"),new DocumentProperties("",
                MicroformatsManager.managerForMFsAndIncluded(HCard.getInstance()),"utf-8",false,false,false));
        List<Microdata> x = context.getInfo();
        System.out.println(x.size());
        assertEquals(true,1==x.size());
        System.out.println("OK");
        System.out.println((x.get(0)).getClass().toString());
        assertEquals(x.get(0).getClass(), ComplexMicrodata.class);
        System.out.println("OK");
//            System.out.println(MicrodataUtils.md2Json(data,null).toString(4));
    }
}
