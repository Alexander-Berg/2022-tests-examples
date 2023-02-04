jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));
jest.mock('@app/libs/scheduler', () => ({
    scheduleEvent: () => {},
    scheduleTask: () => {},
}));
const Scheduler = require('@app/libs/scheduler');

const { ScheduleRemainingTimeMessage } = require('./ScheduleRemainingTimeMessage');

describe('Сообщение о скором окончании аукциона', () => {
    it('Планирует сообщение, если аукцион еще не начался', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 12:00').getTime());

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent')
            .mockImplementation((result) => results.push(result));

        await ScheduleRemainingTimeMessage({
            id: 1,
            auction_start_time: '2021-12-01 12:00:00',
            auction_finish_time: '2021-12-01 13:30:00',
        });

        expect(scheduleEventSpy).toBeCalledTimes(1);
        expect(results[0]).toMatchObject({
            id: 'remaining-time-auction-1',
            fireTime: new Date('2021-12-01 12:29:59'),
        });

        jest.clearAllTimers();
        scheduleEventSpy.mockClear();
    });

    it('Правильно планирует последующие сообщения', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 12:40').getTime());

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent')
            .mockImplementation((result) => results.push(result));

        await ScheduleRemainingTimeMessage({
            id: 1,
            auction_start_time: '2021-12-01 12:00:00',
            auction_finish_time: '2021-12-01 13:30:00',
        });

        expect(scheduleEventSpy).toBeCalledTimes(1);
        expect(results[0]).toMatchObject({
            id: 'remaining-time-auction-1',
            fireTime: new Date('2021-12-01 12:59:59'),
        });

        jest.clearAllTimers();
        scheduleEventSpy.mockClear();
    });

    it('Правильно планирует последнее сообщение перед порогом', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 13:01').getTime());

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent')
            .mockImplementation((result) => results.push(result));

        await ScheduleRemainingTimeMessage({
            id: 1,
            auction_start_time: '2021-12-01 12:00:00',
            auction_finish_time: '2021-12-01 13:40:00', // соообщение будет ровно за 10 минут до конца
        });

        expect(scheduleEventSpy).toBeCalledTimes(1);
        expect(results[0]).toMatchObject({
            id: 'remaining-time-auction-1',
            fireTime: new Date('2021-12-01 13:29:59'),
        });

        jest.clearAllTimers();
        scheduleEventSpy.mockClear();
    });

    it('Не планиурет, если следующее сообщение позже конца аукциона', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 13:25').getTime());

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent')
            .mockImplementation((result) => results.push(result));

        await ScheduleRemainingTimeMessage({
            id: 1,
            auction_start_time: '2021-12-01 12:00:00',
            auction_finish_time: '2021-12-01 13:30:00',
        });

        expect(scheduleEventSpy).toBeCalledTimes(0);

        jest.clearAllTimers();
        scheduleEventSpy.mockClear();
    });

    it('Не планирует, если следующее сообщение попадает в порог', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 13:01').getTime());

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent')
            .mockImplementation((result) => results.push(result));

        await ScheduleRemainingTimeMessage({
            id: 1,
            auction_start_time: '2021-12-01 12:00:00',
            auction_finish_time: '2021-12-01 13:39:00',
        });

        expect(scheduleEventSpy).toBeCalledTimes(0);

        jest.clearAllTimers();
        scheduleEventSpy.mockClear();
    });
});