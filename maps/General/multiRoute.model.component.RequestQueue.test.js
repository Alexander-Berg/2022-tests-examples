ymaps.modules.define(util.testfile(), [
    "multiRoute.model.component.RequestQueue"
], function (provide, RequestQueue) {
    describe("multiRoute.model.component.RequestQueue", function () {
        it("Успешный запрос", function (done) {
            var context = {},
                requestContext = {},
                response = {},
                cancelCallback = sinon.spy(),
                serviceStub = sinon.stub();

            serviceStub.callsArgWithAsync(1, response);

            var query = new RequestQueue(
                    function (request, successCallback, errorCallback) {
                        expect(this).to.be(context);
                        serviceStub(request, successCallback, errorCallback);
                    },
                    cancelCallback,
                    context
                );

            query.push({}, function (data) {
                    expect(cancelCallback.called).to.be(false);
                    expect(this).to.be(requestContext);
                    expect(data).to.eql(response);
                    done();
                }, function () {
                    expect().fail('request fail');
                    done();
                },
                requestContext);
        });

        it("Неудачный запрос", function (done) {
            var context = {},
                requestContext = {},
                err = {},
                cancelCallback = sinon.spy(),
                serviceStub = sinon.stub();

            serviceStub.callsArgWithAsync(2, err);

            var query = new RequestQueue(
                    function (request, successCallback, errorCallback) {
                        expect(this).to.be(context);
                        serviceStub(request, successCallback, errorCallback);
                    },
                    cancelCallback,
                    context
                );

            query.push({}, function () {
                    expect().fail('must be bad request');
                    done();
                }, function (error) {
                    expect(cancelCallback.called).to.be(false);
                    expect(this).to.be(requestContext);
                    expect(error).to.eql(err);
                    done();
                },
                requestContext);
        });

        it("Отмена предыдущих запросов в случае прихода ответа на более поздний запрос", function (done) {
            var context = {},
                requestContext = {},
                response = {},
                cancelCallback = sinon.spy(),
                serviceStub = sinon.stub(),
                query = new RequestQueue(
                    function (request, successCallback, errorCallback) {
                        expect(this).to.be(context);
                        serviceStub(request, successCallback, errorCallback);
                    },
                    cancelCallback,
                    context
                );

            query.push({}, function () { expect().fail(); }, function () { expect().fail(); });
            query.push({}, function () { expect().fail(); }, function () { expect().fail(); });

            serviceStub.callsArgWithAsync(1, response);

            query.push({}, function (data) {
                    expect(cancelCallback.calledTwice).to.be(true);
                    expect(this).to.be(requestContext);
                    expect(data).to.be(response);
                    done();
                }, function () {
                    expect().fail('request fail');
                    done();
                },
                requestContext);
        });

        it("Отмена всех запросов при уничтожении очереди", function (done) {
            var context = {},
                cancelCallback = sinon.spy(),
                serviceStub = sinon.stub();

            serviceStub.callsArgWithAsync(1);

            var query = new RequestQueue(
                    function (request, successCallback, errorCallback) {
                        expect(this).to.eql(context);
                        serviceStub(request, successCallback, errorCallback);
                    },
                    cancelCallback,
                    context
                );

            query.push({}, function () { expect().fail(); }, function () { expect().fail(); });
            query.push({}, function () { expect().fail(); }, function () { expect().fail(); });

            query.clear();

            expect(cancelCallback.calledTwice).to.be(true);
            expect(cancelCallback.alwaysCalledOn(context)).to.be(true);
            done();
        });
    });

    provide();
});
