ymaps.modules.define(util.testfile(), [
    'Hotspot',
    'hotspot.Container',
    'geometry.pixel.Polygon',
    'shape.Polygon'
], function (provide, Hotspot, Container, GeometryPixelPolygon, PolygonShape) {
    describe('test.hotspot.Container', function () {
        var container;
        beforeEach(function () {
            container = new Container();
        });

        afterEach(function () {
            container = null;
        });

        function createShape (coordinates, zIndex) {
            return new Hotspot(
                new PolygonShape(
                    new GeometryPixelPolygon([coordinates], 'evenOdd', {convex: true})
                ), zIndex
            );
        }

        it('Должен добавить объект в контейнер и найти его по позиции', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);

            container.add(shapes);
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(shapes[0]);
        });

        it('Должен учесть порядок добавления шейпов в контейнер', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 1);
            shapes[1] = createShape([[0, 0], [0, 3], [3, 3], [3, 0]], 1);

            container.add(shapes);
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(shapes[1]);
        });

        it('Должен учесть zIndex объектов', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 2);
            shapes[1] = createShape([[0, 0], [0, 3], [3, 3], [3, 0]], 1);

            container.add(shapes);
            var res = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res).to.be(shapes[0]);
        });

        it('Должен добавить шейпы в хранилище и вернуть их в правильном порядке', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 0);
            shapes[1] = createShape([[1, 1], [1, 2], [2, 2], [2, 1]], 1);
            shapes[2] = createShape([[1, 1], [1, 2], [2, 2], [2, 1]], 2);
            shapes[3] = createShape([[1, 1], [1, 2], [2, 2], [2, 1]], 1);

            container.add(shapes);

            var res = container.getObjectsInPosition([1.5, 1.5], 1);
            expect(res.length).to.be(4);
            expect(res[0]).to.be(shapes[2]);
            expect(res[1]).to.be(shapes[3]);
            expect(res[2]).to.be(shapes[1]);
            expect(res[3]).to.be(shapes[0]);
        });

        it('Должен найти объект, когда 2 шейпа лежат в разных внутренних контейнерах', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            shapes[1] = createShape([[0, 0], [0, 10000], [10000, 10000], [10000, 0]]);

            container.add(shapes);
            var res = container.getObjectInPosition([700.5, 700.5], 1);
            expect(res).to.be(shapes[1]);
        });

        it('Должен очистить контейнер', function () {
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            shapes[1] = createShape([[0, 0], [0, 10000], [10000, 10000], [10000, 0]]);

            container.add(shapes);
            container.clear();

            var res1 = container.getObjectInPosition([700.5, 700.5], 1),
                res2 = container.getObjectInPosition([0.5, 0.5], 1);
            expect(res1).to.be(null);
            expect(res2).to.be(null);
        });

        it('Должен кинуть событие empty после очистки контейнера', function () {
            var emptyCounter = 0;
            container.events.add('empty', function () {
                emptyCounter++;
            });
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);

            container.add(shapes);

            container.clear();
            expect(emptyCounter).to.be(1);
            container.clear();
            expect(emptyCounter).to.be(1);
        });

        it('Должен кинуть событие empty после поочерендного удаления элементов', function () {
            var emptyCounter = 0;
            container.events.add('empty', function () {
                emptyCounter++;
            });
            var shapes = [];
            shapes[0] = createShape([[0, 0], [0, 2], [2, 2], [2, 0]]);
            shapes[1] = createShape([[0, 0], [0, 10000], [10000, 10000], [10000, 0]]);

            container.add(shapes);

            // Нужно дернуть функцию, чтобы в контейнере упорядочились элементы
            container.forEach(function () {});
            container.remove(shapes);

            // Нужно дернуть функцию, чтобы в контейнере упорядочились элементы
            container.forEach(function () {});
            expect(emptyCounter).to.be(1);

            container.clear();
            container.forEach(function () {});
            expect(emptyCounter).to.be(1);
        });
    });
    provide({});
});
