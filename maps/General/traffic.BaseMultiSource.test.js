ymaps.modules.define(util.testfile(), [
    'Map',
    'traffic.BaseMultiSource',
    'option.Manager',
    'expect'
], function (provide, Map, BaseMultiSource, OptionManager, expect) {
    var map;
    var trjData;
    var trjTrjeData;

    describe('traffic.BaseMultiSource', function () {
        var jsonp = util.mocha.mock.jsonp();

        beforeEach(function () {
            map = new Map('map', { center: [37.621587,55.74954], zoom: 10, behaviors: [], controls: [], type: null});
        });

        afterEach(function () {
            map.destroy();
        });

        it('Должен сделать запрос за данными только после добавления слоя', function () {
            var layerTraffic = { id: 'trj', options: new OptionManager(), getMap: function() { return map; } };

            var multiSource = new BaseMultiSource('http://source/trj-test/%c', 'callback_%c');
            var trjStub = jsonp.mock.stub('http://source/trj-test/x=619&y=320&z=10?callback=callback_x_619_y_320_z_10')
                .completeWith(trjData);
            var hotspots = new ymaps.vow.Promise(function (resolve, reject) {
                multiSource.requestObjects(layerTraffic, [619, 320], 10, reject);

                multiSource.addLayer(layerTraffic.id, layerTraffic);
                multiSource.requestObjects(layerTraffic, [619, 320], 10, resolve);
            })

            return ymaps.vow.Promise.all([trjStub.once(), hotspots]);
        });

        it('Должен сделать запрос за двумя слоями, добавленными в multiSource', function () {
            var layerTraffic = { id: 'trj', options: new OptionManager(), getMap: function() { return map; } };
            var layerInfo = { id: 'trje', options: new OptionManager(), getMap: function() { return map; } };

            var multiSource = new BaseMultiSource('http://source/trj-test/%c', 'callback_%c');
            var trjTrjeStub = jsonp.mock.stub('http://source/trj-test/x=619&y=320&z=10?callback=callback_x_619_y_320_z_10_callback_x_619_y_320_z_10')
                .completeWith(trjTrjeData);

            multiSource.addLayer(layerTraffic.id, layerTraffic);
            multiSource.addLayer(layerInfo.id, layerInfo);

            var trjData = new ymaps.vow.Promise(function (resolve) {
                multiSource.requestObjects(layerTraffic, [619, 320], 10, resolve);
            });
            var trjeData = new ymaps.vow.Promise(function (resolve) {
                multiSource.requestObjects(layerInfo, [619, 320], 10, resolve);
            });

            return ymaps.vow.Promise.all([trjData, trjeData, trjTrjeStub.once()])
                .spread(function (trj, trje) {
                    expect(trj.length).to.be(1);
                    expect(trj[0].getProperties().hintContent).to.be('45 км/ч');

                    expect(trje.length).to.be(1);
                    expect(trje[0].getProperties().hintContent).to.be('Дорожные работы');
                });
        });
    });

    trjData = {"data":{
        "type":"FeatureCollection",
        "properties": {"HotspotSearchMetaData":{"HotspotSearchRequest":{"layer":"trj","tile":[619,320,10],"lang":"ru_RU"},"HotspotSearchResponse":{"layer":"trj","found":255}}},
        "features":[
            {"type":"Feature","properties":{"HotspotMetaData":{"id":30217,"RenderedGeometry":{"type":"MultiPolygon","coordinates":[[[[262,18],[258,20],[261,25],[265,22],[262,18]]]]}},"hintContent":"45 км/ч","description":45,"unit":"kilometers per hour"}}
        ]}
    };

    trjTrjeData = {"data":{
        "type":"FeatureCollection",
        "properties":{"HotspotSearchMetaData":{"HotspotSearchRequest":{"layer":["trj","trje"],"tile":[74,37,7],"lang":"ru_RU"},"HotspotSearchResponse":{"layer":["trj","trje"],"found":177}}},
        "features":[
            trjData.data,
            {"type":"FeatureCollection","properties":{"HotspotSearchMetaData":{"HotspotSearchRequest":{"layer":"trje","tile":[74,37,7],"lang":"ru_RU"},"HotspotSearchResponse":{"layer":"trje","found":1}}},"features":[{"type":"Feature","properties":{"HotspotMetaData":{"id":"u0fd9188e-d8ec-595c-a836-e6813560d350","RenderedGeometry":{"type":"Polygon","coordinates":[[[206,110],[226,110],[226,130],[206,130],[206,110]]]}},"hintContent":"Дорожные работы","eventType":0,"description":"Дорожные работы"},"geometry":{"type":"Point","coordinates":[59.7463600213655468,30.500860081636737675]}}]}
        ]
    }};

    provide({});
});
