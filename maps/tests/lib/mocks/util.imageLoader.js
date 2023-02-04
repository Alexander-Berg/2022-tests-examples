(function () {
    var RequestStubber = util.createRequestStubber({
        debug: util.debug('mocks:util.imageLoader'),
        completeRequest: function (req, data) {
            req.tag.src = data;
        },
        failRequest: function (req, data) {
            req.tag.onerror(data);
        }
    });

    function noIntercept (tag, url) {
        tag.src = url;
        return tag;
    }

    var instance = null;

    ymaps.__mock__ = ymaps.__mock__ || {};
    ymaps.__mock__['util.imageLoader'] = {intercept: noIntercept};

    util.mocha.mock.imageLoader.init = function (args) {
        if (instance) throw new Error('already stubbed');

        instance = new RequestStubber(args);
        instance.on('destroy', function () {
            ymaps.__mock__['util.imageLoader'].intercept = noIntercept;
            instance = null;
        });

        ymaps.__mock__['util.imageLoader'].intercept = function (tag, url) {
            instance.handle(new RequestStubber.Request(tag, url));
            return tag;
        };

        return instance;
    };
})();
