ymaps.modules.define(util.testfile(), [
    "util.pixelBounds"
], function (provide, utilPixelBounds) {

    describe('util.pixelBounds', function () {

        describe('util.pixelBounds.fromPoints', function () {
            it('Должен прямоугольную область по заданным точкам', function () {
                var result = utilPixelBounds.fromPoints([
                    [3, 3],
                    [5, 5],
                    [-20, 40],
                    [40, 50]
                ]);
                expect(result).to.eql([
                    [-20, 3],
                    [40, 50]
                ]);
            });
        });

        describe('util.pixelBounds.fromBounds', function () {
            it('Должен рассчитать область, которая охватывает все переданные.', function () {
                var result = utilPixelBounds.fromBounds([
                    [
                        [1, 1],
                        [10, 10]
                    ],
                    [
                        [5, 5],
                        [50, 50]
                    ],
                    [
                        [6, 6],
                        [33, 10]
                    ],
                    [
                        [66, 3],
                        [80, 5]
                    ]
                ]);
                expect(result).to.eql([
                    [1, 1],
                    [80, 50]
                ]);
            });
        });

        describe('util.pixelBounds.getCenter', function () {
            it('Должен вернуть центр прямоугольника', function () {
                var result = utilPixelBounds.getCenter([
                    [0, 0],
                    [50, 30]
                ]);
                expect(result).to.eql([25, 15]);
            });
        });

        describe('util.pixelBounds.getSize', function () {
            it('Должен вернуть размер прямоугольника', function () {
                var result = utilPixelBounds.getSize([
                    [-20, -20],
                    [50, 30]
                ]);
                expect(result).to.eql([70, 50]);
            });
        });

        describe('util.pixelBounds.areIntersecting', function () {
            it('Должен вернуть положительный результат проверки пересечения двух областей', function () {
                var result = utilPixelBounds.areIntersecting(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [
                        [30, 30],
                        [130, 130]
                    ]
                );
                expect(result).to.be.ok();
            });

            it('Должен вернуть отрицательный результат проверки пересечения двух областей', function () {
                var result = utilPixelBounds.areIntersecting(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [
                        [130, 130],
                        [230, 140]
                    ]
                );
                expect(result).to.not.be.ok();
            });
        });

        describe('util.pixelBounds.containsPoint', function () {
            it('Должен вернуть положительный результат проверки попадания точки в область', function () {
                var result = utilPixelBounds.containsPoint(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [50, 50]
                );
                expect(result).to.be.ok();
            });

            it('Должен вернуть отрицательный результат проверки попадания точки в область', function () {
                var result = utilPixelBounds.containsPoint(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [150, 150]
                );
                expect(result).to.not.be.ok();
            });
        });


        describe('util.pixelBounds.containsBounds', function () {

            it('Должен вернуть положительный результат проверки попадания области в область', function () {
                var result = utilPixelBounds.containsBounds(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [
                        [50, 50],
                        [70, 70]
                    ]
                );
                expect(result).to.be.ok();
            });

            it('Должен вернуть положительный результат при передаче эквивалентых областей', function () {
                var result = utilPixelBounds.containsBounds(
                    [
                        [20, 20],
                        [80, 80]
                    ],
                    [
                        [20, 20],
                        [80, 80]
                    ]
                );
                expect(result).to.be.ok();
            });

            it('Должен вернуть отрицательный результат проверки попадания области в область', function () {
                var result = utilPixelBounds.containsBounds(
                    [
                        [0, 0],
                        [100, 100]
                    ],
                    [
                        [150, 150],
                        [170, 170]
                    ]
                );
                expect(result).to.not.be.ok();
            });
        });
        
        describe('util.pixelBounds.getIntersection', function () {

            it('Должен вернуть одно пересечение двух прямоугольных областей', function () {
                var result = utilPixelBounds.getIntersection([
                    [10, 10],
                    [40, 40]
                ], [
                    [20, 20],
                    [90, 90]
                ]);

                expect(result).to.be.eql([
                    [20, 20],
                    [40, 40]
                ]);
            });

            it('Не должен вернуть значение', function () {
                var result = utilPixelBounds.getIntersection([
                    [10, 10],
                    [40, 40]
                ], [
                    [50, 50],
                    [90, 90]
                ]);
                expect(result).to.be.eql(null);
            });

            it('Должен вернуть область, которая полностью внутри другой', function () {
                var bounds2 = [
                    [15, 15],
                    [30, 30]
                ];
                var result = utilPixelBounds.getIntersection([
                    [10, 10],
                    [40, 40]
                ], bounds2);
                expect(result).to.be.eql(bounds2);
            });
        });

        describe('util.pixelBounds.clone', function () {
            it('Должен создать копию области', function () {
                var origin = [
                        [0, 0],
                        [10, 10]
                    ],
                    result = utilPixelBounds.clone(origin);
                expect(origin).to.eql(result);
            });
        });

        describe('util.pixelBounds.fit', function () {
            var fit = utilPixelBounds.fit;
            it('Должен вернуть необходимое смещение', function () {
                var res = fit([
                    [0, 0],
                    [100, 100]
                ], [
                    [-10, 10],
                    [100, 100]
                ]);
                expect(res[0]).to.be(0);
                expect(res[1]).to.be(10);
            });

            it('Должен вернуть необходимое смещение c учетом margin заданным одним числом', function () {
                var res = fit([
                    [0, 0],
                    [100, 100]
                ], [
                    [-10, 10],
                    [100, 100]
                ]);
                expect(res[0]).to.be(0);
                expect(res[1]).to.be(10);
            });

            it('Должен вернуть необходимое смещение c учетом margin заданным двумя числами', function () {
                var res = fit([
                    [0, 90],
                    [10, 100]
                ], [
                    [0, 0],
                    [100, 100]
                ], '20');
                expect(res[0]).to.be(20);
                expect(res[1]).to.be(-20);
            });

            it('Должен вернуть необходимое смещение c учетом margin заданным четырмя числами', function () {
                var res = fit([
                    [0, 0],
                    [10, 10]
                ], [
                    [5, 5],
                    [10, 10]
                ], [10, 20, -100, -200]);
                expect(res[0]).to.be(-20);
                expect(res[1]).to.be(15);
            });

            it('Должен вернуть null', function () {
                var res = fit([
                    [0, 0],
                    [100, 100]
                ], [
                    [0, 0],
                    [90, 90]
                ]);
                expect(res).to.not.be.ok();
            });
        });

    });

    provide({});
});
