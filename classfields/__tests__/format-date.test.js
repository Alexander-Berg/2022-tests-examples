/* eslint-disable no-undef */
import MockDate from 'mockdate';
import i18n from '../../i18n';
import {
    formatMonth,
    formatDate,
    formatTime,
    formatWeekDay,
    formatTimeDuration,
    formatTimeDiff,
    daysDiff,
    roundTime,
    day,
    hour,
    minute,
    diffMoreThanTwoDays,
    formatPeriod,
    daysAgo,
    formatDateDiff
} from '../../format-date';

describe('format', () => {
    beforeEach(() => {
        MockDate.set('01-01-2018');
        i18n.setLang('ru');
    });

    describe('month', () => {
        it('returns user-frendly month string', () => {
            const jan = new Date('2017-01-08T11:01:28.302+03:00');
            const dec = new Date('2017-12-08T11:01:28.302+03:00');

            expect(formatMonth(jan)).toEqual('январь');
            expect(formatMonth(dec)).toEqual('декабрь');
        });

        it('returns user-frendly declension month string if second param is truthy', () => {
            const jan = new Date('2017-01-08T11:01:28.302+03:00');
            const dec = new Date('2017-12-08T11:01:28.302+03:00');

            expect(formatMonth(jan, true)).toEqual('января');
            expect(formatMonth(dec, true)).toEqual('декабря');
        });
    });

    describe('date', () => {
        it('returns data in format "DD MMMM"', () => {
            const date = new Date('2017-07-03T11:01:28.302+03:00');

            expect(formatDate(date)).toEqual('3\u00a0июля');
        });

        it('returns data in format "DD MMMM YYYY" if withYear option is truthy', () => {
            const date = new Date('2015-07-03T11:01:28.302+03:00');

            expect(formatDate(date, { withYear: true })).toEqual('3\u00a0июля 2015');
        });

        it('returns data in format "DD MMM" if short option is truthy', () => {
            expect(formatDate('2015-07-03T11:01:28.302+03:00', { short: true })).toEqual('3\u00a0июля');
            expect(formatDate('2015-11-03T11:01:28.302+03:00', { short: true })).toEqual('3\u00a0ноя.');
        });

        it('returns data in format "DD MMM YYYY" if both options is truthy', () => {
            expect(
                formatDate('2015-07-03T11:01:28.302+03:00', { withYear: true, short: true })
            ).toEqual('3\u00a0июля 2015');
            expect(
                formatDate('2015-10-03T11:01:28.302+03:00', { withYear: true, short: true })
            ).toEqual('3\u00a0окт. 2015');
        });
    });

    describe('time', () => {
        it('returns time in format "HH:ss"', () => {
            const date = new Date('2017-07-03T06:01:28.302+03:00');

            expect(formatTime(date)).toEqual('06:01');
        });
    });

    describe('weekday', () => {
        it('returns user-frendly weekday string"', () => {
            const mon = new Date('2017-12-18T06:01:28.302+03:00');
            const tue = new Date('2017-12-19T06:01:28.302+03:00');

            expect(formatWeekDay(mon)).toEqual('понедельник');
            expect(formatWeekDay(tue)).toEqual('вторник');
        });
    });

    describe('timeDiff', () => {
        it('returns "N days left" if time is greater than 1 day', () => {
            const timeLeft1 = new Date('Dec 10 2017') - new Date('Dec 1 2017');
            const timeLeft2 = new Date('Dec 10 2017') - new Date('Dec 8 2017');
            const timeLeft3 = new Date('Dec 10 2017') - new Date('Dec 9 2017');

            expect(formatTimeDuration(timeLeft1)).toEqual({
                formatedTime: '9\u00a0дней',
                time: 9,
                period: 'day'
            });
            expect(formatTimeDuration(timeLeft2)).toEqual({
                formatedTime: '2\u00a0дня',
                time: 2,
                period: 'day'
            });
            expect(formatTimeDuration(timeLeft3)).toEqual({
                formatedTime: '1\u00a0день',
                time: 1,
                period: 'day'
            });
        });

        it('returns "N hours left" if time is greater than 1 hour but lesser than 1 day', () => {
            const timeLeft1 = new Date('Dec 21 2017 20:00') - new Date('Dec 21 2017 11:00');
            const timeLeft2 = new Date('Dec 21 2017 20:00') - new Date('Dec 21 2017 18:00');
            const timeLeft3 = new Date('Dec 21 2017 20:00') - new Date('Dec 21 2017 19:00');

            expect(formatTimeDuration(timeLeft1)).toEqual({
                formatedTime: '9\u00a0часов',
                time: 9,
                period: 'hour'
            });
            expect(formatTimeDuration(timeLeft2)).toEqual({
                formatedTime: '2\u00a0часа',
                time: 2,
                period: 'hour'
            });
            expect(formatTimeDuration(timeLeft3)).toEqual({
                formatedTime: '1\u00a0час',
                time: 1,
                period: 'hour'
            });
        });

        it('returns "N minute left" if time is greater than 1 minute but lesser than 1 hour', () => {
            const timeLeft1 = new Date('Dec 21 2017 01:00') - new Date('Dec 21 2017 00:51');
            const timeLeft2 = new Date('Dec 21 2017 01:00') - new Date('Dec 21 2017 00:58');
            const timeLeft3 = new Date('Dec 21 2017 01:00') - new Date('Dec 21 2017 00:59');

            expect(formatTimeDuration(timeLeft1)).toEqual({
                formatedTime: '9\u00a0минут',
                time: 9,
                period: 'minute'
            });
            expect(formatTimeDuration(timeLeft2)).toEqual({
                formatedTime: '2\u00a0минуты',
                time: 2,
                period: 'minute'
            });
            expect(formatTimeDuration(timeLeft3)).toEqual({
                formatedTime: 'сейчас',
                time: 0,
                period: 'now'
            });
        });

        it('displays lasting time in minutes', () => {
            const timeLeft1 = new Date('Dec 21 2017 01:00') - new Date('Dec 21 2017 00:39');

            expect(formatTimeDuration(timeLeft1, { isLasting: true })).toEqual({
                formatedTime: '21\u00a0минуту',
                time: 21,
                period: 'minute'
            });
        });

        it('returns "less than 1 minute" if time is lesser than 1 minute', () => {
            const timeLeft = new Date('Dec 21 2017 01:00') - new Date('Dec 21 2017 00:59:59');

            expect(formatTimeDuration(timeLeft)).toEqual({
                formatedTime: 'сейчас',
                time: 0,
                period: 'now'
            });
        });
    });

    describe('formatTimeDiff', () => {
        it('ignores time when difference is more than a day', () => {
            const diff1 = formatTimeDiff(new Date('Dec 01 2017'), new Date('Dec 10 2017'));
            const diff2 = formatTimeDiff(new Date('Dec 01 2017 23:59:59'), new Date('Dec 10 2017 00:00:00'));
            const diff3 = formatTimeDiff(new Date('Dec 01 2017 00:00:00'), new Date('Dec 10 2017 23:59:59'));

            expect(diff1).toEqual({
                formatedTime: '9\u00a0дней',
                time: 9,
                period: 'day'
            });
            expect(diff2).toEqual(diff1);
            expect(diff3).toEqual(diff1);
        });

        it('roundes down for minutes', () => {
            const diff = formatTimeDiff(new Date(0), new Date(60 * 60 * 1000 - 1000));

            expect(diff).toEqual({
                formatedTime: '59\u00a0минут',
                time: 59,
                period: 'minute'
            });
        });
    });
});

