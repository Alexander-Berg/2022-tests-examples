ymaps.modules.define(util.testfile(), [
    'Map',
    'MapEvent',
    'hotspot.Manager',
    'Hotspot',
    'shape.Rectangle',
    'geometry.pixel.Rectangle',
    'hotspot.Container',
    'Event'
], function (provide, Map, MapEvent, HotspotManager, Hotspot, ShapeRectangle, PixelRectangle, HotspotContainer, Event) {

    describe('test.hotspot.Manager', function () {
        var map,
            pane,
            container,
            manager,
            shapes,
            log;

        function createShape (coordinates, zIndex) {
            var shape = new Hotspot(
                new ShapeRectangle(new PixelRectangle(coordinates)),
                zIndex
            );
            container.add([shape]);
            shape.events.add(['click', 'mouseenter', 'mouseleave', 'mousemove'], shapeEventHandler);
            shapes.push(shape);
            return shape;
        }

        function destroyShapes () {
            for (var i = 0, l = shapes.length; i < l; i++) {
                shapes[i].events.remove(['click', 'mouseenter', 'mouseleave', 'mousemove'], shapeEventHandler);
            }
            shapes = null;
        }

        function shapeEventHandler (e) {
            log += ' ' + e.get('type');
        }

        beforeEach(function () {
            map = new Map('map', { center: [180, 55.76], zoom: 7, type: null });
            container = new HotspotContainer();
            log = '';
            shapes = [];
            pane = map.panes.get('events');

            manager = HotspotManager.get(pane);
            manager.getContainerList().insert(container);

        });

        afterEach(function () {
            destroyShapes();
            container.clear();
            container = null;
            manager = null;
            map.destroy();
            log = null;
            pane = null;
        });

        it('Должен корректно обработать клик на объекте', function () {
            createShape([
                [0, 0],
                [3, 3]
            ], 1);
            var event = createMapEvent('click', [1, 1]);
            manager.process('click', event);
            expect(log).to.be(' mouseenter click');
        });

        it('Должен снять фокус с элемента', function () {
            createShape([
                [0, 0],
                [3, 3]
            ], 1);
            var event = createMapEvent('click', [1, 1]);
            manager.process('click', event);
            manager.blur();
            expect(log).to.be(' mouseenter click mouseleave');
        });

        it('Должен поймать клик на пейне и прокинуть на шейп', function () {
            createShape([
                [0, 0],
                [3, 3]
            ], 1);
            pane.events.fire('click', createMapEvent('click', [1, 1]));
            expect(log).to.be(' mouseenter click');
        });

        it('Должен поймать клик на шейпе, а потом уйти с шейпа при клике мимо него', function () {
            createShape([
                [0, 0],
                [3, 3]
            ], 1);
            pane.events.fire('click', createMapEvent('click', [1, 1]));
            pane.events.fire('click', createMapEvent('click', [101, 1]));
            expect(log).to.be(' mouseenter click mouseleave');
        });

        function createMapEvent (type, globalPixelPosition) {
            return new MapEvent({
                target: map.panes.get('events'),
                position: map.converter.globalToPage(globalPixelPosition),
                globalPixels: globalPixelPosition,
                map: map,
                type: type,
                domEvent: new Event({
                    pageX: 0,
                    pageY: 0,
                    propagatedData: {}
                })
            });
        }
    });

    provide({});
});
