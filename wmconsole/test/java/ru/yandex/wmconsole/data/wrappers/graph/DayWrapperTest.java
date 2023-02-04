package ru.yandex.wmconsole.data.wrappers.graph;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.yandex.wmconsole.data.info.graph.GraphDay;
import ru.yandex.wmconsole.data.info.graph.GraphPoint;
import ru.yandex.wmtools.common.util.XmlDataWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DayWrapper Tester.
 *
 * @author yakushev
 */
public class DayWrapperTest {

    @Test
    public void testToXml() throws Exception {

        List<GraphPoint> points = new ArrayList<GraphPoint>();
        points.add(new GraphPoint(0.95));
        points.add(new GraphPoint(0.90));
        points.add(new GraphPoint(0.85));

        String dateStr = "2008-08-06";
        Date date = XmlDataWrapper.getDateFormat().parse(dateStr);
        GraphDay day = new GraphDay(date, points);

        GraphDayWrapper wrapper = new GraphDayWrapper(day);

        StringBuilder buffer = new StringBuilder();
        wrapper.toXml(buffer);

        assertEquals(toString(dateStr, points), buffer.toString());
    }

    public static String toString(String dateStr, List<GraphPoint> points) {
        StringBuilder result = new StringBuilder("<day date=\"");
        result.append(dateStr).append("\">");
        for (GraphPoint point : points) {
            result.append(GraphPointWrapperTest.point2String(point));
        }
        result.append("</day>");
        return result.toString();
    }
    
}
