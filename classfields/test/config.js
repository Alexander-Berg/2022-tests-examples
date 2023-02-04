/* global describe, it */

var assert = require('chai').assert,
    mock = require('./mock/browser'),
    User = require('../lib/user'),
    req = { headers : {}, cookies : {} },
    res = { setHeader : function() {}, getHeader : function() {} },
    util = require('@vertis/nodules-libs').util;

function initAuth(config) {
    return new User(req, res, config).init('auth');
}

function initBrowser(config) {
    return new User(req, res, config).init('browser');
}

function initCustomBrowser(agentId, config) {
    return new User(
            { headers : { 'user-agent' : mock.headers['user-agents'][agentId] } },
            {},
            config
        )
        .init('browser');
}

describe('Configuration', function() {
    it('clientIp is localhost by default', function(done) {
        initAuth({ auth : {} })
            .then(function(user) {
                assert.strictEqual(user.config.get('clientIp'), '127.0.0.1');

                done();
            })
            .done();
    });

    describe('auth', function() {
        it('default params', function(done) {
            initAuth({ auth : {} })
                .then(function(user) {
                    var authConfig = user.config.get('auth');

                    assert.deepEqual(authConfig.blackbox, {
                        host : 'blackbox-mimino.yandex.net',
                        path : '/blackbox',
                        timeout : 300,
                        requestId : 'passport',
                        maxRetries : 2,
                        agent : { name : 'blackbox', maxSockets : 80 },
                        allowGzip : false
                    });

                    assert.deepEqual(authConfig.dbFields, [
                        'subscription.login.668',
                        'subscription.login.669',
                        'userinfo.lang.uid'
                    ]);

                    assert.strictEqual(authConfig.emails, 'getall');

                    assert.strictEqual(authConfig.passMdaHost, 'pass');

                    assert.strictEqual(user.config.get('clientIp'), '127.0.0.1');
                    assert.strictEqual(user.config.get('url'), 'http://yandex.ru');

                    done();
                })
                .done();
        });

        it('params partially configured by client', function(done) {
            var config = {
                    clientIp : '128.1.1.1',
                    url : 'http://auto.yandex.ru/volkswagen/jetta',
                    auth : {
                        blackbox : {
                            host : 'blackbox.yandex.net',
                            maxRetries : 1,
                            requestId : 'yandex-auth'
                        },
                        dbFields : [
                            'subscription.login.668',
                            'subscription.suid.90',
                            'subscription.login_rule.33'
                        ],
                        emails : 'getall',
                        passMdaHost : 'pass-test'
                    }
                },
                dbFieldsResult = [
                    'subscription.login.668', // остается как есть
                    'subscription.suid.90',
                    'subscription.login_rule.33',
                    'subscription.login.669', // недостающие гранты добавлены
                    'userinfo.lang.uid'
                ];

            initAuth(config)
                .then(function(user) {
                    var authConfig = user.config.get('auth');

                    assert.deepEqual(
                        authConfig.blackbox,
                        util.extend(
                            {
                                host : 'blackbox.yandex.net',
                                path : '/blackbox',
                                timeout : 300,
                                requestId : 'passport',
                                maxRetries : 2,
                                agent : { name : 'blackbox', maxSockets : 80 },
                                allowGzip : false
                            },
                            config.auth.blackbox
                        ));

                    assert.deepEqual(authConfig.dbFields, dbFieldsResult);

                    assert.strictEqual(authConfig.passMdaHost, config.auth.passMdaHost);

                    assert.strictEqual(user.config.get('clientIp'), config.clientIp);
                    assert.strictEqual(user.config.get('url'), config.url);

                    done();
                })
                .done();
        });

        it('override defaultPermissions configuration by client', function(done) {
            var config = {
                    auth : {
                        defaultPermissions : [],
                        dbFields : ['subscription.suid.90']
                    }
                },
                dbFieldsResult = [
                    'subscription.suid.90'
                ];

            initAuth(config)
                .then(function(user) {
                    var authConfig = user.config.get('auth');
                    assert.deepEqual(authConfig.dbFields, dbFieldsResult);

                    done();
                })
                .done();
        });
    });

    describe('browser', function() {
        it('browser default params', function(done) {
            initBrowser({ browser : {} })
                .then(function(user) {
                    var browserConfig = user.config.get('browser');

                    assert.strictEqual(browserConfig.uatraitsNodeModule, 'uatraits');
                    assert.strictEqual(browserConfig.uatraitsBrowserPath, '/usr/share/uatraits/browser.xml');
                    assert.strictEqual(browserConfig.uatraitsProfilesPath, '/usr/share/uatraits/profiles.xml');
                    assert.strictEqual(browserConfig.uatraitsExtraPath, '/usr/share/uatraits/extra.xml');

                    done();
                })
                .done();
        });

        it('browser override default old browsers', function(done) {
            initCustomBrowser('desktop-ie9', { browser : {
                    oldBrowsers : [ {
                        name : 'MSIE',
                        version : 9
                    } ]
                } })
                .then(function(user) {
                    assert.strictEqual(user.browser.isOld, true);

                    done();
                })
                .done();
        });
    });
});
