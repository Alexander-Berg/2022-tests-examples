ymaps.modules.define(util.testfile(), [
    'Hotspot',
    'hotspot.container.Internal',
    'geometry.pixel.Polygon',
    'shape.Polygon'
], function (provide, Hotspot, InternalContainer, GeometryPixelPolygon, PolygonShape) {
    describe('test.hotspot.container.Internal', function () {
        var container =
        beforeEach(function () {
            container = new InternalContainer();
        });

        afterEach(function () {
            container = null;
        });

        function createShape (coordinates, zIndex) {
            return new Hotspot(
                new PolygonShape(
                    new GeometryPixelPolygon([coordinates], 'evenOdd', {convex: true})
                ),
                zIndex || 0
            );
        }

        it('Должен добавить объект в контейнер и найти его по позиции', function () {
            var shape = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            container.add([shape]);

            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(shape);
            res = container.getObjectsInPosition([0.5, 0.5], 1);
            expect(res.length).to.be(1);
            expect(res[0]).to.be(shape);
        });

        it('Должен добавить два шейпа в хранилище', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            shapes[1] = createShape([[1, 1], [1, 3], [3, 3], [3, 1]]);

            container.add(shapes);

            var res = container.getObjectInPosition([2.5, 2.5], 1);
            expect(res).to.be(shapes[1]);
            res = container.getObjectsInPosition([1.5, 1.5], 1);
            expect(res.length).to.be(2);
        });

        it('Должен вернуть null, если позиция не попала в объект', function () {
            var shape = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            container.add([shape]);
            var res = container.getObjectInPosition([4, 4], 1);
            expect(res).to.be(null);
            res = container.getObjectsInPosition([4, 4], 1);
            expect(res.length).to.be(0);
        });

        it('Должен удалить шейп из контейнера', function () {
            var shape = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            container.add([shape]);
            container.remove([shape]);
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(null);
            res = container.getObjectsInPosition([0.5, 0.5], 1);
            expect(res.length).to.be(0);
        });

        it('Должен добавить шейпы в хранилище и вернуть их в правильном порядке', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 0);
            shapes[1] = createShape([[1, 1], [1, 2], [2, 2], [2, 1]], 2);
            shapes[2] = createShape([[1, 1], [1, 2], [2, 2], [2, 1]], 1);

            container.add(shapes);

            var res = container.getObjectsInPosition([1.5, 1.5], 1);
            expect(res.length).to.be(3);
            expect(res[0]).to.be(shapes[1]);
            expect(res[1]).to.be(shapes[2]);
            expect(res[2]).to.be(shapes[0]);
        });

        it('Должен удалить шейп из контейнера при апдейте', function () {
            var shape = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            container.add([shape]);
            container.getObjectInPosition([0.5, 0.5], 1);
            container.setUnordered(shape);
            container.remove([shape]);
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(null);
        });

        it('Должен добавить, удалить и опять добавить объект в хранилище', function () {
            var shape = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            container.add([shape]);
            container.remove([shape]);

            shape = createShape([[11, 11], [11, 13], [13, 13], [13, 11]]);
            container.add([shape]);
            var res = container.getObjectInPosition([11.5, 11.5], 1);
            expect(res).to.be(shape);
        });

        it('Должен добавить, удалить и опять добавить тот же объект в хранилище', function () {
            var shape1 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]),
                shape2 = createShape([[5, 5], [5, 7], [7, 7], [7, 5]]);
            container.add([shape1, shape2]);
            container.remove([shape1]);
            container.add([shape1]);
            container.remove([shape1, shape2]);
            container.add([shape1]);

            expect(container.getObjectInPosition([1, 1], 1)).to.be(shape1);
            expect(container.getObjectInPosition([6, 6], 1)).to.be(null);
        });

        it('Должен очистить контейнер', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            shapes[1] = createShape([[1, 1], [1, 3], [3, 3], [3, 1]]);

            container.add(shapes);

            container.clear();
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(null);
        });
    });
    provide({});
});
