import * as assert from 'assert';
import * as got from 'got';
import {DateTime} from 'luxon';
import nock from 'nock';
import {SinonFakeTimers, useFakeTimers} from 'sinon';
import {URL} from 'url';

import {
    cleanTestDb,
    insertZone,
    insertSession,
    SessionData
} from '../db-utils';
import {TestServer, client} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';
import {nockBalanceUrl, nockBlackboxUrl} from '../nocks';
import {app} from '../../../app/app';
import {dbClient} from '../../../app/lib/db-client';
import {mosProvider} from '../../../app/providers/mos';
import {invalidateZonesCache} from '../../../app/providers/zones';

describe('/v1/check_price', () => {
    const now = new Date('2022-01-10T15:00:00.000Z'); // 18:00 in msk
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let clock: SinonFakeTimers;
    let blackboxNock: nock.Scope;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/check_price`);
        clock = useFakeTimers({now, toFake: ['Date']});
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
        clock.restore();
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await cleanTestDb();
        invalidateZonesCache();
    });

    afterEach(() => {
        assert.ok(blackboxNock.isDone());

        const pendingMocks = nock.pendingMocks();
        if (pendingMocks.length) {
            throw new Error(`The following mocks were not used. Please check:\n${pendingMocks.join('\n')}`);
        }

        nock.cleanAll();
    });

    const defaultQuery = {
        provider: 'mos',
        providerParkingId: '1234',
        durationMinutes: '30'
    };

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

    const defaultSession: SessionData = {
        uid: '111',
        phone: '+70000000000',
        active: true,
        vehiclePlate: 'A111AA11',
        provider: 'mos',
        providerParkingId: '1234',
        providerSessionId: '11111',
        appId: 'ru.yandex.traffic',
        publicId: '11111111-a06c-4334-9949-f1fc979fb81f'
    };

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

    describe('check schema and headers', () => {
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
            const res = await request(defaultQuery);
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'PHONE_NUMBER_NOT_ADDED');
        });

        it('should throw 400 when provider is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                provider: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when provider is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                provider: 'piter'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when providerParkingId is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                providerParkingId: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                durationMinutes: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not number', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                durationMinutes: 'a'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not in interval', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                durationMinutes: '1'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when sessionId is not uuid', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                sessionId: '1234'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when lang is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                lang: 'rus'
            });
            assert.strictEqual(res.statusCode, 400);
        });
    });

    describe('check price', () => {
        beforeEach(async () => {
            await cleanTestDb();
        });

        it('should throw 400 when session is not found', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultQuery,
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'NOT_FOUND');
        });

        it('should throw 400 when session belongs to another user', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';

            await insertSession({
                ...defaultSession,
                publicId: sessionId,
                uid: '222'
            });

            const res = await request({
                ...defaultQuery,
                sessionId
            });
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'FORBIDDEN');
        });

        it('should throw 400 when parking zone is not found', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'BAD_REQUEST');
        });

        it('should throw 500 when get balance handler is not available', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(500);

            await insertZone({
                provider: defaultQuery.provider,
                providerParkingId: defaultQuery.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 500);
        });

        it('should return correct `balanceChargeAmount` ' +
            'when there is enough money on the account balance', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 100);

            await insertZone({
                provider: defaultQuery.provider,
                providerParkingId: defaultQuery.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                action: 'DO_NOT_CHARGE',
                balanceChargeAmount: '20' // 40 rub/hour => 30 minutes = 20 rub
            });
        });

        it('should return correct `freeTimeNotice` when today is a weekend', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 0);

            await insertZone({
                provider: defaultQuery.provider,
                providerParkingId: defaultQuery.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [DateTime.now().toFormat('yyyy-MM-dd')]
            });

            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                action: 'PARK_FOR_FREE',
                freeTimeNotice: 'Бесплатная парковка до 11 января\nЗа это время платить не нужно'
            });
        });

        it('should return correct `freeTimeNotice` when parking zone is always free', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 0);

            await insertZone({
                provider: defaultQuery.provider,
                providerParkingId: defaultQuery.providerParkingId,
                intervals: []
            });

            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                action: 'PARK_FOR_FREE',
                freeTimeNotice: 'Бесплатная парковка\nЗа это время платить не нужно'
            });
        });

        it('should return correct `NEED_PAYMENT` data ' +
            'when there is not enough money on the account balance', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 3);

            await insertZone({
                provider: defaultQuery.provider,
                providerParkingId: defaultQuery.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const res = await request(defaultQuery);

            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                action: 'NEED_PAYMENT',
                parkingPrice: '20', // 40 rub/hour => 30 minutes = 20 rub
                balanceChargeAmount: '3',
                topupPaymentAmount: '17',
                providerCurrency: mosProvider.currency,
                commissionNotice: 'Комиссия: 0,7-1,0%, минимум 5₽'
            });
        });
    });
});
