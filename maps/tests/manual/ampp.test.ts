import {strict as assert} from 'assert';
import {DateTime} from 'luxon';

import {amppProvider} from './providers/ampp';

const DEBUG_SUBSCRIBER = '79853960743'; // erudeva@
const PARKING_ID = '4020';
const CAR_NUMBER = 'ABC111';
const MINUTES_TO_PARK = 60;

describe('ampp', () => {
    it('/account?action=info', async () => {
        const info = await amppProvider.getAccountInfo(DEBUG_SUBSCRIBER);
        assert.ok(info);
    });

    it('/account?action=balance', async () => {
        const balance = await amppProvider.getAccountBalance(DEBUG_SUBSCRIBER);
        assert.ok(balance > 10);
    });

    it('/payment', async () => {
        const previousBalance = await amppProvider.getAccountBalance(DEBUG_SUBSCRIBER);
        const AMOUNT = '1';
        const result = await amppProvider.topUpBalance(DEBUG_SUBSCRIBER, AMOUNT, `77777${Date.now()}`);
        assert.equal(result.funds, previousBalance + parseInt(AMOUNT, 10));
    });

    it('/parking?action=start', async () => {
        const now = new Date();

        const result = await amppProvider.startParkingSession(
            DEBUG_SUBSCRIBER,
            PARKING_ID,
            CAR_NUMBER,
            MINUTES_TO_PARK.toString()
        );
        const session = await amppProvider.getCurrentParkingSession(DEBUG_SUBSCRIBER);

        assert.ok(session);
        assert.deepStrictEqual(session, result);
        assert.strictEqual(result.parking.placeId, PARKING_ID);
        assert.strictEqual(result.parking.carId, CAR_NUMBER);

        const expectedStartTimeFormatted = DateTime.fromJSDate(now).toFormat('yyyy-MM-dd HH:mm');
        const expectedStopTimeFormatted = DateTime
            .fromJSDate(now)
            .plus({minutes: MINUTES_TO_PARK})
            .toFormat('yyyy-MM-dd HH:mm');
        const startTimeFormatted = DateTime.fromJSDate(new Date(result.parking.startTime)).toFormat('yyyy-MM-dd HH:mm');
        const stopTimeFormatted = DateTime.fromJSDate(new Date(result.parking.stopTime)).toFormat('yyyy-MM-dd HH:mm');

        assert.strictEqual(startTimeFormatted, expectedStartTimeFormatted);
        assert.strictEqual(stopTimeFormatted, expectedStopTimeFormatted);
    });

    it('/parking?action=start (second execution)', async () => {
        await assert.rejects(async () => {
            await amppProvider.startParkingSession(
                DEBUG_SUBSCRIBER,
                PARKING_ID,
                CAR_NUMBER,
                MINUTES_TO_PARK.toString()
            );
        });
    });

    it('/parking?action=extend', async () => {
        const session = await amppProvider.getCurrentParkingSession(DEBUG_SUBSCRIBER);
        const extendedSession = await amppProvider.extendParkingSession(
            DEBUG_SUBSCRIBER,
            CAR_NUMBER,
            MINUTES_TO_PARK.toString()
        );

        assert.notDeepStrictEqual(session, extendedSession);

        const sessionStopTime = DateTime.fromJSDate(new Date(session.parking.stopTime)).toFormat('yyyy-MM-dd HH:mm');
        const extendedSessionStartTime = DateTime.fromJSDate(new Date(extendedSession.parking.startTime))
            .toFormat('yyyy-MM-dd HH:mm');
        const extendedSessionStopTime = DateTime.fromJSDate(new Date(extendedSession.parking.stopTime))
            .toFormat('yyyy-MM-dd HH:mm');
        const expectedExtendedStopTime = DateTime.fromJSDate(new Date(extendedSession.parking.startTime))
            .plus({minutes: MINUTES_TO_PARK})
            .toFormat('yyyy-MM-dd HH:mm');

        assert.strictEqual(extendedSessionStartTime, sessionStopTime);
        assert.strictEqual(extendedSessionStopTime, expectedExtendedStopTime);
    });

    it('/parking?action=extend (second execution)', async () => {
        await assert.rejects(async () => {
            await amppProvider.extendParkingSession(DEBUG_SUBSCRIBER, CAR_NUMBER, MINUTES_TO_PARK.toString());
        });
    });

    it('/parking?action=check', async () => {
        const result = await amppProvider.getCurrentParkingSession(DEBUG_SUBSCRIBER);
        assert.ok(result);
    });

    it('/parking?action=stop', async () => {
        await amppProvider.stopParkingSession(DEBUG_SUBSCRIBER, CAR_NUMBER);
        const session = await amppProvider.getCurrentParkingSession(DEBUG_SUBSCRIBER);
        assert.ok(!session);
    });

    it('/parking?action=stop (second execution)', async () => {
        await assert.rejects(async () => {
            await amppProvider.stopParkingSession(DEBUG_SUBSCRIBER, CAR_NUMBER);
        });
    });
});
