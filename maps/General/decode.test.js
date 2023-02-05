ymaps.modules.define(util.testfile(), [
    'util.coordinates.decode',
    'util.coordinates.encode'
], function (provide, decode, encode) {

    describe('util.coordinates.decode', function () {
        var testEncoded = 'vVM-AmL6UQOKDAAAwP7__5kKAAAO____9w0AAOL-__8=';
        var decoded = decode(testEncoded);

        it('Правильно декодирует координаты', function () {
            var testCoord0 = 37.639101;
            var testCoord1 = 55.704162;
            expect(decoded.length).to.be(4);
            expect(decoded[0][0]).to.be(testCoord0);
            expect(decoded[0][1]).to.be(testCoord1);
        });

        it('Правильно кодирует координаты', function () {
            expect(encode(decoded)).to.be(testEncoded);
        });
    });

    provide();

});
