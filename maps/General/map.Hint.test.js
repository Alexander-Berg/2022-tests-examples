ymaps.modules.define(util.testfile(), [
    'Map',
    'map.Hint',
    'util.dom.element'
], function (provide, Map, Hint, utilDomElement) {
    describe('map.Hint', function () {
        var map;
        var hint;
        var content = 'HelloWorld';

        beforeEach(function () {
            map = new Map('map', {
                center: [0, 0],
                zoom: 5,
                type: null,
                behaviors: ['drag', 'scrollZoom']
            });
            hint = new Hint(map);
        });

        afterEach(function () {
            hint.destroy();
            map.destroy();
        });

        it('Хинт открывается', function (done) {
            var isEventsOk = false;
            var isElementOk = false;

            var check = function () {
                if (isEventsOk && isElementOk) {
                    done();
                }
            }
            hint.events.add(['open', 'close'], function (evt) {
                expect(evt.get('type')).to.eql('open');
                isEventsOk = true;
                check();
            });
            hint.open([0, 0], content, { a: 'b' })
                .then(function () {
                    var element = utilDomElement.findByPrefixedClass(map.container.getElement(), 'hint-overlay');
                    expect(element).to.be.ok();
                    expect(element.textContent || element.innerText).to.eql(content);
                    expect(hint.getOverlaySync().options.get('a')).to.eql('b');
                    isElementOk = true;
                    check();
                });
        });

        it('Хинт закрывается', function (done) {
            var eventsCount = 0;
            var isClosed = false;
            var check = function () {
                if (isClosed && eventsCount == 2) {
                    done();
                }
            }
            hint.events.add(['open', 'close'], function () {
                eventsCount += 1;
                check();
            });
            hint.open([0, 0], content)
                .then(function () {
                    hint.close()
                        .then(function () {
                            var element = utilDomElement.findByPrefixedClass(map.container.getElement(), 'hint-overlay');
                            expect(element).to.not.be.ok();
                            isClosed = true;
                            check();
                        });
                });
        });

        it('У Хинта можно узнать о его видимости', function (done) {
            hint.open([0, 0], content).then(function () {
                expect(hint.isOpen()).to.eql(true);
                hint.close().then(function () {
                    expect(hint.isOpen()).to.eql(false);
                    done();
                });
            });
        });
    });

    provide();
});
