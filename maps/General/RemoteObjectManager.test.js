ymaps.modules.define(util.testfile(), [
    'RemoteObjectManager',
    'Map'
], function (provide, RemoteObjectManager, Map) {

    describe('RemoteObjectManager', function () {
        var map,
            remoteObjectManager;

        // TODO: rewrite all manual window['callback_...']() to jsonp stubs.
        util.mocha.mock.jsonp({passthrough: true});

        beforeEach(function () {
            map = new Map('map', {
                center: [55.755768, 37.617671],
                zoom: 0,
                controls: [],
                type: null
            });
            remoteObjectManager = new RemoteObjectManager('test/%t', {
                paddingTemplate: 'callback_tileBounds_%t_zoom_%z'
            });
        });

        afterEach(function () {
            remoteObjectManager = null;
            map.destroy();
            map = null;
        });

        it('Должен сделать запрос за данными', function (done) {
            remoteObjectManager.objects.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен сделать запрос за данными при размере тайла 512', function (done) {
            map.setZoom(1);
            remoteObjectManager.options.set('loadTileSize', 512);
            remoteObjectManager.objects.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_1']({
                data: {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен сделать запрос за данными и получить кластер', function (done) {
            remoteObjectManager.clusters.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Cluster',
                    id: 0,
                    number: 10,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен сделать запрос за данными на 0 и 1 зуме, получить одинаковый кластер и не перерисовывать его', function (done) {
            var addCounter = 0;
            function changeZoom () {
                map.setZoom(1);
                window['callback_tileBounds_0_0_1_1_zoom_1']({
                    data: {
                        type: 'Cluster',
                        id: 0,
                        number: 10,
                        geometry: {
                            type: 'Point',
                            coordinates: [55.755768, 37.617671]
                        }
                    }
                });
                done();
            }

            remoteObjectManager.clusters.events.add('add', function (e) {
                expect(addCounter).to.be(0);
                addCounter++;
                expect(e.get('objectId')).to.be(0);
                changeZoom();
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Cluster',
                    id: 0,
                    number: 10,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен сделать запрос за данными на 0 и 1 зуме, на 1 зуме ничего не получить и удалить старый объект', function (done) {
            var addCounter = 0;
            function changeZoom () {
                map.setZoom(1);
                window['callback_tileBounds_0_0_1_1_zoom_1']({
                    data: []
                });
            }

            remoteObjectManager.clusters.events.add('add', function (e) {
                expect(addCounter).to.be(0);
                addCounter++;
                expect(e.get('objectId')).to.be(0);
                changeZoom();
            });
            remoteObjectManager.clusters.events.add('remove', function (e) {
                done();
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Cluster',
                    id: 0,
                    number: 10,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен сделать запрос за данными для 0 зума и cделать запросы для 1 зума', function (done) {
            remoteObjectManager.options.set('splitRequests', true);
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });

            function continueTest () {
                var newZoom = map.getZoom() + 1;
                map.setZoom(newZoom);
                var newTileNumber = getTileNumber(map.getCenter(), newZoom),
                    padding = 'callback_tileBounds_' +
                        newTileNumber[0] + '_' +
                        newTileNumber[1] + '_' +
                        newTileNumber[0] + '_' +
                        newTileNumber[1] + '_' +
                        'zoom_1';
                expect(window[padding]).not.to.be(undefined);
                done();
            }
            window.setTimeout(continueTest, 100);

        });

        it('Должен сделать запрос за данными, а затем отфильтровать точечные объекты', function () {
            remoteObjectManager.objects.events.add('add', function (e) {
                expect(remoteObjectManager.getObjectState(1).isFilteredOut).to.be(true);
                expect(remoteObjectManager.getObjectState(0).isFilteredOut).to.be(false);
                done();
            });
            map.geoObjects.add(remoteObjectManager);
            remoteObjectManager.setFilter('object.type == "Cluster"');
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'FeatureCollection',
                    features: [
                        {
                            type: 'Feature',
                            id: 0,
                            number: 10,
                            geometry: {
                                type: 'Cluster',
                                coordinates: [55.755768, 37.617671]
                            }
                        },
                        {
                            type: 'Feature',
                            id: 1,
                            geometry: {
                                type: 'Point',
                                coordinates: [55.755768, 37.617671]
                            }
                        }
                    ]

                }
            });
        });

        it('Должен удалить старые данные и запросить новые при вызове метода reloadData', function (done) {
            remoteObjectManager.objects.events.once('add', function (e) {
                remoteObjectManager.reloadData();
                expect(remoteObjectManager.objects.getLength()).to.be(0);
                remoteObjectManager.objects.events.once('add', function (e) {
                    expect(remoteObjectManager.objects.getLength()).to.be(1);
                    done();
                });
                window['callback_tileBounds_0_0_0_0_zoom_0']({
                    data: {
                        type: 'Feature',
                        id: 0,
                        geometry: {
                            type: 'Point',
                            coordinates: [55.755768, 37.617671]
                        }
                    }
                });
            });
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен корректно вычислить getBounds для точечных объектов', function (done) {
            remoteObjectManager.objects.events.add('add', function (e) {
                var bounds = remoteObjectManager.getBounds(),
                    expectedBounds = [[55.755768, 37.617671], [55.755768, 37.617671]];
                for (var i = 0; i < 2; i++) {
                    for (var j = 0; j < 2; j++) {
                        expect(Math.abs(bounds[i][j] - expectedBounds[i][j])).to.be.lessThan(0.001);
                    }
                }
                done();
            });
            expect(remoteObjectManager.getBounds()).to.be(null);
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: {
                    type: 'Feature',
                    id: 0,
                    geometry: {
                        type: 'Point',
                        coordinates: [55.755768, 37.617671]
                    }
                }
            });
        });

        it('Должен корректно вычислить getBounds для неточечных объектов', function () {
            remoteObjectManager.objects.events.add('add', function (e) {
                var bounds = remoteObjectManager.getBounds(),
                    expectedBounds = [
                        [37.521587, 55.74954],
                        [37.621587, 55.94954]
                    ];
                for (var i = 0; i < 2; i++) {
                    for (var j = 0; j < 2; j++) {
                        expect(Math.abs(bounds[i][j] - expectedBounds[i][j])).to.be.lessThan(0.001);
                    }
                }
                done();
            });
            expect(remoteObjectManager.getBounds()).to.be(null);
            map.geoObjects.add(remoteObjectManager);
            window['callback_tileBounds_0_0_0_0_zoom_0']({
                data: [
                    {
                        type: 'Feature',
                        id: 0,
                        geometry: {
                            type: 'Point',
                            coordinates: [37.571587, 55.84954]
                        }
                    },
                    {
                        type: 'Feature',
                        id: 1,
                        geometry: {
                            type: 'Rectangle',
                            coordinates: [
                                [37.621587, 55.74954],
                                [37.521587, 55.94954]
                            ]
                        }
                    }
                ]
            });
        });

        function getTileNumber (position, zoom) {
            var globalPixels = map.options.get('projection').toGlobalPixels(position, zoom);
            return [Math.floor(globalPixels[0] / 256), Math.floor(globalPixels[1] / 256)];
        }
    });

    provide({});
});
