/* global describe, it, afterEach */

var url = require('url'),
    vow = require('vow'),
    assert = require('chai').assert,
    nock = require('nock'),
    util = require('@vertis/nodules-libs').util,
    mockUser = require('./mock/user'),
    User = require('../lib/user'),
    Auth = require('../components/auth'),
    yandexuid = require('../components/auth/yandexuid');

describe('Auth component', function() {

    var headers,
        requestUri;

    afterEach(function() {
        headers = {};
        requestUri = '';
    });

    function createReq(mock) {
        return {
            method : mock.method || 'GET',
            cookies : mock.cookies
        };
    }

    function createRes(headers) {
        return {
            setHeader : function(name, val) {
                headers[name] = val;
            },
            getHeader : function(name) {
                return headers[name];
            }
        };
    }

    function authInit(user, config) {
        headers = {};

        user = mockUser[user];

        config = util.extend(true, { auth : {} }, config);

        if (user.url) {
            config.url = user.url;
        }

        var req = createReq(user),
            res = createRes(headers);

            var bb = nock('http://blackbox-mimino.yandex.net')
                .filteringPath(function() {
                    return '/blackbox';
                });

        if (user.passportData) {
            bb.get('/blackbox').reply(200, function(uri) {
                requestUri = uri;
                return JSON.parse(JSON.stringify(user.passportData));
            });
        } else if (user.passportData === null) {
            bb.get('/blackbox').reply(200);
        }

        return new User(req, res, config).init('auth');
    }

    it('interface', function(done) {
        authInit('noob')
            .then(function(user) {
                assert.property(user, 'auth', 'User should have an "auth" property');

                var auth = user.auth;

                assert.isDefined(auth.yandexuid, '"Auth#yandexuid" should be defined');
                assert.isDefined(auth.testingGroup, '"Auth#testingGroup" should be defined');
                assert.isDefined(auth.crc, '"Auth#crc" should be defined');
                assert.isNotArray(auth.crc, '"Auth#crc" should not became an Array');

                assert.isBoolean(auth.isAuth, '"Auth#isAuth" should be Boolean');
                assert.isBoolean(auth.isSocialUser, '"Auth#isSocialUser" should be Boolean');
                assert.isBoolean(auth.isLiteUser, '"Auth#isLiteUser" should be Boolean');
                assert.isBoolean(auth.isLiteAuth, '"Auth#isLiteUser" should be Boolean');
                assert.isBoolean(auth.isHostedUser, '"Auth#isHostedUser" should be Boolean');
                assert.isBoolean(auth.isBetaTester, '"Auth#isBetaTester" should be Boolean');
                assert.isBoolean(auth.isYandexEmployee, '"Auth#isYandexEmployee" should be Boolean');
                assert.isBoolean(auth.isNeedRedirectToMobile, '"Auth#isNeedRedirectToMobile" should be Boolean');
                assert.isObject(auth.type);
                assert.isString(auth.type.kind);
                assert.isString(auth.type.id);
                assert.isNull(auth.name);
                assert.isArray(auth.emails, '"Auth#emails should be an array"');

                assert.property(auth, 'uid', '"Auth#uid" should exists');
                assert.property(auth, 'login', '"Auth#login" should exists');
                assert.property(auth, 'passLang', '"Auth#crc" should exists');

                assert.isNull(auth.redirect, '"Auth#redirect" should be null by default');
                assert.isNull(auth.ticket, '"Auth#ticket# should be null by default');

                assert.isFunction(auth.checkCRC);
                assert.isFunction(auth.dbField);
                assert.isFunction(auth.attribute);
                assert.isFunction(auth.alias);
                assert.isFunction(auth.my);
            })
            .done(done);
    });

    it('set blackboxRequestFailed flag for broken response of BlackBox', function(done) {
        vow.all([
            authInit('blackbox-down')
                .then(function(user) {
                    assert.strictEqual(user.auth.blackboxRequestFailed, true,
                        '"Auth#blackboxRequestFailed" should equals true');
                }),
            authInit('yandexoid')
                .then(function(user) {
                    assert.strictEqual(user.auth.blackboxRequestFailed, false,
                        '"Auth#blackboxRequestFailed" should equals false');
                })
        ])
        .then(function() {})
        .done(done);
    });

    it('user with no cookies', function(done) {
        authInit('noob')
            .then(function(user) {
                var auth = user.auth,
                    yuid = auth.yandexuid;

                assert.isFalse(auth.isAuth);
                assert.isFalse(auth.isSocialUser);
                assert.isFalse(auth.isLiteUser);
                assert.isFalse(auth.isLiteAuth);
                assert.isFalse(auth.isHostedUser);
                assert.isFalse(auth.isBetaTester);
                assert.isFalse(auth.isYandexEmployee);
                assert.isTrue(auth.isNeedRedirectToMobile);

                assert.strictEqual(auth.uid, '');
                assert.strictEqual(auth.login, '');
                assert.strictEqual(auth.passLang, '');
                assert.isNull(auth.name);

                assert.strictEqual(auth.type.kind, 'cuid');
                assert.strictEqual(auth.type.id, yuid);

                assert.isNull(auth.redirect);

                assert(yandexuid.test(yuid));
                assert.isString(yuid);
                assert(yuid.length >= 17);

                assert.strictEqual(auth.crc.charAt(0), 'y');
                assert(auth.checkCRC(auth.crc));

                assert.strictEqual(auth.testingGroup, Number(yuid.charAt(yuid.length - 1)));

                assert.strictEqual(auth.dbField('subscription.login_rule.33'), '');
                assert.strictEqual(auth.alias(5), '');

                done();
            })
            .done();
    });

    it('user with cookie my', function(done) {
        authInit('noob-becomes-saloed')
            .then(function(user) {
                var auth = user.auth;

                assert.isTrue(auth.isNeedRedirectToMobile);

                // FIXME: WHUT?! Я же явно поставил украинский!
                assert.strictEqual(auth.my(39), 0);
                assert.strictEqual(auth.my(39, 1), 2);

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('user with broken cookie my', function(done) {
        authInit('noob-broken-cookie-my')
            .then(function(user) {
                assert.property(user, 'auth', 'User should have an "auth" property');

                var auth = user.auth;

                assert.isFunction(auth.my);
                assert.strictEqual(auth.my(39), undefined);
            })
            .done(done);
    });

    it('user with social auth', function(done) {
        authInit('noob-goes-social')
            .then(function(user) {
                var auth = user.auth;

                assert.isTrue(auth.isAuth);
                assert.isTrue(auth.isSocialUser);

                assert.isFalse(auth.isLiteUser);
                assert.isFalse(auth.isLiteAuth);
                assert.isFalse(auth.isHostedUser);

                assert.deepEqual(auth.name, mockUser['noob-goes-social'].passportData.display_name);

                assert.strictEqual(auth.type.kind, 'uid');
                assert.strictEqual(auth.type.id, auth.uid);

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('newly registered user', function(done) {
        authInit('noob-becomes-user')
            .then(function(user) {
                var auth = user.auth,
                    yuid = auth.yandexuid;

                assert.isTrue(auth.isAuth);

                assert.isFalse(auth.isSocialUser);
                assert.isFalse(auth.isLiteUser);
                assert.isFalse(auth.isLiteAuth);
                assert.isFalse(auth.isHostedUser);

                assert.isFalse(auth.isBetaTester);
                assert.isFalse(auth.isYandexEmployee);

                assert.strictEqual(auth.uid, '234776563');
                assert.strictEqual(auth.login, 'fakeflack');

                assert.deepEqual(auth.name, { name : 'fakeflack' });

                assert.strictEqual(auth.type.kind, 'uid');
                assert.strictEqual(auth.type.id, auth.uid);

                assert.strictEqual(auth.passLang, 'ru');

                assert.isTrue(auth.isNeedRedirectToMobile);

                assert(yandexuid.test(yuid));
                assert.isString(yuid);

                assert.strictEqual(auth.crc.charAt(0), 'u');
                assert(auth.checkCRC(auth.crc));

                assert.strictEqual(auth.testingGroup, Number(yuid.charAt(yuid.length - 1)));

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('user with lite auth', function(done) {
        authInit('lite-auth')
            .then(function(user) {
                var auth = user.auth,
                    yuid = auth.yandexuid;

                assert.isTrue(auth.isAuth);

                assert.isFalse(auth.isSocialUser);
                assert.isFalse(auth.isLiteUser);
                assert.isTrue(auth.isLiteAuth);
                assert.isFalse(auth.isHostedUser);

                assert.isTrue(auth.isBetaTester);
                assert.isTrue(auth.isYandexEmployee);

                assert.strictEqual(auth.uid, '20606950');
                assert.strictEqual(auth.login, 'alexrybakov');
                assert.deepEqual(auth.name, { name : 'alexrybakov' });

                assert.strictEqual(auth.type.kind, 'uid');
                assert.strictEqual(auth.type.id, auth.uid);

                assert.strictEqual(auth.passLang, 'ru');

                assert.isTrue(auth.isNeedRedirectToMobile);

                assert(yandexuid.test(yuid));
                assert.isString(yuid);

                assert.strictEqual(auth.crc.charAt(0), 'u');
                assert(auth.checkCRC(auth.crc));

                assert.strictEqual(auth.testingGroup, Number(yuid.charAt(yuid.length - 1)));

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('user with PDD auth', function(done) {
        authInit('pdd-auth')
            .then(function(user) {
                var auth = user.auth,
                    yuid = auth.yandexuid;

                assert.isTrue(auth.isAuth);

                assert.isFalse(auth.isSocialUser);
                assert.isFalse(auth.isLiteUser);
                assert.isFalse(auth.isLiteAuth);
                assert.isTrue(auth.isHostedUser);

                assert.isFalse(auth.isBetaTester);
                assert.isFalse(auth.isYandexEmployee);

                assert.strictEqual(auth.uid, '1130000011801615');
                assert.strictEqual(auth.login, 'robbitter-8239341377@mellior.ru');
                assert.deepEqual(auth.name, { name : 'robbitter-8239341377@mellior.ru' });

                assert.strictEqual(auth.type.kind, 'uid');
                assert.strictEqual(auth.type.id, auth.uid);

                assert.strictEqual(auth.passLang, 'ru');

                assert.isFalse(auth.isNeedRedirectToMobile);

                assert(yandexuid.test(yuid));
                assert.isString(yuid);

                assert.strictEqual(auth.crc.charAt(0), 'u');
                assert(auth.checkCRC(auth.crc));

                assert.strictEqual(auth.testingGroup, Number(yuid.charAt(yuid.length - 1)));

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('user is yandexoid', function(done) {
        authInit('yandexoid')
            .then(function(user) {
                var auth = user.auth,
                    yuid = auth.yandexuid;

                assert.isTrue(auth.isAuth);

                assert.isFalse(auth.isSocialUser);
                assert.isFalse(auth.isLiteUser);
                assert.isFalse(auth.isLiteAuth);
                assert.isFalse(auth.isHostedUser);

                assert.isTrue(auth.isBetaTester);
                assert.isTrue(auth.isYandexEmployee);

                assert.strictEqual(auth.uid, '20606950');
                assert.strictEqual(auth.login, 'alexrybakov');
                assert.deepEqual(auth.name, { name : 'alexrybakov' });

                assert.strictEqual(auth.type.kind, 'uid');
                assert.strictEqual(auth.type.id, auth.uid);

                assert.strictEqual(auth.passLang, 'ru');

                assert.isTrue(auth.isNeedRedirectToMobile);

                assert(yandexuid.test(yuid));
                assert.isString(yuid);

                assert.strictEqual(auth.crc.charAt(0), 'u');
                assert(auth.checkCRC(auth.crc));

                assert.strictEqual(auth.testingGroup, Number(yuid.charAt(yuid.length - 1)));

                assert.isNull(auth.redirect);

                done();
            })
            .done();
    });

    it('user with setting "always show desktop version"', function(done) {
        authInit('noob-always-desktop')
            .then(function(user) {
                assert.isFalse(user.auth.isNeedRedirectToMobile);

                done();
            })
            .done();
    });

    it('user with fresh NOAUTH cookie', function(done) {
        authInit('user-noauth-fresh')
            .then(function(user) {
                var auth = user.auth;

                assert.isFalse(auth.isAuth);
            })
            .done(done);
    });

    it('user with stale NOAUTH cookie', function(done) {
        authInit('user-noauth-stale')
            .then(function(user) {
                var auth = user.auth;

                assert.isFalse(auth.isAuth);
            })
            .done(done);
    });

    describe('need reset', function() {
        it('user need reset', function(done) {
            authInit('user-need-reset')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isTrue(auth.isAuth);
                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'passport.yandex.ru');
                    assert.strictEqual(gotoUrl.query.retpath, 'http://yandex.ru/');

                    done();
                })
                .done();
        });

        it('user need reset on yandex.ua', function(done) {
            authInit('user-need-reset', { url : 'http://yandex.ua' })
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'passport.yandex.ua');
                    assert.strictEqual(gotoUrl.query.retpath, 'http://yandex.ua/');

                    done();
                })
                .done();
        });

        it('user need reset on kinopoisk.ru', function(done) {
            authInit('user-need-reset', { url : 'http://kinopoisk.ru', auth : { baseHost : 'kinopoisk.ru' } })
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'passport.yandex.ru');
                    assert.strictEqual(gotoUrl.query.retpath, 'http://kinopoisk.ru/');

                    done();
                })
                .done();
        });

        it('user need reset on yandex-team.ru', function(done) {
            var authConf = { passResignHost: 'passport.yandex-team' };

            authInit('user-need-reset', { url : 'http://wiki.yandex-team.ru', auth : authConf })
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'passport.yandex-team.ru');
                    assert.strictEqual(gotoUrl.query.retpath, 'http://wiki.yandex-team.ru/');

                    done();
                })
                .done();
        });
    });

    describe('MDA', function() {
        var YANDEX_HOSTNAME = 'yandex.ua',
            EXTERNAL_HOSTNAME = 'kinopoisk.ru';

        function serviceUrl(host) {
            return 'http://' + host;
        }

        function mdaAuthInit(user, config) {
            return authInit(user, util.extend({ url : serviceUrl(YANDEX_HOSTNAME) }, config));
        }

        function mdaAuthInitExternalHost(user) {
            return mdaAuthInit(user, { url : serviceUrl(EXTERNAL_HOSTNAME), auth : { baseHost : EXTERNAL_HOSTNAME } });
        }

        it('should not work for non-GET requests to slave domains', function(done) {
            mdaAuthInit('curl')
                .then(function(user) {
                    assert.isNull(user.auth.redirect);
                    done();
                })
                .done();
        });

        it('should not work for users without cookies support', function(done) {
            mdaAuthInit('noob-cookieless-ua')
                .then(function(user) {
                    assert.isNull(user.auth.redirect);
                    done();
                })
                .done();
        });

        it('should not work if Blackbox is down', function(done) {
            mdaAuthInit('blackbox-down')
                .then(function(user) {
                    assert.isNull(user.auth.redirect);
                    done();
                })
                .done();
        });

        it('should work for users without cookies', function(done) {
            mdaAuthInit('noob')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'pass.yandex.ua');
                    assert.strictEqual(gotoUrl.query.retpath, serviceUrl(YANDEX_HOSTNAME) + '/');

                    done();
                })
                .done();
        });

        it('should not work if mda=0 cookie exists', function(done) {
            mdaAuthInit('noob-no-mda')
                .then(function(user) {
                    assert.isNull(user.auth.redirect);
                    done();
                })
                .done();
        });

        it('user on yandex.ua', function(done) {
            mdaAuthInit('user-goes-from-yandexru')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'pass.yandex.ua');
                    assert.strictEqual(gotoUrl.query.retpath, serviceUrl(YANDEX_HOSTNAME) + '/');

                    done();
                })
                .done();
        });

        it('user on kinopoisk.ru', function(done) {
            mdaAuthInitExternalHost('user-goes-from-yandexru')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'pass.kinopoisk.ru');
                    assert.strictEqual(gotoUrl.query.retpath, serviceUrl(EXTERNAL_HOSTNAME) + '/');

                    done();
                })
                .done();
        });

        it('user with fresh NOAUTH cookie', function(done) {
            mdaAuthInitExternalHost('user-noauth-fresh')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isFalse(auth.isAuth);
                })
                .done(done);
        });

        it('user with stale NOAUTH cookie', function(done) {
            mdaAuthInitExternalHost('user-noauth-stale')
                .then(function(user) {
                    var auth = user.auth;

                    assert.isNotNull(auth.redirect);

                    var gotoUrl = url.parse(auth.redirect.location, true);

                    assert.strictEqual(gotoUrl.hostname, 'pass.kinopoisk.ru');
                    assert.strictEqual(gotoUrl.query.retpath, serviceUrl(EXTERNAL_HOSTNAME) + '/');
                })
                .done(done);
        });

        it('user with stale NOAUTH cookie in Yandex domain (NODULES-544)', function(done) {
            mdaAuthInit('user-noauth-stale', { auth : { baseHost : YANDEX_HOSTNAME } })
                .then(function(user) {
                    var auth = user.auth;

                    assert.isFalse(auth.isAuth);
                    assert.isNull(auth.redirect);
                })
                .done(done);
        });

        it('should set authentication ticket', function(done) {
            authInit(
                'request-tvm',
                {
                    auth : {
                        getAuthTicket : true,
                        client_id: 1,
                        client_secret: 'secret',
                        consumer: 'test'
                    }
                }
            )
                .then(function(user) {
                    assert.isString(user.auth.ticket);
                    done();
                })
                .done();
        });
    });

    describe('yandexuid', function() {
        ['yandex.ru', 'yandex.com.tr'].forEach(function(host) {
            it('should be set according to requested yandex top level domain (' + host + ')', function(done) {
                authInit('noob', { url : 'http://' + host })
                    .then(function() {
                        assert.isTrue(headers['Set-Cookie'].split(';').some(function(c) {
                            return c.indexOf('domain=.' + host) === 0;
                        }), 'cookie\'s domain should be ".' + host + '"');
                        done();
                    })
                    .done();
            });
        });

        it('should be set to baseHost if it is set', function(done) {
            var EXTERNAL_HOSTNAME = 'kinopoisk.ru';

            authInit('noob', { url : 'http://' + EXTERNAL_HOSTNAME, auth : { baseHost : EXTERNAL_HOSTNAME } })
                .then(function() {
                    assert.isTrue([].concat(headers['Set-Cookie'])[0].split(';').some(function(c) {
                        return c.indexOf('domain=.' + EXTERNAL_HOSTNAME) === 0;
                    }), 'cookie\'s domain should be ".' + EXTERNAL_HOSTNAME + '"');
                    done();
                })
                .done();
        });
    });

    describe('static methods for my cookie', function() {

        var reqWithMyCookie = createReq(mockUser.yandexoid),
            reqWithoutMyCookie = createReq(mockUser.noob);

        describe('Auth.my should return function', function() {

            it('for cookieless user', function() {
                assert.isFunction(Auth.my);
            });

            it('for user with my cookie', function() {
                assert.isFunction(Auth.my);
            });

        });

        describe('Auth.my(block) should return value or undefined', function() {

            it('for cookieless user', function() {
                assert.isUndefined(Auth.my(reqWithoutMyCookie)('44'));
            });

            it('for user with my cookie', function() {
                assert.strictEqual(Auth.my(reqWithMyCookie)('44'), 0);
            });

        });

        describe('Auth.isNeedRedirectToMobile should always return boolean', function() {

            it('for cookieless user', function() {
                assert.isTrue(Auth.isNeedRedirectToMobile(reqWithoutMyCookie));
            });

            it('for user with my cookie', function() {
                assert.isTrue(Auth.isNeedRedirectToMobile(reqWithMyCookie));
            });

            it('for user with setting "always show desktop"', function() {
                assert.isFalse(Auth.isNeedRedirectToMobile(createReq(mockUser['noob-always-desktop'])));
            });

        });

        [
            {
                describe : 'Auth.setNeedRedirectToMobile should set "always show mobile version"',
                isSet : true
            },
            {
                describe : 'Auth.setNeedRedirectToMobile should set "always show desktop version"',
                isSet : false
            }
        ]
            .forEach(function(tests) {
                var isSet = tests.isSet;

                describe(tests.describe, function() {
                    [
                        {
                            it : 'for cookieless user',
                            mock : mockUser.noob,
                            onSet : 'YywBAAA=',
                            onUnSet : 'YywBAQA='
                        },
                        {
                            it : 'for user with my cookie',
                            mock : mockUser.yandexoid,
                            onSet : 'YycCAAEoAQIsAQA2AQEA',
                            onUnSet : 'YycCAAEoAQIsAQE2AQEA'
                        }
                    ]
                        .forEach(function(test) {
                            var expectedCookie = test[isSet ? 'onSet' : 'onUnSet'];

                            [
                                undefined, 'ru'
                            ]
                                .forEach(function(tld) {
                                    it(test.it, function() {
                                        var headers = {},
                                            req = createReq(test.mock),
                                            res = createRes(headers),
                                            cookieFromHeaders;

                                        Auth.setNeedRedirectToMobile(req, res, isSet, tld);

                                        if (headers['Set-Cookie']) {
                                            cookieFromHeaders = /^my=([^;]+);/.exec(headers['Set-Cookie'])[1];
                                        } else {
                                            cookieFromHeaders = req.cookies.my;
                                        }

                                        assert.strictEqual(expectedCookie, cookieFromHeaders);
                                    });
                                });
                        });
                });
            });
    });

});
