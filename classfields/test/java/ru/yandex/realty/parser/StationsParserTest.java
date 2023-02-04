package ru.yandex.realty.parser;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.ObjectExtractor;
import ru.yandex.common.serialization.parser.xbi.XbiFactory;
import ru.yandex.realty.model.location.RailwayStation;
import ru.yandex.realty.util.IOUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.util.List;

/**
 * @author Vladimir Yakunin (vyakunin@yandex-team.ru)
 */
public class StationsParserTest {
    public static final RailwayStation RAILWAY_STATION = new RailwayStation(53.55, 38.81, "First", 182182l);
    private XbiFactory xbiFactory;

    @Before
    public void setUp() throws Exception {
        final ObjectExtractor<RailwayStation> extractor =  new ObjectExtractor<RailwayStation>() {
            @Override
            public RailwayStation extract(XMLStreamReader reader) throws XMLStreamException {
                return RAILWAY_STATION;
            }

            @Override
            public Class<RailwayStation> getExtractedClass() {
                return RailwayStation.class;
            }
        };
        xbiFactory = new XbiFactory() {
            @Override
            public <T> ObjectExtractor getExtractor(Class<T> clazz) {
                return extractor;
            }
        };
    }

    @Test
    public void testLoadStations() throws Exception {
        List<RailwayStation> list = StationsParser.parse(IOUtils.toInputStream(new File("../realty-common/test-data/directions.xml.gz")), xbiFactory);
        RailwayStation railwayStation = list.get(0);
        Assert.assertEquals(RAILWAY_STATION, railwayStation);
    }
}
