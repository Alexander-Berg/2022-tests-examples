ymaps.modules.define(util.testfile(), [
    "Map",
    "geometry.MultiPolygon",
    "geometry.serializer",
    "util.math.areEqualPaths"
], function (provide, Map, MultiPolygon, serializer, areEqualPaths) {

    function doublePolygon () {
        return [
            [
                [55.75, 37.50],
                [55.80, 37.60],
                [55.75, 37.70],
                [55.70, 37.70],
                [55.70, 37.50],
                [55.75, 37.50]
            ],
            // Координаты вершин внутреннего контура.
            [
                [55.75, 37.52],
                [55.75, 37.68],
                [55.65, 37.60],
                [55.75, 37.52]
            ]
        ]
    }

    function areEqual (a, b) {
        return areEqualPaths(a, b, 2, true);
    }

    describe('geometry.MultiPolygon', function () {
        var polygon, geoMap, zoom;
        beforeEach(function () {
            zoom = 3;
            geoMap = new Map('map', {
                center: [39, 54],
                type: "yandex#map",
                zoom: zoom,
                behaviors: [],
                type: null
            });
        });

        afterEach(function () {
            geoMap.destroy();
            geoMap = null;
        });

        it('Должен проинициализироваться при вызове пустого конструктора', function () {
            polygon = new MultiPolygon();
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            expect(polygon.getPixelGeometry().getCoordinates()).to.be.eql([]);
        });

        it('Должен проинициализироваться при вызове конструктора c координатами', function () {
            var coords = [doublePolygon()];
            polygon = new MultiPolygon([doublePolygon()]);
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            expect(areEqual(polygon.getCoordinates(), coords)).to.be.ok();
        });

        it('Должен проинициализироваться при вызове конструктора c двумя контурами', function () {
            var coords = [doublePolygon(), doublePolygon()];
            polygon = new MultiPolygon([doublePolygon(), doublePolygon()]);
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            expect(areEqual(polygon.getCoordinates(), coords)).to.be.ok();
        });


        it('Должен декодировать координаты', function () {
            var geometry = new MultiPolygon([doublePolygon(),doublePolygon()]),
                encoded = serializer.serialize(geometry),
                decoded = serializer.deserialize(encoded);
            expect(decoded.getCoordinates().length).to.be.eql(2);
            expect(areEqual(geometry.getCoordinates(), decoded.getCoordinates())).to.be.ok();
        });

        it('Должен должен вернуть ближайщую точку на прямоугольнике', function () {
            polygon = new MultiPolygon([doublePolygon()]);
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            var closest = polygon.getClosest([55.80, 37.60]);
            expect(closest).to.be.ok();
            expect(
                areEqual(closest.position, [55.80, 37.60])
            ).to.be.ok();
        });

        it('contains nonZero', function () {
            polygon = new MultiPolygon([doublePolygon()], 'evenOdd');
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            expect(polygon.contains([55.75, 37.60])).to.not.be.ok();
        });

        it('contains nonZero', function () {
            polygon = new MultiPolygon([doublePolygon()], 'nonZero');
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);
            expect(polygon.contains([55.75, 37.60])).to.be.ok();
        });

    });
    provide(true);
});
