import fs from 'fs';
import nock from 'nock';
import got from 'got';
import {expect} from 'chai';
import sinon from 'sinon';
import {Deferred, delay} from 'prex';
import {createApp} from 'app/app';
import {config} from 'app/config';
import {Hosts, intHostsLoader} from 'app/lib/hosts';
import {KeyManager, KeyCheckOptions, KeyEntity, KeyInfoOptions} from 'app/lib/keys';
import {CountersManager} from 'app/lib/counters/counters-manager';
import {UpdateCountersProvider} from 'app/lib/counters/update-counters-provider';
import {TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const LOCALHOST = /(127.0.0.1|localhost)/;

// Keep ticket in separate file for easy update.
const CLIENT1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/client1').toString().trim();
const CLIENT2_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/client2').toString().trim();
const CLIENT3_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/client3').toString().trim();

class KeyManagerMock implements KeyManager {
    getValidKey(_value: Readonly<KeyCheckOptions>): KeyEntity | null {
        return null;
    }
    getKeyInfo(_value: Readonly<KeyInfoOptions>): KeyEntity | null {
        return null;
    }
}

describe('POST /v1/check', function () {
    // Increase timeout for CI.
    this.timeout(5000);

    const keyManager = new KeyManagerMock();

    let intHosts: Hosts;
    let tvmDaemon: TvmDaemon;
    let testServer: TestServer;

    before(async () => {
        intHosts = await intHostsLoader.get();
        tvmDaemon = await TvmDaemon.start();
        const countersManager = new CountersManager(intHosts.apikeysPaysys);
        const updateCountersProvider = new UpdateCountersProvider(intHosts.apikeysPaysys);
        const app = createApp(intHosts, keyManager, countersManager, updateCountersProvider);
        testServer = await TestServer.start(app);
    });

    after(async () => {
        await testServer.stop();
        await tvmDaemon.stop();
    });

    beforeEach(() => {
        nock.disableNetConnect();
        nock.enableNetConnect(LOCALHOST);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    function check(body: any, ticket = CLIENT1_TICKET): Promise<got.Response<any>> {
        return got(`${testServer.url}/v1/check`, {
            method: 'POST',
            json: true,
            headers: {
                'X-Ya-Service-Ticket': ticket
            },
            body,
            throwHttpErrors: false
        });
    }

    it('should return 400 for request without IP', async () => {
        const res = await check({});
        expect(res.statusCode).to.equal(400);
    });

    it('should return 400 for invalid IP', async () => {
        const res = await check({ip: 'xxx'});
        expect(res.statusCode).to.equal(400);
    });

    it('should return 403 on empty key', async () => {
        const res = await check({ip: '127.0.0.1', key: ''});
        expect(res.statusCode).to.equal(200);
        expect(res.body).to.include({ok: false, statusCode: 403});
    });

    // https://st.yandex-team.ru/MAPSHTTPAPI-1647
    it('should return 200 with error for invalid type key', async () => {
        const res = await check({
            key: ['array', 'of', 'beautiful', 'keys']
        });
        expect(res.statusCode).to.equal(200);
        expect(res.body.ok).to.be.equal(false);
        expect(res.body.statusCode).to.be.equal(403);
    });

    describe('when key is required for the service', () => {
        it('should return 403 on missing key', async () => {
            const res = await check({ip: '127.0.0.1'}, CLIENT3_TICKET);
            expect(res.statusCode).to.equal(200);
            expect(res.body).to.include({ok: false, statusCode: 403});
        });
    });

    it('should return 403 if IP is banned in keyserv', async () => {
        const banCheck = nock(intHosts.keyserv)
            .get('/2.x/')
            .query((query: any) =>
                query.action === 'checkKey' &&
                query.ip === '1.2.3.4' &&
                query.uri === undefined
            )
            .reply(200, `<keystate>
                    <key>
                    </key>
                    <valid>false</valid>
                    <issued>1212679357</issued>
                    <stoplist>
                        <stop>
                            <blocked>1</blocked>
                            <description>"ban5"</description>
                            <modified>2010-07-05 22:35:47</modified>
                        </stop>
                    </stoplist>
                    <restrictions>
                    </restrictions>
                </keystate>`
            );

        const res = await check({ip: '1.2.3.4'});
        expect(res.statusCode).to.equal(200);
        expect(res.body).to.include({ok: false, statusCode: 403});

        banCheck.done();
    });

    it('should return 403 if IP or referrer is banned in keyserv', async () => {
        const banCheck = nock(intHosts.keyserv)
            .get('/2.x/')
            .query((query: any) =>
                query.action === 'checkKey' &&
                query.ip === '1.2.3.4' &&
                query.uri === 'http://example.com'
            )
            .reply(200, `<keystate>
                    <key>
                    </key>
                    <valid>false</valid>
                    <issued>1212679357</issued>
                    <stoplist>
                        <stop>
                            <blocked>1</blocked>
                            <description>"ban5"</description>
                            <modified>2010-07-05 22:35:47</modified>
                        </stop>
                    </stoplist>
                    <restrictions>
                    </restrictions>
                </keystate>`
            );

        const res = await check({ip: '1.2.3.4', referrer: 'http://example.com'});
        expect(res.statusCode).to.equal(200);
        expect(res.body).to.include({ok: false, statusCode: 403});

        banCheck.done();
    });

    describe('when keyserv returns success response', () => {
        let banCheck: nock.Scope;

        beforeEach(() => {
            banCheck = nock(intHosts.keyserv)
                .get('/2.x/')
                .query((query: any) => query.action === 'checkKey')
                .reply(200, `<keystate>
                        <key>
                        </key>
                        <valid>true</valid>
                        <issued>1212679357</issued>
                        <stoplist>
                        </stoplist>
                        <restrictions>
                        </restrictions>
                    </keystate>`
                );
        });

        afterEach(() => {
            banCheck.done();
        });

        testChecksAfterKeyserv();
    });

    describe('when keyserv returns 500', () => {
        let banCheck: nock.Scope;

        beforeEach(() => {
            banCheck = nock(intHosts.keyserv)
                .get('/2.x/')
                .query(true)
                .reply(500);
        });

        afterEach(() => {
            banCheck.done();
        });

        testChecksAfterKeyserv();
    });

    describe('on keyserv timeout', async () => {
        let banCheck: nock.Scope;

        beforeEach(() => {
            banCheck = nock(intHosts.keyserv)
                .get('/2.x/')
                .query(true)
                .delay(config['keyserv.timeout'])
                // Return valid=false for check timeout handling.
                .reply(200, '<keystate><valid>false</valid></keystate>');
        });

        afterEach(() => {
            banCheck.done();
        });

        testChecksAfterKeyserv();
    });

    function testChecksAfterKeyserv() {
        describe('when "key" parameter is passed', () => {
            it('should return 403 if key is not allowed', async () => {
                const keyCheck = sinon.stub(keyManager, 'getValidKey').returns(null);

                const res = await check({ip: '127.0.0.1', key: 'key'});
                expect(res.statusCode).to.equal(200);
                expect(res.body).to.include({ok: false, statusCode: 403});

                expect(keyCheck.calledOnce).to.be.true;
                expect(keyCheck.firstCall.args).to.deep.equal([
                    {
                        token: 'client1_token',
                        key: 'key',
                        ip: '127.0.0.1',
                        referrer: undefined,
                        strictReferrerChecking: undefined
                    }
                ]);

                keyCheck.restore();
            });

            it('should select apikeys service token by TVM id', async () => {
                const keyCheck = sinon.stub(keyManager, 'getValidKey').returns(null);

                let res = await check({ip: '127.0.0.1', key: 'key'});
                expect(res.statusCode).to.equal(200);
                expect(res.body).to.include({ok: false, statusCode: 403});

                expect(keyCheck.callCount).to.equal(1);

                res = await check({ip: '127.0.0.1', key: 'key'}, CLIENT2_TICKET);
                expect(res.statusCode).to.equal(200);
                expect(res.body).to.include({ok: false, statusCode: 403});

                expect(keyCheck.callCount).to.equal(2);
                expect(keyCheck.firstCall.args).to.deep.equal([
                    {
                        token: 'client1_token',
                        key: 'key',
                        ip: '127.0.0.1',
                        referrer: undefined,
                        strictReferrerChecking: undefined
                    }
                ]);
                expect(keyCheck.secondCall.args).to.deep.equal([
                    {
                        token: 'client2_token',
                        key: 'key',
                        ip: '127.0.0.1',
                        referrer: undefined,
                        strictReferrerChecking: undefined
                    }
                ]);

                keyCheck.restore();
            });

            describe('when key is allowed', () => {
                let keyCheck: sinon.SinonStub<[Readonly<KeyCheckOptions>]>;

                const tariff = 'apimaps_free';

                beforeEach(() => {
                    keyCheck = sinon.stub(keyManager, 'getValidKey').returns({tariff});
                });

                afterEach(() => {
                    keyCheck.restore();
                });

                it('should return 200', async () => {
                    const res = await check({ip: '127.0.0.1', key: 'some_key'});
                    expect(res.statusCode).to.equal(200);
                    expect(res.body).to.deep.equal({ok: true, tariff});

                    expect(keyCheck.callCount).to.be.equal(1);
                    expect(keyCheck.firstCall.args).to.deep.equal([
                        {
                            token: 'client1_token',
                            key: 'some_key',
                            ip: '127.0.0.1',
                            referrer: undefined,
                            strictReferrerChecking: undefined
                        }
                    ]);
                });

                it('should allow optional req-id', async () => {
                    const res = await check({ip: '127.0.0.1', key: 'some_key', 'req-id': '123123123'});
                    expect(res.statusCode).to.equal(200);
                });

                it('should return correct tariff', async () => {
                    const tariff = 'specific_tariff';
                    keyCheck.restore();
                    keyCheck = sinon.stub(keyManager, 'getValidKey').returns({tariff});

                    const res = await check({ip: '127.0.0.1', key: 'some_key', 'req-id': '123123123'});
                    expect(res.statusCode).to.equal(200);
                    expect(res.body).to.deep.equal({ok: true, tariff});
                });

                describe('when counters are passed', () => {
                    describe('when update counters batching is disabled for a client', () => {
                        it('should send update counters immediately after response was sent', async () => {
                            const updateCounters = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => (
                                    query.key === 'some_key' &&
                                    query.foo === '1' &&
                                    query.bar === '2'
                                ))
                                .reply(200);

                            const res = await check({
                                ip: '127.0.0.1',
                                key: 'some_key',
                                counters: {
                                    foo: 1,
                                    bar: 2
                                }
                            });
                            expect(res.statusCode).to.equal(200);
                            expect(res.body).to.deep.equal({ok: true, tariff});

                            // Wait some time to guarantee that request for counters update was
                            // completed.
                            await delay(
                                config['apikeys.counters.updateTimeout'] +
                                config['apikeys.counters.batching.flushInterval']
                            );

                            updateCounters.done();
                        });
                    });

                    describe('when update counters batching is enabled for a client', () => {
                        function waitForNextCachedBatchesFlush(): Promise<void> {
                            const deferred = new Deferred<void>();
                            const scope = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => query.key === 'decoy_key')
                                .reply(200);

                            scope.on('request', () => deferred.resolve());

                            check({ip: '127.0.0.1', key: 'decoy_key', counters: {foo: 1}}, CLIENT3_TICKET);

                            return deferred.promise;
                        }

                        beforeEach(async () => {
                            await waitForNextCachedBatchesFlush();
                        });

                        it('should periodically flush cached batch updates', async () => {
                            const batchSize = 6;

                            const scope1 = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => (
                                    query.key === 'key_1' &&
                                    query.foo === '1' &&
                                    query.bar === '2'
                                ))
                                .reply(200);
                            const scope2 = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => (
                                    query.key === 'key_2' &&
                                    query.foo === batchSize.toString() &&
                                    query.bar === (2 * batchSize).toString() &&
                                    query.baz === (3 * batchSize).toString()
                                ))
                                .reply(200);

                            const checks = [];

                            checks.push({
                                ip: '127.0.0.1',
                                key: 'key_1',
                                counters: {foo: 1, bar: 2}
                            });

                            for (let i = 0; i < batchSize; i++) {
                                checks.push({
                                    ip: '127.0.0.1',
                                    key: 'key_2',
                                    counters: {foo: 1, bar: 2, baz: 3}
                                });
                            }

                            const responses = await Promise.all(checks.map((body) => check(body, CLIENT3_TICKET)));
                            for (const res of responses) {
                                expect(res.statusCode).to.equal(200);
                                expect(res.body).to.deep.equal({ok: true, tariff});
                            }

                            expect(scope1.isDone()).to.be.false;
                            expect(scope2.isDone()).to.be.false;

                            await delay(
                                config['apikeys.counters.batching.flushInterval'] +
                                config['apikeys.counters.batching.batchInterval']
                            );

                            scope1.done();
                            scope2.done();
                        });

                        it('should send batch update immediately after reaching max batch size', async () => {
                            const maxBatchSize = config['apikeys.counters.batching.maxBatchSize'];

                            const scope1 = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => (
                                    query.key === 'key_1' &&
                                    query.foo === maxBatchSize.toString() &&
                                    query.bar === (2 * maxBatchSize).toString()
                                ))
                                .reply(200);
                            const scope2 = nock(intHosts.apikeysPaysys)
                                .get('/api/update_counters')
                                .query((query: any) => (
                                    query.key === 'key_1' &&
                                    query.foo === '1' &&
                                    query.bar === '2'
                                ))
                                .reply(200);

                            const checks = [];
                            for (let i = 0; i < maxBatchSize + 1; i++) {
                                checks.push({
                                    ip: '127.0.0.1',
                                    key: 'key_1',
                                    counters: {foo: 1, bar: 2}
                                });
                            }

                            const responses = await Promise.all(checks.map((body) => check(body, CLIENT3_TICKET)));
                            for (const res of responses) {
                                expect(res.statusCode).to.equal(200);
                                expect(res.body).to.deep.equal({ok: true, tariff});
                            }

                            await delay(
                                config['apikeys.counters.batching.flushInterval'] +
                                config['apikeys.counters.batching.updateTimeout']
                            );

                            expect(scope1.isDone()).to.be.true;
                            expect(scope2.isDone()).to.be.false;

                            await delay(
                                config['apikeys.counters.batching.flushInterval'] +
                                config['apikeys.counters.batching.batchInterval']
                            );

                            scope2.done();
                        });
                    });
                });
            });
        });
    }

    describe('when passed referrer in whitelist', () => {
        let banCheck: nock.Scope;
        let keyCheck: sinon.SinonStub<[Readonly<KeyCheckOptions>]>;
        let updateCounters: nock.Scope;

        beforeEach(() => {
            banCheck = nock(intHosts.keyserv)
                .get('/2.x/')
                .query(true)
                .reply(200, '<keystate><valid>false</valid></keystate>');

            keyCheck = sinon.stub(keyManager, 'getValidKey').returns(null);

            updateCounters = nock(intHosts.apikeysPaysys)
                .get('/api/update_counters')
                .query(true)
                .reply(200);
        });

        afterEach(() => {
            keyCheck.restore();
        });

        it('should skip all checks if key is passed', async () => {
            const res = await check({
                ip: '127.0.0.1',
                // See src/tests/fixtures/whitelist configuration
                referrer: 'http://maps.yandex.ru',
                key: 'some_key'
            });
            expect(res.statusCode).to.equal(200);
            expect(res.body.ok).to.equal(true);
            expect(res.body.tariff).to.a('string');

            expect(banCheck.isDone(), 'should not check ban in keyserv').to.be.false;
            expect(keyCheck.called, 'should not check key').to.be.false;
            // Wait some time to guarantee that request for counters update was completed.
            await delay(config['apikeys.counters.updateTimeout']);
            expect(updateCounters.isDone(), 'should not update counters in apikeys').to.be.false;
        });

        it('should skip all checks if key is not passed', async () => {
            const res = await check({ip: '127.0.0.1', referrer: 'http://maps.yandex.ru'});
            expect(res.statusCode).to.equal(200);
            expect(res.body.ok).to.equal(true);
            expect(res.body.tariff).to.a('string');

            expect(banCheck.isDone(), 'should not check ban in keyserv').to.be.false;
            expect(keyCheck.called, 'should not check key').to.be.false;
            // Wait some time to guarantee that request for counters update was completed.
            await delay(config['apikeys.counters.updateTimeout']);
            expect(updateCounters.isDone(), 'should not update counters in apikeys').to.be.false;
        });
    });
});
