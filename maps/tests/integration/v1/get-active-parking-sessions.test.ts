import assert from 'assert';
import * as got from 'got';
import {DateTime} from 'luxon';
import nock from 'nock';
import {URL} from 'url';
import {app} from '../../../app/app';
import {mosProvider} from '../../../app/providers/mos';
import {invalidateZonesCache} from '../../../app/providers/zones';
import {cleanTestDb, insertSession, SessionData, Timeframe} from '../db-utils';
import {nockBlackboxUrl} from '../nocks';
import {client, TestServer} from '../test-server';
import {TvmDaemon} from '../tvm-daemon';

type SessionWithTimeframes = {
    session: SessionData;
    timeframes: Timeframe[];
};

describe('/v1/get_active_parking_sessions', () => {
    let url: URL;
    let server: TestServer;
    let tvmDaemon: TvmDaemon;
    let blackboxNock: nock.Scope;
    const uid = '111';
    const phone = '123456789';
    const appId = 'ru.yandex.traffic';
    const mosTimezone = mosProvider.timezone;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/get_active_parking_sessions`);
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await cleanTestDb();
        invalidateZonesCache();
        blackboxNock = nockBlackboxUrl({
            users: [{
                uid: {value: uid},
                phones: [{
                    attributes: {
                        108: '1',
                        102: '123456789'
                    }
                }]
            }]
        });
    });

    afterEach(() => {
        assert.ok(blackboxNock.isDone());
        nock.cleanAll();
    });

    async function request(): Promise<got.Response<any>> {
        return client.get(url, {
            headers: {
                'X-Ya-User-Ticket': 'stub'
            },
            throwHttpErrors: false,
            responseType: 'json',
            retry: 0
        });
    }

    it('should return all user\'s active sessions', async () => {
        const firstSessionData: SessionWithTimeframes = {
            session: {
                uid,
                phone,
                active: true,
                vehiclePlate: 'A111AA11',
                provider: 'mos',
                providerParkingId: '1234',
                providerSessionId: '11111',
                appId: 'ru.yandex.traffic',
                publicId: '11111111-a06c-4334-9949-f1fc979fb81f'
            },
            timeframes: [
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
            ]
        };
        const secondSessionData: SessionWithTimeframes = {
            session: {
                uid,
                phone,
                active: true,
                vehiclePlate: 'A222AA22',
                provider: 'mos',
                providerParkingId: '5678',
                providerSessionId: '22222',
                appId,
                publicId: '22222222-a06c-4334-9949-f1fc979fb81f'
            },
            timeframes: [
                {
                    start: DateTime.now().minus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    end: DateTime.now().plus({minute: 15}).setZone(mosTimezone).toJSDate(),
                    cost: '10'
                }
            ]
        };
        const thirdSessionData = {
            session: {
                uid,
                phone,
                active: false,
                vehiclePlate: 'A333AA33',
                provider: 'mos',
                providerParkingId: '9101',
                providerSessionId: '11111',
                appId,
                publicId: '33333333-a06c-4334-9949-f1fc979fb81f'
            } as SessionData
        };
        await insertSession(firstSessionData.session, firstSessionData.timeframes);
        await insertSession(secondSessionData.session, secondSessionData.timeframes);
        await insertSession(thirdSessionData.session);

        const res = await request();
        assert.strictEqual(res.statusCode, 200);

        delete res.body.sessions[0].createdAt;
        delete res.body.sessions[1].createdAt;
        assert.deepStrictEqual(res.body, {
            sessions: [
                {
                    id: firstSessionData.session.publicId,
                    provider: firstSessionData.session.provider,
                    providerParkingId: firstSessionData.session.providerParkingId,
                    vehiclePlate: firstSessionData.session.vehiclePlate,
                    userPhone: phone,
                    providerTzdbLocation: mosTimezone,
                    totalCost: '30.00',
                    providerCurrency: mosProvider.currency,
                    timeframes: [
                        {
                            start: firstSessionData.timeframes[0].start.toISOString(),
                            end: firstSessionData.timeframes[0].end.toISOString(),
                            cost: firstSessionData.timeframes[0].cost
                        },
                        {
                            start: firstSessionData.timeframes[1].start.toISOString(),
                            end: firstSessionData.timeframes[1].end.toISOString(),
                            cost: firstSessionData.timeframes[1].cost
                        }
                    ]
                },
                {
                    id: secondSessionData.session.publicId,
                    provider: secondSessionData.session.provider,
                    providerParkingId: secondSessionData.session.providerParkingId,
                    vehiclePlate: secondSessionData.session.vehiclePlate,
                    userPhone: phone,
                    providerTzdbLocation: mosTimezone,
                    totalCost: '10.00',
                    providerCurrency: mosProvider.currency,
                    timeframes: [
                        {
                            start: secondSessionData.timeframes[0].start.toISOString(),
                            end: secondSessionData.timeframes[0].end.toISOString(),
                            cost: '10'
                        }
                    ]
                }
            ]
        });
    });

    it('should return empty list when user does not have active sessions', async () => {
        await insertSession({
            uid: '222', // another uid
            phone: '987654321',
            active: true,
            vehiclePlate: 'A111AA11',
            provider: 'mos',
            providerParkingId: '1234',
            providerSessionId: '11111',
            appId,
            publicId: '030a5aea-a06c-4334-9949-f1fc979fb81f'
        });

        const res = await request();
        assert.strictEqual(res.statusCode, 200);
        assert.deepStrictEqual(res.body, {sessions: []});
    });
});
