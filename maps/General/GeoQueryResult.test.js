ymaps.modules.define(util.testfile(), [
    'Map',
    'geocode',
    'geoQuery',
    'GeoQueryResult',
    'Placemark'
], function (provide, Map, geocode, geoQuery, GeoQueryResult, Placemark) {
    describe('GeoQueryResult', function () {

        var geoObjects,
            result,
            map;

        beforeEach(function () {
            geoObjects = [
                new Placemark([34, 74]),
                new Placemark([45, 54])
            ];
            result = geoQuery(geoObjects);
            map = new Map('map', {
                center: [45, 54],
                zoom: 9,
                type: null
            });
        });

        afterEach(function () {
            map.destroy();
            result = null;
            geoObjects = [];
        });

       it.skip('Должен создать объект из результатов геокодирования, а затем добавить к нему обычные геообъекты', function (done) {
           this.timeout(10000);
           result
               .add(geocode([35, 64], { kind: 'metro' }))
               .add(geoObjects)
               .then(function () {
                   expect(result.indexOf(geoObjects[0])).not.to.be(-1);
                   done();
               });
       });

        it('Должен корректно найти пересечение выборок', function (done) {
            this.timeout(10000);
            var res1 = new GeoQueryResult(geoObjects);
            res1.then(function () {
                var res2 = res1.intersect(result);
                res2.then(function () {
                    expect(res2.getLength()).to.be(2);
                    done();
                });
            });
        });

        it('Должен корректно передать контекст в then', function (done) {
            this.timeout(10000);
            var context = { a: 1 },
                onSuccess = function () {
                    expect(this).to.be(context);
                    done();
                };
            result.then(onSuccess, context);
        });

        it('Должен корректно передать контекст в then при указании fail', function (done) {
            this.timeout(10000);
            var onSuccess = function () {
                    throw new Error('Должен был вызваться onFail');
                },
                context = { a: 1 },
                onFail = function () {
                    expect(this).to.be(context);
                    done();
                },
                deferred = ymaps.vow.defer();
            deferred.reject();
            result.add(deferred.promise()).then(onSuccess, onFail, context);
        });

        it('Должен добавить объекты выборки в кластеризатор', function () {
            this.timeout(10000);
            var clusterer = result.clusterize({ synchAdd: true });
            map.geoObjects.add(clusterer);
            var state = clusterer.getObjectState(geoObjects[1]);
            expect(state.isShown).to.be(true);
        });

        it('Должен добавить объекты на карту', function () {
            this.timeout(10000);
            result.addToMap(map);
            var it = map.geoObjects.getIterator(),
                obj,
                nObj = 0;
            while ((obj = it.getNext()) != it.STOP_ITERATION) {
                nObj++;
            }
            expect(nObj).to.be(2);
        });

        it('Не должен заново добавлять объекты на карту', function () {
            this.timeout(10000);
            var failCallback = function () {
                throw new Error('Объекты не должны менять карту при повторном добавлении на нее.');
            };
            result.addToMap(map);
            result.addEvents('mapchange', failCallback);
            result.addToMap(map);
            result.removeEvents('mapchange', failCallback);
        });

        it('Должен корректно выполнять getParent', function () {
            this.timeout(10000);
            result.addToMap(map);
            expect(result.add(new Placemark([33, 44])).getParent()).to.be(result);
            expect(result.remove(geoObjects[0]).getParent()).to.be(result);
            expect(result.search('geometry.type == "Point"').getParent()).to.be(result);
            expect(result.intersect(geoQuery([])).getParent()).to.be(result);
            expect(result.searchInside(map).getParent()).to.be(result);
            expect(result.searchIntersect(map).getParent()).to.be(result);
            expect(result.searchContaining(map).getParent()).to.be(result);
            expect(result.sort('lat').getParent()).to.be(result);
            expect(result.sortByDistance(map).getParent()).to.be(result);
            expect(result.reverse().getParent()).to.be(result);
            expect(result.slice(0, 10).getParent()).to.be(result);
        });

        it('Должен удалить объекты с карты', function () {
            this.timeout(10000);
            var res = new GeoQueryResult(geoObjects[0]);
            map.geoObjects.add(geoObjects[0]).add(geoObjects[1]);
            res.removeFromMap(map);
            var it = map.geoObjects.getIterator(),
                obj,
                nObj = 0;
            while ((obj = it.getNext()) != it.STOP_ITERATION) {
                nObj++;
            }
            expect(nObj).to.be(1);
        });

        it('Должен спозицинировать карту по объектам', function () {
            this.timeout(10000);
            result.applyBoundsToMap(map);
            expect(map.getZoom()).to.be.lessThan(9);
        });

       it.skip('Должен корректно отработать чайнинг', function (done) {
           this.timeout(10000);
           map.geoObjects.add(geoObjects[0]).add(geoObjects[1]);
           var res = geoQuery(geocode([35, 64], { kind: 'metro' }))
               .addToMap(map)
               .sort('x')
               .searchInside(map)
               .reverse()
               .slice(0, 1)
               .add(geocode([35, 53]))
               .remove(geoObjects)
               .searchContaining(geoObjects[0])
               .search('lat > 0')
               .searchIntersect(map)
               .sortByDistance(geoObjects[0])
               .map(function (object) {return object})
               .intersect(geoQuery([]))
               .setOptions('visible', true)
               .then(function () {
                   expect(res.isReady()).to.be(true);
                   done();
               });
       });
    });

    provide({});
});
