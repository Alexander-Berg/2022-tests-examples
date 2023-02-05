ymaps.modules.define(util.testfile(), [
    'util.bind'
], function (provide, utilBind) {
    describe('util.bind', function () {
        it('Должен корректно передать контекст', function () {
            var context = {};
            var bindFunc = utilBind(function () {
                expect(this).to.be(context);
                expect(arguments.length).to.be(0);
            }, context);

            bindFunc();
        });

        it('Должен корректно передать аргументы', function () {
            var context = {};
            var bindFunc = utilBind(function () {
                expect(this).to.be(context);
                expect(arguments.length).to.be(3);
                expect(arguments[0]).to.be(42);
                expect(arguments[1]).to.be('str');
                expect(arguments[2]).to.be(context);
            }, context);

            bindFunc(42, 'str', context);
        });

        it('Должен корректно передать предзаданные аргументы', function () {
            var context = {};
            var bindFunc = utilBind(function () {
                expect(this).to.be(context);
                expect(arguments.length).to.be(3);
                expect(arguments[0]).to.be(42);
                expect(arguments[1]).to.be('str');
                expect(arguments[2]).to.be(context);
            }, context, 42, 'str');

            bindFunc(context);
        });
    });

    provide({});
});
