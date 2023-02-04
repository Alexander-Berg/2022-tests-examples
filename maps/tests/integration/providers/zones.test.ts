import assert from 'assert';
import {SinonFakeTimers, useFakeTimers} from 'sinon';
import {DateTime} from 'luxon';

import {cleanTestDb} from '../db-utils';
import {getParkingPrice, ProviderData, invalidateZonesCache, getFreeTimeEndDate} from '../../../app/providers/zones';
import {dbClient} from '../../../app/lib/db-client';
import {MoneyDecimal} from '../../../app/lib/decimal';

describe('zones provider', () => {
    const now = new Date('2021-12-25T15:00:00.000Z'); // 18:00 in msk
    const providerData: ProviderData = {
        provider: 'mos',
        providerParkingId: '123',
        timezone: 'Europe/Moscow'
    };
    let clock: SinonFakeTimers;

    before(() => {
        clock = useFakeTimers({now, toFake: ['Date']});
    });

    after(() => {
        clock.restore();
    });

    beforeEach(async () => {
        await cleanTestDb();
        invalidateZonesCache();
    });

    describe('getParkingPrice', () => {
        it('should return `undefined` when zone is not found', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                    (provider, provider_parking_id) VALUES
                    ($1, '456')`, // another parking id
                values: [
                    providerData.provider
                ]
            });
            const actual = await getParkingPrice(providerData, now, 15);

            assert.strictEqual(actual, undefined);
        });

        it('should return `zero` price when zone\'s intervals are empty', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                    (provider, provider_parking_id, intervals) VALUES
                    ($1, $2, '[]')`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId
                ]
            });

            const actual = await getParkingPrice(providerData, now, 15);
            assert.ok(actual!.isEqualTo(new MoneyDecimal(0), 10));
        });

        it('should return the correct price when parking within one paid day', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals) VALUES
                ($1, $2, $3)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }])
                ]
            });

            const actual = await getParkingPrice(providerData, now, 15);
            // 40 rub/hour => 15 minutes: 1/4 * 40 = 10 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(10), 10));
        });

        it('should return zero price when parking on a weekend without raised tariff', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [DateTime.now().toFormat('yyyy-MM-dd')]
            });

            const actual = await getParkingPrice(providerData, now, 15);
            assert.ok(actual!.isEqualTo(new MoneyDecimal(0), 10));
        });

        it('should return the correct price when parking on a weekend with raised tariff', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: true}
                ]
            });
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [DateTime.now().toFormat('yyyy-MM-dd')]
            });

            const actual = await getParkingPrice(providerData, now, 15);
            // 40 rub/hour => 15 minutes: 1/4 * 40 = 10 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(10), 10));
        });

        it('should return zero price when parking on two weekends', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false),
                    ($2, false)`,
                values: [
                    DateTime.now().toFormat('yyyy-MM-dd'),
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd')
                ]
            });

            const actual = await getParkingPrice(providerData, now, 24 * 60); // parking for 24 hours
            assert.ok(actual!.isEqualTo(new MoneyDecimal(0), 10));
        });

        it('should not count the end parking time on the weekend', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd') // next day is weekend
                ]
            });

            const actual = await getParkingPrice(
                providerData,
                now, // 18:00 in msk
                24 * 60 // parking for 24 hours
            );
            // 40 rub/hour => paid time: 24 - 18 = 6 hours => 6 * 40 = 240 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(240), 10));
        });

        it('should not count the weekend time when parking is for two days', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd') // next day is weekend
                ]
            });

            const actual = await getParkingPrice(
                providerData,
                now, // 18:00 in msk
                2 * 24 * 60 // parking for 2 days
            );
            // paid time: today 24 - 18 = 6 hours + 18 hours from the day after tomorrow
            // 40 rub/hour => 6 * 40 + 18 * 40 = 960 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(960), 10));
        });

        it('should return the correct price considering the weekends and the raised tariff', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: true}
                ]
            });
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, true),
                    ($2, false),
                    ($3, true)`,
                values: [
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd'),
                    DateTime.now().plus({hour: 24 * 2}).toFormat('yyyy-MM-dd'),
                    DateTime.now().plus({hour: 24 * 4}).toFormat('yyyy-MM-dd')
                ]
            });

            const actual = await getParkingPrice(
                providerData,
                now,
                5 * 24 * 60 // parking for 5 days
            );
            // as a result, we get 3 paid days: 3 * 24 * 40 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(2880), 10));
        });

        it('should return the correct price considering the `first price` of tariff', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([
                        {
                            from: '08:00',
                            to: '21:00',
                            firstPrice: {
                                value: 10,
                                duration: 30
                            },
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

            const actual = await getParkingPrice(
                providerData,
                now, // 18:00
                24 * 60 // parking for 1 day
            );
            // as a result, we get 2 intervals with "first time"
            // first 30 minutes today and first 30 minutes tomorrow on 08:00 - 21:00
            // 10 rub/30 minutes => 2 * 10 = 20 rub
            // day time without "first time", 40 rub/hour:
            // 21:00 - 18:30 = 2:30 today and 18:00 - 8:30 = 9:30 tomorrow, 12 * 40 = 480 rub
            // night time, 60 rub/hour: (24 - 21) + (8 - 0) = 11 hours, 11 * 60 = 660 rub
            // 20 + 480 + 660 = 1160 rub
            assert.ok(actual!.isEqualTo(new MoneyDecimal(1160), 10));
        });
    });

    describe('getFreeTimeEndDate', () => {
        it('should return `undefined` when zone is not found', async () => {
            const actual = await getFreeTimeEndDate(providerData, now);
            assert.strictEqual(actual, undefined);
        });

        it('should return `undefined` when zone\'s intervals are empty', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                    (provider, provider_parking_id, intervals) VALUES
                    ($1, $2, '[]')`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId
                ]
            });

            const actual = await getFreeTimeEndDate(providerData, now);
            assert.strictEqual(actual, undefined);
        });

        it('should return `undefined` when now is not free day', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd') // tomorrow is weekend
                ]
            });

            const actual = await getFreeTimeEndDate(providerData, now);
            assert.strictEqual(actual, undefined);
        });

        it('should return `undefined` when now is weekend with raised tariff', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: true}
                ]
            });
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false)`,
                values: [
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd') // tomorrow is weekend
                ]
            });

            const actual = await getFreeTimeEndDate(providerData, now);
            assert.strictEqual(actual, undefined);
        });

        it('should return correct time when there are several weekends with different raised tariff flag', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
                    JSON.stringify([{
                        from: '00:00',
                        to: '24:00',
                        mainPrice: {
                            value: 40,
                            duration: 60
                        }
                    }]),
                    {isRaisedTariff: true}
                ]
            });
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, true),
                    ($2, false)`,
                values: [
                    // today is free weekend
                    DateTime.now().toFormat('yyyy-MM-dd'),
                    // tomorrow is weekend only for not raised tariff
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd')
                ]
            });

            const actual = await getFreeTimeEndDate(providerData, now);
            const expected = DateTime.now().plus({hour: 24}).startOf('day'); // start of the next day
            assert.deepEqual(actual, expected);
        });

        it('should return correct time when there are several weekends', async () => {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO zones
                (provider, provider_parking_id, intervals, attributes) VALUES
                ($1, $2, $3, $4)`,
                values: [
                    providerData.provider,
                    providerData.providerParkingId,
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
            await dbClient.executeWriteQuery({
                text: `INSERT INTO mos_weekends
                    (day, is_raised_tariff_free) VALUES
                    ($1, false),
                    ($2, false),
                    ($3, false),
                    ($4, false)`,
                values: [
                    DateTime.now().minus({hour: 24}).toFormat('yyyy-MM-dd'), // previous weekend
                    DateTime.now().toFormat('yyyy-MM-dd'),
                    DateTime.now().plus({hour: 24}).toFormat('yyyy-MM-dd'),
                    DateTime.now().plus({hour: 24 * 4}).toFormat('yyyy-MM-dd') // after 3 days
                ]
            });

            const actual = await getFreeTimeEndDate(providerData, now);
            const expected = DateTime.now().plus({hour: 48}).startOf('day'); // start of the day after tomorrow
            assert.deepEqual(actual, expected);
        });
    });
});
