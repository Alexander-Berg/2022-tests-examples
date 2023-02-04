/* global describe, it */

var assert = require('chai').assert,
    mockUser = require('./mock/user'),
    moclL10n = require('./mock/l10n'),
    User = require('../lib/user'),
    nock = require('nock'),
    l10nParams = {
        'default' : 'tr',
        decl : {
            'ru' : {
                langs : [ 'ru', 'uk' ],
                currencies : [ 'RUR', 'USD', 'EUR', 'UAH', 'BYR', 'KZT' ]
            },
            'ua' : {
                langs : [ 'uk', 'ru' ],
                currencies : [ 'UAH', 'USD', 'RUR', 'EUR' ]
            },
            'tr' : {
                langs : [ 'tr' ],
                currencies : [ 'TRY', 'USD', 'EUR' ]
            }
        }
    },
    autol10nParams = {
        decl : {
            ru : {
                langs : [ 'ru', 'uk' ],
                    currencies : [ 'RUR', 'USD', 'EUR', 'UAH', 'BYR', 'KZT' ]
            },
            ua : {
                langs : [ 'uk', 'ru' ],
                    currencies : [ 'UAH', 'USD', 'RUR', 'EUR' ]
            }
        }
    },
    noCurrenciesParams = {
        'default' : 'tr',
        decl : {
            'ru' : {
                langs : [ 'ru', 'uk' ]
            },
            'ua' : {
                langs : [ 'uk', 'ru' ]
            },
            'tr' : {
                langs : [ 'tr' ]
            }
        }
    };

