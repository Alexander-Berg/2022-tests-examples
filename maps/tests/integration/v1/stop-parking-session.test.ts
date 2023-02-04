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
import {
    cleanTestDb,
    insertSession,
    insertZone,
    SessionData,
    Timeframe
} from '../db-utils';
import {nockBlackboxUrl, nockStopSessionUrl} from '../nocks';
import {client, TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';

describe('/v1/stop_parking_session', () => {
    const now = new Date('2022-01-10T15:00:00.000Z'); // 18:00 in msk
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let clock: SinonFakeTimers;
    let blackboxNock: nock.Scope;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/stop_parking_session`);
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
                provider: undefined,
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when provider is incorrect', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'piter',
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
        });

        it('should throw 400 when sessionId is not uuid', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const res = await request({
                provider: 'mos',
                sessionId: '11111111'
            });
            assert.strictEqual(res.statusCode, 400);
        });
    });

    describe('stop parking session', () => {
        beforeEach(async () => {
            await cleanTestDb();
            invalidateZonesCache();
        });

        const providerParkingId = '1234';
        const defaultSessionData: SessionData = {
            uid: defaultBlackboxResponse.users[0].uid.value,
            phone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
            active: true,
            vehiclePlate: 'A111AA11',
            provider: 'mos',
            providerParkingId,
            providerSessionId: '11111',
            appId: 'ru.yandex.traffic',
            publicId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
        };
        const mosTimezone = mosProvider.timezone;

        it('should return 400 when session is not found', async () => {
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);

            await insertSession({
                ...defaultSessionData,
                publicId: '11111111-a06c-4334-9949-f1fc979fb81f'
            });

            const res = await request({
                provider: 'mos',
                sessionId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
            });
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'NOT_FOUND');
        });

        it('should return 400 when session belongs to another user', async () => {
            nockBlackboxUrl(defaultBlackboxResponse);
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId,
                uid: '222'
            });

            const res = await request({
                provider: 'mos',
                sessionId
            });
            assert.strictEqual(res.statusCode, 400);
            assert.strictEqual(res.body.code, 'FORBIDDEN');
        });

        it('should not send requests to the provider, if the session is not active', async () => {
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const provider = 'mos';
            const timeframesData: Timeframe[] = [
                {
                    start: DateTime.now().minus({minute: 60}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().minus({minute: 30}).setZone(mosTimezone).toJSDate(),
                    cost: '20'
                },
                {
                    start: DateTime.now().minus({minute: 30}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '10'
                }
            ];

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId,
                active: false
            }, timeframesData);

            const res = await request({
                provider,
                sessionId
            });

            delete res.body.createdAt;
            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                id: defaultSessionData.publicId,
                provider,
                providerParkingId,
                vehiclePlate: defaultSessionData.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosProvider.timezone,
                totalCost: '30.00',
                providerCurrency: mosProvider.currency,
                timeframes: timeframesData.map((timeframe) => ({
                    start: timeframe.start.toISOString(),
                    end: timeframe.end.toISOString(),
                    cost: timeframe.cost
                }))
            });
        });

        it('should stop session successfully', async () => {
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const provider = 'mos';
            const mockStopSessionData = {
                id: '12345',
                placeId: providerParkingId,
                startTime: DateTime.now().minus({minute: 15}).setZone(mosTimezone),
                stopTime: DateTime.now().setZone(mosTimezone),
                carId: defaultSessionData.vehiclePlate
            };
            nockStopSessionUrl(200, mockStopSessionData);
            const timeframesData: Timeframe[] = [
                {
                    start: DateTime.now().minus({minute: 30}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '10'
                },
                {
                    start: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '20'
                }
            ];

            await insertZone({
                provider,
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

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId
            }, timeframesData);

            const res = await request({
                provider,
                sessionId
            });

            delete res.body.createdAt;
            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                id: defaultSessionData.publicId,
                provider,
                providerParkingId,
                vehiclePlate: defaultSessionData.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosTimezone,
                // the sum of two intervals of 15 minutes, 40 rub/hour => 2 * 1/4 * 40 = 20 rub
                totalCost: '20.00',
                providerCurrency: mosProvider.currency,
                timeframes: [
                    {
                        start: timeframesData[0].start.toISOString(),
                        end: timeframesData[0].end.toISOString(),
                        cost: timeframesData[0].cost
                    },
                    {
                        start: timeframesData[1].start.toISOString(),
                        end: mockStopSessionData.stopTime.toJSDate().toISOString(),
                        cost: '10' // the price has changed because the end of parking interval has changed
                    }
                ]
            });

            const {data: {rows: historyRows}} = await dbClient.executeReadQuery({
                text: `SELECT type, provider, user_uid FROM history`
            });
            assert.strictEqual(historyRows.length, 1);
            assert.deepStrictEqual(historyRows, [{
                type: 'SESSION_STOPPED',
                user_uid: defaultBlackboxResponse.users[0].uid.value,
                provider
            }]);
        });

        it('should throw 500 when request to provider was failed', async () => {
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const provider = 'mos';
            nockStopSessionUrl(500);

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId
            });

            const res = await request({
                provider,
                sessionId
            });

            assert.strictEqual(res.statusCode, 500);
        });

        it('should not change end and cost of last timeframe when end < now', async () => {
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const provider = 'mos';
            const mockStopSessionData = {
                id: '12345',
                placeId: providerParkingId,
                startTime: DateTime.now().minus({minute: 15}).setZone(mosTimezone),
                stopTime: DateTime.now().setZone(mosTimezone),
                carId: defaultSessionData.vehiclePlate
            };
            nockStopSessionUrl(200, mockStopSessionData);
            const timeframeData: Timeframe = {
                start: DateTime.now().minus({minute: 30}).setZone(mosTimezone).toJSDate(),
                end: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                cost: '10'
            };

            await insertZone({
                provider,
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

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId
            }, [timeframeData]);

            const res = await request({
                provider,
                sessionId
            });

            delete res.body.createdAt;
            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                id: defaultSessionData.publicId,
                provider,
                providerParkingId,
                vehiclePlate: defaultSessionData.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosTimezone,
                totalCost: '10.00',
                providerCurrency: mosProvider.currency,
                timeframes: [
                    {
                        start: timeframeData.start.toISOString(),
                        end: timeframeData.end.toISOString(),
                        cost: timeframeData.cost
                    }
                ]
            });
        });

        it('should change the cost and price of the last timeframe ' +
            'if the session was stopped before the end timestamp', async () => {
            const sessionId = '030a5aea-a06c-4334-9949-f1fc979fb81f';
            blackboxNock = nockBlackboxUrl(defaultBlackboxResponse);
            const provider = 'mos';
            const mockStopSessionData = {
                id: '12345',
                placeId: providerParkingId,
                startTime: DateTime.now().minus({minute: 15}).setZone(mosTimezone),
                stopTime: DateTime.now().setZone(mosTimezone),
                carId: defaultSessionData.vehiclePlate
            };
            nockStopSessionUrl(200, mockStopSessionData);
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

            await insertZone({
                provider,
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

            await insertSession({
                ...defaultSessionData,
                publicId: sessionId
            }, timeframesData);

            const res = await request({
                provider,
                sessionId
            });

            delete res.body.createdAt;
            assert.strictEqual(res.statusCode, 200);
            assert.deepStrictEqual(res.body, {
                id: defaultSessionData.publicId,
                provider,
                providerParkingId,
                vehiclePlate: defaultSessionData.vehiclePlate,
                userPhone: defaultBlackboxResponse.users[0].phones[0].attributes['102'],
                providerTzdbLocation: mosTimezone,
                // the amount is only for one interval of 15 minutes, 40 rub/hour => 1/4 * 40 = 10 rub
                totalCost: '10.00',
                providerCurrency: mosProvider.currency,
                timeframes: [
                    {
                        start: timeframesData[0].start.toISOString(),
                        end: mockStopSessionData.stopTime.toJSDate().toISOString(),
                        cost: '10'
                    }
                ]
            });

            const {data: {rows: dbTimeframes}} = await dbClient.executeReadQuery({
                text: `SELECT timestamp_start, timestamp_end, cost FROM timeframes`
            });

            assert.deepStrictEqual(dbTimeframes, [
                {
                    timestamp_start: timeframesData[0].start,
                    timestamp_end: mockStopSessionData.stopTime.toJSDate(),
                    cost: '10'
                },
                {
                    timestamp_start: timeframesData[1].start,
                    timestamp_end: timeframesData[1].start,
                    cost: '0'
                }
            ]);
        });
    });
});
