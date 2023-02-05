ymaps.modules.define(util.testfile(), [
    'yandex.timetableProvider',
    'util.extend',
    'util.array'
], function (provide, timetableProvider, utilExtend, utilArray) {

    describe('timetableProvider', function () {

        describe('findNearestStation', function () {
            var example = [
                {distance: 10, code: 'code_1'},
                {distance: 4, code: 'code_2'},
                {distance: 156, code: 'code_3'}
            ];

            var makeCopy = function (src) { 
                return utilArray.map(src, function (el) {
                    return utilExtend({}, el);
                });
            };

            it('метод должен правильно находить одну остановку с кратчайшим расстоянием', function () {
                var exampleCopy = makeCopy(example); 
                var res = timetableProvider._findNearestStation(exampleCopy);

                expect(res).to.be(exampleCopy[1]);
            });

            it('метод не должен модифицировать входные данные', function () {
                var unchanged = makeCopy(example); 
                var exampleCopy = makeCopy(example); 
                timetableProvider._findNearestStation(exampleCopy);

                expect(exampleCopy).to.eql(unchanged);
            });

            it('метод должен возвращать null если ему передали пустой массив', function () {
                expect(timetableProvider._findNearestStation([])).to.eql(null);
            });
        });

    });

    provide();

});
