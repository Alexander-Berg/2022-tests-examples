package ru.yandex.jams.coverage;

import org.dom4j.Element;
import org.joda.time.DateTimeZone;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.jams.opengis.BoundingBox;
import ru.yandex.jams.opengis.Line;
import ru.yandex.jams.opengis.Point;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author Sergey Polovko
 */
class TestCoverage extends Coverage {

    private static final Logger logger = LoggerFactory.getLogger(TestCoverage.class);

    static final String CITIES_DATA_XML = "cities-data.xml";
    static final String REGIONS_DATA_XML = "regions-data.xml";

    private final ListF<Region> regions;


    TestCoverage(String fileName) {
        regions = parseRegions(fileName);
        logger.info("Loaded {} regions from {}", regions.size(), fileName);
    }

    @Override
    public ListF<Region> getRegions() {
        return regions;
    }

    @SuppressWarnings("unchecked")
    private ListF<Region> parseRegions(String fileName) {
        Element rootEl = Dom4jUtils.readRootElement(getClass().getResourceAsStream(fileName));

        return Cf.x(rootEl.elements("region")).map(regionEl -> parseRegion((Element) regionEl));
    }

    @SuppressWarnings("unchecked")
    private static Region parseRegion(Element regionEl) {
        int id = Integer.parseInt(regionEl.attributeValue("id"));
        int countryId = Integer.parseInt(regionEl.attributeValue("country-id"));
        String title = regionEl.attributeValue("title");
        DateTimeZone timeZone = DateTimeZone.forID(regionEl.attributeValue("time-zone"));

        ListF<Line> borders = Cf.x(regionEl.selectNodes("borders/line")).map(parseLineF());
        Point regionCenter = Point.fromWktPart(regionEl.elementText("center"));
        BoundingBox boundingBox = parseBoundingBox(regionEl.element("bounding-box"));
        return new Region(id, countryId, title, boundingBox, borders, regionCenter, timeZone);
    }

    private static BoundingBox parseBoundingBox(Element boundingBoxEl) {
        Point from = Point.fromWktPart(boundingBoxEl.elementText("from"));
        Point to = Point.fromWktPart(boundingBoxEl.elementText("to"));
        return new BoundingBox(from.lat(), from.lon(), to.lat(), to.lon());
    }

    private static Function<Element, Line> parseLineF() {
        return element -> Line.fromLatLonList(element.getText());
    }
}
