ymaps.modules.define(util.testfile(), [
    'Map',
    'hotspot.Container',
    'hotspot.manager.ContainerList',
    'Hotspot',
    'hotspot.Manager',
    'shape.Polygon',
    'geometry.pixel.Polygon'
], function (provide, Map, HotspotContainer, ContainerList, Hotspot, HotspotManager, ShapePolygon, PixelPolygon) {

    describe('test.hotspot.manager.ContainerList', function () {
        var map,
            pane,
            containerList;

        function createShape (coordinates, zIndex) {
            return new Hotspot(new ShapePolygon(
                new PixelPolygon([coordinates], 'evenOdd', {convex: true})
            ), zIndex);
        }

        beforeEach(function () {
            map = new Map("map", {
                center: [45, 45],
                zoom: 10,
                controls: [],
                behaviors: [],
                type: null
            });
            pane = map.panes.get('events');
            containerList = new ContainerList(HotspotManager.get(pane), pane);
        });

        afterEach(function () {
            map.destroy();
            map = null;
            pane = null;
            containerList = null;
        });

        it('Должен найти объект в контейнерах без учета zIndex контейнеров', function () {
            var container1 = new HotspotContainer(),
                container2 = new HotspotContainer(),
                shape1 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 2),
                shape2 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 1);
            container1.add([shape1]);
            container2.add([shape2]);
            containerList.insert(container1);
            containerList.insert(container2);
            expect(containerList.getObjectInPosition([0.5, 0.5], 1)).to.be(shape1);
        });

        it('Должен найти нужный объект в контейнерах с одинаковым zIndex', function () {
            var container1 = new HotspotContainer(),
                container2 = new HotspotContainer(),
                shape1 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 2),
                shape2 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 5);
            container1.add([shape1]);
            container2.add([shape2]);
            containerList.insert(container1, 14);
            containerList.insert(container2, 14);
            expect(containerList.getObjectInPosition([0.5, 0.5], 1)).to.be(shape2);
        });

        it('Должен найти нужный объект в контейнерах с разными zIndex', function () {
            var container1 = new HotspotContainer(),
                container2 = new HotspotContainer(),
                shape1 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 2),
                shape2 = createShape([[0, 0], [0, 2], [2, 2], [2, 0]], 5);
            container1.add([shape1]);
            container2.add([shape2]);
            containerList.insert(container1, 14);
            containerList.insert(container2, 2);
            expect(containerList.getObjectInPosition([0.5, 0.5], 1)).to.be(shape1);
        });
    });

    provide({});
});
