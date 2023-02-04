(function () {
    var debug = util.debug('mocks:yandex.counter');
    ymaps.__mock__ = ymaps.__mock__ || {};
    ymaps.__mock__['yandex.counter'] = {
        intercept: function (tag, url) {
            debug(url.replace(/^.*(?=\/pid=443\/)|\/rnd=.*$/g, ''));
            tag.src = util.nullGif;
        }
    };
})();
