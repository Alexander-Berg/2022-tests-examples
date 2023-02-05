ymaps.modules.define(util.testfile(), [
    "Map",
    "geometry.MultiLineString",
    "geometry.serializer",
    "util.math.areEqual"
], function (provide, Map, MultiLineString, serializer, areEqual) {

    describe('geometry.MultiLineString', function () {
        var line, geoMap, zoom;
        beforeEach(function () {
            line = new MultiLineString([[
                [1, 2],
                [3, 4],
                [5, 6]
            ], [
                [7, 8],
                [9, 10]
            ]]);
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
            line = new MultiLineString();
            line.setMap(geoMap);
            line.options.setParent(geoMap.options);
            expect(line.getPixelGeometry().getCoordinates()).to.be.eql([]);
        });

        it('Должен проинициализироваться при вызове конструктора c координатами', function () {
            var coords = [[
                [1, 1],
                [2, 2]
            ]];
            line = new MultiLineString([[
                [1, 1],
                [2, 2]
            ]]);
            line.setMap(geoMap);
            line.options.setParent(geoMap.options);
            expect(line.getCoordinates()).to.be.eql(coords);
        });

        it('Должен проинициализироваться при вызове конструктора c двумя контурами', function () {
            var coords = [[
                [1, 2],
                [3, 4],
                [5, 6]
            ], [
                [7, 8],
                [9, 10]
            ]];
            line = new MultiLineString([[
                [1, 2],
                [3, 4],
                [5, 6]
            ], [
                [7, 8],
                [9, 10]
            ]]);
            line.setMap(geoMap);
            line.options.setParent(geoMap.options);
            expect(line.getCoordinates()).to.be.eql(coords);
        });

        it('Должен проинициализироваться при вызове конструктора c опциями', function () {
            line = new MultiLineString([[
                [1, 1],
                [2, 2]
            ]], {geodesic: true});
            line.setMap(geoMap);
            line.options.setParent(geoMap.options);
            expect(line.options.get('geodesic', false)).to.be.eql(true);
        });

        it('Метод getPixelGeometry должен вернуть корректно значение координат', function () {
            line.setMap(geoMap);
            line.options.setParent(geoMap.options);
            line.setCoordinates([[], [
                [0, 0],
                [85.084, -180]
            ]]);
            var pixelsBase = line.getPixelGeometry().getCoordinates(),
                pixels = pixelsBase[1];
            pixels = [
                [Math.round(pixels[0][0]), Math.round(pixels[0][1])],
                [Math.round(pixels[1][0]), Math.round(pixels[1][1])]
            ];
            expect(pixelsBase[0]).to.be.eql([]);

            expect(pixels).to.be.eql([
                [128 * Math.pow(2, zoom), 128 * Math.pow(2, zoom)],
                [0, 0]
            ]);
        });

        it('Должен декодировать координаты', function () {
            var geometry = new MultiLineString([[
                    [1, 2],
                    [2, 3]
                ], [
                    [37.593578, 55.735094],
                    [37.592159, 55.732468999999995],
                    [37.589374, 55.734162]
                ]]),
                encoded = serializer.serialize(geometry),
                decoded = serializer.deserialize(encoded);
            expect(decoded.getCoordinates().length).to.be.eql(2);
            expect(geometry.getCoordinates()).to.be.eql(decoded.getCoordinates());
        });

        describe('getClosest', function () {

            it('Должен должен вернуть ближайщую точку на прямоугольнике', function () {
                line = new MultiLineString([[], [
                    [-23.910059107939137, 56.328124999999694],
                    [53.016000330121834, 56.328124999999694],
                    [53.016000330121834, 136.32812499999974],
                    [-23.910059107939137, 136.32812499999974],
                    [-23.910059107939137, 56.328124999999694]
                ], []]);
                line.setMap(geoMap);
                line.options.setParent(geoMap.options);
                var closest = line.getClosest([9.895341639681464, 161.3157749999996]);
                expect(closest).to.be.ok();
                expect(
                    areEqual(closest.position, [9.895341639578158, 136.32812499999974])
                ).to.be.ok();
            });

            it('Должен должен вернуть ближайщую точку на прямоугольнике. Точка внутри фигуры.', function () {
                line = new MultiLineString([[], [
                    [-4.819385274295243, 46.48437499999942],
                    [63.32999614158463, 46.48437499999942],
                    [63.32999614158463, 126.48437499999928],
                    [-4.819385274295243, 126.48437499999928],
                    [-4.819385274295243, 46.48437499999942]
                ], []]);
                line.setMap(geoMap);
                line.options.setParent(geoMap.options);
                var closest = line.getClosest([27.873435980843286, 91.00327499999861]);
                expect(closest).to.be.ok();
                expect(
                    areEqual(closest.position, [-4.819385274234155, 91.00327499999861])
                ).to.be.ok();
            });

            it('Должен должен вернуть ближайщую точку на прямоугольнике при пересечении фигуры 180й меридианы', function () {
                line = new MultiLineString([[], [
                    [5.782826434292808, 146.32812499999787],
                    [67.69534925717343, 146.32812499999787],
                    [67.69534925717343, -133.67187500000105],
                    [5.782826434292808, -133.67187500000105],
                    [5.782826434292808, 146.32812499999787]
                ], []]);
                line.setMap(geoMap);
                line.options.setParent(geoMap.options);
                var closest = line.getClosest([31.555027430934665, 85.37827499999852]);
                expect(closest).to.be.ok();
                expect(
                    areEqual(closest.position, [31.55502743094471, 146.32812499999787])
                ).to.be.ok();
            });

            it('Должен должен вернуть ближайщую точку на прямоугольнике. Фигура в первом мире, а точка в нулевом.', function () {
                line = new MultiLineString([[], [
                    [12.763134720894817, 80.93749999999704],
                    [70.21923899701443, 80.93749999999704],
                    [70.21923899701443, 160.93749999999878],
                    [12.763134720894817, 160.93749999999878],
                    [12.763134720894817, 80.93749999999704]
                ], []]);
                line.setMap(geoMap);
                line.options.setParent(geoMap.options);
                var closest = line.getClosest([42.25051976915414, -134.69985000000318]);
                expect(closest).to.be.ok();
                expect(
                    areEqual(closest.position, [42.25051976918028, 160.93749999999872])
                ).to.be.ok();
            });

            it('Должен должен вернуть ближайщую точку на прямоугольнике. Фигура в нулевом мире, а точка в первом.', function () {
                line = new MultiLineString([[], [
                    [7.1893353767544275, -149.687500000002],
                    [68.22353867833041, -149.687500000002],
                    [68.22353867833041, -69.68750000000108],
                    [7.1893353767544275, -69.68750000000108],
                    [7.1893353767544275, -149.687500000002]
                ], []]);
                line.setMap(geoMap);
                line.options.setParent(geoMap.options);
                var closest = line.getClosest([42.770723164888, 120.53452499999786]);
                expect(closest).to.be.ok();
                expect(
                    areEqual(closest.position, [42.77072316491305, -149.687500000002])
                ).to.be.ok();
            });
        });
    });
    provide(true);
});
