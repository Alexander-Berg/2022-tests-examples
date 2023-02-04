package ru.yandex.realty.util.geom;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.newArrayList;

/**
 * @author vyakunin (mailto: vyakunin@yandex-team.ru)
 */
public class IntersectionAreaCalculatorTest {
    private static final double EPSILON = 1e-4;

    private List<Geometry> geometries;
    private double sq_r;
    private double sq_redblue;
    private double sq_redblueblack;
    private double red_blue;
    private double blue_redsqblack;
    private double red_triangle;
    private int square;
    private int blackFigure;
    private int redFigure;
    private int blueFigure;
    private int triangle;

    private void testSimple(List<Geometry> geometries, double scale) {
        double areaScale = scale * scale;
        List<Geometry> redBlueBlack = list(geometries.get(redFigure),
                geometries.get(blueFigure), geometries.get(blackFigure));
        List<Geometry> redSquareBlack = list(geometries.get(redFigure),
                geometries.get(square), geometries.get(blackFigure));
        List<Geometry> redBlue = list(geometries.get(redFigure), geometries.get(blueFigure));
        double calculatedSq_r = IntersectionAreaCalculator
                .calculate(geometries.get(square), list(geometries.get(redFigure)));
        Assert.assertEquals(sq_r * areaScale, calculatedSq_r, EPSILON);
        double calculatedSq_rbb = IntersectionAreaCalculator
                .calculate(geometries.get(square), redBlueBlack);
        Assert.assertEquals(sq_redblueblack * areaScale, calculatedSq_rbb, EPSILON);
        double calculatedSq_rb = IntersectionAreaCalculator
                .calculate(geometries.get(square), redBlue);
        Assert.assertEquals(sq_redblue * areaScale, calculatedSq_rb, EPSILON);
        double calculatedRed_b = IntersectionAreaCalculator
                .calculateIntersectionArea(geometries.get(redFigure), geometries.get(blueFigure));
        Assert.assertEquals(red_blue * areaScale, calculatedRed_b, EPSILON);
        double calculatedBlue_rsb = IntersectionAreaCalculator
                .calculate(geometries.get(blueFigure), redSquareBlack);
        Assert.assertEquals(blue_redsqblack * areaScale, calculatedBlue_rsb, EPSILON);
        double calculatedTr_r = IntersectionAreaCalculator
                .calculateIntersectionArea(geometries.get(triangle), geometries.get(redFigure));
        Assert.assertEquals(red_triangle * areaScale, calculatedTr_r, EPSILON);
    }

    @Before
    public void setUp() throws Exception {
        Geometry squareGeom = buildSquare();
        Geometry blackFigureGeom = buildBlackFigure();
        Geometry blueFigureGeom = buildBlueFigure();
        Geometry redFigureGeom = buildRedFigure();
        Geometry triangleGeom = buildTriangle();
        geometries = newArrayList();
        geometries.add(squareGeom);
        square = 0;
        geometries.add(blackFigureGeom);
        blackFigure = 1;
        geometries.add(redFigureGeom);
        redFigure = 2;
        geometries.add(blueFigureGeom);
        blueFigure = 3;
        geometries.add(triangleGeom);
        triangle = 4;
        sq_r = 5.5;
        sq_redblue = 6;
        sq_redblueblack = 7;
        red_blue = 0;
        blue_redsqblack = 1.5;
        red_triangle = 0.25;
    }

    private static Geometry buildTriangle() {
        Coordinate[] coordinates = new Coordinate[4];
        coordinates[0] = new Coordinate(2, 1);
        coordinates[1] = new Coordinate(3, 2);
        coordinates[2] = new Coordinate(3, 1);
        coordinates[3] = new Coordinate(2, 1);
        return buildPolygon(coordinates);
    }

    private static Geometry buildBlueFigure() {
        Coordinate[] coordinates = new Coordinate[9];
        coordinates[0] = new Coordinate(3, 2);
        coordinates[1] = new Coordinate(6, 5);
        coordinates[2] = new Coordinate(7, 5);
        coordinates[3] = new Coordinate(7, 4);
        coordinates[4] = new Coordinate(6, 4);
        coordinates[5] = new Coordinate(6, 3);
        coordinates[6] = new Coordinate(5, 3);
        coordinates[7] = new Coordinate(5, 2);
        coordinates[8] = new Coordinate(3, 2);
        return buildPolygon(coordinates);
    }

