var assert = require('chai').assert,
    util = require('../lib/util'),
    connect = require('connect'),
    request = require('supertest'),
    Cookie = require('tough-cookie').Cookie,
    currentDate = new Date(),
    testCookie = {
        name : 'ololo',
        value : 'trololo',
        path : '/',
        domain : 'yandex.ru',
        expires : new Date(currentDate.getFullYear() + 20, currentDate.getMonth(), currentDate.getDate(), 0, 0, 0, 0),
        httpOnly : true
    };

module.exports = {
    'check with more than 1 cookie' : function(done) {
        var app = connect().use(function(req, res) {
                util.setCookie(res, {
                    name : 'cookie1',
                    value : 'value1'
                });
                util.setCookie(res, {
                    name : 'cookie2',
                    value : 'value2'
                });
                util.setCookie(res, {
                    name : 'cookie3',
                    value : 'value3'
                });

                res.statusCode = 200;
                res.end();
            });

        request(app).get('/')
            .end(function(err, res) {
                var header = res.header['set-cookie'],
                    cookie1 = Cookie.parse(header[0]),
                    cookie2 = Cookie.parse(header[1]),
                    cookie3 = Cookie.parse(header[2]);

                assert.strictEqual(cookie1.key, 'cookie1');
                assert.strictEqual(cookie2.key, 'cookie2');
                assert.strictEqual(cookie3.key, 'cookie3');
                assert.strictEqual(cookie1.value, 'value1');
                assert.strictEqual(cookie2.value, 'value2');
                assert.strictEqual(cookie3.value, 'value3');

                return done();
            });
    },
    'check cookie with all options' : function(done) {
        var app = connect().use(function(req, res) {
                util.setCookie(res, {
                    name : testCookie.name,
                    value : testCookie.value,
                    path : testCookie.path,
                    domain : testCookie.domain,
                    expires : testCookie.expires.getTime() / 1000,
                    httpOnly : testCookie.httpOnly
                });

                res.statusCode = 200;
                res.end();
            });

        request(app).get('/')
            .end(function(err, res) {
                var cookie = Cookie.parse(res.headers['set-cookie'][0]);

                assert.strictEqual(cookie.key, testCookie.name, 'key is correct');
                assert.strictEqual(cookie.value, testCookie.value, 'value is correct');
                assert.strictEqual(cookie.domain, testCookie.domain, 'domain is correct');
                assert.strictEqual(cookie.path, testCookie.path, 'path is correct');
                assert.strictEqual(cookie.expires.toString(), testCookie.expires.toString(), 'expires is correct');
                assert.strictEqual(cookie.httpOnly, testCookie.httpOnly, 'httpOnly is correct');

                return done();
            });
    },
    'check cookie with no options' : function(done) {
        var app = connect().use(function(req, res) {
                util.setCookie(res, {
                    name : testCookie.name,
                    value : testCookie.value
                });

                res.statusCode = 200;
                res.end();
            });

        request(app).get('/')
            .end(function(err, res) {
                var cookie = Cookie.parse(res.headers['set-cookie'][0]);

                assert.strictEqual(cookie.key, testCookie.name, 'key is correct');
                assert.strictEqual(cookie.value, testCookie.value, 'value is correct');

                return done();
            });
    },
    'check redirect' : function(done) {
        var app = connect().use(function(req, res) {
            util.redirect(res, 'http://google.com', 301);
        });

        request(app).get('/')
            .expect(301)
            .expect('Location', 'http://google.com')
            .end(function(err) {
                return done(err);
            });
    },
    'check redirect with default status code' : function(done) {
        var app = connect().use(function(req, res) {
            util.redirect(res, 'http://google.com');
        });

        request(app).get('/')
            .expect(302)
            .expect('Location', 'http://google.com')
            .end(function(err) {
                return done(err);
            });
    },
    'check soft redirect' : function(done) {
        var app = connect().use(function(req, res) {
            util.softRedirect(res, 'http://google.com');
            res.setHeader('X-Trololo', 'ololo');
            res.end();
        });

        request(app).get('/')
            .expect(302)
            .expect('Location', 'http://google.com')
            // проверяем, что res.end() не был вызван раньше времени
            .expect('X-Trololo', 'ololo')
            .end(function(err) {
                return done(err);
            });
    }
};