describe('L10N component', function() {

    function createReq(userMock, regionMock) {
        return {
            cookies : userMock.cookies,
            headers : regionMock.headers
        };
    }

    function createRes() {
        return {
            setHeader : function() {},
            getHeader : function() {}
        };
    }

    function l10nInit(user, l10n, l10nParams) {
        user = mockUser[user];
        l10n = moclL10n[l10n];

        var req = createReq(user, l10n),
            res = createRes();

        if (user.passportData) {
            nock('http://blackbox-mimino.yandex.net')
                .filteringPath(function() {
                    return '/blackbox';
                })
                .get('/blackbox')
                .reply(200, JSON.parse(JSON.stringify(user.passportData)));
        }

        return new User(req, res, {
                clientIp : l10n.clientIp,
                url : l10n.url,
                auth : {},
                l10n : l10nParams,
                region : {}
            })
            .init('auth')
            .then(function(user) {
                return user.init('region');
            })
            .then(function(user) {
                return user.init('l10n');
            });
    }

    it('interface', function(done) {
        l10nInit('yandexoid', 'russia.spb', l10nParams)
            .then(function(user) {
                var l10n = user.l10n;

                assert.isString(l10n.locale.id);
                assert.isArray(l10n.locale.list);
                assert.isString(l10n.locale.default);
                assert.isObject(l10n.locale.SOURCES);
                assert.isNumber(l10n.locale.source);
                assert.isFunction(l10n.locale.idBySource);
                assert.isFunction(l10n.locale.setId);

                assert.isString(l10n.lang.id);
                assert.isArray(l10n.lang.list);
                assert.isString(l10n.lang.default);
                assert.isObject(l10n.lang.SOURCES);
                assert.isNumber(l10n.lang.source);
                assert.isFunction(l10n.lang.idBySource);
                assert.isFunction(l10n.lang.setId);

                assert.isString(l10n.currency.id);
                assert.isString(l10n.currency.default);
                assert.isArray(l10n.currency.list);
                assert.isObject(l10n.currency.SOURCES);
                assert.isNumber(l10n.currency.source);
                assert.isFunction(l10n.currency.idBySource);
                assert.isFunction(l10n.currency.setId);

                done();
            })
            .done();
    });

    it('russian user', function(done) {
        l10nInit('yandexoid', 'russia.spb', l10nParams)
            .then(function(user) {
                var l10n = user.l10n,
                    locale = 'ru',
                    params = l10nParams.decl[locale];

                assert.strictEqual(l10n.locale.id, locale);
                assert.strictEqual(l10n.locale.default, l10nParams.default);
                assert.deepEqual(l10n.locale.list, Object.keys(l10nParams.decl));
                assert.strictEqual(l10n.locale.source, l10n.locale.SOURCES.DETECTED);

                assert.strictEqual(l10n.lang.id, params.langs[0]);
                assert.deepEqual(l10n.lang.list, [ { id : 'ru', name : 'Ru' } ]);
                assert.strictEqual(l10n.lang.default, params.langs[0]);
                assert.strictEqual(l10n.lang.source, l10n.lang.SOURCES.DETECTED);

                assert.strictEqual(l10n.currency.id, params.currencies[0]);
                assert.strictEqual(l10n.currency.default, params.currencies[0]);
                assert.deepEqual(l10n.currency.list, params.currencies);
                assert.strictEqual(l10n.currency.source, l10n.currency.SOURCES.DETECTED);

                done();
            })
            .done();
    });

    it('russian user, tries to override l10n params with invalid values', function(done) {
        l10nInit('yandexoid', 'russia.spb.overrides', l10nParams)
            .then(function(user) {
                var l10n = user.l10n,
                    locale = 'ru',
                    params = l10nParams.decl[locale];

                assert.strictEqual(l10n.locale.id, locale);
                assert.strictEqual(l10n.locale.default, l10nParams.default);
                assert.deepEqual(l10n.locale.list, Object.keys(l10nParams.decl));
                assert.strictEqual(l10n.locale.source, l10n.locale.SOURCES.DETECTED);

                assert.strictEqual(l10n.lang.id, params.langs[0]);
                assert.deepEqual(l10n.lang.list, [ { id : 'ru', name : 'Ru' } ]);
                assert.strictEqual(l10n.lang.default, params.langs[0]);
                assert.strictEqual(l10n.lang.source, l10n.lang.SOURCES.DETECTED);

                assert.strictEqual(l10n.currency.id, params.currencies[0]);
                assert.strictEqual(l10n.currency.default, params.currencies[0]);
                assert.deepEqual(l10n.currency.list, params.currencies);
                assert.strictEqual(l10n.currency.source, l10n.currency.SOURCES.DETECTED);

                done();
            })
            .done();
    });

    it('russian user, turkish headers and domain', function(done) {
        l10nInit('yandexoid', 'turkey.stambul', l10nParams)
            .then(function(user) {
                var l10n = user.l10n,
                    params = l10nParams.decl.tr;

                assert.strictEqual(l10n.locale.id, l10nParams.default);
                assert.strictEqual(l10n.locale.default, l10nParams.default);
                assert.deepEqual(l10n.locale.list, Object.keys(l10nParams.decl));
                assert.strictEqual(l10n.locale.source, l10n.locale.SOURCES.DETECTED);

                assert.strictEqual(user.region.id, 2);

                assert.strictEqual(l10n.lang.id, params.langs[0]);
                assert.deepEqual(l10n.lang.list, [ { id : 'tr', name : 'Tr' } ]);
                assert.strictEqual(l10n.lang.default, params.langs[0]);
                assert.strictEqual(l10n.lang.source, l10n.lang.SOURCES.DETECTED);

                done();
            })
            .done();
    });

    it('russian user, turkish headers and domain, l10n params overriden in url', function(done) {
        l10nInit('yandexoid', 'turkey.stambul.overrides', l10nParams)
            .then(function(user) {
                var l10n = user.l10n,
                    params = l10nParams.decl.ru;

                assert.strictEqual(l10n.locale.id, 'ru');
                assert.strictEqual(l10n.locale.default, 'tr');
                assert.deepEqual(l10n.locale.list, Object.keys(l10nParams.decl));
                assert.strictEqual(l10n.locale.source, l10n.locale.SOURCES.URL);

                assert.strictEqual(l10n.lang.id, 'ru');
                assert.deepEqual(l10n.lang.list, [ { id : 'ru', name : 'Ru' } ]);
                assert.strictEqual(l10n.lang.default, 'ru');
                assert.strictEqual(l10n.lang.source, l10n.locale.SOURCES.URL);

                assert.strictEqual(l10n.currency.id, 'USD');
                assert.strictEqual(l10n.currency.default, params.currencies[0]);
                assert.deepEqual(l10n.currency.list, params.currencies);
                assert.strictEqual(l10n.currency.source, l10n.currency.SOURCES.URL);

                done();
            })
            .done();
    });

    it('russian user, turkish domain, l10n params in url, extra is set', function(done) {
        l10nInit('yandexoid', 'turkey.stambul.overrides', l10nParams)
            .then(function(user) {
                var l10n = user.l10n,
                    locale = l10n.locale,
                    lang = l10n.lang,
                    currency = l10n.currency;

                assert.strictEqual(locale.id, 'ru');
                assert.strictEqual(locale.source, locale.SOURCES.URL);

                assert.strictEqual(lang.id, 'ru');
                assert.strictEqual(lang.source, locale.SOURCES.URL);

                assert.strictEqual(currency.id, 'USD');
                assert.strictEqual(currency.source, currency.SOURCES.URL);

                locale.setId('tr');
                lang.setId('tr');
                currency.setId('TRY');

                assert.strictEqual(locale.id, 'tr');
                assert.strictEqual(locale.source, locale.SOURCES.EXTRA);

                assert.strictEqual(lang.id, 'tr');
                assert.strictEqual(lang.source, lang.SOURCES.EXTRA);

                assert.strictEqual(currency.id, 'TRY');
                assert.strictEqual(currency.source, currency.SOURCES.EXTRA);

                assert.strictEqual(locale.idBySource(locale.SOURCES.DEFAULT), 'tr');
                assert.strictEqual(locale.idBySource(locale.SOURCES.DETECTED), 'tr');
                assert.strictEqual(locale.idBySource(locale.SOURCES.SETTINGS), null);
                assert.strictEqual(locale.idBySource(locale.SOURCES.URL), 'ru');
                assert.strictEqual(locale.idBySource(locale.SOURCES.EXTRA), 'tr');

                // Тут важно понимать, что перекрыв локаль, мы сбросили дефолтные язык и валюту
                // потому что массив их значений формируется из декларации локали
                assert.strictEqual(lang.idBySource(lang.SOURCES.DEFAULT), 'ru');
                assert.strictEqual(lang.idBySource(lang.SOURCES.DETECTED), 'ru');
                assert.strictEqual(lang.idBySource(lang.SOURCES.SETTINGS), null);
                assert.strictEqual(lang.idBySource(lang.SOURCES.URL), 'ru');
                assert.strictEqual(lang.idBySource(lang.SOURCES.EXTRA), 'tr');

                assert.strictEqual(currency.idBySource(currency.SOURCES.DEFAULT), 'RUR');
                assert.strictEqual(currency.idBySource(currency.SOURCES.DETECTED), 'RUR');
                assert.strictEqual(currency.idBySource(currency.SOURCES.SETTINGS), null);
                assert.strictEqual(currency.idBySource(currency.SOURCES.URL), 'USD');
                assert.strictEqual(currency.idBySource(currency.SOURCES.EXTRA), 'TRY');

                assert.deepEqual(locale.idsBySource(locale.SOURCES.DEFAULT), ['tr']);
                assert.deepEqual(locale.idsBySource(locale.SOURCES.DETECTED), ['tr']);
                assert.deepEqual(locale.idsBySource(locale.SOURCES.SETTINGS), null);
                assert.deepEqual(locale.idsBySource(locale.SOURCES.URL), ['ru']);
                assert.deepEqual(locale.idsBySource(locale.SOURCES.EXTRA), ['tr']);

                done();
            })
            .done();
    });

    it('check currency reinitialize', function(done) {
        l10nInit('noob', 'turkey.stambul', l10nParams)
            .then(function(user) {
                var region = user.region,
                    l10n = user.l10n;

                assert.strictEqual(l10n.currency.id, 'TRY');

                region.setId(2);

                assert.strictEqual(l10n.currency.id, 'TRY');
                assert.strictEqual(region.country, 225);

                l10n.reInitCurrencies(user);

                assert.strictEqual(l10n.currency.id, 'RUR');

                done();
            })
            .done();
    });

    it('no currency declared', function(done) {
        l10nInit('yandexoid', 'turkey.stambul', noCurrenciesParams)
            .then(function(user) {
                var l10n = user.l10n;

                assert.strictEqual(l10n.locale.id, 'tr');

                assert.isNull(l10n.currency);

                done();
            })
            .done();
    });

    it('check correct region linguistics in turkey', function(done) {
        l10nInit('noob', 'turkey.stambul', l10nParams)
            .then(function(user) {
                assert.strictEqual(user.region.name, 'Kadıköy');

                done();
            })
            .done();
    });

    it('check no cookies user in russia', function(done) {
        l10nInit('noob', 'russia.spb', l10nParams)
            .then(function(user) {
                assert.strictEqual(user.l10n.locale.id, 'ru');
                assert.strictEqual(user.l10n.lang.id, 'ru');
                // по-умолчанию langdetect не отдаёт переключалку для .ru
                // переписываемся с авторами на эту тему
                assert.deepEqual(user.l10n.lang.list, [ { id : 'ru', name : 'Ru' }]);
                assert.strictEqual(user.l10n.currency.id, 'RUR');

                done();
            })
            .done();
    });

    it('check no cookies user in turkey', function(done) {
        l10nInit('noob', 'turkey.stambul', l10nParams)
            .then(function(user) {
                assert.strictEqual(user.l10n.locale.id, 'tr');
                assert.strictEqual(user.l10n.lang.id, 'tr');
                assert.deepEqual(user.l10n.lang.list, [ { id : 'tr', name : 'Tr' } ]);
                assert.strictEqual(user.l10n.currency.id, 'TRY');

                done();
            })
            .done();
    });

    it('check no cookies user in ukraine', function(done) {
        l10nInit('noob', 'ukraine', l10nParams)
            .then(function(user) {
                assert.strictEqual(user.l10n.locale.id, 'ua');
                assert.strictEqual(user.l10n.lang.id, 'uk');
                assert.deepEqual(user.l10n.lang.list, [ { id : 'uk', name : 'Ua' }, { id : 'ru', name : 'Ru' } ]);
                assert.strictEqual(user.l10n.currency.id, 'UAH');

                done();
            })
            .done();
    });

    it('check currency in belarus', function(done) {
        l10nInit('noob', 'belarus', autol10nParams)
            .then(function(user) {
                assert.strictEqual(user.l10n.locale.id, 'ru');
                assert.strictEqual(user.l10n.lang.id, 'ru');

                assert.strictEqual(user.region.country, 149);
                assert.strictEqual(user.l10n.currency.id, 'BYR');

                done();
            })
            .done();
    });

    describe('check Crimea translocale', function() {

        it('for russian user', function(done) {
            l10nInit('noob', 'russia.spb', l10nParams)
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.infoById(977).country, 225);

                    assert.isTrue(region.idIsIn(977, 225));

                    region.setId(977);

                    assert.strictEqual(region.country, 225);

                    done();
                })
                .done();
        });

        it('for turkish user', function(done) {
            l10nInit('noob', 'turkey.stambul', l10nParams)
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.infoById(977).country, 187);

                    assert.isTrue(region.idIsIn(977, 187));

                    region.setId(977);

                    assert.strictEqual(region.country, 187);

                    done();
                })
                .done();
        });

        it('for ukrainian user', function(done) {
            l10nInit('noob', 'ukraine', l10nParams)
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.infoById(977).country, 187);

                    assert.isTrue(region.idIsIn(977, 187));

                    region.setId(977);

                    assert.strictEqual(region.country, 187);

                    done();
                })
                .done();
        });
    });

});
