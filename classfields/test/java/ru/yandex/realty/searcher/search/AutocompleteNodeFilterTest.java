package ru.yandex.realty.searcher.search;

import org.junit.Test;
import ru.yandex.common.util.functional.Filter;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.search.AutocompleteNodeFilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: azakharov
 * Date: 12.07.17
 */
public class AutocompleteNodeFilterTest {

    private Node metro = createMetroNode("Садовая", "метро Садовая");
    private Node city = createNode(GeoObjectType.CITY, "Выборг", "город Выборг");
    private Node subjectFederation = createNode(GeoObjectType.SUBJECT_FEDERATION, "Республика Крым", "Республика Крым");

    @Test
    public void testFilterByM() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("м");
        // REALTY-4497
        assertFalse(filter.fits(metro));
    }

    @Test
    public void testFilterByMetr() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("метр");
        // REALTY-4497
        assertFalse(filter.fits(metro));
    }

    @Test
    public void testFilterBySad() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("Сад");
        assertTrue(filter.fits(metro));
    }

    @Test
    public void testFilterBySadovaya() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("Садовая");
        assertTrue(filter.fits(metro));
    }

    @Test
    public void testFilterByMetroSadovaya() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("метро Садовая");
        assertTrue(filter.fits(metro));
    }

    @Test
    public void testVyborg() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("выборг");
        assertTrue(filter.fits(city));
    }

    @Test
    public void testGorodVyborg() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("город выборг");
        assertTrue(filter.fits(city));
    }

    @Test
    public void testRespKrym() {
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("крым");
        assertTrue(filter.fits(subjectFederation));
    }

    @Test
    public void testMetroShosseEntuziastov() {
        Node node = createMetroNode("Шоссе Энтузиастов", "метро Шоссе Энтузиастов");
        Filter<Node> filter = AutocompleteNodeFilter.getNameFilter("шоссе энту");
        //REALTY-12156
        assertTrue(filter.fits(node));
    }

    private Node createMetroNode(String displayName, String officialName) {
        return createNode(GeoObjectType.METRO_STATION, displayName, officialName);
    }

    private Node createNode(GeoObjectType type, String displayName, String officialName) {
        Node n = Node.createNodeForGeoObjectType(type);
        Name name = new Name();
        name.setDisplay(displayName);
        name.setOfficial(officialName);
        n.setName(name);
        return n;
    }
}
