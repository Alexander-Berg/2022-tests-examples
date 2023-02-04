/* eslint-disable max-len */
jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));

jest.mock('@app/models');
const { AuctionsDao } = require('@app/models');

jest.mock('@app/libs/logger');

jest.mock('@app/handlers/auctions', () => ({
    AnounceNewAuction: () => {},
    StartAuction: () => {},
}));

jest.mock('@app/libs/scheduler', () => ({
    scheduleEvent: () => {},
    scheduleTask: () => {},
}));
const Scheduler = require('@app/libs/scheduler');

jest.mock('./ScheduleRemainingTimeMessage', () => ({
    ScheduleRemainingTimeMessage: () => {},
}));

jest.mock('@config', () => ({
    auctions: {
        announce_gap: { hours: 1 },
        default_duration: { hours: 0, minutes: 45 },
        max_start_time: { hours: 17, minutes: 30, seconds: 0 },
        min_start_time: { hours: 10, minutes: 30, seconds: 0 },
        remaining_time_interval: { minutes: 15 },
        remaining_time_message_threshold: { minutes: 5 },
        ends_soon_interval: { minutes: 1 },
        cancel_threshold: { minutes: 15 },
        start_price: 0,
        prolong_count: 10000,
        holidays: [
            '2022-01-08',
            '2022-02-23',
            '2022-02-25',
            '2022-03-07',
            '2022-03-08',
        ],
    }
}));

const { ScheduleNextAuction } = require('./ScheduleNextAuction');

describe('Планирование следующего аукциона', () => {
    it('Планирует следующий аукцион на сегодня, если есть время, но не ранее 11:30', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 08:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-01 11:30:10', '2021-12-01 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на сейчас, если сегодня есть время', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 12:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-01 13:00:10', '2021-12-01 13:45:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на следующий рабочий день', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-02 11:30:10', '2021-12-02 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на сегодня, если сегодня пятница и есть время', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-03 12:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-03 13:00:10', '2021-12-03 13:45:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на понедельник, если сегодня пятница и нет времени', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-03 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-06 11:30:10', '2021-12-06 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на понедельник, если сегодня суббота', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-04 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-06 11:30:10', '2021-12-06 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует следующий аукцион на понедельник, если сегодня воскресенье', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-05 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-06 11:30:10', '2021-12-06 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после праздника, а сегодня день закончился', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-22 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-24 11:30:10', '2022-02-24 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день, если сегодня праздник, а завтра будний день, но день аукционов не начался', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-23 09:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-24 11:30:10', '2022-02-24 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день, если сегодня праздник, а завтра будний день', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-23 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-24 11:30:10', '2022-02-24 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после праздника и выходных, а сегодня день закончился', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-24 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-28 11:30:10', '2022-02-28 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после праздника и выходных, когда сегодня праздник, но день не начался', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-25 09:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-28 11:30:10', '2022-02-28 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после праздника и выходных, когда сегодня праздник', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-02-25 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-02-28 11:30:10', '2022-02-28 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после выходных и праздников, а сегодня день закончился', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-03-04 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-03-09 11:30:10', '2022-03-09 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после выходных и праздников, когда сегодня суббота, но день не начался', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-03-05 09:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-03-09 11:30:10', '2022-03-09 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после выходных и праздников, когда сегодня суббота', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-03-05 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-03-09 11:30:10', '2022-03-09 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион на следующий рабочий день после выходных и праздников, когда сегодня воскресенье', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2022-03-06 15:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2022-03-09 11:30:10', '2022-03-09 12:15:10');

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Планирует аукцион по id', async () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-03 18:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction(1);

        expect(scheduleAuctionSpy).toBeCalledTimes(1);
        expect(scheduleAuctionSpy).toHaveBeenLastCalledWith(1, '2021-12-06 11:30:10', '2021-12-06 12:15:10');


        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it('Не планирует аукцион, если его нет', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 12:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(undefined));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        await ScheduleNextAuction();

        expect(scheduleAuctionSpy).toBeCalledTimes(0);

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
    });

    it.skip('Вызывает шедулер для планирования эвентов', async() => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 12:00').getTime());

        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getFirstWaitingAuction').mockImplementation(() => Promise.resolve(mockAuction));
        jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));

        const scheduleAuctionSpy = jest.spyOn(AuctionsDao, 'scheduleAuction').mockImplementation((_, startTime, finishTime) => {
            Promise.resolve(Object.assign(mockAuction, { auction_start_time: startTime, auction_finish_time: finishTime }));
        });

        const results = [];
        const scheduleEventSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation((result) => results.push(result));

        await ScheduleNextAuction();

        expect(scheduleEventSpy).toBeCalledTimes(4);
        expect(results[0]).toMatchObject({
            id: 'starts-soon-auction-1',
            fireTime: new Date('2021-12-01 12:00:10'),
        });
        expect(results[1]).toMatchObject({
            id: 'starts-auction-1',
            fireTime: new Date('2021-12-01 12:10:10'),
        });
        expect(results[2]).toMatchObject({
            id: 'ends-soon-auction-1',
            fireTime: new Date('2021-12-01 13:39:10'),
        });
        expect(results[3]).toMatchObject({
            id: 'finish-auction-1',
            fireTime: new Date('2021-12-01 13:40:10'),
        });

        jest.clearAllTimers();
        scheduleAuctionSpy.mockClear();
        scheduleEventSpy.mockClear();
    });
});