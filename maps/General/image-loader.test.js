ymaps.modules.define(util.testfile(), [
    'util.imageLoader',
    'vow',
    'util.imageLoaderComponent.config'
], function (provide, imageLoader, vow, config) {
    var loadTimeout = 500;

    describe('util.imageLoader', function () {
        afterEach(function () {
            imageLoader.clear();
        });

        it('должен загрузить картинку через dataUri', function () {
            return new ymaps.vow.Promise(function (resolve) {
                imageLoader.load(util.nullGif, function (img, state) { resolve({img: img, state: state}); });
            })
                .then(function (data) {
                    expect(data.state).to.be(true);
                    expect(data.img.width).to.be(1);
                })
        });

        it('не должен загрузить несуществующую картинку', function (done) {
            imageLoader.load('/surely-missing.png', function (img, state) {
                expect(state).to.be(false);
                done();
            });
        });

        it('Должен корректно отменить запрос на загрузку', function () {
            var cb = function (img, state) {
                throw new Error('request must be cancelled!');
            };
            imageLoader.load(util.nullGif, cb);
            imageLoader.cancel(util.nullGif, cb);
        });

        it('Должен корректно отменить запрос на загрузку для конкретного колбека', function (done) {
            var cb = function (img, state) {
                throw new Error('request must be cancelled!');
            };
            imageLoader.load(util.nullGif, cb);
            imageLoader.load(util.nullGif, function () {
                done();
            });

            imageLoader.cancel(util.nullGif, cb);
        });

        it.skip('Должен зарезолвить запрос по истечению срока жизни кеша', function (done) {
            var oldValue = config.requestTimeout;
            config.requestTimeout = 1;

            imageLoader.load('/src/util/contentSizeObserver/w100.png', function (img, state) {
                expect(state).to.be(false);
                config.requestTimeout = oldValue;
                done();
            });
        });

        it('Должен загружать картинку не CORS-enabled по-умолчанию', function (done) {
            imageLoader.load(util.apiUrlRoot + '/tests/mock/mapTiles/_/tiles?l=map&x=0&y=0&z=&scale=1&lang=ru_RU', function (img, state) {
                expect(state).ok();
                expect(img.crossOrigin == null).ok();

                // TODO: currently we are running phantom with --web-security=false and serving img from the same origin.
                // so it allows us to access pixels of any image.
                if (!window._phantom && false) {
                    expect(function () { getTopLeftPixel(img); }).to.throwException(/tainted|insecure/i);
                }

                done();
            });
        });

        // phantomjs have problems with everyting
        it.skip('Должен загружать картинку CORS-enabled по требованию', function (done) {
            imageLoader.load({
                url: util.apiUrlRoot + '/tests/mock/mapTiles/_/tiles?l=map&x=0&y=0&z=1&scale=1&lang=ru_RU&cors=1',
                crossOrigin: true
            }, function (img, state) {
                expect(state).ok();
                expect(img.crossOrigin).to.match(/anonymous/i);
                expect(function () { getTopLeftPixel(img) }).to.not.throwException();
                done();
            });
        });
    });

    function getTopLeftPixel (img) {
        var canvas = document.createElement('canvas');
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        return ctx.getImageData(0, 0, 1, 1).data;
    }

    provide();
});
