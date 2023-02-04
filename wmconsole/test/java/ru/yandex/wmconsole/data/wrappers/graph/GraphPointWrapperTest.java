package ru.yandex.wmconsole.data.wrappers.graph;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.yandex.wmconsole.data.info.graph.GraphPoint;

/**
 * GraphPointWrapper Tester.
 *
 * @author yakushev
 */
public class GraphPointWrapperTest {

    @Test
    public void testToXml() {

        GraphPoint point = new GraphPoint(0.95, 100);

        GraphPointWrapper wrapper = new GraphPointWrapper(point);

        StringBuilder buffer = new StringBuilder();
        wrapper.toXml(buffer);

        assertEquals(point2String(point), buffer.toString());
    }

    public static String point2String(GraphPoint p) {
        return "<point value=\""  + p.getValue() + "\" size=\"" + p.getSize() + "\"></point>";
    }

}
