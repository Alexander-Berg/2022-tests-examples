ymaps.modules.define(util.testfile(), [
    'util.objectKeys'
], function (provide, objectKeys) {
    describe('util.objectKeys', function () {
        it('should return object keys', function () {
            expect(objectKeys({man: 5, devil: 6, god: 7}).sort()).to.eql(['devil', 'god', 'man']);
        });

        it('should throw error if given argument is not an object', function () {
            expect(objectKeys).withArgs(1).to.throwException('Object.keys called on non-object');
        });
    });

    provide();
});
