ymaps.modules.define(util.testfile(), [
    'option.Manager',
    'option.presetStorage',
    'option.Mapper'
], function (provide, OptionManager, presetStorage, Mapper) {
    describe('option.Manager', function () {
        describe('#presets', function () {
            var grandParentOptionManager;
            var parentOptionManager;
            var optionManager;
            beforeEach(function () {
                grandParentOptionManager = new OptionManager({
                    a: 1,
                    b: 2,
                    d: 3
                });
                parentOptionManager = new OptionManager({
                    b: 1,
                    d: 2
                }, grandParentOptionManager);
                optionManager = new OptionManager({
                    d: 1
                }, parentOptionManager);
            });

            it('Создание ObjectManager без пресета', function () {
                expect(optionManager.get('a')).to.be(1);
                expect(optionManager.get('b')).to.be(1);
                expect(optionManager.get('d')).to.be(1);
            });

            it('Создание ObjectManager с пресетом', function () {
                presetStorage.add('my', {
                    a: -1
                });
                optionManager = new OptionManager({
                    preset: [{
                        b: -1
                    }, {
                        d: -1
                    }]
                });
                expect(optionManager.get('b')).to.be(-1);
                expect(optionManager.get('d')).to.be(-1);
                optionManager.set({ preset: 'my' });
                expect(optionManager.get('a')).to.be(-1);
            });
        });

        describe('#setters', function () {
            var grandParentOptionManager;
            var parentOptionManager;
            var parentOptionManager2;
            var optionManager;
            var res;
            beforeEach(function () {
                res = '';
                grandParentOptionManager = new OptionManager({
                    a: 1,
                    b: 4,
                    d: 3
                });
                parentOptionManager = new OptionManager({
                    b: 1,
                    d: 5
                });
                parentOptionManager2 = new OptionManager({
                    a: 0
                });
                optionManager = new OptionManager({
                    d: 2,
                    x: 'test'
                });
            });

            it('Должен правильно устанавливаться и возвращаться родитель', function () {
                parentOptionManager.setParent(grandParentOptionManager);
                optionManager.setParent(parentOptionManager);
                expect(optionManager.getParent()).to.be(parentOptionManager);
                expect(parentOptionManager.getParent()).to.be(grandParentOptionManager);
            });

            it('Должен правильно работать слушатель на change', function () {
                optionManager.events.add('change', function (e) {
                    res += e.get('target').get('d') + '/';
                });
                optionManager.setParent(parentOptionManager);
                optionManager.set('d', 10).setParent(parentOptionManager2);
                parentOptionManager.set('d', 5);
                expect(res).to.be('2/10/10/');
            });

            it('Должен правильно работать set, сохранять данные при вызове', function () {
                var userData = { a: 1 };
                optionManager = new OptionManager(userData);
                optionManager.set('a', 'a');
                expect(optionManager.get('a')).to.be('a');
                expect(userData.a).to.be(1);
            });

            it('Должны правильно браться параметры у родительских manager-ов', function () {
                optionManager.events.add('change', function (e) {
                    res += e.get('target').get('a') + '/';
                });
                parentOptionManager.setParent(grandParentOptionManager);
                optionManager.setParent(parentOptionManager);
                parentOptionManager.setParent(null);
                parentOptionManager.set('a', 11);
                optionManager.set('a', 2);
                expect(res).to.be('1/undefined/11/2/');
            });

            it('Должен правильно работать метод freeze, не изменять опции', function () {
                optionManager.events.add('change', function (e) {
                    res += e.get('target').get('a') + '/';
                });
                optionManager.freeze().set('a', 20);
                parentOptionManager.setParent(grandParentOptionManager);
                optionManager.setParent(parentOptionManager);
                parentOptionManager.set('a', 10);
                expect(res).to.be('');
                optionManager.unfreeze();
                expect(res).to.be('20/');
            });

            it('Должен правильно работать метод unsetAll, очищать все поля у вызываемого OptionManager-а', function () {
                optionManager.setParent(parentOptionManager);
                optionManager.unsetAll();
                expect(optionManager.get('d')).to.be(5);
                expect(optionManager.get('x')).to.not.be.ok();
            });

            it('Должен правильно работать unset, удалять данные при вызове', function () {
                var userData = { a: 1 };
                optionManager = new OptionManager(userData);
                optionManager.unset('a');
                expect(optionManager.get('a')).to.not.be.ok();
                expect(userData.a).to.be(1);
            });
        });

        describe('#Mapper', function () {
            it('Должен правильно работать mapper', function () {
                var mapper = new Mapper('plain');
                var parent = new OptionManager({
                    zoomControlPosition: 'a',
                    projection: 'b'
                }, null, null, mapper);
                var child1 = new OptionManager({
                }, parent, 'zoomControl');
                var child2 = new OptionManager({
                    position: 'c'
                }, parent, 'typeSelector');

                mapper.setRule({
                    key: 'position',
                    rule: 'prefixed'
                });

                expect(child1.get('position')).to.be('a');
                expect(child1.get('projection')).to.be('b');
                expect(child2.get('position')).to.be('c');
            });
        });

        describe('#getAll', function () {
            var optionManager;
            beforeEach(function () {
                optionManager = new OptionManager();
            });
            it('Должен возвращаться объект опций при вызове getAll', function () {
                var userData = {
                    a: 1,
                    b: 2
                };
                optionManager.set(userData);
                expect(optionManager.getAll()).to.eql(userData);
                expect(optionManager.getAll().a).to.be(1);
            });

            it('Должен возвращаться пустой объект при вызове getAll, если optionManager не содержит ничего', function () {
                expect(optionManager.getAll()).to.eql({});
            });

            it('Должен возвращаться правильный объект при вызове getAll на пустой OptionManager', function () {
                optionManager.unset('a');
                expect(optionManager.getAll().a).to.not.be.ok();
            });
        });
    });
    provide();
});
