var assert = require('chai').assert,
    Url = require('../lib/url'),
    ServiceUrl = Url.create();

ServiceUrl.HOST_PREFIXES_BY_VIEW_TYPE = {
    mobile : 'm.',
    'touch-pad' : 'pad.',
    'touch-phone' : 'touch.'
};

module.exports = {
    'check getter viewType, host prefixes are set' : function() {
        assert.strictEqual(
            new ServiceUrl({ url : 'http://auto.yandex.ru' }).viewType(),
            'desktop');
        assert.strictEqual(
            new ServiceUrl({ url : 'http://m.auto.yandex.ru' }).viewType(),
            'mobile');
        assert.strictEqual(
            new ServiceUrl({ url : 'http://pad.auto.yandex.ru' }).viewType(),
            'touch-pad');
        assert.strictEqual(
            new ServiceUrl({ url : 'http://touch.auto.yandex.ru' }).viewType(),
            'touch-phone');
    },
    'check getter viewType, viewType is set by options' : function() {
        assert.strictEqual(
            new Url({ url : 'http://auto.yandex.ru', viewType : 'desktop' }).viewType(),
            'desktop');
        assert.strictEqual(
            new Url({ url : 'http://auto.yandex.ru', viewType : 'mobile' }).viewType(),
            'mobile');
        assert.strictEqual(
            new Url({ url : 'http://auto.yandex.ru', viewType : 'touch-pad' }).viewType(),
            'touch-pad');
        assert.strictEqual(
            new Url({ url : 'http://auto.yandex.ru', viewType : 'touch-phone' }).viewType(),
            'touch-phone');
        assert.strictEqual(
            new Url({ url : 'http://m.auto.yandex.ru', viewType : 'opa' }).viewType(),
            'mobile');
        assert.strictEqual(
            new ServiceUrl({ url : 'http://pad.auto.yandex.ru', viewType : 'opa' }).viewType(),
            'touch-pad');
    },
    'check setter viewType' : function() {
        var url = new Url({ url : 'http://auto.yandex.ru' });

        url.viewType('mobile');
        assert.strictEqual(url.viewType(), 'mobile');

        url.viewType('touch-phone');
        assert.strictEqual(url.viewType(), 'touch-phone');

        url.viewType('opa');
        assert.strictEqual(url.viewType(), 'touch-phone');
    }
};
