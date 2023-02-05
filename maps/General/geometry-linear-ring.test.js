ymaps.modules.define(util.testfile(), [
    "util.defineClass",
    "geometry.base.LinearRing"
], function (provide, defineClass, LinearRing) {
    describe("LinearRing", function () {

        var e7 = 1e-7;

        it("Проверка закрытия контура", function (done) {
            var line = new LinearRing([[0, 0], [10, 0], [10, 10]]);
            expect(line.getLength()).to.be(4);

            var line = new LinearRing([[0, 0], [10, 0], [10, 10], [0, 0]]);
            expect(line.getLength()).to.be(4);
            done();
        });

        it("Проверка encode/decode", function (done) {
            var line = new LinearRing([[0, 0], [10, 0], [10, 10], [10 + e7, 10 + e7], [e7, e7]]);
            var encoded = line.constructor.toEncodedCoordinates(line),
                decoded = line.constructor.fromEncodedCoordinates(encoded),
                encoded2 = line.constructor.toEncodedCoordinates(decoded);

            expect(encoded).to.be(encoded2);
            done();
        });

        provide();
    });
});
