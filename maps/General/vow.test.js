ymaps.modules.define(util.testfile(), [
    'vow'
], function (provide, vow) {
    describe('vow', function () {
        it('exports Promise', function () {
            expect(vow.Promise).to.be.a('function');
        });
    });

    provide();
});
