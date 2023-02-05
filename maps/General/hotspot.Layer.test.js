ymaps.modules.define(util.testfile(), [
    'Map',
    'hotspot.Layer',
    'hotspot.ObjectSource',
    'hotspot.Manager',
    'MapEvent',
    'hotspot.layer.Object',
    'Event'
], function (provide, Map, HotspotLayer, ObjectSource, hotspotManager, MapEvent, HotspotLayerObject, Event) {
    describe('hotspot.Layer', function () {
        var map,
            hotspotLayer,
            objectSource;

        var jsonp = util.mocha.mock.jsonp();
        var hotspot001Stub;

        beforeEach(function () {
            map = new Map('map', { center: [37.621587,55.74954], zoom: 1, type: null});
            objectSource = new ObjectSource('http://source/test', 'callback_%x_%y_%z');
            hotspotLayer = new HotspotLayer(objectSource);

            hotspot001Stub = jsonp.mock.stub(/^http:\/\/source\/test\?.*\bcallback=callback_0_0_1\b/)
                .completeWith(hotspotData_0_0_1);
        });

        afterEach(function () {
            map.destroy();
            map = null;
        });

        function triggerHotspot001Event(eventType, globalPixelPosition) {
            globalPixelPosition = globalPixelPosition || [0, 0];
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

        function getPositionFor001(layer) {
            return (layer || hotspotLayer).options.get('projection').fromGlobalPixels([0, 0], map.getZoom());
        }

        it('Должен добавить слой на карту и не упасть', function () {
            map.layers.add(hotspotLayer);
        });

        it('Должен корректно обработать keyTemplate в виде функции', function () {
            var hotspotLayerWithKeyTemplateFn = new HotspotLayer(new ObjectSource('http://source/test', function (tile, zoom) {
                return 'callback_' + tile[0] + '_' + tile[1] + '_' + zoom;
            }));

            map.layers.add(hotspotLayerWithKeyTemplateFn);

            triggerHotspot001Event('mousedown');

            return hotspot001Stub.once();
        });

        it('Должен вернуть объект через метод getObjectInPosition при загруженных данных', function () {
            map.layers.add(hotspotLayer);

            triggerHotspot001Event('mousedown');

            return hotspot001Stub.once()
                .then(function () {
                    return hotspotLayer.getObjectInPosition(getPositionFor001());
                })
                .then(function (hotspotObject) {
                    expect(hotspotObject).not.to.be(null);
                    expect(hotspotObject).to.be.a(HotspotLayerObject);
                });
        });

        it('Должен вернуть объекты через метод getObjectsInPosition при загруженных данных', function () {
            map.layers.add(hotspotLayer);

            triggerHotspot001Event('mousedown');
            return hotspot001Stub.once()
                .then(function () {
                    return hotspotLayer.getObjectsInPosition(getPositionFor001());
                })
                .then(function (hotspotObjects) {
                    expect(hotspotObjects.length).to.be(2);
                    hotspotObjects.forEach(function (hotspotObject) {
                        expect(hotspotObject).to.be.a(HotspotLayerObject);
                    });
                })
        });

        it('Должен вернуть объекты через метод getObjectInPosition из временного хранилища', function () {
            map.layers.add(hotspotLayer);

            return ymaps.vow.Promise.all([hotspotLayer.getObjectInPosition(getPositionFor001()), hotspot001Stub.once()])
                .spread(function (hotspotObject) {
                    expect(hotspotObject).not.to.be(null);
                    expect(hotspotObject).to.be.a(HotspotLayerObject);
                });
        });

        it('Должен вернуть объекты через метод getObjectInPosition из временного хранилища при дробном зуме', function () {
            map.options.set('avoidFractionalZoom', false);
            map.setZoom(1.23);
            map.layers.add(hotspotLayer);


            return ymaps.vow.Promise.all([hotspotLayer.getObjectInPosition(getPositionFor001()), hotspot001Stub.once()])
                .spread(function (hotspotObject) {
                    expect(hotspotObject).not.to.be(null);
                    expect(hotspotObject).to.be.a(HotspotLayerObject);
                });
        });

        it('Должен вернуть объекты через метод getObjectsInPosition из временного хранилища', function () {
            map.layers.add(hotspotLayer);

            return ymaps.vow.Promise.all([hotspotLayer.getObjectsInPosition(getPositionFor001()), hotspot001Stub.once()])
                .spread(function (hotspotObjects) {
                    expect(hotspotObjects.length).to.be(2);
                    hotspotObjects.forEach(function (hotspotObject) {
                        expect(hotspotObject).to.be.a(HotspotLayerObject);
                    });
                });
        });

        it('Должен корректно отработать два вызова getObjectsInPosition подряд', function () {
            map.layers.add(hotspotLayer);

            return ymaps.vow.Promise.all([
                    hotspotLayer.getObjectsInPosition(getPositionFor001()),
                    hotspotLayer.getObjectsInPosition(getPositionFor001()),
                    hotspot001Stub.once()
                ]).spread(function (hotspotObjects1, hotspotObjects2) {
                    expect(hotspotObjects1.length).to.be(2);
                    expect(hotspotObjects2.length).to.be(2);
                });
        });

        it('Должен корректно отработать два вызова метода, обращающихся к одному временному тайлу', function () {
            map.layers.add(hotspotLayer);

            hotspotLayer.getObjectInPosition(getPositionFor001());

            return ymaps.vow.Promise.all([hotspotLayer.getObjectInPosition(getPositionFor001()), hotspot001Stub.once()])
                .spread(function (hotspotObject) {
                    expect(hotspotObject).not.to.be(null);
                });
        });

        it('Должен добавить слой на карту, сгенерировать событие, инициирующее загрузку данных, и поймать событие.', function () {
            map.layers.add(hotspotLayer);

            triggerHotspot001Event('mousedown');

            return ymaps.vow.Promise.all([util.waitEventOnce(hotspotLayer.events, 'mousedown'), hotspot001Stub.once()])
                .spread(function (e) {
                    expect(e.get('target')).to.be(hotspotLayer);
                    expect(e.get('activeObject')).to.be.a(HotspotLayerObject);
                    expect(e.get('type')).to.be('mousedown');
                });
        });

        it('Должен добавить слой на карту, загрузить данные и потом найти объект из этих данных', function () {
            map.layers.add(hotspotLayer);

            triggerHotspot001Event('mousedown');
            return hotspot001Stub.once()
                .then(function () {
                    var promise = util.waitEventOnce(hotspotLayer.events, 'mouseup');
                    triggerHotspot001Event('mouseup');
                    return promise;
                })
                .then(function (e) {
                    expect(e.get('type')).to.be('mouseup');
                });
        });

        it('Должен корректно отработать обращение сначала к временному, а потом к основному контейнеру объектов', function () {
            var counter = 0;
            var incHotspotLayer = new HotspotLayer(new ObjectSource('http://source/test-inc', function (tile, zoom) {
                return 'callback_' + tile[0] + '_' + tile[1] + '_' + zoom + '_' + counter++;
            }));

            map.layers.add(incHotspotLayer);

            var stub1 = jsonp.mock.stub(/^http:\/\/source\/test-inc\?.*\bcallback=callback_0_0_1_0\b/)
                .completeWith(hotspotData_0_0_1);

            var stub2 = jsonp.mock.stub(/^http:\/\/source\/test-inc\?.*\bcallback=callback_0_0_1_1\b/)
                .completeWith(hotspotData_0_0_1);

            incHotspotLayer.getObjectsInPosition(getPositionFor001(incHotspotLayer));
            triggerHotspot001Event('mousedown');

            return ymaps.vow.Promise.resolve()
                .then(function () { return stub1.once(); })
                .then(function () { return stub2.once(); })
                .then(function () {
                    var promise = util.waitEventOnce(incHotspotLayer.events, 'mouseup');
                    triggerHotspot001Event('mouseup');
                    return promise;
                })
                .then(function (e) {
                    expect(e.get('type')).to.be('mouseup');
                });
        });

        it('Должен корректно отработать смену шаблона', function () {
            map.layers.add(hotspotLayer);


            var mousedownPromise = util.waitEventOnce(hotspotLayer.events, 'mousedown');
            triggerHotspot001Event('mousedown');

            var stub = jsonp.mock.stub(/^http:\/\/source\/test\?.*\bcallback=callback2_0_0_1\b/)
                .completeWith(hotspotData2_0_0_1);

            objectSource.setKeyTemplate('callback2_%x_%y_%z');
            hotspotLayer.update();

            triggerHotspot001Event('mousedown');

            return ymaps.vow.Promise.resolve()
                .then(function () { return hotspot001Stub.once(); })
                .then(function () { return stub.once(); })
                .then(function () { return mousedownPromise; })
                .then(function (e) {
                    expect(e.get('activeObject').getId()).to.be(2);
                });
        });

        it('Не должен сгенерировать mouseenter-mouseleave при переходе с объекта на объект с одинаковым id', function () {
            hotspot001Stub.unstub();

            map.layers.add(hotspotLayer);

            var stub001 = jsonp.mock.stub(/^http:\/\/source\/test\?.*\bcallback=callback_0_0_1\b/)
                .completeWith(hotspotData2_0_0_1);

            triggerHotspot001Event('mousedown');

            var mouseleavePromise = util.waitEventOnce(hotspotLayer.events, 'mouseleave');

            return ymaps.vow.Promise.all([util.waitEventOnce(hotspotLayer.events, 'mouseenter'), stub001.once()])
                .spread(function (event) {
                    expect(event.get('activeObject').getId()).to.be.equal(2);
                })
                .then(function () {
                    var stub101 = jsonp.mock.stub(/^http:\/\/source\/test\?.*\bcallback=callback_1_0_1\b/)
                        .completeWith(hotspotData2_1_0_1);

                    triggerHotspot001Event('mousemove', [260, 0]);

                    return stub101.once();
                })
                .then(function () {
                    expect(mouseleavePromise.isResolved()).not.to.be.ok;
                })
                .then(function () {
                    triggerHotspot001Event('mousemove', [5000, 5000]);
                    return mouseleavePromise;
                })
                .then(function (event) {
                    expect(event.get('activeObject').getId()).to.be.equal(2);
                });
        });
    });

    var hotspotData_0_0_1 = {"data": {
        "type": "FeatureCollection",
        "features": [{
            "properties": {
                "categoryId": "building-residential",
                "HotspotMetaData": {
                    "id": 10469893,
                    "RenderedGeometry": {
                        "type": "Polygon",
                        "coordinates": [[[-315,24],[32,186],[141,-48],[-206,-210],[-315,24]],[[-186,-101],[-238,9],[-152,50],[-100,-60],[-186,-101]]]
                    }
                }
            }
        },
            {
                "properties": {
                    "categoryId": "building-residential",
                    "hintContent": "выше",
                    "HotspotMetaData": {
                        "id":10469894,
                        "RenderedGeometry": {
                            "type": "Polygon",
                            "coordinates": [[[65,-173],[-15,7],[234,117],[314,-63],[65,-173]]]
                        }
                    }
                }
            }]
    }};

    var hotspotData2_0_0_1 = {"data": {
        "type": "FeatureCollection",
        "features": [{
            "properties": {
                "categoryId": "building-residential",
                "HotspotMetaData": {
                    "id": 2,
                    "RenderedGeometry": {
                        "type": "Polygon",
                        "coordinates": [[[-2, -2],[-2, 2000],[2000, 2000],[2000, -2],[-2, -2]]]
                    }
                }
            }
        }]
    }};

    var hotspotData2_1_0_1 = {"data": {
        "type": "FeatureCollection",
        "features": [{
            "properties": {
                "categoryId": "building-residential",
                "HotspotMetaData": {
                    "id": 2,
                    "RenderedGeometry": {
                        "type": "Polygon",
                        "coordinates": [[[-315,24],[32,186],[141,-48],[-206,-210],[-315,24]],[[-186,-101],[-238,9],[-152,50],[-100,-60],[-186,-101]]]
                    }
                }
            }
        }]
    }};

    provide({});
});
