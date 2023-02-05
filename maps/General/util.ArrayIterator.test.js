ymaps.modules.define(util.testfile(), [
    "util.ArrayIterator"
], function (provide, ArrayIterator) {
    describe("util.ArrayIterator", function () {
        it("Удаление элементов из массива во время итерирования", function (done) {
            var arr = [1, 2, 3, 4],
                iterator = new ArrayIterator(arr),
                cnt = 0;

            for (var i = 0, l = arr.length; i < l; i++) {
                arr.splice(i, 1);
                if (iterator.getNext() == iterator.STOP_ITERATION) {
                    break;
                }
                cnt++;
            }

            expect(arr).to.have.length(2);
            expect(cnt).to.be(2);

            done();
        });
    });

    provide();
});
