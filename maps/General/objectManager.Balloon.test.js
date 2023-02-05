ymaps.modules.define(util.testfile(), [
    'ObjectManager',
    'objectManager.addon.objectsBalloon',
    'Map'
], function (provide, ObjectManager, balloonAddon, Map) {
    describe('objectManager.Balloon', function () {
        this.timeout(10000);

        var myMap,
            objectManager,
            features;

        beforeEach(function () {
            myMap = new Map('map', {
                center: [55.755768, 37.617671],
                zoom: 5,
                controls: [],
                type: null
            });
            features = [
                {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                },
                {
                    type: 'Feature',
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.775768, 37.657671]
                    }
                }
            ];
            objectManager = new ObjectManager();
            objectManager.add(features);
            myMap.geoObjects.add(objectManager);
        });

        afterEach(function () {
            myMap.destroy();
        });

        it('Должен корректно отработать при задании опции geoObjectOpenBalloonOnClick [MAPSAPI-9299]', function () {
            var ojectManagerWithOptions = new ObjectManager({
                geoObjectOpenBalloonOnClick: false
            });
            myMap.geoObjects.add(ojectManagerWithOptions);
        });

        it('Должен корректно отработать последовательность open-close на открытом балуне', function (done) {
            objectManager.objects.balloon.open(1).then(function () {
                objectManager.objects.balloon.open(1);
                var promise = objectManager.objects.balloon.close();
                promise.then(function () {
                    expect(promise.isFulfilled()).to.be(true);
                    expect(objectManager.objects.balloon.isOpen(1)).to.be(false);
                    done();
                });
            });
        });

        it('Должен корректно отработать последовательность open-close в одном тике', function (done) {
            objectManager.objects.balloon.open(1).then(function () {
                done('Был получен resolve');
            }, function () {
                done();
            });
            objectManager.objects.balloon.close();
        });

        it('Должен корректно отработать последовательность open-destroy в одном тике', function (done) {
            objectManager.objects.balloon.open(1).then(function () {
                done('Был получен resolve');
            }, function () {
                done();
            });
            objectManager.objects.balloon.destroy();
        });

        it('Должен скрыть метки объектов при открытии балуна', function (done) {
            var balloonManager = objectManager.objects.balloon;
            balloonManager.open(1).then(function () {
                expect(objectManager.objects.overlays.getById(1).getMap()).to.be(null);
                balloonManager.open(0).then(function () {
                    expect(objectManager.objects.overlays.getById(1).getMap()).not.to.be(null);
                    expect(objectManager.objects.overlays.getById(0).getMap()).to.be(null);
                    done();
                });
            });
        });

        it('Должен очистить менеджер балуна при изменении опции hasBalloon', function () {
            expect(objectManager.objects.balloon).to.be.ok();
            objectManager.options.set('hasBalloon', false);
            expect(objectManager.objects.balloon).not.to.be.ok();
        });

        // MAPSAPI-9861, MAPSAPI-9863
        it('Должен произвести отписку от события карты при удалении менеджера балуна', function () {
            expect(objectManager.objects.balloon).to.be.ok();
            objectManager.options.set('hasBalloon', false);
            expect(objectManager.objects.balloon).not.to.be.ok();
            myMap.setCenter([40, 40]);
        });

        // MAPSAPI-10343
        it('Должен задать макет контента балуна конкретной точке [MAPSAPI-10343]', function (done) {
            var objectManager = new ObjectManager({
                geoObjectOpenBalloonOnClick: false
            });
            var layoutInited = false;
            var BalloonLayout = ymaps.templateLayoutFactory.createClass('123', {
                build: function () {
                    layoutInited = true;
                    BalloonLayout.superclass.build.call(this);
                }
            });
            ymaps.layout.storage.add('b#layoutBalloon', BalloonLayout);

            objectManager.add({
                type: 'Feature',
                id: 17,
                geometry: {
                    type: 'Point',
                    coordinates: [55.755768, 37.617671]
                },
                options: {
                    balloonContentLayout: 'b#layoutBalloon'
                }
            });

            myMap.geoObjects.add(objectManager);

            objectManager.objects.balloon.open(17).then(function() {
                ymaps.layout.storage.remove('b#layoutBalloon');
                expect(layoutInited).to.be.ok();
                done();
            }, this);
        });
    });
    provide({});
});
