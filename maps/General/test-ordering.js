define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Ordering',

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

        beforeOldContainer: function (x) {
            this.oldContainer = new ymaps.hotspot.container.InternalOld();
            for (var i = 0; i < x; i++) {
                this.oldContainer.add([this.hotspots[i]]);
            }
        },

        testOldContainer: function (x) {
            this.oldContainer.forEach(function () {});
        },

        beforeNewContainer: function (x) {
            this.newContainer = new ymaps.hotspot.container.Internal();
            for (var i = 0; i < x; i++) {
                this.newContainer.add([this.hotspots[i]]);
            }
        },

        testNewContainer: function (x) {
            this.newContainer.forEach(function () {});
        },

        beforeOldContainerBigParts: function (x) {
            this.oldContainer = new ymaps.hotspot.container.InternalOld();
            for (var i = 0; i < x / 10; i++) {
                this.oldContainer[i % 2 ? 'remove' : 'add'](this.hotspots.slice(i * x / 10, i * x / 10 + x / 10));
            }
        },

        testOldContainerBigParts: function (x) {
            this.oldContainer.forEach(function () {});
        },

        beforeNewContainerBigParts: function (x) {
            this.newContainer = new ymaps.hotspot.container.Internal();
            for (var i = 0; i < x / 10; i++) {
                this.newContainer[i % 2 ? 'remove' : 'add'](this.hotspots.slice(i * x / 10, i * x / 10 + x / 10));
            }
        },

        testNewContainerBigParts: function (x) {
            this.newContainer.forEach(function () {});
        }
    });
});
