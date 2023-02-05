import * as timeUtils from './time-utils';

describe('timeUtils', () => {
    describe('parse()', () => {
        it('should parse string in hh:mm format', () => {
            const time = timeUtils.parse('14:25');
            expect(time).toEqual({hours: 14, minutes: 25});
        });

        it('should parse string 24:00 in hh:mm format', () => {
            const time = timeUtils.parse('24:00');
            expect(time).toEqual({hours: 24, minutes: 0});
        });

        it('should parse string in hh:mm:ss format', () => {
            const time = timeUtils.parse('07:01:25');
            expect(time).toEqual({hours: 7, minutes: 1});
        });

        it('should return null for invalid format', () => {
            expect(timeUtils.parse('')).toBeNull();
            expect(timeUtils.parse('1:a')).toBeNull();
            expect(timeUtils.parse('1')).toBeNull();
            expect(timeUtils.parse('1:2')).toBeNull();
            expect(timeUtils.parse('01')).toBeNull();
            expect(timeUtils.parse('01:2')).toBeNull();
            expect(timeUtils.parse('12:0')).toBeNull();

            expect(timeUtils.parse('24:05')).toBeNull();
            expect(timeUtils.parse('23:60')).toBeNull();
            expect(timeUtils.parse('-01:00')).toBeNull();
            expect(timeUtils.parse('00:-01')).toBeNull();
        });
    });

    describe('toString()', () => {
        it('should return string representation of time in hh:mm format', () => {
            const time1 = {hours: 1, minutes: 2};
            expect(timeUtils.toString(time1)).toBe('01:02');

            const time2 = {hours: 12, minutes: 0};
            expect(timeUtils.toString(time2)).toBe('12:00');

            const time3 = {hours: 13, minutes: 25};
            expect(timeUtils.toString(time3)).toBe('13:25');
        });
    });

    describe('diff()', () => {
        it('should compare', () => {
            const time1 = {hours: 1, minutes: 1};
            let time2 = {hours: 1, minutes: 1};
            expect(timeUtils.diff(time1, time2)).toBe(0);
            expect(timeUtils.diff(time2, time1)).toBe(0);

            time2 = {hours: 2, minutes: 1};
            expect(timeUtils.diff(time1, time2)).toBe(-60);
            expect(timeUtils.diff(time2, time1)).toBe(60);

            time2 = {hours: 1, minutes: 2};
            expect(timeUtils.diff(time1, time2)).toBe(-1);
            expect(timeUtils.diff(time2, time1)).toBe(1);
        });
    });

    describe('stringToMinutes()', () => {
        it('should convert', () => {
            expect(timeUtils.stringToMinutes('1:01')).toBe(61);
            expect(timeUtils.stringToMinutes('01:01')).toBe(61);

            expect(timeUtils.stringToMinutes('1:00')).toBe(60);
            expect(timeUtils.stringToMinutes('13:00')).toBe(780);

            expect(timeUtils.stringToMinutes('0:00')).toBe(0);
            expect(timeUtils.stringToMinutes('24:00')).toBe(1440);
        });
    });
});
