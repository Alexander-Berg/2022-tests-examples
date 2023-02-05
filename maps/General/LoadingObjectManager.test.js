ymaps.modules.define(util.testfile(), [
    'LoadingObjectManager',
    'Map'
], function (provide, LoadingObjectManager, Map) {

    describe('LoadingObjectManager', function () {
        var map,
            loadingObjectManager;

        // TODO: rewrite all manual window['callback_...']() to jsonp stubs.
        util.mocha.mock.jsonp({passthrough: true});

        beforeEach(function () {
            map = new Map('map', {
                center: [55.755768, 37.617671],
                zoom: 0,
                controls: [],
                type: null
            });
            loadingObjectManager = new LoadingObjectManager('test/%t', {
                paddingTemplate: 'callback_tileBounds_%t_zoom_%z'
            });
        });

        afterEach(function () {
            loadingObjectManager = null;
            map.destroy();
            map = null;
        });

        it('Должен сделать запрос за данными', function (done) {
            loadingObjectManager.objects.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(loadingObjectManager);
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

        it('Должен сделать запрос за данными на 1 зуме', function (done) {
            map.setZoom(1);
            loadingObjectManager.objects.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(loadingObjectManager);
            window['callback_tileBounds_0_0_1_1_zoom_1']({
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

        it('Должен сделать запрос за данными на 1 зуме при размере тайла 512', function (done) {
            map.setZoom(1);
            loadingObjectManager.options.set('loadTileSize', 512);
            loadingObjectManager.objects.events.add('add', function (e) {
                expect(e.get('objectId')).to.be(0);
                done();
            });
            map.geoObjects.add(loadingObjectManager);
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

        if (!window.mochaPhantomJS) {
            it('Должен сделать запрос за данными для 0 зума и не делать запросы для 1 зума', function (done) {
                loadingObjectManager.options.set('splitRequests', true);
                map.geoObjects.add(loadingObjectManager);
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

                function continueTest() {
                    var newZoom = map.getZoom() + 1;
                    map.setZoom(newZoom);
                    var newTileNumber = getTileNumber(map.getCenter(), newZoom),
                        padding = 'callback_tileBounds_' +
                            newTileNumber[0] + '_' +
                            newTileNumber[1] + '_' +
                            newTileNumber[0] + '_' +
                            newTileNumber[1] + '_' +
                            'zoom_1';
                    expect(window[padding]).to.be(undefined);
                    done();
                }

                window.setTimeout(continueTest, 100);

            });

            it('Должен сделать запрос за 4 тайлами на 1м зуме и не делать запрос для 0 зума', function (done) {
                loadingObjectManager.options.set('splitRequests', true);
                map.setZoom(1);
                map.geoObjects.add(loadingObjectManager);

                map.setGlobalPixelCenter([100, 100]);
                map.setGlobalPixelCenter([400, 100]);
                map.setGlobalPixelCenter([100, 400]);
                map.setGlobalPixelCenter([400, 400]);

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

                function continueTest() {
                    map.setZoom(0);
                    expect(window['callback_tileBounds_0_0_0_0_zoom_0']).to.be(undefined);
                    done();
                }

                window['callback_tileBounds_1_0_1_0_zoom_1']({
                    data: {}
                });
                window['callback_tileBounds_0_1_0_1_zoom_1']({
                    data: {}
                });
                window['callback_tileBounds_1_1_1_1_zoom_1']({
                    data: {}
                });

                window.setTimeout(continueTest, 100);
            });
        }

        it('Должен удалить старые данные и запросить новые при вызове метода reloadData', function (done) {
            loadingObjectManager.objects.events.once('add', function (e) {
                loadingObjectManager.reloadData();
                expect(loadingObjectManager.objects.getLength()).to.be(0);
                loadingObjectManager.objects.events.once('add', function (e) {
                    expect(loadingObjectManager.objects.getLength()).to.be(1);
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
            map.geoObjects.add(loadingObjectManager);
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
            loadingObjectManager.objects.events.add('add', function (e) {
                var bounds = loadingObjectManager.getBounds(),
                    expectedBounds = [[55.755768, 37.617671], [55.755768, 37.617671]];
                for (var i = 0; i < 2; i++) {
                    for (var j = 0; j < 2; j++) {
                        expect(Math.abs(bounds[i][j] - expectedBounds[i][j])).to.be.lessThan(0.001);
                    }
                }
                done();
            });
            expect(loadingObjectManager.getBounds()).to.be(null);
            map.geoObjects.add(loadingObjectManager);
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
            loadingObjectManager.objects.events.add('add', function (e) {
                var bounds = loadingObjectManager.getBounds(),
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
            expect(loadingObjectManager.getBounds()).to.be(null);
            map.geoObjects.add(loadingObjectManager);
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
