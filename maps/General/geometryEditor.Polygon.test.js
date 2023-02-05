ymaps.modules.define(util.testfile(), [
    "Map",
    "GeoObject",
    "util.eventEye",
    "system.browser",

    "geoObject.addon.editor"
], function (
    provide, Map, GeoObject, eventEye, systemBrowser
) {
    describe("geometryEditor.Polygon", function () {
        var geoMap, polygon;

        before(function () {
            geoMap = new Map("map", {
                center: [55, -120],
                zoom: 1,
                type: null,
                behaviors: ["scrollZoom", "drag"]
            });
        });

        beforeEach(function () {
            polygon = new GeoObject({
                geometry: {
                    type: "Polygon",
                    coordinates: [
                        [
                            [55, 70],
                            [60, 70],
                            [60, 75],
                            [55, 75]
                        ],
                        [
                            [57, 72],
                            [58, 72],
                            [58, 73],
                            [57, 73]
                        ]
                    ]
                }
            }, {
                graphicsStrokeColor: "#009922",
                graphicsFillColor: "#00ee2288",
                graphicsStrokeWidth: 5
            });
        });

        afterEach(function () {
            if (polygon.getParent()) {
                polygon.getParent().remove(polygon);
            }
        });

        after(function () {
            geoMap.destroy();
        });

        // Устанавливаем максимальное время исполнения для каждого кейса.
        this.timeout(10000);

        /*it("Ошибка при старте редактирования для недобавленной на карту геометрии", function (done) {
            try {
                polygon.editor.startEditing();
                expect().fail("Нет ошибки");
            } catch (e) {
                expect(e.message).to.be("geometryEditor.Base: геометрия не имеет ссылки на карту.");
            }
            done();
        });*/

        it("Работа опции maxPoints", function (done) {
            geoMap.geoObjects.add(polygon);
            polygon.editor.startEditing()
                .done(function () {
                    polygon.editor.getView().done(function (view) {
                        expect(view.getEdgePlacemarks().getLength()).to.be(2);
                        expect(view.getEdgePlacemarks().get(0).getLength()).to.be(4);
                        expect(view.getEdgePlacemarks().get(1).getLength()).to.be(4);

                        polygon.options.set("editorMaxPoints", 8);
                        expect(view.getEdgePlacemarks().getLength()).to.be(2);
                        expect(view.getEdgePlacemarks().get(0).getLength()).to.be(0);
                        expect(view.getEdgePlacemarks().get(1).getLength()).to.be(0);

                        polygon.options.unset("editorMaxPoints");
                        expect(view.getEdgePlacemarks().getLength()).to.be(2);
                        expect(view.getEdgePlacemarks().get(0).getLength()).to.be(4);
                        expect(view.getEdgePlacemarks().get(1).getLength()).to.be(4);

                        polygon.editor.stopEditing();
                        polygon.options.set("editorMaxPoints", 8);
                        polygon.editor.startDrawing();
                        expect(polygon.state.get("drawing")).to.not.be.ok();
                        done();
                    });
                });
        });

        it("Удаление рисуемого контура через геометрию должно останавливать его рисование MAPSAPI-3098", function (done) {
            polygon.editor.state.set("drawingPath", 1);
            geoMap.geoObjects.add(polygon);
            polygon.editor.startDrawing().done(function () {
                polygon.geometry.remove(1);
                expect(polygon.editor.state.get("drawingPath")).to.be(0);
                done();
            });
        });
    });

    provide();
});
