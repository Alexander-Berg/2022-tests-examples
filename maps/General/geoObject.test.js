ymaps.modules.define(util.testfile(), [
    'Map',
    'Placemark',
    'Circle',
    'domEvent.manager',
    'DomEvent',
    'system.browser'
], function (provide, Map, Placemark, Circle, domEventManager, DomEvent, browser) {

    describe('GeoObject', function () {
        var map;
        beforeEach(function () {
            map = new Map('map', {
                center: [55.751574, 37.573856],
                zoom: 9,
                controls: [],
                type: null
            });
        });

        afterEach(function () {
            map.destroy();
        });

        // TODO перенести другие тесты геообъекта сюда

        describe('drag', function () {

            it('Drag placemark', function (done) {
                this.timeout(10000);

                var placemark = new Placemark(map.getCenter(), {}, {draggable: true});
                map.geoObjects.add(placemark);
                placemark.getOverlay().done(function (overlay) {
                    var bounds = overlay.getShape().getBounds(),
                        clientPixels = map.converter.globalToPage(bounds[0]);

                    simulateDrag(
                        map.panes.get("events").getElement(),
                        clientPixels[0] + 10,
                        clientPixels[1] + 10
                    );

                    var newBounds = overlay.getShape().getBounds();
                    expect(bounds[0][0]).to.not.be(newBounds[0][0]);
                    expect(bounds[0][1]).to.not.be(newBounds[0][1]);
                    done();
                });
            });

            it('Map drag through placemark', function (done) {
                this.timeout(10000);

                var placemark = new Placemark(map.getCenter());
                map.geoObjects.add(placemark);
                placemark.getOverlay().done(function (overlay) {
                    var bounds = overlay.getShape().getBounds(),
                        clientPixels = map.converter.globalToPage(bounds[0]);

                    simulateDrag(
                        map.panes.get("events").getElement(),
                            clientPixels[0] + 15,
                            clientPixels[1] + 15
                    );

                    var newBounds = overlay.getShape().getBounds();
                    expect(bounds[0][0]).to.be(newBounds[0][0]);
                    expect(bounds[0][1]).to.be(newBounds[0][1]);
                    done();
                });
            });

            it('Drag circle', function (done) {
                this.timeout(10000);

                var circle = new Circle([map.getCenter(), 10000], {}, {draggable: true});
                map.geoObjects.add(circle);
                circle.getOverlay().done(function (overlay) {
                    var bounds = overlay.getShape().getBounds(),
                        clientPixels = map.converter.globalToPage(bounds[0]);

                    simulateDrag(
                        map.panes.get("events").getElement(),
                        clientPixels[0] + 20,
                        clientPixels[1] + 20
                    );

                    var newBounds = overlay.getShape().getBounds();
                    expect(bounds[0][0]).to.not.be(newBounds[0][0]);
                    expect(bounds[0][1]).to.not.be(newBounds[0][1]);
                    done();
                });
            });

            it('Map drag through circle', function (done) {
                this.timeout(10000);

                var circle = new Circle([map.getCenter(), 10000]);
                map.geoObjects.add(circle);
                circle.getOverlay().done(function (overlay) {
                    var bounds = overlay.getShape().getBounds(),
                        clientPixels = map.converter.globalToPage(bounds[0]);

                    simulateDrag(
                        map.panes.get("events").getElement(),
                            clientPixels[0] + 20,
                            clientPixels[1] + 20
                    );

                    var newBounds = overlay.getShape().getBounds();
                    expect(bounds[0][0]).to.be(newBounds[0][0]);
                    expect(bounds[0][1]).to.be(newBounds[0][1]);
                    done();
                });
            });

            it('Drag events', function (done) {
                this.timeout(10000);

                var result = "",
                    placemark = new Placemark(map.getCenter(), {}, {draggable: true});
                map.geoObjects.add(placemark);

                placemark.events.add(['dragstart', 'drag', 'dragend'], function (event) {
                    result += event.get('type') + ' ';
                });

                placemark.getOverlay().done(function (overlay) {
                    var bounds = overlay.getShape().getBounds(),
                        clientPixels = map.converter.globalToPage(bounds[0]);

                    simulateDrag(
                        map.panes.get("events").getElement(),
                        clientPixels[0] + 10,
                        clientPixels[1] + 10
                    );
                    expect(result).to.be('dragstart drag drag dragend ');
                    done();
                });
            });

            function simulateDrag (domElement, x, y) {
                simulateMouseEvent(domElement, 'mouseover', x, y);
                simulateMouseEvent(domElement, 'mousedown', x, y);
                simulateMouseEvent(document.documentElement, 'mousemove', x + 50, y + 50);
                simulateMouseEvent(document.documentElement, 'mousemove', x + 60, y + 60);
                simulateMouseEvent(document.documentElement, 'mouseup', x + 60, y + 60);
            }

            function simulateMouseEvent (domElement, type, clientX, clientY) {
                domEventManager.fire(domElement, type,
                    new DomEvent({
                        type: type,
                        button: 0,
                        clientX: clientX,
                        clientY: clientY,
                        pageX: clientX,
                        pageY: clientY,
                        preventDefault: function () {}
                    })
                );
            }
        });
    });

    provide();
});
