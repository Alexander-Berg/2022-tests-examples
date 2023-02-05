ymaps.modules.define(util.testfile(), [
    'util.Associate'
], function (provide, Associate) {
    describe('util.Associate', function () {
        var original;
        var rule;
        var context;
        var associate;
        var inst;
        describe('#rule', function () {
            beforeEach(function () {
                original = {};
                context = {};
                rule = function () {
                    return original;
                };
                associate = new Associate(rule);
                inst = associate.get(context);
            });

            it('Должено совпадать сохраненное по ассоциация значение', function () {
                expect(inst).to.be(original);
            });
            it('Не должно совпадать значение', function () {
                expect(inst).to.not.be((new Associate(function () { return {} })).get(context));
            });
        });

        describe('#context', function () {
            beforeEach(function () {
                original = {};
                context = {};
                rule = function () {
                    return {};
                };
                associate = new Associate(rule);
                inst = associate.get(context);
            });

            it('Должены совпадать значения, вызванные по одному и тому же контексту', function () {
                expect(inst).to.be(associate.get(context));
            });
            it('Не должны совпадать значения, вызванные по разным контекстам', function () {
                expect(inst).to.not.be(associate.get({}));
            });
        });

        describe('#arguments', function () {
            beforeEach(function () {
                original = {};
                context = {};
                rule = function (context, param) {
                    return { context: context, param: param };
                };
                associate = new Associate(rule);
                inst = associate.get(context, 42, 43);
            });

            it('Должены совпадать значения, вызванные по одному и тому же контексту', function () {
                expect(inst).to.be(associate.get(context));
            });

            it('Не должны совпадать значения, вызванные по разным контекстам', function () {
                expect(inst).to.not.be(associate.get({}));
            });

            it('Должны совпадать контексты, переданый в ассоциацию и вернувшийся при запросе', function () {
                expect(inst.context).to.be(context);
            });

            it('Должны совпадать параметры, переданные в ассоциацию и вернувшиеся при запросе', function () {
                expect(inst.param).to.be(42);
            });

            it('Должены совпадать значения, вызванные по одному и тому же контексту', function () {
                var context2 = {};
                expect(associate.get(context2)).to.be(associate.get(context2));
            });
        });
    });
    provide();
});
