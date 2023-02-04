import * as assert from 'assert';
import {
    simplifyScheduleDatetimes,
    extendScheduleDatetimes
} from '../../server/events/tz-utils';

import {IntervalSchedule} from '../../server/events/interfaces/schedules';

function getIntervalSchedule(datetimeFrom: string, datetimeTo: string): IntervalSchedule {
    return <IntervalSchedule>{
        type: 'Interval',
        from: datetimeFrom,
        to: datetimeTo
    };
}

function assertNotNan(actual: Number): void {
    assert.notStrictEqual(actual, NaN);
}

// A timezone without daylight saving time (DST)
const omskLocation = 'Asia/Omsk';
const omskUtcOffset = '+06:00';

// A timezone with DST
const nyLocation = 'America/New_York';
const nyUtcOffset = '-05:00';
const nyUtcOffsetDst = '-04:00';

describe('Utility functions for working with timezones in events', () => {
    describe('Simplify datetimes', () => {
        it('should simplify dates', () => {
            const datetimeFrom = '2019-02-19T02:20';
            const datetimeFromWithTz = datetimeFrom + omskUtcOffset;
            const datetimeTo = '2019-02-28T02:20';
            const datetimeToWithTz = datetimeTo + omskUtcOffset;

            const containerWithTimezones = getIntervalSchedule(datetimeFromWithTz, datetimeToWithTz);
            const containerWithoutTimezones = <IntervalSchedule>simplifyScheduleDatetimes(
                containerWithTimezones,
                omskLocation
            );

            assertNotNan(Date.parse(datetimeFrom));
            assertNotNan(Date.parse(datetimeTo));

            assert.strictEqual(
                // Using Date.parse, to take into account differences
                // in the output format and the way the test data was written.
                // see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/parse
                Date.parse(containerWithoutTimezones.from),
                Date.parse(datetimeFrom)
            );

            assert.strictEqual(
                Date.parse(containerWithoutTimezones.to),
                Date.parse(datetimeTo)
            );
        });

        // On 10.03.2019 at 01:59:59 the location America/New_York transitions to daylight saving time (DST).
        // So the time transitions from 01:59:59 to 03:00:00. And the UTC offset changes from -05:00 to -04:00.
        it('should simplify dates around daylight saving time transitions', () => {
            const datetimeFrom = '2019-03-10T01:20';
            const datetimeFromWithTz = datetimeFrom + nyUtcOffset;
            const datetimeTo = '2019-03-31T03:20';
            // This time is already after the DST transtion so it should be with the DST offset.
            const datetimeToWithTz = datetimeTo + nyUtcOffsetDst;

            const containerWithTimezones = getIntervalSchedule(datetimeFromWithTz, datetimeToWithTz);
            const containerWithoutTimezones = <IntervalSchedule>simplifyScheduleDatetimes(
                containerWithTimezones,
                nyLocation
            );

            assertNotNan(Date.parse(datetimeFrom));
            assertNotNan(Date.parse(datetimeTo));

            assert.strictEqual(
                Date.parse(containerWithoutTimezones.from),
                Date.parse(datetimeFrom)
            );

            assert.strictEqual(
                Date.parse(containerWithoutTimezones.to),
                Date.parse(datetimeTo)
            );
        });

        it('should throw if the datetimes after conversion do not match the expected results', () => {
            // The interval from 02:00:00 to 02:59:59 do not exist in location America/New_York on 10.03.2019.
            // Timestamps that point to a point in that interval will get an hour added to them.
            const datetimeFrom = '2019-03-10T02:30'; // This will be converted 03:30, which is after the "to" time
            const datetimeFromWithTz = datetimeFrom + nyUtcOffset;
            const datetimeTo = '2019-03-10T03:20';
            // This time is already after the DST transtion so it should be with the DST offset.
            const datetimeToWithTz = datetimeTo + nyUtcOffsetDst;

            const containerWithTimezones = getIntervalSchedule(datetimeFromWithTz, datetimeToWithTz);
            assert.throws(() => {
                simplifyScheduleDatetimes(containerWithTimezones, nyLocation);
            });
        });

        it('should convert and simplify datetime strings in different timezones to the given location', () => {
            const datetimeFromInUtc = '2019-02-19T07:00+00:00';
            const datetimeFromInOmsk = '2019-02-19T13:00';
            const datetimeToInUtc = '2019-02-19T08:00+00:00';
            const datetimeToInOmsk = '2019-02-19T14:00';

            const containerWithTimezones = getIntervalSchedule(datetimeFromInUtc, datetimeToInUtc);

            const containerWithoutTimezones = <IntervalSchedule>simplifyScheduleDatetimes(
                containerWithTimezones,
                omskLocation
            );

            assertNotNan(Date.parse(datetimeFromInOmsk));
            assertNotNan(Date.parse(datetimeToInOmsk));

            assert.strictEqual(
                Date.parse(containerWithoutTimezones.from),
                Date.parse(datetimeFromInOmsk)
            );

            assert.strictEqual(
                Date.parse(containerWithoutTimezones.to),
                Date.parse(datetimeToInOmsk)
            );
        });
    });

    describe('Datetime conversion to the extended format', () => {
        it('should extend dates with timezones', () => {
            const datetimeFrom = '2019-02-19T02:20';
            const datetimeFromWithTz = datetimeFrom + omskUtcOffset;
            const datetimeTo = '2019-02-28T02:20';
            const datetimeToWithTz = datetimeTo + omskUtcOffset;

            const containerWithoutTimezones = getIntervalSchedule(datetimeFrom, datetimeTo);
            const containerWithTimezones = <IntervalSchedule>extendScheduleDatetimes(
                containerWithoutTimezones,
                omskLocation
            );

            assertNotNan(Date.parse(datetimeFromWithTz));
            assertNotNan(Date.parse(datetimeToWithTz));

            assert.strictEqual(
                Date.parse(containerWithTimezones.from),
                Date.parse(datetimeFromWithTz)
            );

            assert.strictEqual(
                Date.parse(containerWithTimezones.to),
                Date.parse(datetimeToWithTz)
            );
        });

        it('should extend dates around daylight saving time transitions', () => {
            const datetimeFrom = '2019-03-10T01:20';
            const datetimeFromWithTz = datetimeFrom + nyUtcOffset;
            const datetimeTo = '2019-03-10T03:20';
            // This time is already after the DST transtion so it should be with the DST offset.
            const datetimeToWithTz = datetimeTo + nyUtcOffsetDst;

            const containerWithoutTimezones = getIntervalSchedule(datetimeFrom, datetimeTo);
            const containerWithTimezones = <IntervalSchedule>extendScheduleDatetimes(
                containerWithoutTimezones,
                nyLocation
            );

            assertNotNan(Date.parse(datetimeFromWithTz));
            assertNotNan(Date.parse(datetimeToWithTz));

            assert.strictEqual(
                Date.parse(containerWithTimezones.from),
                Date.parse(datetimeFromWithTz)
            );

            assert.strictEqual(
                Date.parse(containerWithTimezones.to),
                Date.parse(datetimeToWithTz)
            );
        });

        it('should convert keep the timezones to the given location', () => {
            const datetimeFromInUtc = '2019-02-19T07:00+00:00';
            const datetimeFromInOmsk = '2019-02-19T13:00+06:00';
            const datetimeToInUtc = '2019-02-19T08:00+00:00';
            const datetimeToInOmsk = '2019-02-19T14:00+06:00';

            const containerWithTimezonesInUtc = getIntervalSchedule(datetimeFromInUtc, datetimeToInUtc);

            const containerWithTimezonesInOmsk = <IntervalSchedule>extendScheduleDatetimes(
                containerWithTimezonesInUtc,
                omskLocation
            );

            assertNotNan(Date.parse(datetimeFromInOmsk));
            assertNotNan(Date.parse(datetimeToInOmsk));

            assert.strictEqual(
                Date.parse(containerWithTimezonesInOmsk.from),
                Date.parse(datetimeFromInOmsk)
            );

            assert.strictEqual(
                Date.parse(containerWithTimezonesInOmsk.to),
                Date.parse(datetimeToInOmsk)
            );
        });
    });
});
