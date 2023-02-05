ymaps.modules.define(util.testfile(), [
    'util.coordinates.reverse'
], function (provide, reverseCoordinates) {

    describe('util.coordinates.reverse', function () {
        it('Должен корректно перевернуть координаты точки', function () {
            var coordinates = reverseCoordinates([12, 21]);
            expect(coordinates[0]).to.be(21);
            expect(coordinates[1]).to.be(12);
        });

        it('Должен корректно перевернуть координаты массива точек', function () {
            var coordinates = reverseCoordinates([[12, 21],[13, 31]]);

            expect(coordinates[0][0]).to.be(21);
            expect(coordinates[0][1]).to.be(12);

            expect(coordinates[1][0]).to.be(31);
            expect(coordinates[1][1]).to.be(13);
        });
    });

    provide();
});