describe('daysDiff', () => {
    it('returns a days diff between two dates', () => {
        const start = new Date('Dec 1 2017');
        const end = new Date('Dec 17 2017');

        const result = daysDiff(start, end);

        expect(result).toBe(16);
    });

    it('ignores time', () => {
        const diff1 = daysDiff(new Date('Dec 1 2017 00:00:00'), new Date('Dec 17 2017 23:59:59'));
        const diff2 = daysDiff(new Date('Dec 1 2017 23:59:59'), new Date('Dec 17 2017 00:00:00'));

        expect(diff1).toBe(16);
        expect(diff2).toBe(diff1);
    });

    it('returns 0 if pass the same dates', () => {
        const start = new Date('Dec 1 2017');
        const end = new Date('Dec 1 2017');

        const result = daysDiff(start, end);

        expect(result).toBe(0);
    });

    it('returns a negative value if start date is greater than end date', () => {
        const start = new Date('Dec 10 2017');
        const end = new Date('Dec 1 2017');

        const result = daysDiff(start, end);

        expect(result).toBe(-9);
    });
});

describe('daysAgo', () => {
    it('returns “today” for the same day', () => {
        expect(daysAgo(new Date('Dec 18'), new Date('Dec 18'))).toBe('сегодня');
    });

    it('returns “yesterday” for the previous day', () => {
        expect(daysAgo(new Date('Dec 17'), new Date('Dec 18'))).toBe('вчера');
        expect(daysAgo(new Date('Dec 18 23:59'), new Date('Dec 19 00:01'))).toBe('вчера');
    });

    it('returns “day ago” if diff less then year', () => {
        expect(daysAgo(new Date('Dec 1'), new Date('Dec 3'))).toMatch('2');
        expect(daysAgo(new Date('Dec 1'), new Date('Dec 19'))).toMatch('18');
        expect(daysAgo(new Date('Jan 1'), new Date('March 18'))).toMatch('76');
        expect(daysAgo(new Date('Jan 5 2017'), new Date('Jan 4 2018'))).toMatch('364');
        expect(daysAgo(new Date('Jan 5 2016'), new Date('Jan 4 2017'))).toMatch('365');
    });

    it('returns date if diff is year or more', () => {
        expect(daysAgo(new Date('Jan 5 2017'), new Date('Jan 5 2018'))).toEqual('5\u00a0января 2017');
        expect(daysAgo(new Date('Jan 5 2016'), new Date('Jan 5 2017'))).toEqual('5\u00a0января 2016');
        expect(daysAgo(new Date('Dec 9 2016'), new Date('Dec 19 2017'))).toEqual('9\u00a0декабря 2016');
        expect(daysAgo(new Date('Dec 9 2016'), new Date('Dec 19 2020'))).toEqual('9\u00a0декабря 2016');
    });

    it('returns date if diff is days no less then specified', () => {
        expect(
            daysAgo(
                new Date('Jan 5 2017'),
                new Date('Jan 9 2017'),
                { shouldShowAsDate: diffMoreThanTwoDays, withYear: false }
            )
        ).toEqual('5\u00a0января');
    });

    it('returns yerstaday or today if diff is days no less then specified and less then 1', () => {
        expect(
            daysAgo(
                new Date('Jan 5 2017'),
                new Date('Jan 6 2017'),
                { shouldShowAsDate: () => true }
            )
        ).toEqual('вчера');
    });
});

