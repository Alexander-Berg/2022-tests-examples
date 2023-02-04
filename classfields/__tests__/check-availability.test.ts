import MockDate from 'mockdate';

import { checkAvailability } from '../check-availability';

import { simpleData, complicatedTimePatternData, oneDayData, roundTheClockData, simpleDataLateClosing } from './mocks';

describe('checkAvailability', () => {
    it('открыто', () => {
        MockDate.set('2020-08-24T10:00');

        expect(checkAvailability(simpleData)).toEqual({
            isOpened: true,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '18:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '10:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('закрыто, будет открыто сегодня', () => {
        MockDate.set('2020-08-24T08:00');

        expect(checkAvailability(simpleData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '18:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '10:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('закрыто, будет открыто завтра', () => {
        MockDate.set('2020-08-27T20:00');

        expect(checkAvailability(simpleData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 5,
                time: '18:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 5,
                time: '10:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('закрыто, будет открыто с начала периода', () => {
        MockDate.set('2020-08-28T20:00');

        expect(checkAvailability(simpleData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '18:00',
                isTodayOrNextDay: false,
            },
            willOpen: {
                day: 1,
                time: '10:00',
                isTodayOrNextDay: false,
            },
        });
    });

    it('закрыто на обед, откроется сегодня', () => {
        MockDate.set('2020-08-24T15:00');

        expect(checkAvailability(complicatedTimePatternData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '18:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '16:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('закрыто, откроется сегодня, время с обедом', () => {
        MockDate.set('2020-08-24T08:00');

        expect(checkAvailability(complicatedTimePatternData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '14:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '10:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('открыто, время с обедом', () => {
        MockDate.set('2020-08-24T11:00');

        expect(checkAvailability(complicatedTimePatternData)).toEqual({
            isOpened: true,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '14:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '10:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('корректно отрабатывает с выходными днями', () => {
        MockDate.set('2020-08-24T10:00');

        expect(checkAvailability(oneDayData)).toEqual({
            isOpened: false,
            roundTheClock: false,
            willClose: {
                day: 3,
                time: '18:00',
                isTodayOrNextDay: false,
            },
            willOpen: {
                day: 3,
                time: '10:00',
                isTodayOrNextDay: false,
            },
        });
    });

    it('открыто круглые сутки', () => {
        MockDate.set('2020-08-24T10:00');

        expect(checkAvailability(roundTheClockData)).toEqual({
            isOpened: true,
            roundTheClock: true,
            willClose: {
                day: 1,
                time: '23:59',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '00:00',
                isTodayOrNextDay: true,
            },
        });
    });
});

describe('checkAvailability в чужом регионе', () => {
    const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
    beforeAll(() => {
        global.Date.prototype.getTimezoneOffset = jest.fn(() => -360);
    });

    afterAll(() => {
        global.Date.prototype.getTimezoneOffset = originalGetTimezoneOffset;
    });

    it('открыто', () => {
        MockDate.set('2020-08-24T11:00');

        expect(checkAvailability(simpleData)).toEqual({
            isOpened: true,
            roundTheClock: false,
            willClose: {
                day: 1,
                time: '21:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '13:00',
                isTodayOrNextDay: true,
            },
        });
    });

    it('открыто, время переносится на следующий день', () => {
        MockDate.set('2020-08-24T11:00');

        expect(checkAvailability(simpleDataLateClosing)).toEqual({
            isOpened: true,
            roundTheClock: false,
            willClose: {
                day: 2,
                time: '02:00',
                isTodayOrNextDay: true,
            },
            willOpen: {
                day: 1,
                time: '13:00',
                isTodayOrNextDay: true,
            },
        });
    });
});
