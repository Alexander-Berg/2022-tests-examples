import assert from 'assert';
import * as got from 'got';
import {DateTime} from 'luxon';
import nock from 'nock';
import {SinonFakeTimers, useFakeTimers} from 'sinon';
import {URL} from 'url';
import {app} from '../../../app/app';
import {dbClient} from '../../../app/lib/db-client';
import {mosProvider} from '../../../app/providers/mos';
import {invalidateZonesCache} from '../../../app/providers/zones';
import {Provider} from '../../../app/types';
import {cleanTestDb, insertZone, insertSession} from '../db-utils';
import {client, TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';
import {nockBalanceUrl, nockBlackboxUrl, nockStartSessionUrl} from '../nocks';

describe('/v1/start_parking_session', () => {
    const now = new Date('2022-01-10T15:00:00.000Z'); // 18:00 in msk
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let clock: SinonFakeTimers;
    let blackboxNock: nock.Scope;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/start_parking_session`);
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

    afterEach(() => {
        assert.ok(blackboxNock.isDone());

        const pendingMocks = nock.pendingMocks();
        if (pendingMocks.length) {
            throw new Error(`The following mocks were not used. Please check:\n${pendingMocks.join('\n')}`);
        }

        nock.cleanAll();
    });

    async function request(body: Record<string, any>): Promise<got.Response<any>> {
        return client.post(url, {
            headers: {
                'X-Ya-User-Ticket': 'stub'
            },
            throwHttpErrors: false,
            responseType: 'json',
            json: body,
            retry: 0
        });
    }

    const defaultBody = {
        provider: 'mos' as Provider,
        providerParkingId: '1234',
        durationMinutes: 30,
        vehiclePlate: 'A111AA11',
        appId: 'ru.yandex.yandexmaps.debug'
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
            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'PHONE_NUMBER_NOT_ADDED');
        });

        it('should throw 400 when provider is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                provider: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when provider is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                provider: 'piter'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when providerParkingId is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                providerParkingId: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                durationMinutes: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not number', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                durationMinutes: 'a'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not in interval', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                durationMinutes: '1'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when vehiclePlate is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                vehiclePlate: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when appId is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                appId: undefined
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when appId is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                appId: 'ru'
            });
            assert.strictEqual(res.statusCode, 400);
        });
    });

    describe('start parking session', () => {
        beforeEach(async () => {
            await cleanTestDb();
            invalidateZonesCache();
        });

        it('should throw 400 when vehiclePlate contains unsupported symbols', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                ...defaultBody,
                vehiclePlate: 'Ш111ШШ11'
            });

            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'BAD_REQUEST');
            assert.strictEqual(res.body.message, 'Vehicle plate "Ш111ШШ11" ' +
                'contains unsupported symbols. Please refer to the API docs for help.');
        });

        it('should throw 400 when session is already started', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);

            await insertSession({
                uid: defaultBlackboxResponse.users[0].uid.value,
                phone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                active: true,
                vehiclePlate: defaultBody.vehiclePlate,
                provider: defaultBody.provider,
                providerParkingId: defaultBody.providerParkingId,
                providerSessionId: '11111',
                appId: defaultBody.appId,
                publicId: '11111111-a06c-4334-9949-f1fc979fb81f'
            });

            const res = await request(defaultBody);

            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'SESSION_ALREADY_STARTED');
        });

        it('should throw 400 when parking zone does not exist', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 100);

            const res = await request(defaultBody);

            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'BAD_REQUEST');
        });

        it('should throw 500 when getting balance was failed', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(500);

            const res = await request(defaultBody);

            assert.strictEqual(res.statusCode, 500);
        });

        it('should throw 500 when start session was failed', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 100);
            nockStartSessionUrl(500);

            await insertZone({
                provider: defaultBody.provider,
                providerParkingId: defaultBody.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const res = await request(defaultBody);

            assert.strictEqual(res.statusCode, 500);
        });

        it('should start session successfully', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            nockBalanceUrl(200, 100);
            const mockSessionData = {
                id: '12345',
                placeId: defaultBody.providerParkingId,
                startTime: DateTime.now(),
                stopTime: DateTime.now().plus({minute: defaultBody.durationMinutes}),
                carId: defaultBody.vehiclePlate
            };
            nockStartSessionUrl(200, mockSessionData);

            await insertZone({
                provider: defaultBody.provider,
                providerParkingId: defaultBody.providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const res = await request(defaultBody);

            assert.strictEqual(res.statusCode, 200);
            assert.ok(res.body.id);

            delete res.body.id;
            delete res.body.createdAt;
            assert.deepStrictEqual(res.body, {
                provider: defaultBody.provider,
                providerParkingId: defaultBody.providerParkingId,
                vehiclePlate: defaultBody.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosProvider.timezone,
                totalCost: '20.00', // 40 rub/hour => 20 rub/30 minutes
                providerCurrency: mosProvider.currency,
                timeframes: [{
                    start: mockSessionData.startTime.toJSDate().toISOString(),
                    end: mockSessionData.stopTime.toJSDate().toISOString(),
                    cost: '20'
                }]
            });

            const {data: {rows: historyRows}} = await dbClient.executeReadQuery({
                text: `SELECT type, provider, user_uid FROM history`
            });
            assert.strictEqual(historyRows.length, 1);
            assert.deepStrictEqual(historyRows, [{
                type: 'SESSION_STARTED',
                user_uid: defaultBlackboxResponse.users[0].uid.value,
                provider: defaultBody.provider
            }]);
        });
    });
});
