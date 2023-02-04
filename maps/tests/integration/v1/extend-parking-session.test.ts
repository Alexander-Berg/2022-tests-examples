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
import {cleanTestDb, insertSession, insertZone, SessionData, Timeframe} from '../db-utils';
import {nockBlackboxUrl, nockExtendSessionUrl} from '../nocks';
import {client, TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';

describe('/v1/extend_parking_session', () => {
    const now = new Date('2022-01-10T15:00:00.000Z'); // 18:00 in msk
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let clock: SinonFakeTimers;
    let blackboxNock: nock.Scope;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/extend_parking_session`);
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
        it('should throw 400 when provider is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                durationMinutes: 30,
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when provider is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'piter',
                durationMinutes: 30,
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is missing', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'mos',
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not number', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'mos',
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f',
                durationMinutes: 'a'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when durationMinutes is not in interval', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'mos',
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f',
                durationMinutes: 1
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when sessionId is not uuid', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'mos',
                durationMinutes: 30,
                sessionId: '11111111'
            });
            assert.strictEqual(res.statusCode, 400);
        });
    });

    describe('extend parking session', () => {
        beforeEach(async () => {
            await cleanTestDb();
            invalidateZonesCache();
        });

        const providerParkingId = '1234';
        const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
        const defaultSessionData: SessionData = {
            uid: defaultBlackboxResponse.users[0].uid.value,
            phone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
            active: true,
            vehiclePlate: 'A111AA11',
            provider: 'mos',
            providerParkingId,
            providerSessionId: '11111',
            appId: 'ru.yandex.traffic',
            publicId: sessionId
        };
        const mosTimezone = mosProvider.timezone;
        const duration = 30;
        const defaultBody = {
            provider: 'mos',
            durationMinutes: duration,
            sessionId
        };

        it('should return 400 when session is not found', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);

            await insertSession({
                ...defaultSessionData,
                publicId: '11111111-a06c-4334-9949-f1fc979fb81f'
            });

            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'NO_ACTIVE_SESSIONS');
        });

        it('should return 400 when session is not active', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);

            await insertSession({
                ...defaultSessionData,
                active: false
            });

            const res = await request({
                ...defaultBody,
                sessionId: defaultSessionData.publicId
            });
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'NO_ACTIVE_SESSIONS');
        });

        it('should return 400 when session belongs to another user', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);

            await insertSession({
                ...defaultSessionData,
                uid: '222'
            });

            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'FORBIDDEN');
        });

        it('should return 400 when session has already extended', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const timeframesData: Timeframe[] = [
                {
                    start: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '20'
                },
                {
                    start: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 30}).setZone(mosTimezone).toJSDate(),
                    cost: '10'
                }
            ];

            await insertSession(defaultSessionData, timeframesData);

            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'SESSION_ALREADY_EXTENDED');
        });

        it('should throw 500 when session extension was failed in provider', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const timeframesData: Timeframe[] = [
                {
                    start: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '20'
                }
            ];

            await insertSession(defaultSessionData, timeframesData);

            nockExtendSessionUrl(500);

            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 500);
        });

        it('should extend session successfully', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const timeframesData: Timeframe[] = [
                {
                    start: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '20' // 30 minutes, 40 rub/hour => 20 rub
                }
            ];

            await insertSession(defaultSessionData, timeframesData);
            await insertZone({
                provider: defaultBody.provider,
                providerParkingId,
                intervals: [{
                    from: '00:00',
                    to: '24:00',
                    mainPrice: {
                        value: 40,
                        duration: 60
                    }
                }]
            });

            const mockSessionData = {
                id: '12345',
                placeId: providerParkingId,
                startTime: DateTime.now().plus({minute: 15}).setZone(mosTimezone),
                stopTime: DateTime.now().plus({minute: 15 + duration}).setZone(mosTimezone),
                carId: defaultSessionData.vehiclePlate
            };
            nockExtendSessionUrl(200, mockSessionData);

            const res = await request(defaultBody);
            assert.strictEqual(res.statusCode, 200);

            delete res.body.createdAt;
            assert.deepStrictEqual(res.body, {
                id: defaultSessionData.publicId,
                provider: defaultBody.provider,
                providerParkingId,
                vehiclePlate: defaultSessionData.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosTimezone,
                // the sum of two intervals of 30 minutes, 40 rub/hour => 2 * 1/2 * 40 = 40 rub
                totalCost: '40.00',
                providerCurrency: mosProvider.currency,
                timeframes: [
                    {
                        start: timeframesData[0].start.toISOString(),
                        end: timeframesData[0].end.toISOString(),
                        cost: timeframesData[0].cost
                    },
                    {
                        start: timeframesData[0].end.toISOString(),
                        end: mockSessionData.stopTime.toJSDate().toISOString(),
                        cost: '20'
                    }
                ]
            });

            const {data: {rows: historyRows}} = await dbClient.executeReadQuery({
                text: `SELECT type, provider, user_uid FROM history`
            });
            assert.strictEqual(historyRows.length, 1);
            assert.deepStrictEqual(historyRows, [{
                type: 'SESSION_EXTENDED',
                user_uid: defaultBlackboxResponse.users[0].uid.value,
                provider: defaultBody.provider
            }]);
        });
    });
});
