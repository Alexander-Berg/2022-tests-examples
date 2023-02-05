define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Adding',

        axes: ['Hotspots'],

        beforeAll: function (x) {
            this.hotspots = [];
            for (var i = 0; i < x; i++) {
                this.hotspots.push(new ymaps.Hotspot(
                    new ymaps.shape.Polygon(
                        new ymaps.geometry.pixel.Polygon(this.generateConvexCoords(), 'evenOdd', { convex: true })
                    )
                ));
            }
        },

        generateConvexCoords: function () {
            var x = Math.round(Math.random() * 10000),
                y = Math.round(Math.random() * 10000);

            return [[
                [x, y], [x + 100, y], [x + 100, y + 100], [x, y + 100]
            ]];
        },

        beforeOldContainerAdd: function (x) {
            this.oldContainer = new ymaps.hotspot.container.InternalOld();
        },

        testOldContainerAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldContainer.add([this.hotspots[i]]);
            }
        },

        beforeNewContainerAdd: function (x) {
            this.newContainer = new ymaps.hotspot.container.Internal();
        },

        testNewContainerAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.newContainer.add([this.hotspots[i]]);
            }
        },

        beforeOldContainerAddBigParts: function (x) {
            this.oldContainer = new ymaps.hotspot.container.InternalOld();
        },

        testOldContainerAddBigParts: function (x) {
            for (var i = 0; i < x / 10; i++) {
                this.oldContainer.add(this.hotspots.slice(i * x / 10, i * x / 10 + x / 10));
            }
        },

        beforeNewContainerAddBigParts: function (x) {
            this.newContainer = new ymaps.hotspot.container.Internal();
        },

        testNewContainerAddBigParts: function (x) {
            for (var i = 0; i < x / 10; i++) {
                this.newContainer.add(this.hotspots.slice(i * x / 10, i * x / 10 + x / 10));
            }
        }
    });
});
