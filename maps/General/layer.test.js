ymaps.modules.define(util.testfile(), [
    'Map',
    'vow',
    'Layer'
], function (provide, Map, vow, Layer) {
    // MAPSAPI-14054
    describe('Layer', function () {
        this.timeout(10000);

        var map;
        var readyTileNumber;
        var totalTileNumber;

        var jsonp = util.mocha.mock.jsonp();
        var coverageMock;

        beforeEach(function () {
            coverageMock = jsonp.mock.stub(/\/services\/coverage\/v2\/\?.*\bl=map\b/)
                .completeWith({"status":"success","data":[{"id":"map","zoomRange":[0,21],"copyrights":["© Яндекс"],"LayerMetaData":[{"scaled":true}]}]})
                .play();

            map = new ymaps.Map('map', {
                center: [55.751574, 37.573856],
                zoom: 2
            });
            readyTileNumber = 1;
            totalTileNumber = 2;
        });

        afterEach(function () {
            map.destroy();
            map = null;
        });

        it('Должен поймать момент, когда загрузился стартовый viewport', function (done) {
            var tileLoadChangeHandler = function (e) {
                if (e.get('readyTileNumber') === e.get('totalTileNumber')) {
                    tileChangeEvents.removeAll();
                    done();
                }
            };
            var tileChangeEvents = map.layers.events.group().add('tileloadchange', tileLoadChangeHandler);
        });

        it('Должен отловить все события по загрузке тайлов', function (done) {
            var tileLoadChangeHandler = function (e) {
                readyTileNumber = e.get('readyTileNumber');
                totalTileNumber = e.get('totalTileNumber');

                if (e.get('readyTileNumber') === e.get('totalTileNumber')) {
                    tileChangeEvents.removeAll();
                    expect(readyTileNumber).to.be(totalTileNumber);
                    done();
                }
            };
            var tileChangeEvents = map.layers.events.group().add('tileloadchange', tileLoadChangeHandler);
        });

        it('Должен пересчитать тайлы после смены зума', function (done) {
            var isFirstComplete = true;
            var check = function () {
                if (isFirstComplete && readyTileNumber === totalTileNumber) {
                    isFirstComplete = false;
                    map.setZoom(map.getZoom() + 1);
                } else if (!isFirstComplete && readyTileNumber === totalTileNumber) {
                    tileChangeEvents.removeAll();
                    expect(readyTileNumber).to.be(totalTileNumber);
                    done();
                }
            };

            var tileChangeEvents = map.layers.events.group().add('tileloadchange', function (e) {
                readyTileNumber = e.get('readyTileNumber');
                totalTileNumber = e.get('totalTileNumber');
                check();
            });
        });

        it('Должен пересчитать тайлы после смены координат', function (done) {
            var isFirstComplete = true;
            var readyTileNumberPrev = 0;
            var totalTileNumberPrev = 0;
            var check = function () {
                if (isFirstComplete && readyTileNumber === totalTileNumber) {
                    isFirstComplete = false;
                    map.setCenter([0, 0]);
                } else if (!isFirstComplete && readyTileNumber === totalTileNumber &&
                    (totalTileNumberPrev < totalTileNumber || readyTileNumberPrev < readyTileNumber)) {
                    tileChangeEvents.removeAll();
                    expect(readyTileNumber).to.be(totalTileNumber);
                    done();
                }
            };

            var tileChangeEvents = map.layers.events.group().add('tileloadchange', function (e) {
                readyTileNumber = e.get('readyTileNumber');
                totalTileNumber = e.get('totalTileNumber');
                check();
                readyTileNumberPrev = readyTileNumber;
                totalTileNumberPrev = totalTileNumber;
            });
        });

        it('Должен вызвать метод imagePreprocessor', function () {
            return coverageMock.pause().once()
                .then(function () {
                    var defer = ymaps.vow.defer();
                    var layer = new Layer(util.nullGif, {
                        imagePreprocessor: function (img, tileData) {
                            expect(img.src).to.be(util.nullGif);
                            expect(tileData.tileNumber).to.be.ok();
                            expect(tileData.tileZoom).to.be.ok();
                            defer.resolve();

                            return ymaps.vow.resolve(img);
                        }
                    });
                    map.layers.add(layer);
                    return defer.promise();
                })
        });
    });
    provide();
});
