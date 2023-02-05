ymaps.modules.define(util.testfile(), [
    "suggest",
    "vow",
    "yandex.geocodeProvider.storage"
], function (provide, suggest, vow, storage) {
    describe('suggest', function () {
        it('Показ поисковых подсказок с кастомным провайдером', function (done) {
            suggest('test', {
                provider: { suggest: function (request, options) {
                    return vow.resolve([{
                        displayName: 'test1',
                        value: 'test2'
                    }, {
                        displayName: 'test3',
                        value: 'test4'
                    }]);
                }}
            }).then(function (result) {
                expect(result.length).to.be(2);
                expect(result[1].displayName).to.be('test3');
                expect(result[1].value).to.be('test4');
                done();
            });
        });

        it('Показ поисковых подсказок с кастомным провайдером по ключу', function (done) {
            storage.define('test', [], function (provide) {
                provide({
                    suggest: function (request, options) {
                        return vow.resolve([{
                            displayName: 'test1',
                            value: 'test2'
                        }, {
                            displayName: 'test3',
                            value: 'test4'
                        }]);
                    }
                });
            });

            suggest('test', {
                provider: 'test'
            }).then(function (result) {
                expect(result.length).to.be(2);
                expect(result[1].displayName).to.be('test3');
                expect(result[1].value).to.be('test4');
                done();
            });
        });

        it('Показ поисковых подсказок - должны примениться опции', function (done) {
            var bounds = [[0, 0], [10, 10]];

            storage.define('test2', [], function (provide) {
                provide({
                    suggest: function (request, options) {
                        expect(options.boundedBy).to.be(bounds);
                        expect(options.strictBounds).to.be(false);
                        expect(options.results).to.be(10);

                        return vow.resolve([{
                            displayName: 'test1',
                            value: 'test2'
                        }, {
                            displayName: 'test3',
                            value: 'test4'
                        }]);
                    }
                });
            });

            suggest('test', {
                provider: 'test2',
                boundedBy: bounds,
                strictBounds: false,
                results: 10
            }).then(function () {
                done();
            });
        });
    });

    provide({});
});
