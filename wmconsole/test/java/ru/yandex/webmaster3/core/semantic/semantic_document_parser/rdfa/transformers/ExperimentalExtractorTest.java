package ru.yandex.webmaster3.core.semantic.semantic_document_parser.rdfa.transformers;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 14.08.12
 * Time: 22:22
 */
public class ExperimentalExtractorTest extends TestCase {
    @Test
    public void testGetResults() throws Exception {
//        final Pair<List<RDFaEntity>, List<RDFaException>> result = ExperimentalExtractor.getResults(String.format(
//                "<a href=\"#helpdiv\" class=\"modal\" style=\"cursor:pointer\" title=\"Click to Popup the login form.\" rel=\"{size: {x: 206, y: 333}, ajaxOptions: {method: \"get\"}}\">"),
//                "http://localhost");
//        final List<String> jsons = (new Function<Xmler.Tag, String>() {
//
//            @Override
//            public String apply(final Xmler.Tag tag) {
//                final StringBuilder builder = new StringBuilder();
//                tag.toXml(builder);
//                return builder.toString();
//            }
//        }).map(RDFaUtils.getVerifierNodes(result.first, false));
//        System.out.println(Su.join(jsons, "\n"));
    }

    @Test
    public void testName() throws Exception {
//        Pair<List<RDFaEntity>, List<RDFaException>> r =
//                ExperimentalExtractor.getResults("<div vocab=\"http://schema.org/\" typeof=\"Product\">\n" +
//                        "  <span property=\"name\">Kenmore White 17\" Microwave</span>\n" +
//                        "  <img src=\"kenmore-microwave-17in.jpg\" alt='Kenmore 17\" Microwave' />\n" +
//                        "  <div property=\"aggregateRating\"\n" +
//                        "     typeof=\"AggregateRating\">\n" +
//                        "   Rated <span property=\"ratingValue\">3.5</span>/5\n" +
//                        "   based on <span property=\"reviewCount\">11</span> customer reviews\n" +
//                        "  </div>\n" +
//                        "  <div property=\"offers\"  typeof=\"Offer\">\n" +
//                        "    <!--price is 1000, a number, with locale-specific thousands separator\n" +
//                        "        and decimal mark, and the $ character is marked up with the\n" +
//                        "        machine-readable code \"USD\" -->\n" +
//                        "    <span property=\"priceCurrency\" content=\"USD\">$</span><span\n" +
//                        "      property=\"price\" content=\"1000.00\">1,000.00</span>\n" +
//                        "    <link property=\"availability\" href=\"http://schema.org/InStock\" />In stock\n" +
//                        "  </div>\n" +
//                        "  Product description:\n" +
//                        "  <span property=\"description\">0.7 cubic feet countertop microwave.\n" +
//                        "  Has six preset cooking categories and convenience features like\n" +
//                        "  Add-A-Minute and Child Lock.</span>\n" +
//                        "  Customer reviews:\n" +
//                        "  <div property=\"review\"  typeof=\"Review\">\n" +
//                        "    <span property=\"name\">Not a happy camper</span> -\n" +
//                        "    by <span property=\"author\">Ellie</span>,\n" +
//                        "    <meta property=\"datePublished\" content=\"2011-04-01\">April 1, 2011\n" +
//                        "    <div property=\"reviewRating\"  typeof=\"Rating\">\n" +
//                        "      <meta property=\"worstRating\" content = \"1\">\n" +
//                        "      <span property=\"ratingValue\">1</span>/\n" +
//                        "      <span property=\"bestRating\">5</span>stars\n" +
//                        "    </div>\n" +
//                        "    <span property=\"description\">The lamp burned out and now I have to replace\n" +
//                        "    it. </span>\n" +
//                        "  </div>\n" +
//                        "  <div property=\"review\"  vocab=\"https://schema.org/\" typeof=\"Review\">\n" +
//                        "    <span property=\"name\">Value purchase</span> -\n" +
//                        "    by <span property=\"author\">Lucas</span>,\n" +
//                        "    <meta property=\"datePublished\" content=\"2011-03-25\">March 25, 2011\n" +
//                        "    <div property=\"reviewRating\"  typeof=\"Rating\">\n" +
//                        "      <meta property=\"worstRating\" content = \"1\"/>\n" +
//                        "      <span property=\"ratingValue\">4</span>/\n" +
//                        "      <span property=\"bestRating\">5</span>stars\n" +
//                        "    </div>\n" +
//                        "    <span property=\"description\">Great microwave for the price. It is small and\n" +
//                        "    fits in my apartment.</span>\n" +
//                        "  </div>\n" +
//                        "  ...\n" +
//                        "</div>", "http://localhost");
//        for (final RDFaException ex : r.second) {
//            System.out.println(ex.getMessage());
//        }

//        final JSONLDParser parser = new JSONLDParser();
//        ContextManager contextManager = new ContextManager(Collections.EMPTY_MAP);
//        contextManager.loadContext("http://schema.org/",new FileInputStream("/home/rasifiel/schema.jsonld"),
//                java.nio.charset.Charset.forName("UTF-8"));
//        parser.setContextManager(contextManager);
//        List<RDFaEntity> entities = Cu.join(Cu.map(new Function<RDFaEntity,List<RDFaEntity>>(){
//
//            @Override
//            public List<RDFaEntity> apply(final RDFaEntity entity) {
//                return parser.expand(entity, null, new Context(null, null));
//            }
//        },r.first));
//        final List<String> jsons = (new Function<RDFaEntity, String>() {
//
//            @Override
//            public String apply(final RDFaEntity tag) {
//                final StringBuilder builder = new StringBuilder();
//                tag.toJson(builder);
//                return builder.toString();
//            }
//        }).map(entities);
//        System.out.println(Su.join(jsons, "\n"));
    }
}
