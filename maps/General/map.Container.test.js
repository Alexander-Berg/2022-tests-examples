ymaps.modules.define(util.testfile(), [
    'map.Container',
    'Map',

    'util.math.areEqual',
    'util.dom.element',
    'util.dom.className',
    'util.css',

    'expect'
], function (provide, MapContainer, Map, areEqual, domElement, domClassName, utilCss) {
    describe('map.Container', function () {
        it('Должен при уничтожении убрать после себя данные от перехода в fullscreen', function () {
            var container = domElement.create({
                    tagName: 'div',
                    css: {display: 'none'}
                }),
                testMapElement = document.getElementById('map'),
                map;

            testMapElement.parentNode.insertBefore(container, testMapElement);
            map = new Map(container, {
                center: [54, 39],
                zoom: 10,
                type: null
            });

            map.container.enterFullscreen();
            map.destroy();

            expect(domClassName.has(document.body, utilCss.addPrefix('fullscreen'))).to.be(false);
            container.parentNode.removeChild(container);
        });

        describe('Тестирование опции autoFitToViewport', function () {
            var autoFitCaseMap,
                container;

            beforeEach(function () {
                container = domElement.create({
                    tagName: 'div',
                    css: {
                        display: 'none'
                    }
                });

                var testMapElement = document.getElementById('map');
                testMapElement.parentNode.insertBefore(container, testMapElement);

                autoFitCaseMap = new Map(container, {
                    center: [54, 39],
                    zoom: 10,
                    type: null
                }, {
                    autoFitToViewport: 'ifNull'
                });
            });

            afterEach(function () {
                autoFitCaseMap.destroy();
                container.parentNode.removeChild(container);
            });

            it('Должен корректно инициализироваться из скрытого контейнера при "ifNull"', function (done) {
                container.style.display = 'block';

                var listener = autoFitCaseMap.events.group()
                    .add('sizechange', function () {
                        expect(areEqual(autoFitCaseMap.container.getSize(), [0, 0])).not.to.be(true);
                        listener.removeAll();
                        done();
                    });
            });

            it('Должен всегда корректно подстраиваться под контейнер при "always"', function (done) {
                autoFitCaseMap.options.set('autoFitToViewport', 'always');
                container.style.display = 'block';

                var listener = autoFitCaseMap.events.group()
                    .add('sizechange', function () {
                        listener.removeAll();
                        listener.add('sizechange', function () {
                            expect(areEqual(autoFitCaseMap.container.getSize(), [222, 333])).to.be(true);
                            listener.removeAll();
                            done();
                        });

                        var containerStyle = container.style;
                        containerStyle.display = 'none';
                        containerStyle.width = "222px";
                        containerStyle.height = "333px";
                        containerStyle.display = "block";
                    });
            });

            it('Должен перестать автоматически перестраивать контейнер при смене опции на "none"', function (done) {
                autoFitCaseMap.options.set('autoFitToViewport', 'none');
                container.style.display = 'block';

                var listener = autoFitCaseMap.events.group()
                    .add('sizechange', function () {
                        listener.removeAll();
                        expect().fail("Сработал sizechange");
                        done();
                    });

                setTimeout(function () {
                    expect(areEqual(autoFitCaseMap.container.getSize(), [0, 0])).to.be(true);
                    listener.removeAll();
                    done();
                }, 300);
            });
        }); //autoFitToViewport
    });

    provide();
});
