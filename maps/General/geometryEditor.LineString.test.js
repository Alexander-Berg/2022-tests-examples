ymaps.modules.define(util.testfile(), [
    "Map",
    "GeoObject",
    "util.eventEye",

    "geoObject.addon.editor"
], function (provide, Map, GeoObject, eventEye) {
    describe("geometryEditor.LineString", function () {
        var geoMap, polyline;

        before(function () {
            geoMap = new Map("map", {
                center: [55, -120],
                zoom: 1,
                type: null,
                behaviors: ["scrollZoom", "drag"]
            });
        });

        beforeEach(function () {
            polyline = new GeoObject({
                geometry: {
                    type: "LineString",
                    coordinates: [
                        [55, 55],
                        [60, 60],
                        [55, 65]
                    ]
                }
            }, {
                graphicsStrokeColor: '#1a3dc188',
                graphicsStrokeWidth: 20
            });
        });

        afterEach(function () {
            if (polyline.getParent()) {
                polyline.getParent().remove(polyline);
            }
        });

        after(function () {
            geoMap.destroy();
        });

        /*it("Ошибка при старте редактирования для недобавленной на карту геометрии", function (done) {
            try {
                polyline.editor.startEditing();
                expect().fail("Нет ошибки");
            } catch (e) {
                expect(e.message).to.be("geometryEditor.Base: геометрия не имеет ссылки на карту.");
            }
            done();
        });*/

        // Пока не работает из-за того, что нельзя передать координаты в клик.
//        it("Старт редактирования для геометрии без точек", function (done) {
//            polyline.geometry.setCoordinates([]);
//
//            geoMap.geoObjects.add(polyline);
//
//            polyline.editor.startDrawing();
//            polyline.editor.getView().then(function (view) {
//                var element = geoMap.panes.get('events').getElement();
//
//                Simulate.event(element, 'click', {
//                    clientX: 192,
//                    clientY: 192,
//                    relatedTarget: element
//                });
//
//                view.getVertexPlacemarks().get(0).getOverlay().then(function (overlay) {
//                    expect().to.be.ok(overlay.getMap());
//                    done();
//                }, function () {
//                    expect().fail("Нет оверлея");
//                    done();
//                }, this);
//            }, this);
//        });

        it("Работа опции maxPoints", function (done) {
            this.timeout(10000);
            geoMap.geoObjects.add(polyline);
            polyline.editor.startEditing()
                .done(function () {
                    polyline.editor.getView().done(function (view) {
                        expect(view.getEdgePlacemarks().getLength()).to.be(2);
                        polyline.options.set("editorMaxPoints", 3);
                        expect(view.getEdgePlacemarks().getLength()).to.be(0);
                        polyline.options.unset("editorMaxPoints");
                        expect(view.getEdgePlacemarks().getLength()).to.be(2);
                        polyline.editor.stopEditing();
                        polyline.options.set("editorMaxPoints", 3);
                        polyline.editor.startDrawing();
                        expect(polyline.state.get("drawing")).to.not.be.ok();
                        done();
                    });
                });
        });
    });

    provide();
});
