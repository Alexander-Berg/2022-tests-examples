ymaps.modules.define(util.testfile(), [
    'util.safeAccess',
    'expect'
], function (provide, safeAccess) {
    describe('util.safeAccess', function () {
        var data = {
                title: "test",
                data: {
                    name: "Alpha"
                },
                arr: ["g", ["o"]]
            };

        it('Должен получить поле из первого уровня', function () {
            var result = safeAccess(data, 'title');
            expect(result).to.be('test');
        });

        it('Должен получить поле из объекта по пути, занному строкой', function () {
            var result = safeAccess(data, 'data.name');
            expect(result).to.be('Alpha');
        });

        it('Должен получить поле из объекта по пути, заданному массивом', function () {
            var result = safeAccess(data, ['data', 'name']);
            expect(result).to.be('Alpha');
        });
    });

    provide();
});
