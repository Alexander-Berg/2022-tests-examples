(function () {
    var debug = util.debug('mocks:util.jsonp');
    var RequestStubber = util.createRequestStubber({
        debug: debug,
        completeRequest: function (req, data) {
            var callback = (req.url.match(/[?&]callback=([^&]*)/) || [])[1] || '__UNKNOWN_CALLBACK__';
            window[callback](data);
            req.tag.onload();
        },
        failRequest: function (req, _data) {
            req.tag.onerror();
        }
    });

    function noIntercept (_tag, url) {
        debug.force('REQUEST WITHOUT MOCK', url);
        throw new Error(debug.str('REQUEST WITHOUT MOCK', url));
    }

    var instance = null;

    ymaps.__mock__ = ymaps.__mock__ || {};
    ymaps.__mock__['util.jsonp'] = {intercept: noIntercept};

    util.mocha.mock.jsonp.init = function (args) {
        if (instance) throw new Error('already stubbed');

        args = args || {};
        if (args.passthrough) {
            if (ENABLED_TESTFILES.length === 1) {
                ymaps.__mock__['util.jsonp'].intercept = function (tag, url) {
                    tag.src = url;
                    return tag;
                }

                // Intentionally does not change intercept.
                instance = {destroy: function() { instance = null; }};
                return instance;
            }

            debug.force('Cannot passthrough if there is more than one enabled testfiles on the page.')
        }

        instance = new RequestStubber(args);
        instance.on('destroy', function () {
            ymaps.__mock__['util.jsonp'].intercept = noIntercept;
            instance = null;
        });

        ymaps.__mock__['util.jsonp'].intercept = function (_tag, url) {
            var tag = document.createElement('intercepted-script');
            instance.handle(new RequestStubber.Request(tag, url));
            tag.setAttribute('data-src', url);
            return tag;
        };

        return instance;
    };
})();
