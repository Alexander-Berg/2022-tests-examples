ymaps.modules.define(util.testfile(), [
    "Map",
    "geoObject.component.BoundsAggregator",
    "GeoObjectCollection",
    "Placemark",
    "util.math.areEqual"
], function (provide, Map, BoundsAggregator, GeoObjectCollection, Placemark, areEqual) {
    describe("geoObject.component.BoundsAggregator", function () {
        var map,
            collection,
            coords = [
                [55.797533, 37.412847],
                [55.839285, 37.592748],
                [55.837739, 37.741063],
                [55.756511, 37.805608],
                [55.689854, 37.63944]
            ],
            boundsAggregator,
            pixelBoundsCallback = sinon.spy(),
            geoBoundsCallback = sinon.spy();

        function areEqualBounds (bounds1, bounds2) {
            return !bounds1 && !bounds2 || bounds1 && bounds2 &&
                areEqual(bounds1[0], bounds2[0], 1e-6) && areEqual(bounds1[1], bounds2[1], 1e-6);
        }

        function createBoundsAggregator (pixelBoundsCallback, geoBoundsCallback) {
            return boundsAggregator = new BoundsAggregator(collection, {
                onPixelBoundsChange: {
                    callback: pixelBoundsCallback,
                    context: this
                },
                onGeoBoundsChange: {
                    callback: geoBoundsCallback,
                    context: this
                }
            });
        }

        before(function () {
            map = new Map('map', {
                center: [55.74954, 37.621587],
                zoom: 10,
                type: null
            });
        });

        beforeEach(function () {
            collection = new GeoObjectCollection();

            for (var i = 0, l = coords.length; i < l; i++) {
                collection.add(new Placemark(coords[i]));
            }

            map.geoObjects.add(collection);
        });

        afterEach(function () {
            if (boundsAggregator) {
                boundsAggregator.destroy();
            }
            map.geoObjects.remove(collection);

            pixelBoundsCallback.reset();
            geoBoundsCallback.reset();
        });

        it("Состояние после конструктора", function (done) {
            this.timeout(10000);

            createBoundsAggregator(pixelBoundsCallback, geoBoundsCallback);

            expect(areEqualBounds(boundsAggregator.getBounds(), [
                [55.689854, 37.412847], [55.839285, 37.805608]
            ])).to.be.ok();
            expect(pixelBoundsCallback.called).to.be(false);
            expect(geoBoundsCallback.called).to.be(false);

            done();
        });

        it("Удаление элементов из коллекции", function (done) {
            this.timeout(10000);

            createBoundsAggregator(pixelBoundsCallback, function (newGeoBounds, oldGeoBounds) {
                expect(pixelBoundsCallback.callCount).to.be(2);
                expect(areEqualBounds(newGeoBounds, [
                    [55.689854, 37.63944], [55.837739, 37.805608]
                ])).to.be.ok();
                expect(areEqualBounds(oldGeoBounds, [
                    [55.689854, 37.412847], [55.839285, 37.805608]
                ])).to.be.ok();
                expect(areEqualBounds(boundsAggregator.getBounds(), newGeoBounds)).to.be.ok();

                done();
            });

            collection.remove(collection.get(0));
            collection.remove(collection.get(0));
        });

        it("Добавление элемента не меняющего общие границы", function (done) {
            this.timeout(10000);

            createBoundsAggregator(pixelBoundsCallback, geoBoundsCallback);

            collection.add(new Placemark([55.837739, 37.741063]));

            setTimeout(function () {
                expect(pixelBoundsCallback.callCount).to.be(1);
                expect(geoBoundsCallback.called).to.be(false);
                expect(areEqualBounds(boundsAggregator.getBounds(), [
                    [55.689854, 37.412847], [55.839285, 37.805608]
                ])).to.be.ok();

                done();
            }, 5);
        });
    });

    provide();
});
