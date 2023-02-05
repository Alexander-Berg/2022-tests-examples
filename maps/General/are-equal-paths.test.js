ymaps.modules.define(util.testfile(), [
    'util.math.areEqualPaths'
], function (provide, areEqualPaths) {
    describe('util.math.areEqualPaths', function () {
        it('Массивы должны быть равны на уровне 0', function () {
            var onePath = [[1, 2], [3, 4]];
            var otherPath = [[1, 2], [3, 4]];
            expect(areEqualPaths(onePath, otherPath, 0)).to.be(true);
        });

        it('Массивы не должны быть равны по длине на уровне 0', function () {
            var onePath = [[1, 2]];
            var otherPath = [[1, 2], [3, 4]];
            expect(areEqualPaths(onePath, otherPath, 0)).to.be(false);
        });

        it('Массивы не должны быть равны на уровне 0', function () {
            var onePath = [[1, 2], [3, 4]];
            var otherPath = [[0, 0], [3, 4]];
            expect(areEqualPaths(onePath, otherPath, 0)).to.be(false);
        });

        it('Массивы должны быть равны до уровня 2', function () {
            var onePath = [
                [
                    [[1, 2], [3, 4]],
                    [[1, 2], [3, 4]]
                ]
            ];
            var otherPath = [
                [
                    [[1, 2], [3, 4]],
                    [[1, 2], [3, 4]]
                ]
            ];
            expect(areEqualPaths(onePath, otherPath, 2)).to.be(true);
        });

        it('Массивы не должны быть равны по длине до уровня 2', function () {
            var onePath = [
                [
                    [[1, 2], [3, 4]],
                    [[1, 2], [3, 4]]
                ]
            ];
            var otherPath = [
                [
                    [[1, 2]],
                    [[1, 2], [3, 4]]
                ]
            ];
            expect(areEqualPaths(onePath, otherPath, 2)).to.be(false);
        });

        it('Массивы не должны быть равны до уровня 2', function () {
            var onePath = [
                [
                    [[1, 2], [3, 4]],
                    [[1, 2], [3, 4]]
                ]
            ];
            var otherPath = [
                [
                    [[1, 2], [0, 0]],
                    [[1, 2], [3, 4]]
                ]
            ];
            expect(areEqualPaths(onePath, otherPath, 2)).to.be(false);
        });

        it('Массивы должны быть равны с учетом погрешности на уровне 0', function () {
            var onePath = [[1.05, 2], [3, 4]];
            var otherPath = [[1.06, 2], [3, 4]];
            expect(areEqualPaths(onePath, otherPath, 0, true, 0.1)).to.be(true);
        });

        it('Массивы не должны быть равны с учетом погрешности на уровне 0', function () {
            var onePath = [[1.5, 2]];
            var otherPath = [[1.6, 2], [3, 4]];
            expect(areEqualPaths(onePath, otherPath, 0, true, 0.1)).to.be(false);
        });

        it('Массивы должны быть равны с учетом погрешности до уровня уровня 1', function () {
            var onePath = [
                [
                    [1.05, 2],
                    [3, 4]
                ]
            ];
            var otherPath = [
                [
                    [1.06, 2],
                    [3, 4]
                ]
            ];
            expect(areEqualPaths(onePath, otherPath, 1, true, 0.1)).to.be(true);
        });

        it('Массивы не должны быть равны с учетом погрешности до уровня 1', function () {
            var onePath = [
                [
                    [1.5, 2],
                    [3, 4]
                ]
            ];
            var otherPath = [
                [
                    [1.6, 2],
                    [3, 4]
                ]
            ];
            expect(areEqualPaths(onePath, otherPath, 1, true, 0.1)).to.be(false);
        });
    });
    provide();
});
