package ru.yandex.webmaster.common.antispam.sanctions;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.wmtools.common.error.InternalException;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: azakharov
 * Date: 06.06.14
 * Time: 15:38
 */
public class AntispamSanctionsServiceTest {

    @Test
    public void testParseEmptySanctions() throws InternalException {
        final String testData = "{\"Sanctions\":[]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(Collections.<String, Set<String>>emptyMap());

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testParseOneHostWithSanctions() throws InternalException {
        final String testData = "{\"Sanctions\":[{\"Owner\": \"lenta.ru\", \"Sanctions\": [\"LINK_PESS\"]}]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(
                Collections.singletonMap("lenta.ru", Cf.set("LINK_PESS")));

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testParseOneHostWith2Sanctions() throws InternalException {
        final String testData = "{\"Sanctions\":[{\"Owner\": \"lenta.ru\", \"Sanctions\": [\"LINK_PESS\", \"BAN\"]}]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(
                Collections.singletonMap("lenta.ru", Cf.set("LINK_PESS", "BAN")));

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testParseOneHostWithoutSanctions() throws InternalException {
        final String testData = "{\"Sanctions\":[{\"Owner\": \"lenta.ru\", \"Sanctions\": []}]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(
                Collections.singletonMap("lenta.ru", Collections.<String>emptySet()));

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testTwoHostsWithAndWithoutSanctions1() throws InternalException {
        final String testData = "{\"Sanctions\":[{\"Owner\": \"lenta.ru\", \"Sanctions\": []}," +
                                                "{\"Owner\": \"hh.ru\", \"Sanctions\": [\"LINK_PESS\"]} ]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final Map<String, Set<String>> expectedSanctions = new HashMap<>();
        expectedSanctions.put("lenta.ru", Collections.<String>emptySet());
        expectedSanctions.put("hh.ru", Cf.set("LINK_PESS"));
        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(expectedSanctions);

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testTwoHostsWithAndWithoutSanctions2() throws InternalException {
        final String testData = "{\"Sanctions\":[{\"Owner\": \"lenta.ru\", \"Sanctions\": [\"LINK_PESS\"]}," +
                "{\"Owner\": \"hh.ru\", \"Sanctions\": []} ]}";
        final InputStream stream = IOUtils.toInputStream(testData);

        final Map<String, Set<String>> expectedSanctions = new HashMap<>();
        expectedSanctions.put("lenta.ru", Cf.set("LINK_PESS"));
        expectedSanctions.put("hh.ru", Collections.<String>emptySet());
        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(expectedSanctions);

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Test
    public void testSmallFileParsing() throws InternalException {
        final StringBuilder testData = new StringBuilder("{ \"Sanctions\" : [");
        Map<String, Set<String>> expectedSanctions = new HashMap<>();
        final Set<String> sanctions = Cf.set("LINKS_PESS");
        String separator = " ";
        for (int i = 0 ; i < 500000; i++) {
            testData.append(separator);

            final String hostName = String.format("host%d", i);
            expectedSanctions.put(hostName, sanctions);
            testData.append("{\"Owner\": \"").append(hostName).append("\", ");
            testData.append("\"Sanctions\": [\"").append(sanctions.iterator().next()).append("\"]");
            testData.append("} ");
            separator = ",";
        }
        testData.append(" ] }");

        final InputStream stream = IOUtils.toInputStream(testData.toString());

        final CheckedSanctionsHandler h = new CheckedSanctionsHandler(expectedSanctions);

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);

        h.checkPostConditions();
    }

    @Ignore // ignore long test for everyday use
    @Test
    public void testBigFileParsing2() throws InternalException, IOException {
        FileWriter wr = new FileWriter("/tmp/bigfile.json");

        wr.write("{ \"Sanctions\" : [");
        Map<String, Set<String>> expectedSanctions = new HashMap<>();
        final Set<String> sanctions = Cf.set("LINKS_PESS");
        String separator = " ";
        for (int i = 0 ; i < 1000000; i++) {
            wr.write(separator);

            final String hostName = String.format("host%d", i);
            expectedSanctions.put(hostName, sanctions);
            wr.append("{\"Owner\": \"").append(hostName).append("\", ");
            wr.append("\"Sanctions\": [\"").append(sanctions.iterator().next()).append("\"]");
            wr.append("} ");
            separator = ",";
        }
        wr.append(" ] }");
        wr.close();

        final InputStream stream = new FileInputStream("/tmp/bigfile.json");

        AntispamSanctionsService.HostSanctionsHandler h = new AntispamSanctionsService.HostSanctionsHandler() {
            @Override
            public void handle(String hostName, Set<String> sanctions) {
                //
            }
        };

        final AntispamSanctionsService antispamSanctionsService = new AntispamSanctionsService();
        antispamSanctionsService.parseAntispamSanctions(stream, h);
    }

    public class CheckedSanctionsHandler implements AntispamSanctionsService.HostSanctionsHandler {

        private final Map<String, Set<String>> expectedSanctions;
        private final Map<String, Set<String>> workingSet;
        private final Map<String, Set<String>> foundSanctions;

        public CheckedSanctionsHandler(Map<String, Set<String>> expectedSanctions) {
            this.expectedSanctions = expectedSanctions;
            this.workingSet = new HashMap<>(expectedSanctions);
            this.foundSanctions = new HashMap<>();
        }

        @Override
        public void handle(String hostName, Set<String> sanctions) {
            Assert.assertNull(foundSanctions.get(hostName));

            foundSanctions.put(hostName, sanctions);

            Set<String> expected = expectedSanctions.get(hostName);
            Assert.assertNotNull("expected sanctions for " + hostName + " not found ", expected);
            Assert.assertEquals("expected sanctions not equals to found ", expected, sanctions);

            workingSet.remove(hostName);
        }

        public void checkPostConditions() {
            Assert.assertTrue("Not all expected sanctions found in input stream", workingSet.isEmpty());
        }
    }
}
