ymaps.modules.define(util.testfile(), [
    'util.coordinates.scaleInvert'
], function (provide, scaleInvert) {

    describe('test.util.coordinates.scaleInvert', function () {

        var c1 = [10, 10];
        var c2 = [20, 20];
        var scale = 1.5;
        var f = scaleInvert.centerToFixed(c1, c2, scale);
        
        it('centerToFixed работает правильно', function () {
            expect(f[0]).to.be(60);
            expect(f[1]).to.be(60);
        });
        
        it('fixedToCenter работает правильно', function () {
            var c3 = scaleInvert.fixedToCenter(c1, [f[0] / scale, f[1] / scale], scale);
            expect(c3[0]).to.be(c2[0] * scale);
            expect(c3[1]).to.be(c2[1] * scale);
        });

    });

    provide();

});
