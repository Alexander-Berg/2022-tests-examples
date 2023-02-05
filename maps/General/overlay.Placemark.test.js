ymaps.modules.define(util.testfile(), [
    'geometry.pixel.Point',
    'geometry.pixel.Rectangle',
    'shape.Rectangle',
    'Map',
    'data.Manager',
    'option.Manager',
    'option.Mapper',
    'templateLayoutFactory',
    'overlay.Placemark',
    'hotspot.Manager',
    'Hotspot',
    'layout.storage',
    'canvasLayout.storage',
    'vow',

    // Подключаем стандартные опции для оверлея.
    'theme.islands.geoObject.meta.full'
], function (
    provide,
    PixelPoint, PixelRectangle, ShapeRectangle, Map, DataManager, OptionManager,
    OptionMapper, templateLayoutFactory, PlacemarkOverlay, HotspotManager, Hotspot,
    layoutStorage, canvasLayoutStorage, vow
) {

    describe('overlay.Placemark', function () {
        var map,
            optionMapper = new OptionMapper();

        optionMapper.setRule({
            name: 'overlay.Placemark',
            rule: function (key, name) {
                return [addPrefix(key, 'geoObjectIcon')];
            }
        });

        optionMapper.setRule({
            name: 'overlay.Placemark',
            key: 'projection',
            rule: 'plain'
        });

        function addPrefix (key, prefix) {
            return prefix + key.slice(0, 1).toUpperCase() + key.slice(1);
        }

        beforeEach(function () {
            map = new Map('map', {
                center: [55.751574, 37.573856],
                controls: [],
                type: null,
                zoom: 3
            });
        });

        afterEach(function () {
            map.destroy();
        });

        function createPlacemarkOverlay (point, properties, options) {
            var overlay = new PlacemarkOverlay(new PixelPoint(point), properties, options),
                optionManager = new OptionManager({ pane: 'places' });
            optionManager.setParent(map.options);
            optionManager.setMapper(optionMapper);
            overlay.options.setParent(optionManager);
            return overlay;
        }

        it('Простая инициализация', function () {
            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter());
            overlay.setMap(map);
        });

        it('Добавление и удаление с карты', function () {
            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter());
            overlay.setMap(map);
            overlay.setMap(null);
        });

        it('Собственный синхронный html-макет (dom)', function () {
            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter(), {}, {
                layout: templateLayoutFactory.createClass('<div id="myId">123</div>')
            });

            overlay.setMap(map);

            expect(document.getElementById('myId')).to.be.ok();
        });

        it('Собственный асинхронный html-макет (dom)', function (done) {
            layoutStorage.define('overlay.Placemark.test.1', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div id="template">123</div>'),
                    20));
            });

            var overlay = createPlacemarkOverlay([0, 0], { }, {
                layout: 'overlay.Placemark.test.1'
            });

            overlay.setMap(map);

            setTimeout(function () {
                expect(document.getElementById('template')).to.be.ok();
                done();
            }, 1000);
        });
        /*
        // TODO обновить этот тест перед публикацией интерфейса i-canvas-layout
        it('Собственный асинхронный html-макет (canvas)', function (done) {
            var counter = 0;
            canvasLayoutStorage.define('overlay.Placemark.test.2', function (provide) {
                setTimeout(function () {
                    ++counter;
                    provide({
                        renderLayout: function (renderingContext) {},
                        setData: function (data) {},
                        getData: function () {},
                        build: function () {},
                        destroy: function () {},
                        getShape: function () {},
                        getSize: function () {}
                    });
                }, 20);
            });

            var overlay = createPlacemarkOverlay([0, 0], { }, {
                layout: 'overlay.Placemark.test.2'
            });

            overlay.setMap(map);
            setTimeout(function () {
                expect(counter).to.be(1);
                done();
            }, 50);
        *         });
        */

        it('Получение шейпа', function () {
            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter());
            overlay.setMap(map);
            var shape = overlay.getShape();
            expect(shape).to.be.ok();
            var bounds = shape.getBounds();
            expect(bounds).to.eql([
                [1226.753491911111, 604.0592176857858],
                [1253.753491911111, 645.0592176857858]
            ]);
        });

        it('Событие shapechange', function (done) {
            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter());
            overlay.setMap(map);
            overlay.events.once('shapechange', function () {
                var shape = overlay.getShape();
                expect(shape).to.be.ok();
                done();
            });
            var coords = map.getGlobalPixelCenter();
            overlay.setGeometry(
                new PixelPoint([
                    coords[0] + 0.2,
                    coords[1] + 0.2
                ])
            );
        });

        it('Событие shapechange после получения асинхронного макета (dom)', function (done) {
            layoutStorage.define('overlay.Placemark.test.5', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div id="template">123</div>', {
                        getShape: function () {
                            return new ShapeRectangle(new PixelRectangle([0, 0, 52, 52]));
                        }
                    }),
                    20));
            });

            var overlay = createPlacemarkOverlay(map.getGlobalPixelCenter(), {}, {
                layout: 'overlay.Placemark.test.5'
            });
            overlay.setMap(map);
            expect(overlay.getShape()).to.not.be.ok();
            overlay.events.add('shapechange', function () {
                expect(overlay.getShape()).to.be.ok();
                done();
            });
        });

        // TODO Добавить тест "Событие shapechange после получения асинхронного макета (canvas)"
        // публикацией интерфейса i-canvas-layout

        it('Удаление хотспотов после удаления с карты', function () {
            var containerList = HotspotManager.get(map.panes.get('events')).getContainerList(),
                coords = map.getGlobalPixelCenter(),
                overlay = createPlacemarkOverlay(map.getGlobalPixelCenter());
            overlay.setMap(map);
            expect(containerList.getObjectInPosition(coords, map.getZoom())).to.be.a(Hotspot);
            overlay.setMap(null);
            expect(containerList.getObjectInPosition(coords, map.getZoom())).to.be(null);
            // В режиме release обращение к приватным методам работать не будет.
            //TODO: До момента когда эта логика появится в текущем билдере
            /*if (containerList._list) {
                expect(containerList._list.hash).to.be.empty();
            }*/
        });

        it('Должен не создавать хотспот при отключенной интерактивности', function () {
            var containerList = HotspotManager.get(map.panes.get('events')).getContainerList(),
                coords = map.getGlobalPixelCenter(),
                overlay = createPlacemarkOverlay(map.getGlobalPixelCenter(), {}, {interactive: false});
            overlay.setMap(map);
            expect(containerList.getObjectInPosition(coords, map.getZoom())).to.be(null);
            overlay.options.set('interactive', true);
            expect(containerList.getObjectInPosition(coords, map.getZoom())).to.be.a(Hotspot);
            overlay.options.set('interactive', false);
            expect(containerList.getObjectInPosition(coords, map.getZoom())).to.be(null);
        });
    });

    provide();
});