describe('roundTime', () => {
    it('returns rounded to days date', () => {
        const date = '2018-05-16T10:46:15.103Z';

        const result = new Date(roundTime(date, day)).toISOString();

        expect(result).toBe('2018-05-16T00:00:00.000Z');
    });

    it('returns rounded to hours date', () => {
        const date = '2018-05-16T10:46:15.103Z';

        const result = new Date(roundTime(date, hour)).toISOString();

        expect(result).toBe('2018-05-16T10:00:00.000Z');
    });

    it('returns rounded to minutes date', () => {
        const date = '2018-05-16T10:46:15.103Z';

        const result = new Date(roundTime(date, minute)).toISOString();

        expect(result).toBe('2018-05-16T10:46:00.000Z');
    });
});

describe('formatPeriod', () => {
    describe('summary title', () => {
        it('shows single date', () => {
            expect(formatPeriod('2018-06-11T00:00:00+03:00', '2018-06-11T12:00:00+03:00')).toEqual('11\u00a0июня');
        });

        it('shows single date from another year', () => {
            expect(formatPeriod('2017-06-11T00:00:00+03:00', '2017-06-11T12:00:00+03:00')).toEqual('11\u00a0июня 2017');
            expect(
                formatPeriod('2017-09-11T00:00:00+03:00', '2017-09-11T12:00:00+03:00')
            ).toEqual('11\u00a0сентября 2017');
        });

        it('shows range from single month', () => {
            expect(formatPeriod('2018-06-11T00:00:00+03:00', '2018-06-15T12:00:00+03:00')).toEqual('11 — 15 июня');
        });

        it('shows range from single month from another year', () => {
            expect(formatPeriod('2017-06-11T00:00:00+03:00', '2017-06-15T12:00:00+03:00')).toEqual('11 — 15 июня 2017');
        });

        it('shows range from current year', () => {
            expect(
                formatPeriod('2018-05-11T00:00:00+03:00', '2018-06-11T12:00:00+03:00')
            ).toEqual('11\u00a0мая — 11\u00a0июня');
            expect(
                formatPeriod('2018-01-11T00:00:00+03:00', '2018-11-11T12:00:00+03:00')
            ).toEqual('11\u00a0января — 11\u00a0ноября');
        });

        it('shows range from different years', () => {
            expect(
                formatPeriod('2017-06-11T00:00:00+03:00', '2018-06-11T12:00:00+03:00')
            ).toEqual('11\u00a0июня 2017 — 11\u00a0июня 2018');
            expect(
                formatPeriod('2017-01-11T00:00:00+03:00', '2018-09-11T12:00:00+03:00')
            ).toEqual('11\u00a0янв. 2017 — 11\u00a0сен. 2018');
        });

        it('shows range from another year', () => {
            expect(
                formatPeriod('2017-05-11T00:00:00+03:00', '2017-06-11T12:00:00+03:00')
            ).toEqual('11\u00a0мая 2017 — 11\u00a0июня 2017');
            expect(
                formatPeriod('2017-01-11T00:00:00+03:00', '2017-09-11T12:00:00+03:00')
            ).toEqual('11\u00a0янв. 2017 — 11\u00a0сен. 2017');
        });
    });
});

