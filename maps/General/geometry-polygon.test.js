ymaps.modules.define(util.testfile(), [
    "Map",
    "geometry.Polygon",
    "geometry.serializer",
    "util.math.areEqualPaths"
], function (provide, Map, Polygon, serializer, areEqualPaths) {

    describe('geometry.Polygon', function () {
        var polygon, geoMap, zoom;
        beforeEach(function () {
            polygon = new Polygon([
                [[1, 2], [3, 4]],
                [[5, 6], [7, 8], [8, 5]]
            ]);
            this.zoom = 0;
            geoMap = new Map('map', {
                center: [39, 54],
                type: null,
                zoom: this.zoom,
                behaviors: []
            });
        });

        afterEach(function () {
            geoMap.destroy();
            geoMap = null;
        });


        it('Должен проинициализироваться при вызове пустого конструктора', function () {
            polygon = new Polygon();

            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);

            expect(polygon.getBounds()).to.be.eql(null);
            expect(polygon.getPixelGeometry().getCoordinates()).to.have.length(0);
        });


        it('Должен проинициализироваться при вызове конструктора c пустыми координатами', function () {
            polygon = new Polygon([
                [[1, 2], [3, 4]],
                []
            ]);

            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);

            expect(areEqualPaths(polygon.getBounds(), [[1, 2], [3, 4]], 0, true)).to.be.ok();
            expect(polygon.getPixelGeometry().getCoordinates()[1]).to.have.length(0);
        });


        it('Должен создать пиксельную геометрию', function () {
            polygon.options.set('coordRendering', 'straightPath');
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);

            polygon.setCoordinates([[[0, 0], [85.084, -179.9], [85.084, 179.9]]]);

            var pixels = roundRecursive(polygon.getPixelGeometry(0).getCoordinates());

            expect(pixels).to.have.length(1);
            expect(pixels[0]).to.have.length(4);
        });

        it('Должен декодировать координаты', function () {
            var
                expected = [
                    [
                        [32.06169, 43.32006], [32.76482, 52.28859], [57.37419, 50.52997], [47.53044, 44.33806],
                        [32.06169, 43.32006]
                    ],
                    [
                        [76.66344, 46.26446], [80.88219, 56.15117], [88.265, 55.55794], [89.31969, 44.5309],
                        [76.66344, 46.26446]
                    ]
                ],
                geometry = Polygon.fromEncodedCoordinates(
                    "-jjpAfwClQKaugoAUtmIAFqCdwFkKuX_2stp_9qEof8y9xP_cHfw_w==;kMqRBIzwwQJ-X0AA9tuWABqncACy8vb_4hcQAKC9V_-G4T7_uHMaAA=="
                );
            expect(expected).to.be.eql(geometry.getCoordinates());
        });


        it('Должен закодировать координаты', function () {
            var geometry = new Polygon([
                    [
                        [32.06169, 43.32006], [32.76482, 52.28859], [57.37419, 50.52997], [47.53044, 44.33806],
                        [32.06169, 43.32006]
                    ],
                    [
                        [76.66344, 46.26446], [80.88219, 56.15117], [88.265, 55.55794], [89.31969, 44.5309],
                        [76.66344, 46.26446]
                    ]
                ]),
                encoded = Polygon.toEncodedCoordinates(geometry);
            expect(encoded).to.be.eql("-jjpAfwClQKaugoAUtmIAFqCdwFkKuX_2stp_9qEof8y9xP_cHfw_w==;kMqRBIzwwQJ-X0AA9tuWABqncACy8vb_4hcQAKC9V_-G4T7_uHMaAA==");
        });


        it('getClosest', function () {
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);

            var result = polygon.getClosest([0, 0]);
            expect(result).to.be.ok();
            expect(result.closestPointIndex).to.be(0);
            expect(result.pathIndex).to.be(0);
        });


        it('contains', function () {
            polygon.setMap(geoMap);
            polygon.options.setParent(geoMap.options);

            expect(!polygon.contains([5, 7])).to.be.ok()
            expect(polygon.contains([7, 6])).to.be.ok()
            expect(polygon.contains([1, 2])).to.be.ok()
        });
    });


    function roundRecursive (array) {
        var result = [];

        for (var i = 0; i < array.length; i++) {
            if (isNaN(array[i])) {
                result.push(roundRecursive(array[i]));
            } else {
                result.push(Math.round(array[i]));
            }
        }

        return result;
    }

    provide(true);
});
