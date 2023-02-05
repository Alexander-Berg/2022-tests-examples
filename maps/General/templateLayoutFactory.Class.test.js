ymaps.modules.define(util.testfile(), [
    'templateLayoutFactory',
    'Placemark',
    'data.Manager',
    'option.Manager',
    'layout.storage',
    'layout.component.checkEmptiness'
], function (provide, templateLayoutFactory, Placemark, DataManager, OptionManager, layoutStorage,
    checkEmptinessComponent) {

    describe('templateLayoutFactory.Class', function () {
        var testNode,
            layout,
            LayoutClass,
            checkEmptinessFunc = function (data) {
                return checkEmptinessComponent(
                    LayoutClass,
                    data
                );
            };

        function createByTemplate (template, data, methods, staticMethods) {
            LayoutClass = templateLayoutFactory.createClass(template, methods, staticMethods);
            layout = new LayoutClass(data);
            layout.setParentElement(testNode);
        }

        beforeEach(function () {
            testNode = document.createElement('div');
            document.body.appendChild(testNode);
        });

        afterEach(function () {
            if (testNode) {
                testNode.innerHTML = '';
                document.body.removeChild(testNode);
            }
            if (layout) {
                layout.setParentElement(null);
                layout.destroy();
                layout = null;
            }

            LayoutClass = null;
        });

        it('Создание класса', function () {
            createByTemplate("test", {});
            expect(testNode.firstChild.innerHTML).to.be('test');
        });

        it('Подстановка данных', function () {
            createByTemplate("{{ name }}: {{ age }} years, {% if married %}married{% else %}single{% endif %}, {{ work.company }} {{ work.dep}}", {
                name: "Alex",
                age: 25,
                work: {
                    company: "Yandex",
                    dep: null
                },
                married: false
            });
            expect(testNode.firstChild.innerHTML).to.be("Alex: 25 years, single, Yandex ");
        });

        it('Подстановка данных геообъекта', function () {
            var placemark = new Placemark([0, 0], { data: "test" }, { option: "test" });

            createByTemplate(
                "{{ geometry.coordinates.1 }}, {{ properties.data }}, {{ options.option }}, {% if state.hover %}{% else %}false{% endif %}, " +
                "{{ geoObject.geometry.coordinates }}, {{ geoObject.properties.data }}, {{ geoObject.options.option }}, " +
                "{% if geoObject.state.hover %}{% else %}false{% endif %}", {
                    geometry: placemark.geometry,
                    properties: placemark.properties,
                    options: placemark.options,
                    state: placemark.state,
                    geoObject: placemark
                }
            );

            expect(testNode.firstChild.innerHTML).to.be("0, test, test, false, 0,0, test, test, false");
        });

        describe('Проверка пустоты', function () {

            describe('isEmpty', function () {

                it('Просто макет с данными (заполненный)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc'
                    };
                    createByTemplate("{{ text1 }}{{ text2 }}", data);

                    expect(layout.isEmpty()).to.not.be(true);
                });

                it('Просто макет с данными (пустой)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc'
                    };
                    createByTemplate("{{ text3 }}{{ text4 }}", data);

                    expect(layout.isEmpty()).to.be(true);
                });

                it('Макет с дочерним макетом (класс)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text1 }}')
                    };
                    createByTemplate("{% include sublayout1 %}", data);

                    expect(layout.isEmpty()).to.not.be(true);
                });

                it('Макет с пустым дочерним макетом (класс)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text3 }}')
                    };
                    createByTemplate("{% include sublayout1 %}", data);

                    expect(layout.isEmpty()).to.be(true);
                });

                it('Макет со множеством дочерних макетов (заполненный)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text1 }}{% include sublayout2 %}'),
                        sublayout2: templateLayoutFactory.createClass('{{ text3 }}{% include sublayout3 %}'),
                        sublayout3: templateLayoutFactory.createClass('{{ text2 }}'),
                        sublayout4: templateLayoutFactory.createClass('')
                    };
                    createByTemplate("{% include sublayout1 %}{% include sublayout4 %}", data);

                    expect(layout.isEmpty()).to.not.be(true);
                });

                it('Макет со множеством дочерних макетов (пустой)', function () {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text4 }}{% include sublayout2 %}'),
                        sublayout2: templateLayoutFactory.createClass('{{ text3 }}{% include sublayout3 %}'),
                        sublayout3: templateLayoutFactory.createClass('{{ text6 }}'),
                        sublayout4: templateLayoutFactory.createClass('')
                    };
                    createByTemplate("{% include sublayout1 %}{% include sublayout4 %}", data);

                    expect(layout.isEmpty()).to.be(true);
                });

                describe('Событие @emptinesschange', function () {

                    it('Событие в самом макете', function (done) {
                        var data = {
                            manager: new DataManager({})
                        };
                        createByTemplate("{{ manager.text }}", data);

                        expect(layout.isEmpty()).to.be(true);
                        layout.events.add('emptinesschange', function () {
                            expect(layout.isEmpty()).to.be(false);
                            done();
                        }, this);

                        data.manager.set('text', '123');
                    });

                    it('Событие в дочернем макете', function (done) {
                        var data = {
                            sublayout: templateLayoutFactory.createClass('{{ manager.text }}'),
                            manager: new DataManager({})
                        };
                        createByTemplate("{% include sublayout %}", data);

                        expect(layout.isEmpty()).to.be(true);
                        layout.events.add('emptinesschange', function () {
                            expect(layout.isEmpty()).to.be(false);
                            done();
                        }, this);

                        data.manager.set('text', '123');
                    });

                });

                // TODO в будущем будут асинхронные подмакеты. Тогда добавим тест и на это.
            });

            describe('checkEmptiness', function () {

                it('Просто макет с данными (заполненный)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc'
                    };
                    createByTemplate("{{ text1 }}{{ text2 }}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.not.be(true);
                        done();
                    });
                });

                it('Просто макет с данными (пустой)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc'
                    };
                    createByTemplate("{{ text3 }}{{ text4 }}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.be(true);
                        done();
                    });
                });

                it('Макет с дочерним макетом (класс)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text1 }}')
                    };
                    createByTemplate("{% include sublayout1 %}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.not.be(true);
                        done();
                    });
                });

                it('Макет с пустым дочерним макетом (класс)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text3 }}')
                    };
                    createByTemplate("{% include sublayout1 %}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.be(true);
                        done();
                    });
                });

                it('Макет с дочерним макетом (ключ)', function (done) {
                    layoutStorage.add('customLayout#asdasdads',
                        templateLayoutFactory.createClass('{{ text1 }}')
                    );

                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: 'customLayout#asdasdads'
                    };
                    createByTemplate("{% include sublayout1 %}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.not.be(true);
                        layoutStorage.remove('customLayout#asdasdads');
                        done();
                    });
                });

                it('Макет со множеством дочерних макетов (заполненный)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text1 }}{% include sublayout2 %}'),
                        sublayout2: templateLayoutFactory.createClass('{{ text3 }}{% include sublayout3 %}'),
                        sublayout3: templateLayoutFactory.createClass('{{ text2 }}'),
                        sublayout4: templateLayoutFactory.createClass('')
                    };
                    createByTemplate("{% include sublayout1 %}{% include sublayout4 %}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.not.be(true);
                        done();
                    });
                });

                it('Макет со множеством дочерних макетов (пустой)', function (done) {
                    var data = {
                        text1: 'zzZZZ',
                        text2: 'abc',
                        sublayout1: templateLayoutFactory.createClass('{{ text4 }}{% include sublayout2 %}'),
                        sublayout2: templateLayoutFactory.createClass('{{ text3 }}{% include sublayout3 %}'),
                        sublayout3: templateLayoutFactory.createClass('{{ text6 }}'),
                        sublayout4: templateLayoutFactory.createClass('')
                    };
                    createByTemplate("{% include sublayout1 %}{% include sublayout4 %}", data, {}, { checkEmptiness: checkEmptinessFunc });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.be(true);
                        done();
                    });
                });

                it('Параметр checkSelfEmptiness', function (done) {
                    var data = {
                        sublayout: templateLayoutFactory.createClass('{{ manager.some_text2 }}'),
                        manager: new DataManager({some_text1: 'zzz'})
                    };
                    createByTemplate("a{{ manager.some_text1 }}sdasd{% include sublayout %}adsasd", data, {}, {
                        checkEmptiness: function (data) {
                            return checkEmptinessComponent(
                                LayoutClass,
                                data,
                                {checkSelfEmptiness: false}
                            );
                        } });

                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.be(true);
                        data.manager.set('some_text2', '123');
                        LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                            expect(isEmpty).to.be(false);
                            done();
                        });
                    });
                });

                it('Дочерний макет не определяет checkEmptiness', function (done) {
                    var data = {
                        manager: new DataManager(),
                        sublayout: templateLayoutFactory.createClass('{{ manager.some_text1 }}')
                    };

                    createByTemplate("{% include sublayout %}", data, {}, { checkEmptiness: checkEmptinessFunc });
                    
                    LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                        expect(isEmpty).to.be(true);
                        data.manager.set('some_text1', '123');
                        LayoutClass.checkEmptiness(data).done(function (isEmpty) {
                            expect(isEmpty).to.be(false);
                            done();
                        });
                    });
                });
            });
        });

        // TODO добавить тест testContentLayout (симулирование клика пока неизвестно как сделать).
    });

    provide({});
});
