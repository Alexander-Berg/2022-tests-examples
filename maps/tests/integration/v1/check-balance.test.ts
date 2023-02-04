import assert from 'assert';
import * as got from 'got';
import nock from 'nock';
import {URL} from 'url';
import {app} from '../../../app/app';
import {mosProvider} from '../../../app/providers/mos';
import {cleanTestDb} from '../db-utils';
import {nockBalanceUrl, nockBlackboxUrl} from '../nocks';
import {client, TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';

describe('/v1/check_balance', () => {
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let blackboxNock: nock.Scope;

    const defaultBlackboxResponse = {
        users: [{
            uid: {value: '111'},
            phones: [{
                attributes: {
                    108: '1',
                    102: '123456789'
                }
            }]
        }]
    };

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/check_balance`);
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
        nock.enableNetConnect();
    });

    afterEach(() => {
        assert.ok(blackboxNock.isDone());

        const pendingMocks = nock.pendingMocks();
        if (pendingMocks.length) {
            throw new Error(`The following mocks were not used. Please check:\n${pendingMocks.join('\n')}`);
        }

        nock.cleanAll();
    });

    async function request(query: Record<string, string | undefined>): Promise<got.Response<any>> {
        return client.get(url, {
            headers: {
                'X-Ya-User-Ticket': 'stub'
            },
            searchParams: query,
            throwHttpErrors: false,
            responseType: 'json',
            retry: 0
        });
    }

    describe('check schema', () => {
        it('should throw 400 when phone is missing', async () => {
            blackboxNock = nockBlackboxUrl({
                users: [{
                    ...defaultBlackboxResponse.users[0],
                    phones: [{
                        attributes: {
                            ...defaultBlackboxResponse.users[0].phones[0].attributes,
                            108: undefined
                        }
                    }]
                }]
            });
            const res = await request({provider: 'mos'});
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'PHONE_NUMBER_NOT_ADDED');
        });

        it('should throw 400 when provider is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({});
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when provider is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({provider: 'piter'});
            assert.strictEqual(res.statusCode, 400);
        });
    });

    describe('check balance', () => {
        beforeEach(async () => {
            await cleanTestDb();
        });

        it('should throw 500 when request to provider was failed', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(500);

            const res = await request({provider: 'mos'});
            assert.strictEqual(res.statusCode, 500);
        });

        it('should return balance on the account', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 100);

            const res = await request({provider: 'mos'});
            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                amount: '100',
                providerCurrency: mosProvider.currency
            });
        });
    });
});