    private static Geometry buildRedFigure() {
        Coordinate[] coordinates = new Coordinate[8];
        coordinates[0] = new Coordinate(1, 1);
        coordinates[1] = new Coordinate(1, 7);
        coordinates[2] = new Coordinate(2, 8);
        coordinates[3] = new Coordinate(4, 8);
        coordinates[4] = new Coordinate(4, 4);
        coordinates[5] = new Coordinate(2, 2);
        coordinates[6] = new Coordinate(3, 1);
        coordinates[7] = new Coordinate(1, 1);
        return buildPolygon(coordinates);
    }

    private static Geometry buildSquare() {
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(2, 3);
        coordinates[1] = new Coordinate(2, 6);
        coordinates[2] = new Coordinate(5, 6);
        coordinates[3] = new Coordinate(5, 3);
        coordinates[4] = new Coordinate(2, 3);
        return buildPolygon(coordinates);
    }

    private static Geometry buildBlackFigure() {
        Coordinate[] coordinates = new Coordinate[10];
        coordinates[0] = new Coordinate(3, 7);
        coordinates[1] = new Coordinate(6, 7);
        coordinates[2] = new Coordinate(7, 6);
        coordinates[3] = new Coordinate(8, 7);
        coordinates[4] = new Coordinate(8, 2);
        coordinates[5] = new Coordinate(6, 2);
        coordinates[6] = new Coordinate(6, 5);
        coordinates[7] = new Coordinate(4, 5);
        coordinates[8] = new Coordinate(4, 6);
        coordinates[9] = new Coordinate(3, 7);
        return buildPolygon(coordinates);
    }

    private static Geometry buildPolygon(Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        CoordinateArraySequence coordinateArraySequence = new CoordinateArraySequence(coordinates);
        LinearRing linearRing = new LinearRing(coordinateArraySequence, geometryFactory);
        return new Polygon(linearRing, null, geometryFactory);
    }

    private void testSimple() {
        testWithScaleAndShift(1, 0, 0);
    }

    private void testWithScaleAndShift(double scale, double dx, double dy) {
        List<Geometry> shiftedGeometries = shiftGeometries(geometries, dx, dy);
        List<Geometry> shiftedAndScaledGeometries = scaleGeometries(shiftedGeometries, scale);
        testSimple(shiftedAndScaledGeometries, scale);
    }

    private static List<Geometry> scaleGeometries(List<Geometry> geometries, double scale) {
        List<Geometry> scaledGeometries = newArrayList();
        for (Geometry geometry : geometries) {
            Coordinate[] coordinates = geometry.getCoordinates();
            coordinates = scaleCoordinates(coordinates, scale);
            scaledGeometries.add(buildPolygon(coordinates));
        }
        return scaledGeometries;
    }

    private static Coordinate[] scaleCoordinates(Coordinate[] coordinates, double scale) {
        Coordinate[] shiftedCoordinates = new Coordinate[coordinates.length];
        int i = 0;
        for (Coordinate coordinate : coordinates) {
            shiftedCoordinates[i++] = new Coordinate(coordinate.x * scale, coordinate.y * scale);
        }
        return shiftedCoordinates;
    }

    private static List<Geometry> shiftGeometries(List<Geometry> geometries, double dx, double dy) {
        List<Geometry> shiftedGeometries = newArrayList();
        for (Geometry geometry : geometries) {
            Coordinate[] coordinates = geometry.getCoordinates();
            coordinates = shiftCoordinates(coordinates, dx, dy);
            shiftedGeometries.add(buildPolygon(coordinates));
        }
        return shiftedGeometries;
    }

    private static Coordinate[] shiftCoordinates(Coordinate[] coordinates, double dx, double dy) {
        Coordinate[] shiftedCoordinates = new Coordinate[coordinates.length];
        int i = 0;
        for (Coordinate coordinate : coordinates) {
            shiftedCoordinates[i++] = new Coordinate(coordinate.x + dx, coordinate.y + dy);
        }
        return shiftedCoordinates;
    }

    @Test
    public void testCalculate() throws Exception {
        testSimple();
    }

    @Test
    public void testCalculateBigPositiveNumbers() throws Exception {
        testWithScaleAndShift(18238.542, 0, 0);
    }

    @Test
    public void testCalculateBigNumbers() throws Exception {
        testWithScaleAndShift(1837418, -6, -6);
    }

    @Test
    public void testCalculateAroundZero() throws Exception {
        testWithScaleAndShift(0.001, -6, -6);
    }

    @Test
    public void testCalculateBigNegative() throws Exception {
        testWithScaleAndShift(173642, -20, -20);
    }

}
