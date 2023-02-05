ymaps.modules.define(util.testfile(), [
    'yandex.yandexMapUrlProvider'
], function (provide, mapUrlProvider) {
    describe('Провайдер ссылок для Яндекс.Карт (yandexMapUrlProvider)', function () {
        describe('Метод .getRateUrl', function () {
            it('возвращает правильную ссылку', function () {
                var oid = '1095876672';
                var result = mapUrlProvider.getRateUrl(oid);
                expect(result.indexOf('orgpage%5Bid%5D=' + oid)).to.not.be(-1);
                expect(result.indexOf('add-review')).to.not.be(-1);
                expect(result.indexOf('from=api-maps')).to.not.be(-1);
                expect(result.indexOf('utm_source=api-maps')).to.not.be(-1);
                expect(result.indexOf('utm_medium')).to.not.be(-1);
            });
        });
    });

    provide();
});
