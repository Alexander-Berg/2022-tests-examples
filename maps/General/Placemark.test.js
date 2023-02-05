ymaps.modules.define(util.testfile(), [
    'Map',
    'Placemark',
    'geometry.Point',
    'geoObject.addon.balloon',
    'util.dom.element',
    'templateLayoutFactory',
    'canvasLayout.storage',
    'layout.storage',
    'system.browser'
], function (provide, Map, Placemark, PointGeometry, balloonAddon, domElement,
    templateLayoutFactory, canvasLayoutStorage, layoutStorage, browser) {

    describe('Placemark', function () {
        var map;
        beforeEach(function () {
            map = new Map('map', {
                center: [37, 56],
                zoom: 4,
                type: null,
                controls: []
            });
        });

        afterEach(function () {
            map.destroy();
        });

        it('Должен создать метку при передачи координат с конструктор.', function () {
            var placemark = new Placemark([37, 56]),
                coords = placemark.geometry.getCoordinates();
            expect(coords).to.have.length(2);
            expect(coords[0]).to.be(37);
            expect(coords[1]).to.be(56);
        });

        it('Должен создать метку при передаче объекта с описанием геометрии.', function () {
            var placemark = new Placemark({
                    type: "Point",
                    coordinates: [37, 56]
                }),
                coords = placemark.geometry.getCoordinates();
            expect(coords).to.have.length(2);
            expect(coords[0]).to.be(37);
            expect(coords[1]).to.be(56);
        });

        it('Должен создать метку при передаче IGeometry в конструкторе.', function () {
            var placemark = new Placemark(new PointGeometry([37, 56])),
                coords = placemark.geometry.getCoordinates();
            expect(coords).to.have.length(2);
            expect(coords[0]).to.be(37);
            expect(coords[1]).to.be(56);
        });

        it('Должен добавить метку на карту.', function () {
            var placemark = new Placemark([37, 56]);
            map.geoObjects.add(placemark);
        });

        it('Должен удалить метку с карты.', function (done) {
            var placemark = new Placemark([37, 56]);
            map.geoObjects.add(placemark);
            setTimeout(function () {
                map.geoObjects.remove(placemark);
                done();
            }, 500);
        });

        it('Должен создать метку с балуном. Балун после открытия должен иметь макет.', function (done) {
            this.timeout(10000);
            var placemark = new Placemark([37, 56], {
                balloonContent: "test test"
            });
            map.geoObjects.add(placemark);
            placemark.balloon.open().then(function () {
                var parentElement = map.container.getElement(),
                    balloonNode = domElement.findByPrefixedClass(parentElement, 'balloon'),
                    balloonContent = domElement.findByPrefixedClass(parentElement, 'balloon__content');

                expect(balloonNode).to.be.ok();
                expect(balloonContent).to.be.ok();
                done();
            }, this);
        });

        it('Должен создать метку с пользовательским HTML-макетом.', function (done) {
            var placemark = new Placemark([37, 56], {}, {
                iconLayout: templateLayoutFactory.createClass('<div id=ididid>123</div>'),
                syncOverlayInit: true
            });
            map.geoObjects.add(placemark);

            placemark.getOverlay().done(function (overlay) {
                expect(
                    document.getElementById('ididid')
                ).to.be.ok();
                done();
            }, function () {
                expect().fail('Был получен reject на получение оверлея');
                done();
            }, this);
        });

        it('Должен обратиться к хранилищу html макетов при установке опции iconRenderMode равной "dom".', function (done) {
            layoutStorage.define('placemark.test.1', function () {
                done();
                return ymaps.vow.resolve();
            });

            canvasLayoutStorage.define('placemark.test.1', function () {
                expect().fail('Обращение не к тому хранилищу');
            });

            var placemark = new Placemark([37, 56], {}, {
                iconLayout: {
                    domLayout: 'placemark.test.1',
                    canvasLayout: 'placemark.test.1'
                },
                iconRenderMode: 'dom'
            });
            map.geoObjects.add(placemark);
        });

        it('Должен обратиться к хранилищу canvas макетов при установке опции iconRenderMode равной "auto".', function (done) {
            layoutStorage.define('placemark.test.2', function () {
                expect().fail('Обращение не к тому хранилищу');
            });

            canvasLayoutStorage.define('placemark.test.2', function () {
                done();
                return ymaps.vow.resolve();
            });

            var placemark = new Placemark([37, 56], {}, {
                iconLayout: {
                    domLayout: 'placemark.test.2',
                    canvasLayout: 'placemark.test.2'
                },
                iconRenderMode: 'auto'
            });

            map.geoObjects.add(placemark);
        });

        it('Должен обратиться к обычному хранилищу макетов при установке опции iconLayout строкой.', function (done) {
            layoutStorage.define('placemark.test.3', function () {
                done();
                return ymaps.vow.resolve();
            });

            var placemark = new Placemark([37, 56], {}, {
                iconLayout: 'placemark.test.3',
                iconRenderType: 'auto'
            });
            map.geoObjects.add(placemark);
        });
    });
    provide({});
});
