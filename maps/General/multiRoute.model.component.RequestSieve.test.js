ymaps.modules.define(util.testfile(), [
    "multiRoute.model.component.RequestSieve"
], function (provide, RequestSieve) {
    describe("multiRoute.model.component.RequestSieve", function () {
        var clock;

        before(function () {
            clock = sinon.useFakeTimers();
        });

        after(function () {
            clock.restore();
        });

        it("Без вызова process колбек не вызывается", function (done) {
            var callback = sinon.spy(),
                sieve = new RequestSieve(10, callback);

            clock.tick(10);
            expect(callback.called).to.be(false);

            done();
        });

        it("Первый вызов - быстрый, второй - через таймаут", function (done) {
            var callback = sinon.spy(),
                sieve = new RequestSieve(10, callback);

            sieve.process();
            clock.tick(1);
            expect(callback.calledOnce).to.be(true);

            sieve.process();
            clock.tick(5);
            expect(callback.calledOnce).to.be(true);
            clock.tick(5);
            expect(callback.calledTwice).to.be(true);

            done();
        });

        it("Множественные вызовы process сливаются в рамках одного таймаута", function (done) {
            var callback = sinon.spy(),
                sieve = new RequestSieve(10, callback);

            sieve.process();
            sieve.process();
            clock.tick(1);
            expect(callback.calledOnce).to.be(true);

            sieve.process();
            sieve.process();
            clock.tick(10);
            expect(callback.calledTwice).to.be(true);

            done();
        });

        it("Вызов stop отменяет накопленный process, но не отменяет последующие", function (done) {
            var callback = sinon.spy(),
                sieve = new RequestSieve(10, callback);

            sieve.process();
            sieve.stop();
            clock.tick(1);
            expect(callback.called).to.be(false);

            sieve.process();
            clock.tick(1);
            expect(callback.calledOnce).to.be(true);

            done();
        });
    });

    provide();
});
