ymaps.modules.define(util.testfile(), [
    'yandex.taxi.helper'
], function (provide, taxiHelper) {
    describe('yandex.taxi.helper', function () {
        describe('Метод .getCheapestOption', function () {
            it('должен вернуть undefined если routeInfo не задан', function () {
                expect(taxiHelper.getCheapestOption()).to.eql(null); 
            });

            it('должен вернуть undefined если routeInfo.options отсутствует', function () {
                expect(taxiHelper.getCheapestOption({})).to.eql(null); 
            });

            it('должен вернуть undefined если routeInfo.options пуст', function () {
                expect(taxiHelper.getCheapestOption({options: []})).to.eql(null); 
            });

            it('должен найти наиболее дешевый маршрут', function () {
                expect(taxiHelper.getCheapestOption({options: [
                    {minPrice: 10},
                    {minPrice: 20},
                    {minPrice: 5}
                ]})).to.eql({minPrice: 5});
            });
        });
    });

    provide();
});
