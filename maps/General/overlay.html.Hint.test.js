ymaps.modules.define(util.testfile(), [
    'geometry.pixel.Point',
    'Map',
    'templateLayoutFactory',
    'overlay.html.Hint',
    'data.Manager',
    'option.Manager',
    'layout.storage',
    'layout.component.checkEmptiness',
    'vow',
    'util.dom.element',
    'util.css'
], function (provide, PixelPoint, Map, templateLayoutFactory, HintOverlay, DataManager,
    OptionManager, layoutStorage, checkEmptinessComponent, vow, domElement, utilCss) {

    describe('overlay.html.Hint', function () {

        var map,
            hint,
            defaultHintLayout = templateLayoutFactory.createClass('<ymaps class="' + utilCss.addPrefix('hint') + '">Hint:&nbsp;{{ content }}</ymaps>');

        beforeEach(function () {
            map = new Map('map', {
                center: [39, 54],
                type: null,
                zoom: 1,
                controls: [],
                behaviors: ['drag', 'scrollZoom', 'multiTouch']
            });
        });

        afterEach(function () {
            map.destroy();
        });

        function findByClass (className) {
            return domElement.find(map.container.getElement(), '.' + className);
        }

        function createHintOverlay (point, properties, options) {
            hint = new HintOverlay(new PixelPoint(point), properties, options);
            hint.options.setParent(map.options);
            return hint;
        }

        it('Добавление на карту', function () {
            createHintOverlay([0, 0], {
                content: '123zzz'
            }, {
                layout: defaultHintLayout,
                pane: 'outerHint'
            });

            hint.setMap(map);

            expect(findByClass(utilCss.addPrefix('hint'))).to.be.ok();
        });

        it('Удаление с карты', function () {
            createHintOverlay([0, 0], {
                content: '123zzz'
            }, {
                layout: defaultHintLayout,
                pane: 'outerHint'
            });

            hint.setMap(map);
            hint.setMap(null);
            expect(findByClass(utilCss.addPrefix('hint'))).to.not.be.ok();
        });

        it('Проверка стандартного пейна', function () {
            var hint = createHintOverlay([0, 0], {
                content: '123zzz'
            }, {
                layout: defaultHintLayout,
                pane: 'outerHint'
            });
            hint.setMap(map);
            expect(findByClass(utilCss.addPrefix('outerHint-pane')) == findByClass(utilCss.addPrefix('hint-overlay')).parentNode).to.be.ok();
        });

        it('Проверка кастомного пейна', function () {
            var hint = createHintOverlay([0, 0], {
                content: '123zzz'
            }, {
                layout: defaultHintLayout,
                pane: 'outerHint'
            });
            hint.setMap(map);
            hint.options.set('pane', 'hint');
            expect(findByClass(utilCss.addPrefix('hint-pane')) == findByClass(utilCss.addPrefix('hint-overlay')).parentNode).to.be.ok();
        });

        it('Установка размера', function () {
            var minWidth,
                middleWidth,
                maxWidth,
                hint = createHintOverlay([0, 0], {
                    content: '123zzz'
                }, {
                    layout: defaultHintLayout,
                    pane: 'outerHint'
                });

            hint.setMap(map);

            middleWidth = hint.getElement().firstChild.firstChild.offsetWidth;
            hint.setData({});
            minWidth = hint.getElement().firstChild.firstChild.offsetWidth;
            hint.setData({ content: 'FFFFFFFFFFFFFUUUUUUUUUUUUUUUUUUU!!!!11111' });
            maxWidth = hint.getElement().firstChild.firstChild.offsetWidth;

            expect(maxWidth > middleWidth).to.be(true);
            expect(middleWidth > minWidth).to.be(true);
        });

        it('Асинхронный макет', function (done) {
            layoutStorage.define('test.overlay.html.Hint.1', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div id="template">123</div>'),
                    100));
            });
            var hintOverlay = createHintOverlay([0, 0], {
                content: '123zzzZZZz'
            }, {
                panelMode: false,
                pane: 'outerHint',
                layout: 'test.overlay.html.Hint.1'
            });

            hintOverlay.setMap(map);

            hintOverlay.getLayout().then(function (layout) {
                expect(document.getElementById('template')).to.be.ok();
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            }, this);
        });

        it('isEmpty макет загружен', function () {
            var hintOverlay = createHintOverlay([0, 0], {}, {
                panelMode: false,
                pane: 'balloon',
                layout: templateLayoutFactory.createClass('123')
            });
            hintOverlay.setMap(map);
            expect(hintOverlay.isEmpty()).to.be(false);
        });

        it('isEmpty макет не загружен', function () {
            layoutStorage.define('test.overlay.html.Hint.2', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('123'),
                    200));
            });

            var hintOverlay = createHintOverlay([0, 0], {}, {
                panelMode: false,
                pane: 'balloon',
                layout: 'test.overlay.html.Hint.2'
            });
            hintOverlay.setMap(map);

            expect(hintOverlay.isEmpty()).to.be(true);
        });

        it('Событие emptinesschange', function (done) {
            var dataManager = new DataManager({text1: '123'}),
                balloonOverlay = createHintOverlay([0, 0], {
                    properties: dataManager
                }, {
                    panelMode: false,
                    pane: 'balloon',
                    layout: templateLayoutFactory.createClass('{{ properties.text1 }}')
                });
            balloonOverlay.setMap(map);
            expect(balloonOverlay.isEmpty()).to.be(false);
            balloonOverlay.events.add('emptinesschange', function () {
                expect(balloonOverlay.isEmpty()).to.be(true);
                done();
            });
            dataManager.unset('text1');
        });

        it('checkEmptiness (стандартный макет)', function (done) {
            this.timeout(10000);

            var data = {
                content: '123',
                options: new OptionManager({
                    pane: 'outerHint'
                }, map.options, 'hint')
            };
            HintOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness (свой макет)', function (done) {
            this.timeout(10000);

            var layoutClass = templateLayoutFactory.createClass('{{ properties.content }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        layout: layoutClass,
                        pane: 'outerHint'
                    }, map.options, 'hint')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            HintOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness c макетом, который не определяет checkEmptiness', function (done) {
            this.timeout(10000);

            var data = {
                properties: new DataManager({content: '123'}),
                options: new OptionManager({
                    layout: templateLayoutFactory.createClass('{{ properties.content }}'),
                    pane: 'outerHint'
                }, map.options, 'hint')
            };

            var LayoutClass = data.options.get('layout');

            HintOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness с пустым макетом', function (done) {
            this.timeout(10000);

            var layoutClass = templateLayoutFactory.createClass('{{ properties.content2 }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        layout: layoutClass,
                        pane: 'outerHint'
                    }, map.options, 'hint')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            HintOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(true);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('Собственный checkEmptiness у макета', function (done) {
            this.timeout(10000);

            var layoutClass = templateLayoutFactory.createClass('{{ properties.content }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        panelMode: false,
                        layout: layoutClass
                    }, map.options, 'hint')
                },
                counter = 0;

            layoutClass.checkEmptiness = function (data) {
                counter++;
                return vow.resolve(false);
            };

            HintOverlay.checkEmptiness(data).done(function (result) {
                expect(counter).to.be(1);
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });
    });
    provide();
});
