var assert = require('chai').assert,
    url1 = 'http://m.auto.yandex.ru:80/mini/cooper/9383453/offers?body_type=HATCHBACK',
    url2 = 'http://beta.yandex.com.tr/',
    url3 = 'https://mail.yandex.ru/',
    url4 = 'http://m.yandex.ru',
    Url = require('../lib/url'),
    url = new Url({
        url : url1,
        routers : {
            desktop : require('./lib/routes/desktop'),
            mobile : require('./lib/routes/mobile')
        },
        commonParams : {
            rid : '2'
        },
        excludeParam: function(paramName) {  return (/^_/).test(paramName); }
    }),
    trUrl = new Url({
        url : url2,
        routers : {
            desktop : require('./lib/routes/desktop')
        }
    }),
    httpsUrl = new Url({
        url : url3,
        routers : {
            desktop : require('./lib/routes/desktop')
        }
    }),
    noMobileRoutesUrl = new Url({
        url : url4,
        routers : {
            desktop : require('./lib/routes/desktop')
        }
    }),
    catalogParams = {
        mark : 'audi',
        model : 'a4',
        cid : '123456'
    },
    advParams = { offer_id : '123456' };

module.exports = {
    'viewType is correct' : function() {
        assert.strictEqual(url.viewType(), 'mobile', 'viewtype is mobile');
        assert.strictEqual(trUrl.viewType(), 'desktop', 'viewtype is desktop');
    },
    'check unknown routes' : function() {
        assert.strictEqual(noMobileRoutesUrl.viewType(), 'mobile', 'viewtype is mobile');
        assert.strictEqual(
            noMobileRoutesUrl.link('catalog', catalogParams),
            '/audi/a4/123456',
            'viewtype is mobile, but only desktop router is specified and is used'
        );
    },
    'check susanin link' : function() {
        assert.strictEqual(
            url.link('catalog', catalogParams),
            '/catalog?rid=2&mark=audi&model=a4&cid=123456',
            'no such route on mobile, default is taken'
        );
        assert.strictEqual(
            trUrl.link('catalog', catalogParams),
            '/audi/a4/123456',
            'route is present and link is correct'
        );
    },
    'check susanin link with protocol and host' : function() {
        assert.strictEqual(
            url.link('advertisement', advParams, 'mobile', true),
            'http://m.auto.yandex.ru:80/advertisement/123456?rid=2',
            'mobile advertisement link is ok'
        );
        assert.strictEqual(
            url.link('advertisement', advParams, undefined, true),
            'http://m.auto.yandex.ru:80/advertisement/123456?rid=2',
            'mobile advertisement link w/o viewtype decl is ok'
        );
    },
    'check getDomain' : function() {
        assert.strictEqual(Url.getDomain(url1), 'm.auto.yandex.ru', 'static method whole domain');
        assert.strictEqual(trUrl.getDomain(), 'beta.yandex.com.tr', 'instance method whole domain');
        assert.strictEqual(httpsUrl.getDomain(), 'mail.yandex.ru', 'instance method whole domain');

        assert.strictEqual(Url.getDomain(url1, 1), 'ru', 'static method first level domain');
        assert.strictEqual(url.getDomain(1), 'ru', 'instance method first level domain');

        assert.strictEqual(Url.getDomain(url1, 2), 'yandex.ru', 'static method second level domain');
        assert.strictEqual(url.getDomain(2), 'yandex.ru', 'instance method second level domain');

        assert.strictEqual(Url.getDomain(url1, 3), 'auto.yandex.ru', 'static method third level domain');
        assert.strictEqual(url.getDomain(3), 'auto.yandex.ru', 'instance method third level domain');

        assert.strictEqual(Url.getDomain(url1, 4), 'm.auto.yandex.ru', 'static method fourth level domain');
        assert.strictEqual(url.getDomain(4), 'm.auto.yandex.ru', 'instance method fourth level domain');

        assert.strictEqual(Url.getDomain('auto.yandex.ru'), 'auto.yandex.ru', 'with no protocol');
        assert.strictEqual(Url.getDomain('https://auto.yandex.ru'), 'auto.yandex.ru', 'with https protocol');
        assert.strictEqual(Url.getDomain('ftp://auto.yandex.ru'), 'auto.yandex.ru', 'with ftp protocol');
    },
    'check tld' : function() {
        assert.strictEqual(url.tld(), 'ru', 'ru tld is ok');
        assert.strictEqual(trUrl.tld(), 'tr', 'tr tld is ok');
    },
    'check regionDomain' : function() {
        assert.strictEqual(url.regionDomain(), 'ru', 'ru regionDomain is ok');
        assert.strictEqual(trUrl.regionDomain(), 'com.tr', 'tr regionDomain is ok');
    },
    'check current' : function() {
        assert.strictEqual(url.current(), url1, 'ru url is ok');
        assert.strictEqual(trUrl.current(), url2, 'tr url is ok');
    },
    'check canonical' : function() {
        assert.strictEqual(url.canonical(), 'http://m.auto.yandex.ru:80/mini/cooper/9383453/offers', 'ru url is ok');
    },
    'check host' : function() {
        assert.strictEqual(url.host(), 'm.auto.yandex.ru:80', 'ru url is ok');
        assert.strictEqual(trUrl.host(), 'beta.yandex.com.tr', 'tr url is ok');
    },
    'check protocol' : function() {
        assert.strictEqual(url.protocol(), 'http:', 'http is ok');
        assert.strictEqual(httpsUrl.protocol(), 'https:', 'https is ok');
    },
    'check origin' : function() {
        assert.strictEqual(url.origin(), 'http://m.auto.yandex.ru:80', 'ru url is ok');
        assert.strictEqual(httpsUrl.origin(), 'https://mail.yandex.ru', 'tr url is ok');
    },
    'check query' : function() {
        assert.deepEqual(url.query(), { body_type : 'HATCHBACK' }, 'ru query is ok');
        assert.deepEqual(trUrl.query(), {}, 'tr query is ok');
    },
    'check isRouteMatch' : function() {
        assert.isTrue(url.isRouteMatch('offers'));
        assert.isFalse(url.isRouteMatch('advertisement'));
    },
    'check selfLink' : function() {
        assert.strictEqual(url.selfLink(), '/mini/cooper/9383453/offers?rid=2&body_type=HATCHBACK');
        assert.strictEqual(url.selfLink({
            publicParam: 1,
            _privateParam: 1
        }), '/mini/cooper/9383453/offers?rid=2&body_type=HATCHBACK&publicParam=1');
        assert.strictEqual(
            url.selfLink({
                publicParam: 1,
            }, 'desktop', true),
            'http://auto.yandex.ru:80/mini/cooper/9383453/offers?rid=2&body_type=HATCHBACK&publicParam=1'
        );
    },
    'check setCommonParams' : function() {
        url.setCommonParams({ rid : '213' });
        assert.strictEqual(url.link('catalog', catalogParams), '/catalog?rid=213&mark=audi&model=a4&cid=123456');
    },
    'check pathname' : function() {
        assert.strictEqual(url.pathname(), '/mini/cooper/9383453/offers');
        assert.strictEqual(trUrl.pathname(), '/');
    }
};
