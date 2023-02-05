ymaps.modules.define(util.testfile(), [
    'option.Router',
    'option.Manager'
], function (provide, OptionRouter, OptionManager) {
    describe('option.Router', function () {
        var optionManager1;
        var optionManager2;
        var optionRouter;

        beforeEach(function () {
            optionManager1 = new OptionManager({
                a: 1
            });

            optionManager2 = new OptionManager({
                a: -1,
                b: -2
            });

            optionRouter = new OptionRouter(optionManager1, optionManager2);
        });

        describe('#base', function () {
            it('Должно вернуться правильные сохраненное значение', function () {
                expect(optionRouter.resolve('a')).to.be(1);
                expect(optionRouter.resolve('b')).to.be(-2);
            });
        });

        describe('#getParentsLength', function () {
            it('Должно вернуться правильное кол-во родителей в router-е', function () {
                expect(optionRouter.getParentsLength()).to.be(2);
            });


            it('Должно вернуться правильное кол-во родителей в router-е', function () {
                optionRouter.spliceParents(0, 1, new OptionManager({}));
                expect(optionRouter.getParentsLength()).to.be(2);
            });

            it('Должно вернуться правильное кол-во родителей в router-е', function () {
                optionRouter.spliceParents(0, 1);
                expect(optionRouter.getParentsLength()).to.be(1);
            });
        });

        describe('#spliceParents', function () {
            it('Должны сработать слушатели на удаление родителей', function () {
                var change = 0;
                optionRouter.events.add("change", function () {
                    change++;
                });
                optionRouter.spliceParents(0, 1, new OptionManager({}));
                expect(change).to.be(1);
            });

            it('Должны измениться поля опций, после удаления родителей', function () {
                optionRouter.spliceParents(0, 1, new OptionManager({
                    b: 2
                }));
                expect(optionRouter.resolve('a')).to.be(-1);
                expect(optionRouter.resolve('b')).to.be(2);
            });
        });

        describe('#indexOfParent', function () {
            it('Должен вернуться правильный индекс родительского optionManager-а', function () {
                expect(optionRouter.indexOfParent(optionManager2)).to.be(1);
            });
        });
    });
    provide();
});
