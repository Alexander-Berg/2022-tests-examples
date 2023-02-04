var blackbox = require('..'),
    nock = require('nock'),
    url = require('url'),
    chai = require('chai').use(require('chai-as-promised')),
    assert = chai.assert;

describe('blackbox', function() {
    describe('client', function() {
        it('should return blackbox client object', function() {
            assert.isObject(blackbox({}));
        });
    });

    describe('API', function() {
        before(function() {
            nock.disableNetConnect();
        });

        var blackboxClientParams = {
                host: 'blackbox-mimino.yandex.net',
                path: '/blackbox'
            },
            mockBlackboxServer,
            client;

        function testRequestParams(requestFn, testFn) {
            mockBlackboxServer
                .get(blackboxClientParams.path)
                .reply(200, function(uri) {
                    var parsedUri = url.parse(uri, true),
                        query = parsedUri.query;

                    testFn(query);

                    return {};
                });

            return assert.isFulfilled(requestFn(client));
        }

        beforeEach(function() {
            mockBlackboxServer = nock('http://' + blackboxClientParams.host)
                .filteringPath(function() {
                    return blackboxClientParams.path;
                });

            client = blackbox(blackboxClientParams);
        });

        afterEach(function() {
            nock.cleanAll();
        });

        describe('#runMethod()', function() {
            it('should resolve with json response from server', function() {
                var fakeResponse = {
                    status: { value: 'VALID' }
                };

                mockBlackboxServer
                    .get(blackboxClientParams.path)
                    .reply(200, function() {
                        return fakeResponse;
                    });

                return assert.eventually.deepEqual(client.runMethod('meth1'), fakeResponse);
            });

            it('should resolve with null on inappropriate server response', function() {
                mockBlackboxServer
                    .get(blackboxClientParams.path)
                    .reply(200, 'Ok');

                return assert.eventually.strictEqual(client.runMethod('meth1'), null);
            });

            it('should resolve with null on empty server response', function() {
                mockBlackboxServer
                    .get(blackboxClientParams.path)
                    .reply(200);

                return assert.eventually.strictEqual(client.runMethod('meth1'), null);
            });

            it('should resolve with null on server request fail', function() {
                mockBlackboxServer
                    .get(blackboxClientParams.path)
                    .reply(500);

                return assert.eventually.strictEqual(client.runMethod('meth1'), null);
            });

            it('should properly send method params', function() {
                return testRequestParams(
                    function(client) {
                        return client.runMethod('meth1', {
                            dbfields: [ 'subscription.login.2', 'subcription.suid.2' ],
                            aliases: [ 1, 2, 3 ],
                            attributes: [ 10, 11, 12 ]
                        });
                    },
                    function(query) {
                        assert.equal(query.method, 'meth1', 'method query is incorrect');
                        assert.equal(query.format, 'json', 'format query is incorrect');
                        assert.equal(
                            query.dbfields,
                            [ 'subscription.login.2', 'subcription.suid.2' ].join(','),
                            'dbfields query is incorrect');
                        assert.equal(query.aliases, '1,2,3', 'aliases query is incorrect');
                        assert.equal(query.attributes, '10,11,12', 'attributes query is incorrect');
                    });
            });
        });

        describe('#sessionId()', function() {
            var TEST_SESSION_ID = '123s1';

            it('should resolve if "sessionid" is empty', function() {
                return assert.isFulfilled(client.sessionId({ sessionid: '' }));
            });

            it('should send default method params', function() {
                return testRequestParams(
                    function(client) {
                        return client.sessionId({ sessionid: TEST_SESSION_ID });
                    },
                    function(query) {
                        assert.equal(query.method, 'sessionid', 'method query is incorrect');
                        assert.equal(query.sessionid, TEST_SESSION_ID, 'sessionid query is incorrect');
                        assert.equal(query.regname, 'yes', 'regname query is not "yes"');
                        assert.equal(query.renew, 'yes', 'renew query is not "yes"');
                    });
            });

            it('should be possible to override "regname" param', function() {
                return testRequestParams(
                    function(client) {
                        return client.sessionId({
                            sessionid: TEST_SESSION_ID,
                            regname: 'no'
                        });
                    },
                    function(query) {
                        assert.equal(query.regname, 'no', 'regname param is not overridden');
                    });
            });

            it('should be possible to override "renew" param', function() {
                return testRequestParams(
                    function(client) {
                        return client.sessionId({
                            sessionid: TEST_SESSION_ID,
                            renew: 'no'
                        });
                    },
                    function(query) {
                        assert.equal(query.renew, 'no', 'renew param is not overridden');
                    });
            });
        });

        describe('#oauth()', function() {
            it('should properly pass "token"', function() {
                var TEST_OAUTH_TOKEN = '123123123';

                return testRequestParams(
                    function(client) {
                        return client.oauth({ token: TEST_OAUTH_TOKEN });
                    },
                    function(query) {
                        assert.equal(query.method, 'oauth', 'method query is incorrect');
                        assert.equal(query.oauth_token,
                            TEST_OAUTH_TOKEN,
                            'oauth_token query is incorrect');
                    });
            });
        });
    });
});
