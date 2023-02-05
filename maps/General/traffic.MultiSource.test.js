ymaps.modules.define(util.testfile(), [
    'Map',
    'traffic.MultiSource',
    'option.Manager',
    'expect'
], function (provide, Map, MultiSource, OptionManager, expect) {
    var map;

    describe('traffic.MultiSource', function () {
        var jsonp = util.mocha.mock.jsonp();

        beforeEach(function () {
            map = new Map('map', { center: [37.621587,55.74954], zoom: 10, behaviors: [], type: null});
        });

        afterEach(function () {
            map.destroy();
        });

        it('Должен получить данные объекта', function () {
            var multiSource = new MultiSource('/test-', 'test', 123);
            multiSource.options.set({
                objectUrlTemplate: {
                    'trj': 'test'
                },
                keyUrlTemplate: {
                    'trj': 'jamInfo-%j'
                }
            });

            var layerMock = {
                getMap: function() { return map; },
                options: new OptionManager(),
                id: 'trj'
            };
            multiSource.addLayer(layerMock.id, layerMock);

            var stub = jsonp.mock.stub(/test\?callback=jamInfo_3111/).completeWith({id: '3111', name: 'test'});

            var objectDataPromise = new ymaps.vow.Promise(function (resolve) {
                var data = { provider: 'traffic', id: '3111', layerId: 'trj' };
                var objectMock = {
                    getProperties: function() {return data;},
                    getId: function () {return data.id}
                };

                multiSource.requestObjectData(objectMock, resolve);
            });

            return stub.once()
                .then(function () { return objectDataPromise; })
                .then(function (data) {
                    expect(data.id).to.be('3111');
                    expect(data.name).to.be('test');
                });
        });
    });

    provide({});
});
