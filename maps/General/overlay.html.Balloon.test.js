ymaps.modules.define(util.testfile(), [
    'geometry.pixel.Point',
    'Map',
    'data.Manager',
    'option.Manager',
    'templateLayoutFactory',
    'overlay.html.Balloon',
    'layout.storage',
    'layout.component.checkEmptiness',
    'vow'
], function (provide, PixelPoint, Map, DataManager, OptionManager, templateLayoutFactory,
    BalloonOverlay, layoutStorage, checkEmptinessComponent, vow) {

    describe('overlay.html.Balloon', function () {
        var map;
        beforeEach(function () {
            map = new Map('map', {
                center: [0, 0],
                controls: [],
                type: null,
                zoom: 3
            });
        });

        // teardown.
        afterEach(function () {
            map.destroy();
        });

        function createBalloonOverlay (point, properties, options) {
            var balloon = new BalloonOverlay(new PixelPoint(point), properties, options);
            balloon.options.setParent(map.options);
            return balloon;
        }

        // TODO тесты похожие на Hint
        it('Синхронный макет', function () {
            this.timeout(10000);
            var balloonOverlay = createBalloonOverlay([0, 0], {
                content: '123zzzZZZz'
            }, {
                panelMode: false,
                pane: 'balloon',
                layout: templateLayoutFactory.createClass('<ymaps id="balloon">{{ content }}</ymaps>')
            });
            balloonOverlay.setMap(map);
            expect(document.getElementById('balloon')).to.be.ok();
            expect(document.getElementById('balloon').innerHTML).to.equal('123zzzZZZz');
            balloonOverlay.setMap(null);
            expect(document.getElementById('balloon')).to.not.be.ok();
        });

        it('Синхронный макет (панель)', function () {
            this.timeout(10000);
            var balloonOverlay = createBalloonOverlay([0, 0], {
                content: '123zzzZZZz'
            }, {
                panelMode: true,
                pane: 'balloon',
                panelLayout: templateLayoutFactory.createClass('<ymaps id="balloonPanel">{{ content }}</ymaps>')
            });
            balloonOverlay.setMap(map);
            expect(document.getElementById('balloonPanel')).to.be.ok();
            expect(document.getElementById('balloonPanel').innerHTML).to.equal('123zzzZZZz');
            balloonOverlay.setMap(null);
            expect(document.getElementById('balloonPanel')).to.not.be.ok();
        });

        it('Асинхронный макет', function (done) {
            this.timeout(10000);
            layoutStorage.define('test.overlay.html.Balloon.1', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div id="template">123</div>'),
                    200));
            });
            var balloonOverlay = createBalloonOverlay([0, 0], {
                content: '123zzzZZZz'
            }, {
                panelMode: false,
                pane: 'balloon',
                layout: 'test.overlay.html.Balloon.1'
            });

            balloonOverlay.setMap(map);

            balloonOverlay.getLayout().done(function (layout) {
                expect(document.getElementById('template')).to.be.ok();
                done();
            }, function (error) {

                expect().fail('Был получен reject ' + error.message);
                done();
            }, this);
        });

        // TODO тест не проходит - ждем таску MAPSAPI-8152
        it('Асинхронный макет (панель)', function (done) {
            this.timeout(10000);
            layoutStorage.define('test.overlay.html.Balloon.2', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div>123</div>'),
                    200));
            });
            var balloonOverlay = createBalloonOverlay([0, 0], {
                content: '123zzzZZZz'
            }, {
                panelMode: true,
                pane: 'balloon',
                panelLayout: 'test.overlay.html.Balloon.2'
            });

            balloonOverlay.setMap(map);
            balloonOverlay.getLayout().done(function (layout) {
                expect(layout).to.be.ok();
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            }, this);
        });

        it('isEmpty макет загружен', function () {
            this.timeout(10000);
            var balloonOverlay = createBalloonOverlay([0, 0], {}, {
                panelMode: false,
                pane: 'balloon',
                layout: templateLayoutFactory.createClass('123')
            });
            balloonOverlay.setMap(map);
            expect(balloonOverlay.isEmpty()).to.be(false);
        });

        it('Событие emptinesschange', function (done) {
            this.timeout(10000);
            var dataManager = new DataManager({text1: '123'}),
                balloonOverlay = createBalloonOverlay([0, 0], {
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

        it('isEmpty макет не загружен', function () {
            this.timeout(10000);
            layoutStorage.define('test.overlay.html.Balloon.3', function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('123'),
                    200));
            });

            var balloonOverlay = createBalloonOverlay([0, 0], {}, {
                panelMode: false,
                pane: 'balloon',
                layout: 'test.overlay.html.Balloon.3'
            });
            balloonOverlay.setMap(map);

            expect(balloonOverlay.isEmpty()).to.be(true);
        });

        it('checkEmptiness (стандартный макет)', function (done) {
            this.timeout(10000);
            var data = {
                contentBody: 'zzz',
                options: new OptionManager({
                    panelMode: false
                }, map.options, 'balloon')
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness (свой макет, синхронный)', function (done) {
            this.timeout(10000);
            var layoutClass = templateLayoutFactory.createClass('{{ properties.content }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        panelMode: false,
                        layout: layoutClass
                    }, map.options, 'balloon')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
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
                        panelMode: false,
                        layout: layoutClass
                    }, map.options, 'balloon')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(true);
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
                    panelMode: false,
                    layout: templateLayoutFactory.createClass('{{ properties.content }}')
                })
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness (панель)', function (done) {
            this.timeout(10000);
            var layoutClass = templateLayoutFactory.createClass('{{ properties.content }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        panelMode: true,
                        contentLayout: layoutClass
                    }, map.options, 'balloon')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        it('checkEmptiness без данных (панель)', function (done) {
            this.timeout(10000);
            var layoutClass = templateLayoutFactory.createClass('{{ properties.content2 }}'),
                data = {
                    properties: new DataManager({content: '123'}),
                    options: new OptionManager({
                        panelMode: true,
                        contentLayout: layoutClass
                    }, map.options, 'balloon')
                };

            layoutClass.checkEmptiness = function (data) {
                return checkEmptinessComponent(
                    layoutClass,
                    data
                );
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
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
                    })
                },
                counter = 0;

            layoutClass.checkEmptiness = function (data) {
                counter++;
                return vow.resolve(false);
            };

            BalloonOverlay.checkEmptiness(data).done(function (result) {
                expect(counter).to.be(1);
                expect(result).to.be(false);
                done();
            }, function (error) {
                expect().fail('Был получен reject ' + error.message);
                done();
            });
        });

        // TODO нужно еще будет добавить тесты на опции, режим панели
    });
    provide();
});
