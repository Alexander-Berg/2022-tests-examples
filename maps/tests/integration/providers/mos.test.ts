import assert from 'assert';
import {DateTime} from 'luxon';
import nock from 'nock';
import {SinonFakeTimers, useFakeTimers} from 'sinon';

import {cleanTestDb} from '../db-utils';
import {nockBalanceUrl, nockExtendSessionUrl, nockStartSessionUrl, nockStopSessionUrl} from '../nocks';
import {dbClient} from '../../../app/lib/db-client';
import {MoneyDecimal} from '../../../app/lib/decimal';
import {ErrorResult, mosProvider} from '../../../app/providers/mos';
import {invalidateZonesCache} from '../../../app/providers/zones';

describe('mos provider', () => {
    const now = new Date('2021-12-25T15:00:00.000Z'); // 18:00 in msk
    const providerCurrency = mosProvider.currency;
    const providerTimezone = mosProvider.timezone;
    const userPhone = '79000000000';
    let clock: SinonFakeTimers;

    before(() => {
        clock = useFakeTimers({now, toFake: ['Date']});
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(() => {
        clock.restore();
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await cleanTestDb();
        invalidateZonesCache();
    });

    afterEach(() => {
        const pendingMocks = nock.pendingMocks();
        if (pendingMocks.length) {
            throw new Error(`The following mocks were not used. Please check:\n${pendingMocks.join('\n')}`);
        }

        nock.cleanAll();
    });

    describe('checkPrice', () => {
        const placeId = '1234';

        beforeEach(async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    'mos',
                    placeId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: false}
                ]
            });
        });

        it('should return full price when the account balance is zero', async () => {
            nockBalanceUrl(200, 0);

            const actual = await mosProvider.checkPrice({
                placeId,
                userPhone,
                duration: 15
            });

            // 40 rub/hour => 15 minutes: 1/4 * 40 = 10 rub
            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    action: 'NEED_PAYMENT',
                    parkingPrice: '10',
                    balanceChargeAmount: '0',
                    topupPaymentAmount: '10',
                    providerCurrency
                }
            });
        });

        it('should return partial price when there is not enough money on the account balance', async () => {
            nockBalanceUrl(200, 3);

            const actual = await mosProvider.checkPrice({
                placeId,
                userPhone,
                duration: 15
            });

            // 40 rub/hour => 15 minutes: 1/4 * 40 = 10 rub
            // on balance 3 rub => topup = 7 rub
            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    action: 'NEED_PAYMENT',
                    parkingPrice: '10',
                    balanceChargeAmount: '3',
                    topupPaymentAmount: '7',
                    providerCurrency
                }
            });
        });

        it('should return `DO_NOT_CHARGE` when there is enough money on the account balance', async () => {
            nockBalanceUrl(200, 20);

            const actual = await mosProvider.checkPrice({
                placeId,
                userPhone,
                duration: 15
            });

            // 40 rub/hour => 15 minutes: 1/4 * 40 = 10 rub
            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    action: 'DO_NOT_CHARGE',
                    balanceChargeAmount: '10'
                }
            });
        });

        it('should return `PARK_FOR_FREE` when today is a weekend', async () => {
            nockBalanceUrl(200, 0);

            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [DateTime.now().toFormat('yyyy-MM-dd')]
            });

            const actual = await mosProvider.checkPrice({
                placeId,
                userPhone,
                duration: 15
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    action: 'PARK_FOR_FREE',
                    freeTimeEnd: DateTime.now().plus({hour: 24}).startOf('day')
                }
            });
        });

        it('should return correct price for extend parking', async () => {
            nockBalanceUrl(200, 10);
            const providerParkingId = '5678';

            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    'mos',
                    providerParkingId,
                    JSON.stringify([
                        {
                            from: '08:00',
                            to: '21:00',
                            mainPrice: {
                                value: 40,
                                duration: 60
                            }
                        },
                        {
                            from: '21:00',
                            to: '08:00',
                            mainPrice: {
                                value: 60,
                                duration: 60
                            }
                        }
                    ]),
                    {isRaisedTariff: true}
                ]
            });

            const startSession = DateTime.now().minus({minute: 30}).toJSDate();

            const actual = await mosProvider.checkPrice(
                {
                    placeId: providerParkingId,
                    userPhone,
                    duration: 60
                },
                {
                    id: '1',
                    public_id: '030a5aea-a06c-4334-9949-f1fc979fb81f',
                    active: true,
                    created_at: startSession,
                    updated_at: startSession,
                    user_uid: '111',
                    user_phone: '70000000000',
                    vehicle_plate: 'A111AA11',
                    provider: 'mos',
                    provider_parking_id: providerParkingId,
                    provider_session_id: '111111',
                    app_id: 'ru.yandex.traffic',
                    timeframes: [
                        {
                            timestamp_start: startSession,
                            timestamp_end: DateTime.now().plus({hour: 2, minute: 30}).toJSDate(),
                            cost: '120' // parking for 3 hours
                        }
                    ]
                }
            );

            // after the extension, we get 30 minutes from one interval (40 rub/hour)
            // and 30 minutes from another (60 rub/hour) =>
            // 20 + 30 = 50 rub

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    action: 'NEED_PAYMENT',
                    parkingPrice: '50',
                    balanceChargeAmount: '10',
                    topupPaymentAmount: '40',
                    providerCurrency
                }
            });
        });

        it('should return status = error when parking is not available', async () => {
            const actual = await mosProvider.checkPrice({
                placeId: '5678',
                userPhone,
                duration: 15
            });

            assert.deepStrictEqual(actual, {
                status: 'error',
                type: 'failed',
                message: 'Parking is not available',
                errors: []
            });
        });
    });

    describe('getParkingDurationAndPrice', () => {
        const placeId = '1234';
        const userPhone = '79000000000';

        beforeEach(async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    'mos',
                    placeId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: false}
                ]
            });
        });

        it('should return status = error when getting balance was failed', async () => {
            nockBalanceUrl(500);
            const actual = await mosProvider.getParkingDurationAndPrice({
                placeId,
                userPhone,
                duration: 15,
                start: new Date()
            });

            assert.strictEqual(actual.status, 'error');
            assert.strictEqual((actual as ErrorResult).type, 'unexpected');
        });

        it('should return status = error when parking zone does not exist', async () => {
            nockBalanceUrl(200, 20);
            const actual = await mosProvider.getParkingDurationAndPrice({
                placeId: '5678',
                userPhone,
                duration: 15,
                start: new Date()
            });

            assert.deepStrictEqual(actual, {
                status: 'error',
                type: 'failed',
                message: 'Parking is not available',
                errors: []
            });
        });

        it('should return price for the entire duration ' +
            'when there is enough money on the account balance', async () => {
            nockBalanceUrl(200, 100);
            const duration = 30;

            const actual = await mosProvider.getParkingDurationAndPrice({
                placeId,
                userPhone,
                duration,
                start: new Date()
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    duration,
                    price: new MoneyDecimal(20)
                }
            });
        });

        it('should return partial price and duration ' +
            'when there is not enough money for the entire duration of parking', async () => {
            nockBalanceUrl(200, 18);
            const duration = 30;

            const actual = await mosProvider.getParkingDurationAndPrice({
                placeId,
                userPhone,
                duration,
                start: new Date()
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    duration: 27,
                    price: new MoneyDecimal(18)
                }
            });
        });

        it('should return status = error ' +
            'when there is not enough money for parking with a five-minute interval', async () => {
            nockBalanceUrl(200, 10);
            const actual = await mosProvider.getParkingDurationAndPrice({
                placeId,
                userPhone,
                duration: 30,
                start: new Date()
            });

            assert.deepStrictEqual(actual, {
                status: 'error',
                type: 'failed',
                message: 'Not enough funds on the account balance',
                errors: []
            });
        });
    });

    describe('startParkingSession', () => {
        const placeId = '1234';
        const vehiclePlate = 'A101AA11';

        it('should return session data from ampp when start session was successful', async () => {
            const duration = 30;
            const mockSessionData = {
                id: '12345',
                placeId,
                startTime: DateTime.now(),
                stopTime: DateTime.now().plus({minute: duration}),
                carId: vehiclePlate
            };
            nockStartSessionUrl(200, mockSessionData);

            const actual = await mosProvider.startParkingSession({
                userPhone,
                vehiclePlate,
                parkingId: placeId,
                duration
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    sessionId: mockSessionData.id,
                    vehiclePlate: mockSessionData.carId,
                    parkingId: mockSessionData.placeId,
                    startTime: mockSessionData.startTime.setZone(providerTimezone),
                    stopTime: mockSessionData.stopTime.setZone(providerTimezone)
                }
            });
        });

        it('should return status = error when start session is failed', async () => {
            nockStartSessionUrl(500);

            const actual = await mosProvider.startParkingSession({
                userPhone,
                vehiclePlate,
                parkingId: placeId,
                duration: 30
            });

            assert.strictEqual(actual.status, 'error');
        });
    });

    describe('stopParkingSession', () => {
        const placeId = '1234';
        const vehiclePlate = 'A101AA11';

        it('should return session data from ampp when stop session was successful', async () => {
            const duration = 30;
            const mockSessionData = {
                id: '12345',
                placeId,
                startTime: DateTime.now(),
                stopTime: DateTime.now().plus({minute: duration}),
                carId: vehiclePlate
            };
            nockStopSessionUrl(200, mockSessionData);

            const actual = await mosProvider.stopParkingSession({
                userPhone,
                vehiclePlate
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    sessionId: mockSessionData.id,
                    vehiclePlate,
                    parkingId: placeId,
                    startTime: mockSessionData.startTime.setZone(providerTimezone),
                    stopTime: mockSessionData.stopTime.setZone(providerTimezone)
                }
            });
        });

        it('should return status = error when stop session is failed', async () => {
            nockStopSessionUrl(500);

            const actual = await mosProvider.stopParkingSession({
                userPhone,
                vehiclePlate
            });

            assert.strictEqual(actual.status, 'error');
        });
    });

    describe('extendParkingSession', () => {
        const placeId = '1234';
        const vehiclePlate = 'A101AA11';

        it('should return session data from ampp when extend session was successful', async () => {
            const duration = 30;
            const mockSessionData = {
                id: '12345',
                placeId,
                startTime: DateTime.now(),
                stopTime: DateTime.now().plus({minute: duration}),
                carId: vehiclePlate
            };
            nockExtendSessionUrl(200, mockSessionData);

            const actual = await mosProvider.extendParkingSession({
                userPhone,
                vehiclePlate,
                duration
            });

            assert.deepStrictEqual(actual, {
                status: 'ok',
                payload: {
                    sessionId: mockSessionData.id,
                    vehiclePlate,
                    parkingId: placeId,
                    startTime: mockSessionData.startTime.setZone(providerTimezone),
                    stopTime: mockSessionData.stopTime.setZone(providerTimezone)
                }
            });
        });

        it('should return status = error when stop session is failed', async () => {
            nockExtendSessionUrl(500);

            const actual = await mosProvider.extendParkingSession({
                userPhone,
                vehiclePlate,
                duration: 30
            });

            assert.strictEqual(actual.status, 'error');
        });
    });
});
