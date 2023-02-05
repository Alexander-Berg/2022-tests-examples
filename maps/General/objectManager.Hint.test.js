ymaps.modules.define(util.testfile(), [
    'ObjectManager',
    'objectManager.addon.objectsHint',
    'Map'
], function (provide, ObjectManager, hintAddon, Map) {
    describe('objectManager.Hint', function () {
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

        it('Должен показать хинт и кинуть событие', function (done) {
            var objectsLog = '',
                objectsHintLog = '';
            objectManager.objects.events.add(['hintopen'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.open(0).then(function () {
                expect(objectManager.objects.hint.isOpen(0)).to.be(true);
                expect(objectsLog).to.be(' hintopen');
                expect(objectsHintLog).to.be(' open');
                done();
            });
        });

        it('Не должен кинуть лишнего события open при повторном открытии хинта', function (done) {
            var objectsLog = '',
                objectsHintLog = '';
            objectManager.objects.events.add(['hintopen'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.open(0).then(function () {
                objectManager.objects.hint.open(0).then(function () {
                    expect(objectsLog).to.be(' hintopen');
                    expect(objectsHintLog).to.be(' open');
                    done();
                });
            });
        });

        it('Должен открыть и закрыть хинт и кинуть события', function (done) {
            var objectsLog = '',
                objectsHintLog = '';
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.open(0).then(function () {
                objectManager.objects.hint.close().then(function () {
                    expect(objectManager.objects.hint.isOpen(0)).to.be(false);
                    expect(objectsLog).to.be(' hintopen hintclose');
                    expect(objectsHintLog).to.be(' open close');
                    done();
                });
            });
        });

        it('Должен корректно отработать повторное открытие хинта на одном объекте', function (done) {
            var objectsLog = '',
                objectsHintLog = '';
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.open(0);
            objectManager.objects.hint.open(0).then(function () {
                expect(objectManager.objects.hint.isOpen(0)).to.be(true);
                expect(objectsLog).to.be(' hintopen');
                expect(objectsHintLog).to.be(' open');
                done();
            });
        });

        it('Должен корректно отработать повторное закрытие хинта на одном объекте', function (done) {
            var objectsLog = '',
                objectsHintLog = '';
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.open(0).then(function () {
                objectManager.objects.hint.close();
                objectManager.objects.hint.close().then(function () {
                    expect(objectManager.objects.hint.isOpen(0)).to.be(false);
                    expect(objectsLog).to.be(' hintopen hintclose');
                    expect(objectsHintLog).to.be(' open close');
                    done();
                });
            });
        });

        it('Должен корректно отработать последовательность открыть-закрыть', function (done) {
            objectManager.objects.hint.open(0).then(function () {
                done('Был получен resolve');
            }, function () {
                done();
            });
            objectManager.objects.hint.close();
        });

        it('Должен корректно отработать последовательность открыть-уничтожить [MAPSAPI-10082]', function (done) {
            objectManager.objects.hint.open(0).then(function () {
                done('Был получен resolve');
            }, function () {
                done();
            });
            objectManager.objects.hint.destroy();
        });

        it('Должен корректно отработать последовательность открыть-закрыть-открыть', function (done) {
            var objectsLog = '',
                objectsHintLog = '',
                hintManager = objectManager.objects.hint;
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            hintManager.open(0);
            hintManager.close();
            hintManager.open(0).then(function () {
                expect(hintManager.isOpen(0)).to.be(true);
                expect(objectsLog).to.be(' hintopen');
                expect(objectsHintLog).to.be(' open');
                done();
            });
        });

        it('Должен корректно отработать последовательность (открыть-закрыть)x2', function (done) {
            var objectsLog = '',
                objectsHintLog = '',
                hintManager = objectManager.objects.hint;
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type');
            });
            hintManager.open(0);
            hintManager.close();
            hintManager.open(0);
            hintManager.close().then(function () {
                expect(hintManager.isOpen(0)).to.be(false);
                expect(objectsLog).to.be('');
                expect(objectsHintLog).to.be('');
                done();
            });
        });

        it('Должен корректно отработать изменение опции hasHint', function () {
            objectManager.objects.options.set('hasHint', false);
            expect(objectManager.objects.hint).to.be(undefined);
        });

        it('Должен корректно отработать изменение опции hasHint при открытом хинте', function (done) {
            var hintManager = objectManager.objects.hint;
            hintManager.open(0).then(function () {
                objectManager.objects.options.set('hasHint', false);
                expect(objectManager.objects.hint).to.be(undefined);
                done();
            });
        });

        it('Должен корректно отработать открытие хинта на соседнем объекте', function (done) {
            var objectsLog = '',
                objectsHintLog = '',
                hintManager = objectManager.objects.hint;
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type') + e.get('objectId');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type') + e.get('objectId');
            });

            hintManager.open(0).then(function () {
                hintManager.open(1).then(function () {
                    expect(objectsLog).to.be(' hintopen0 hintclose0 hintopen1');
                    expect(objectsHintLog).to.be(' open0 close0 open1');
                    expect(hintManager.isOpen(0)).to.be(false);
                    expect(hintManager.isOpen(1)).to.be(true);
                    done();
                });
            });
        });

        it('Должен корректно отработать открытие хинта на соседнем объекте, а затем на себе', function (done) {
            var objectsLog = '',
                objectsHintLog = '',
                hintManager = objectManager.objects.hint;
            objectManager.objects.events.add(['hintopen', 'hintclose'], function (e) {
                objectsLog += ' ' + e.get('type') + e.get('objectId');
            });
            objectManager.objects.hint.events.add(['open', 'close'], function (e) {
                objectsHintLog += ' ' + e.get('type') + e.get('objectId');
            });

            hintManager.open(0);
            hintManager.open(1);
            hintManager.open(0).then(function () {
                expect(objectsLog).to.be(' hintopen0');
                expect(objectsHintLog).to.be(' open0');
                expect(hintManager.isOpen(0)).to.be(true);
                expect(hintManager.isOpen(1)).to.be(false);
                done();
            });
        });


        // MAPSAPI-10343
        it('Должен задать макет контента хинта конкретной точке [MAPSAPI-10343]', function (done) {
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
            ymaps.layout.storage.add('b#layoutHint', BalloonLayout);

            objectManager.add({
                type: 'Feature',
                id: 17,
                geometry: {
                    type: 'Point',
                    coordinates: [55.755768, 37.617671]
                },
                options: {
                    hintContentLayout: 'b#layoutHint'
                }
            });

            myMap.geoObjects.add(objectManager);

            objectManager.objects.hint.open(17).then(function() {
                ymaps.layout.storage.remove('b#layoutHint');
                expect(layoutInited).to.be.ok();
                done();
            }, this);
        });
    });
    provide({});
});
