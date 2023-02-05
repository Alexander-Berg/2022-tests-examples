ymaps.modules.define(util.testfile(), [
    'Clusterer',
    'clusterer.addon.hint',
    'Map',
    'Placemark'
], function (provide, Clusterer, hintAddon, Map, Placemark) {
    describe('clusterer.Hint', function () {
        var myMap,
            clusterer,
            placemarks;

        beforeEach(function () {
            myMap = new Map('map', {
                center: [55.755768,37.617671],
                zoom: 5,
                controls: [],
                type: null
            });
            placemarks = [
                new Placemark([55.755768,37.617671]),
                new Placemark([55.755768,37.617671]),

                new Placemark([55.775768,37.657671]),
                new Placemark([55.775768,37.657671])
            ];
            clusterer = new Clusterer({ groupByCoordinates: true, hintCloseTimeout: 0, hintOpenTimeout: 0  });
            clusterer.add(placemarks);
            myMap.geoObjects.add(clusterer);
        });

        afterEach(function () {
            myMap.destroy();
        });

        it('Должен показать хинт и кинуть событие', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster).then(function () {
                expect(clusterer.hint.isOpen(cluster)).to.be(true);
                expect(clustererLog).to.be(' hintopen');
                expect(clustererHintLog).to.be(' open');
                done();
            });
        });

        it('Не должен кинуть лишнее событие open при повторном открытии хинта', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster).then(function () {
                clusterer.hint.open(cluster).then(function () {
                    expect(clustererLog).to.be(' hintopen');
                    expect(clustererHintLog).to.be(' open');
                    done();
                });
            });
        });

        it('Должен открыть и закрыть хинт и кинуть события', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen', 'hintclose'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open', 'close'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster).then(function () {
                clusterer.hint.close().then(function () {
                    expect(clusterer.hint.isOpen(cluster)).to.be(false);
                    expect(clustererLog).to.be(' hintopen hintclose');
                    expect(clustererHintLog).to.be(' open close');
                    done();
                });
            });
        });

        it('Должен корректно отработать повторное открытие хинта на одном объекте', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster);
            clusterer.hint.open(cluster).then(function () {
                expect(clusterer.hint.isOpen(cluster)).to.be(true);
                expect(clustererLog).to.be(' hintopen');
                expect(clustererHintLog).to.be(' open');
                done();
            });
        });

        it('Должен корректно отработать повторное закрытие хинта на одном объекте', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen', 'hintclose'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open', 'close'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster).then(function () {
                clusterer.hint.close();
                clusterer.hint.close().then(function () {
                    expect(clusterer.hint.isOpen(cluster)).to.be(false);
                    expect(clustererLog).to.be(' hintopen hintclose');
                    expect(clustererHintLog).to.be(' open close');
                    done();
                });
            });
        });

        it('Должен корректно отработать последовательность открыть-закрыть-открыть', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster);
            clusterer.hint.close();
            clusterer.hint.open(cluster).then(function () {
                expect(clusterer.hint.isOpen(cluster)).to.be(true);
                expect(clustererLog).to.be(' hintopen');
                expect(clustererHintLog).to.be(' open');
                done();
            });
        });

        it('Должен корректно отработать последовательность (открыть-закрыть)x2', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster);
            clusterer.hint.close();
            clusterer.hint.open(cluster);
            clusterer.hint.close().then(function () {
                expect(clusterer.hint.isOpen(cluster)).to.be(false);
                expect(clustererLog).to.be('');
                expect(clustererHintLog).to.be('');
                done();
            });
        });

        it('Должен корректно отработать изменение опции hasHint', function () {
            clusterer.options.set('hasHint', false);
            expect(clusterer.hint).to.be(undefined);
        });

        it('Должен корректно отработать изменение опции hasHint при открытом хинте', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '';
            clusterer.events.add(['hintopen', 'hintclose'], function (e) {
                clustererLog += ' ' + e.get('type');
            });
            clusterer.hint.events.add(['open', 'close'], function (e) {
                clustererHintLog += ' ' + e.get('type');
            });
            var cluster = clusterer.getObjectState(placemarks[0]).cluster;
            clusterer.hint.open(cluster).then(function () {
                clusterer.options.set('hasHint', false);
                expect(clusterer.hint).to.be(undefined);
                done();
            });
        });

        it('Должен корректно отработать открытие хинта на соседнем кластере', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '',
                cluster1 = clusterer.getObjectState(placemarks[0]).cluster,
                cluster2 = clusterer.getObjectState(placemarks[2]).cluster;

            clusterer.events.add(['hintopen', 'hintclose'], function (e) {
                clustererLog += ' ' + e.get('type') + (e.get('cluster') == cluster1 ? '1' : '2');
            });
            clusterer.hint.events.add(['open', 'close'], function (e) {
                clustererHintLog += ' ' + e.get('type') + (e.get('cluster') == cluster1 ? '1' : '2');
            });

            clusterer.hint.open(cluster1).then(function () {
                clusterer.hint.open(cluster2).then(function () {
                    expect(clustererLog).to.be(' hintopen1 hintclose1 hintopen2');
                    expect(clustererHintLog).to.be(' open1 close1 open2');
                    expect(clusterer.hint.isOpen(cluster1)).to.be(false);
                    expect(clusterer.hint.isOpen(cluster2)).to.be(true);
                    done();
                });
            });
        });

        it('Должен корректно отработать открытие хинта на соседнем кластере, а затем на себе', function (done) {
            this.timeout(10000);

            var clustererLog = '',
                clustererHintLog = '',
                cluster1 = clusterer.getObjectState(placemarks[0]).cluster,
                cluster2 = clusterer.getObjectState(placemarks[2]).cluster;

            clusterer.events.add(['hintopen', 'hintclose'], function (e) {
                clustererLog += ' ' + e.get('type') + (e.get('cluster') == cluster1 ? '1' : '2');
            });
            clusterer.hint.events.add(['open', 'close'], function (e) {
                clustererHintLog += ' ' + e.get('type') + (e.get('cluster') == cluster1 ? '1' : '2');
            });

            clusterer.hint.open(cluster1);
            clusterer.hint.open(cluster2);
            clusterer.hint.open(cluster1).then(function () {
                expect(clustererLog).to.be(' hintopen1');
                expect(clustererHintLog).to.be(' open1');
                expect(clusterer.hint.isOpen(cluster1)).to.be(true);
                expect(clusterer.hint.isOpen(cluster2)).to.be(false);
                done();
            });
        });
    });
    provide({});
});
