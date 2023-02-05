ymaps.modules.define(util.testfile(), [
    'hotspot.ObjectSource',
    'Map',
    'option.Manager',
    'projection.wgs84Mercator'
], function (provide, ObjectSource, Map, OptionManager, wgs84Mercator) {
    describe('hotspot.ObjectSource.test', function () {
        var map;
        var jsonp = util.mocha.mock.jsonp();

        beforeEach(function () {
            map = new Map('map', { center: [37.621587,55.74954], zoom: 10, type: null });
        });

        afterEach(function () {
            map.destroy();
            map = null;
        });

        it('Должен корректно отработать тайл, возвращающий ошибку', function () {
            var objSource = new ObjectSource('http://source/test', 'test_callback');

            var layer = {
                    getMap: function() { return map; },
                    options: new OptionManager({ projection: wgs84Mercator })
            };

            var stub = jsonp.mock.stub(/^http:\/\/source\/test\?callback=test_callback$/)
                .completeWith({"error":{"code":404,"message":"Not found"}});

            var objectsPromise = new ymaps.vow.Promise(function (resolve) {
                objSource.requestObjects(layer, [618, 321], 10, resolve);
            });

            return ymaps.vow.Promise.all([objectsPromise, stub.once()])
                .spread(function (data) {
                    expect(data).to.be.an('array');
                    expect(data.length).to.equal(0);
                });
        });

        it('Должен корректно применить функцию-шаблон', function () {
            var source = new ObjectSource(function(tile, zoom) {
                return tile[0] + 'la' + tile[1] + 'la' + zoom;
            }, 'key');

            expect(source.getTileUrl([1, 2], 3)).to.be('1la2la3');
        });

        it('Должен корректно применить текстовый шаблон', function () {
            var source = new ObjectSource('domain1=%d|8__domain2=%d|4__domain3=%d|8__domain4=%d', 'key');
            expect(source.getTileUrl([1, 2], 3)).to.be('domain1=7__domain2=3__domain3=7__domain4=3');
        });

        it('Должен корректно отработать запрос за данными', function () {
            var objSource = new ObjectSource('http://source/test', 'jsonp__testCallback');
            var layer = {
                getMap: function() { return map; },
                options: new OptionManager()
            };

            map.setGlobalPixelCenter([79120 * 256 + 128, 40900 * 256 + 128], 17);

            var stub = jsonp.mock.stub(/^http:\/\/source\/test\?callback=jsonp__testCallback$/)
                .completeWith(hotspotData);

            var objectsPromise = new ymaps.vow.Promise(function (resolve) {
                objSource.requestObjects(layer, [79120, 40900], 17, resolve);
            });

            return ymaps.vow.Promise.all([objectsPromise, stub.once()])
                .spread(function (data) {
                    expect(data.length).to.be(1);
                });
        });
    });

    var hotspotData = {
        "data": {
            "type": "FeatureCollection",
            "features": [
                {
                    "properties": {
                        "HotspotMetaData": {
                            "id": 1950371,
                            "RenderedGeometry": {
                                "type": "Polygon",
                                "coordinates": [[[108,19],[76,109],[86,214],[195,229],[310,225],[341,127], [316,28],[212,11],[108,19]]]
                            }
                        }
                    }
                }
            ]
        }
    };

    provide({});
});