describe('formatDateDiff', () => {
    it('correctly format diff less then month with flag', () => {
        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-06-02T03:00:00.000Z', { minPeriod: 'month' })
        ).toEqual('меньше месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-06-03T03:00:00.000Z', { minPeriod: 'month' })
        ).toEqual('меньше месяца');
    });

    it('correctly format diff in days', () => {
        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-06-02T03:00:00.000Z')
        ).toEqual('1\u00a0день');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-06-03T03:00:00.000Z')
        ).toEqual('2\u00a0дня');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-06-21T03:00:00.000Z')
        ).toEqual('20\u00a0дней');
    });

    it('correctly format diff in month', () => {
        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-07-01T03:00:00.000Z')
        ).toEqual('1\u00a0месяц');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-08-01T03:00:00.000Z')
        ).toEqual('2\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-08-11T03:00:00.000Z')
        ).toEqual('2\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2020-09-11T03:00:00.000Z')
        ).toEqual('3\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2021-04-01T03:00:00.000Z')
        ).toEqual('10\u00a0месяцев');
    });

    it('correctly format diff in only years', () => {
        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2021-06-01T03:00:00.000Z')
        ).toEqual('1\u00a0год');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2022-06-01T03:00:00.000Z')
        ).toEqual('2\u00a0года');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2025-06-01T03:00:00.000Z')
        ).toEqual('5\u00a0лет');
    });

    it('correctly format diff in years and month', () => {
        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2021-07-01T03:00:00.000Z')
        ).toEqual('1\u00a0год и 1\u00a0месяц');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2022-08-01T03:00:00.000Z')
        ).toEqual('2\u00a0года и 2\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2023-09-01T03:00:00.000Z')
        ).toEqual('3\u00a0года и 3\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2023-08-01T03:00:00.000Z')
        ).toEqual('3\u00a0года и 2\u00a0месяца');

        expect(
            formatDateDiff('2020-06-01T03:00:00.000Z', '2026-04-01T03:00:00.000Z')
        ).toEqual('5\u00a0лет и 10\u00a0месяцев');
    });
});
/* eslint-enable no-undef */
