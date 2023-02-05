ymaps.modules.define(util.testfile(), [
    "geometry.pixel.MultiPolygon",
    "geometry.pixel.Polygon",
    "util.math.areEqual"
], function (provide, MultiPolygon, Polygon, areEqual) {

    describe('geometry.pixel.MultiPolygon', function () {


        it('Smoke test', function () {
            var coords = [
                    // первый многоугольник
                    [
                        // контуры первого многоугольника
                        [
                            [0, 0], [0, 10], [10, 10], [10, 0], [0, 0]
                        ],
                        [
                            [0, 0], [0, 5], [5, 5], [5, 0], [0, 0]
                        ]
                    ],
                    [
                        // контуры второго многоугольника
                        [
                            [2, 2], [2, 12], [12, 12], [12, 2], [2, 2]
                        ],
                        [
                            [2, 2], [2, 5], [5, 5], [5, 2], [2, 2]
                        ]
                    ]
                ],
                poly = new MultiPolygon(coords, 'evenOdd', true),
                pointIn = [11, 11],
                pointOut = [1, 1],
                bounds = [[0, 0], [12, 12]];

            expect(poly.contains(pointIn)).to.be.ok();//, 'Точка лежит внутри мультиполигона.');
            expect(poly.contains(pointOut)).to.not.be.ok();//, 'Точка лежит вне мультиполигона.');
            var polyBounds = poly.getBounds();
            expect(polyBounds[0][0] == bounds[0][0]
                && polyBounds[0][1] == bounds[0][1]
                && polyBounds[1][0] == bounds[1][0]
                && polyBounds[1][1] == bounds[1][1]).to.be.ok();//, "Неверно определились границы.");

            expect(poly.getClosest([-1, -1]).geometryIndex).to.be(0);//, "Неверно определился ближайший к точке многоугольник.");
            expect(poly.getClosest([-1, -1]).pathIndex).to.be(0);//, "Неверно определился ближайший к точке контур.");
        });

        it('equal', function () {
            var onePoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true),
                otherPoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true);

            expect(onePoly.equals(otherPoly)).to.be.ok();
        });


        it('nonequal', function () {
            var onePoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true),
                otherPoly = new MultiPolygon([
                    [
                        [[1, 2], [0, 0]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true);

            expect(onePoly.equals(otherPoly)).to.not.be.ok();
        });


        it('nonequal by type', function () {
            var onePoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true),
                otherPoly = new Polygon([
                    [[1, 2], [3, 4]]
                ], 'evenOdd', true);

            expect(onePoly.equals(otherPoly)).to.not.be.ok();
        });


        it('nonequal by fill rule', function () {
            var onePoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'evenOdd', true),
                otherPoly = new MultiPolygon([
                    [
                        [[1, 2], [3, 4]],
                        [[1, 2], [3, 4]]
                    ]
                ], 'nonZero', true);

            expect(onePoly.equals(otherPoly)).not.to.be.ok();
        });
    });
    provide(true);
});
