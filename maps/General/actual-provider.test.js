ymaps.modules.define(util.testfile(), [
    'Map',
    'MapEvent',
    'Event',
    'hotspot.Manager',
    'traffic.provider.Actual',
    'traffic.provider.actual.timestampProvider'
], function (provide, Map, MapEvent, Event, hotspotManager, ActualProvider, TimestampProvider) {
    describe('traffic.provider.Actual', function () {
        this.retries(0);

        var STAMPS_MATCHER = /\/services\/coverage\/v2\/layers_stamps\?.*\bl=trf,trfe/;
        var STAMPS_RESPONSE = function (v) { return {"status":"success","data":{"trf":{"version":v.trf,"zoomRange":[1,21]}, "trfe":{"version":v.trfe,"zoomRange":[1,21]}}}; };

        var COVERAGE_MATCHER = /\/services\/coverage\/v2\/\?.*\bl=trf($|&)/;
        var COVERAGE_RESPONSE = {"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"archive":true,"events":true}]}]};

        var INFO_MATCHER = /\/services\/traffic\/v1\/info\?.*\bformat=js\b/;
        var INFO_REPONSE = {"timestamp": "1639749390","regions":[{"regionId":"213","hint":"","length":"0","isotime":"2021-12-17T16:56:30+0300","localTime":"16:56","date":"2021-12-17","level":"8","style":"red"}]};

        var TRF_MATCHER = function TRF_MATCHER (tm, req) {
            return req.startsWith(util.env(ymaps).hosts.traffic + '/1.1/tiles') &&
                req.query.l === 'trf' &&
                req.query.tm === String(tm);
        };

        var ROAD_EVENTS_COMMON_MATCHER = function (l, exactTile, v, req) {
            return req.startsWith(util.env(ymaps).hosts.roadEventsRenderer + '/1.1/tiles') &&
                (exactTile === null || exactTile.join(',') === [req.query.x, req.query.y, req.query.z].join(',')) &&
                req.query.l === l &&
                req.query.v === String(v);
        };

        var TRFE_MATCHER = ROAD_EVENTS_COMMON_MATCHER.bind(null, 'trfe', null);
        var TRJE_MATCHER = ROAD_EVENTS_COMMON_MATCHER.bind(null, 'trje');

        var DESCRIPTION_MATCHER = function (id, req) {
            return req.startsWith(util.env(ymaps).hosts.api.services.traffic + '/v1/description') &&
                req.query.l === 'trje' &&
                req.query.id === id;
        };

        var map;
        var provider;

        var jsonp = util.mocha.mock.jsonp();
        var stampsStub;
        var coverageStub;
        var infoStub;

        var imageLoader = util.mocha.mock.imageLoader();
        var trfTileStub;

        var CENTER = [54, 36];

        beforeEach(function () {
            // Disable global request caches.
            CENTER[0] += 0.000001;

            map = new Map('map', { center: CENTER, zoom: 10, controls: [], type: null });
            provider = new ActualProvider();

            stampsStub = jsonp.mock.stub(STAMPS_MATCHER).completeWith(STAMPS_RESPONSE({trf: '1111', trfe: '1111.11.11.11.11.11'}));
            coverageStub = jsonp.mock.stub(COVERAGE_MATCHER).completeWith(COVERAGE_RESPONSE);
            infoStub = jsonp.mock.stub(INFO_MATCHER).completeWith(INFO_REPONSE);

            trfTileStub = imageLoader.mock.stub(TRF_MATCHER.bind(null, '1111'))
                .completeWith(util.tile.filled$({id: 'trf'}));
        });

        afterEach(function () {
            provider.setMap(null);
            map.destroy();
        });

        function initProvider() {
            provider.setMap(map);
            return ymaps.vow.Promise.all([
                stampsStub.once(),
                coverageStub.once(),
                infoStub.once(),
                util.waitDataManagerField(provider.state, 'isInited', Boolean)
            ]).then(function () {
                expect(jsonp.mock.allPending().length).to.be(0);
            });
        }

        it('Не должен делать запросы пока не добавлен на карту', function () {
            return ymaps.vow.delay(null, 100);
        });

        it('Должен делать запросы в /services/coverage и /services/traffic при добавлении на карту', function () {
            expect(provider.state.get('isInited')).to.not.be.ok();
            provider.setMap(map);
            expect(provider.state.get('isInited')).to.not.be.ok();

            return stampsStub.once()
                .then(function () { return coverageStub.once(); })
                .then(function () { return util.waitDataManagerField(provider.state, 'isInited', Boolean); })
                .then(function () { return infoStub.once(); })
                .then(function () { trfTileStub.process(); });
        });

        it('Должен отдавать слои (недокументированный метод)', function () {
            trfTileStub.play();

            return initProvider()
                .then(function () {
                    var layers = provider.getLayers();
                    expect(layers).to.be.ok();
                    expect(layers.png).to.be.a(ymaps.Layer);
                    expect(layers.info).to.be.a(ymaps.hotspot.Layer);
                    expect(layers.traffic).to.be.a(ymaps.hotspot.Layer);
                });
        });

        it('Должен реагировать на изменение timestamp\'а', function () {
            var trf2TileStub;

            return initProvider()
                .then(function () {
                    expect(trfTileStub.pending.length).to.be(16);
                    trfTileStub.process();

                    trfTileStub.pause();
                    trf2TileStub = imageLoader.mock.stub(TRF_MATCHER.bind(null, '2222'))
                        .completeWith(util.tile.filled$({id: 'trf2'}));

                    stampsStub.completeWith(STAMPS_RESPONSE({trf: '2222', trfe: '2222.22.22.22.22.22'}));
                    TimestampProvider.get(map).get(); // Triggers traffic.AutoUpdater.
                    return stampsStub.once();
                })
                .then(function () {
                    return infoStub.once({timeout: 50});
                })
                .then(function () {
                    expect(trf2TileStub.pending.length).to.be(16);
                    trf2TileStub.process();
                });
        });

        it('Не должен отправлять запросы за хотспотами', function () {
            trfTileStub.play();

            return initProvider();
        });

        it('Должен выставлять стейт в null после удаления с карты', function () {
            trfTileStub.play();

            return initProvider()
                .then(function () {
                    provider.setMap(null);
                })
                .then(function () {
                    provider.state.getAll();
                    expect(provider.state.getAll()).to.be.eql({
                        isInited: null,
                        timestamp: null,
                        timestampTrfe: null,
                        level: null,
                        iconStyle: null,
                        localtime: null,
                        isotime: null,
                        trafficDataLoading: null,
                        regionIds: null
                    });
                });
        });

        describe('Дорожные события', function () {
            var trfeTileStub;
            beforeEach(function () {
                trfTileStub.play();

                trfeTileStub = imageLoader.mock.stub(TRFE_MATCHER.bind(null, '1111.11.11.11.11.11'), 'TRFE')
                    .completeWith(util.tile.filled$({color: '#0000ff', id: 'trfe', opacity: 0.2, $x: 64, $y: 64}))
                    .play();
            });

            it('Должен загружать тайлы если они включены при добавлении на карту', function () {
                provider.state.set({infoLayerShown: true});

                return initProvider()
                    .then(util.poll$(function () { return trfTileStub.processed.length === 16 && trfeTileStub.processed.length === 16; }));
            });

            it('Должен загружать тайлы если они включены после добавления на карту', function () {
                return initProvider()
                    .then(util.poll$(function () { return trfTileStub.processed.length === 16; }))
                    .then(function () { provider.state.set({infoLayerShown: true}); })
                    .then(util.poll$(function () { return trfeTileStub.processed.length === 16; }));
            });

            it('Должен прекратить загружать тайлы после того как ДС выключены', function () {
                provider.state.set({infoLayerShown: true});

                return initProvider()
                    .then(util.poll$(function () { return trfTileStub.processed.length === 16 && trfeTileStub.processed.length === 16; }))
                    .then(function () {
                        provider.state.set({infoLayerShown: false});
                        trfeTileStub.pause();
                        map.setZoom(map.getZoom() + 1);
                        return coverageStub.once();
                    })
                    .then(util.poll$(function () { return trfTileStub.processed.length === 32; }));
            });

            it('Должны реагировать на изменение timestamp\'а', function () {
                provider.state.set({infoLayerShown: true});

                return initProvider()
                    .then(util.poll$(function () { return trfTileStub.processed.length === 16 && trfeTileStub.processed.length === 16; }))
                    .then(function () {
                        trfeTileStub.unstub();
                        trfeTileStub = imageLoader.mock.stub(TRFE_MATCHER.bind(null, '2222.22.22.22.22.22'), 'TRFE_2')
                            .completeWith(util.tile.filled$({color: '#0000ff', id: 'trfe2', opacity: 0.2, $x: 192, $y: 192}))
                            .play();

                        stampsStub.completeWith(STAMPS_RESPONSE({trf: '1111', trfe: '2222.22.22.22.22.22'}));
                        TimestampProvider.get(map).get(); // Triggers traffic.AutoUpdater.
                        return stampsStub.once();
                    })
                    .then(function () { return infoStub.once({timeout: 50}); })
                    .then(util.poll$(function () { return trfeTileStub.processed.length === 16; }));
            });

            describe('Хотспоты', function () {
                var trjeStub;
                var trje404Stub;
                var descriptionStub;
                beforeEach(function () {
                    provider.state.set({infoLayerShown: true});

                    descriptionStub = jsonp.mock.stub(DESCRIPTION_MATCHER.bind(null, TRJE_EVENT_ID))
                        .completeWith(DESCRIPTION_RESPONSE);
                    trje404Stub = jsonp.mock.stub(TRJE_MATCHER.bind(null, null, '1111.11.11.11.11.11'), 'TRJE_404')
                        .completeWith(TRJE_RESPONSE_404);
                    trjeStub = jsonp.mock.stub(TRJE_MATCHER.bind(null, TRJE_TILE_NUMBER, '1111.11.11.11.11.11'), 'TRJE')
                        .completeWith(TRJE_RESPONSE);

                    return initProvider()
                        .then(function () {
                            var trjeRequest = trjeStub.once();
                            triggerHotspotEvent('mousemove', map.getCenter());
                            return trjeRequest;
                        })
                        .then(function () {
                            expect(trje404Stub.processed.length).to.be(0);
                        });
                });

                function triggerHotspotEvent(eventType, coords, zoom) {
                    zoom = zoom !== undefined ? zoom : map.getZoom();
                    var globalPixelPosition = map.options.get('projection').toGlobalPixels(coords, zoom);
                    var event = new MapEvent({
                        map: map,
                        type: eventType,
                        target: map.panes.get('events'),
                        position: map.converter.globalToPage(globalPixelPosition),
                        globalPixels: globalPixelPosition,
                        domEvent: new Event({pageX: 0, pageY: 0})
                    });

                    hotspotManager.get(map.panes.get('events')).process(eventType, event);
                }

                it('Должен запрашивать хотспоты при движении мышкой', function () {
                    expect(true).to.be.ok(); // Everything is checked in beforeEach.
                });

                it('Должен запрашивать информацию о конкретном ДС и открыть балун по клику', function () {
                    var descriptionRequest = descriptionStub.once();
                    var balloonOpenEvent = util.waitEventOnce(map.events, 'balloonopen');

                    triggerHotspotEvent('click', map.getCenter());
                    return ymaps.vow.Promise.all([balloonOpenEvent, descriptionRequest])
                        .then(function () {
                            expect(map.balloon.getData().id).to.be(TRJE_EVENT_ID);
                        });
                });

                it('Не должен запрашивать информацию о ДС и открывать балун по клику в месте где нет данных', function () {
                    var trje404Request = trje404Stub.once();
                    triggerHotspotEvent('click', map.getBounds()[0]);
                    return trje404Request
                        // Ideally we should wait for map@click, but we're not simulating real dom click.
                        // Just wait to ensure that nothing else is happening.
                        .then(function () { return ymaps.vow.delay(null, 200); });
                });
            });
        });
    });

    var TRJE_EVENT_ID = 'u84616b6b-db81-5a64-8191-ab367734f608';
    var TRJE_TILE_NUMBER = [614, 329, 10];
    var TRJE_RESPONSE_404 = {"error":{"code":404,"message":"Not found"}};
    var TRJE_RESPONSE = { "data": {
        "type": "FeatureCollection",
        "properties": { "HotspotSearchMetaData": {
            "HotspotSearchRequest": {
                "layer": "trje",
                "tile": TRJE_TILE_NUMBER,
                "lang": "ru_RU"
            },
            "HotspotSearchResponse": {
                "layer": "trje",
                "found": 4
            }
        } },
        "features": [{
            "type": "Feature",
            "properties": {
                "eventType": 3,
                "description": "Перекрытие движения",
                "hintContent": "Перекрытие движения",
                "HotspotMetaData": {
                    "id": TRJE_EVENT_ID,
                    "RenderedGeometry": {
                        "type": "MultiPolygon",
                        "coordinates": [[[
                            [64, 64],
                            [192, 64],
                            [192, 192],
                            [64, 192],
                            [64, 64]
                        ]]]
                    }
                }
            },
            "geometry": {
                "type": "Point",
                "coordinates": [54, 36]
            }
        }]
    } };

    var DESCRIPTION_RESPONSE = {
        "id": TRJE_EVENT_ID,
        "type": 3,
        "description": "Перекрытие",
        "startTime": "01 января 2020 03:00",
        "source": "urn:uuid:c8d658eb-2c6e-4a50-8eaf-c2ac2ba3742d",
        "localizedSource": "Яндекс",
        "href": "https://maps.yandex.ru/traffic",
        "endTime": "31 декабря 2022 03:00"
    };

    provide({});
});
