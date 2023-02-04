package ru.yandex.webmaster3.core.semantic.schema_org_information_extractor;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 3/26/12
 * Time: 12:12 PM
 */
public class ParserTest {

    Parser parser;


    @Before
    public void setUp() throws Exception {
        //final URL url = new URL("http://danbri.org/2012/tmp-schema/schema-org-rdf/sandbox/schema-org-nav/rdfa.html");
        //final String content = IOUtils.readInputStream(url.openStream());
        //final String content = IOUtils.readWholeFile("/home/aleksart/Yandex/workspace/semantic-web/mf-verifier/src/script/dublincore-rdfa.html");
        //final String content = IOUtils.readWholeFile("/home/aleksart/Yandex/workspace/semantic-web/mf-verifier/src/script/good-relations-rdfa.html");
        final String content = "<div typeof=\"rdf:Property\" resource=\"http://schema.org/tracks\">\n" +
                "    <span class=\"h\" property=\"rdfs:label\">tracks</span>\n" +
                "    <span property=\"http://schema.org/supercededBy\" href=\"http://schema.org/track\"/>\n" +
                "    <span property=\"rdfs:comment\">A music recording (track)&amp;#x2014;usually a single song (legacy spelling; see singular form, track).</span>\n" +
                "    <span>Domain: <a property=\"http://schema.org/domainIncludes\" href=\"http://schema.org/MusicPlaylist\">MusicPlaylist</a></span>\n" +
                "    <span>Domain: <a property=\"http://schema.org/domainIncludes\" href=\"http://schema.org/MusicGroup\">MusicGroup</a></span>\n" +
                "    <span>Range: <a property=\"http://schema.org/rangeIncludes\" href=\"http://schema.org/MusicRecording\">MusicRecording</a></span>\n" +
                "</div>";
        parser = new Parser(content.toString());
    }

    @Test
    public void testGetEntities() throws Exception {
        final List<RDFsEntity> entities = parser.getEntities();
        for (final RDFsEntity entity : entities) {
            System.out.println(entity);
        }

    }
}
