ymaps.modules.define(util.testfile(), [
    "Map",
    "GeoObject",
    "util.eventEye",

    "geoObject.addon.editor"
], function (provide, Map, GeoObject, eventEye) {
    describe("geoObject.addon.editor", function () {
        var geoMap;

        before(function () {
            geoMap = new ymaps.Map("map", {
                center: [-120, 55],
                zoom: 1,
                type: null,
                behaviors: ["scrollZoom", "drag"]
            });
        });

        beforeEach(function () {
        });

        afterEach(function () {
        });

        after(function () {
            geoMap.destroy();
        });

        it("При отсутствии геометрии у геообъекта, аддон редактора не должен создаваться", function (done) {
            this.timeout(10000);
            var geoObject = new GeoObject();
            geoMap.geoObjects.add(geoObject);

            expect().to.be.an("undefined");
            done();
        });

        it("Если геообъект имеет неизвестную геометрию, аддон редактора не должен создаваться", function (done) {
            this.timeout(10000);
            var geoObject = new GeoObject({
                geometry: {
                    type: "Rectangle",
                    coordinates: [
                        [0, 0]
                    ]
                }
            });

            geoMap.geoObjects.add(geoObject);

            expect().to.be.an("undefined");
            done();
        });

        it("Остановка редактирования при смене карты", function (done) {
            this.timeout(10000);
            var geoObject = new GeoObject({
                geometry: {
                    type: "LineString",
                    coordinates: [
                        [0, 0], [5, 5]
                    ]
                }
            });

            geoMap.geoObjects.add(geoObject);

            eventEye.observe(geoObject.editor, ["editingstart", "editingstop"]);

            geoObject.editor.events.add('editingstart', function () {
                geoMap.geoObjects.remove(geoObject);

                expect(eventEye.length()).to.be(2);
                expect(eventEye.valuesOf("type")).to.be.eql([
                    "editingstart", "editingstop"
                ]);
                expect(geoObject.editor.state.get("editing")).to.not.be.ok(2);

                done();
            });

            geoMap.geoObjects.add(geoObject);
            geoObject.editor.startEditing();
        });
    });

    provide();
});
