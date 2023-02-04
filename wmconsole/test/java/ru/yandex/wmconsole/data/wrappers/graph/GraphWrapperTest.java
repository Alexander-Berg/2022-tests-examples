package ru.yandex.wmconsole.data.wrappers.graph;


import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.wmconsole.data.info.graph.Graph;
import ru.yandex.wmconsole.data.info.graph.GraphDay;
import ru.yandex.wmconsole.data.info.graph.GraphPoint;
import ru.yandex.wmconsole.data.info.graph.GraphSystem;
import ru.yandex.wmtools.common.util.XmlDataWrapper;

import java.util.Arrays;
import java.util.List;

/**
 * GraphWrapper Tester.
 *
 * @author yakushev
 */
public class GraphWrapperTest {

    private Graph graph = new Graph();

    private String dateStr1 = "2008-08-06";
    private List<GraphPoint> points1 = Arrays.asList(
            new GraphPoint(0.5),
            new GraphPoint(0.6),
            new GraphPoint(0.7)
    );

    private String dateStr2 = "2008-08-07";
    private List<GraphPoint> points2 = Arrays.asList(
            new GraphPoint(0.8),
            new GraphPoint(0.9),
            new GraphPoint(1.0)
    );

    @Before
    public void setUp() throws Exception {
        graph.addParam("show-zebra", "true");
        graph.addSystem(new GraphSystem("line1", false, "#fddada"));
        graph.addSystem(new GraphSystem("line2", true));

        graph.addDay(createDay(dateStr1, points1));
        graph.addDay(createDay(dateStr2, points2));
    }

    @Test
    public void testToXml() throws Exception {
        GraphWrapper wrapper = new GraphWrapper(graph);
        StringBuilder buffer = new StringBuilder();
        wrapper.toXml(buffer);

        StringBuilder result = new StringBuilder("<graph show-zebra=\"true\"><systems>");
        result.append("<system name=\"line1\" color=\"#fddada\"></system>");
        result.append("<system name=\"line2\" selected=\"true\"></system>");
        result.append("</systems>");

        result.append("<day-group>");
        result.append(DayWrapperTest.toString(dateStr1, points1));
        result.append(DayWrapperTest.toString(dateStr2, points2));
        result.append("</day-group>");
        result.append("</graph>");

        assertEquals(result.toString(), buffer.toString());
    }

    private GraphDay createDay(String dateStr, List<GraphPoint> points) throws Exception {
        return new GraphDay(XmlDataWrapper.getDateFormat().parse(dateStr), points);
    }

}
