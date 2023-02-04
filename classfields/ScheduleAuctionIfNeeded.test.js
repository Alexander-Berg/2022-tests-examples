jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));

jest.mock('@app/libs/scheduler', () => ({
    scheduleTask: () => {},
}));

jest.mock('@app/models');
const { AuctionsDao } = require('@app/models');

jest.mock('./ScheduleNextAuction');
const NextScheduler = require('./ScheduleNextAuction');

const { ScheduleAuctionIfNeeded } = require('./ScheduleAuctionIfNeeded');

describe('Планирование аукциона при старте приложения', () => {
    it('Планирует эвенты, если есть запланированный аукцион', async () => {
        const mockAuction = { id: 1 };
        jest.spyOn(AuctionsDao, 'getScheduledAuctions').mockImplementation(() => Promise.resolve([mockAuction]));
        const scheduleEventsSpy = jest.spyOn(NextScheduler, 'ScheduleAuctionEvents').mockImplementation(() => {});

        await ScheduleAuctionIfNeeded();

        expect(scheduleEventsSpy).toBeCalledTimes(1);
        expect(scheduleEventsSpy).toHaveBeenLastCalledWith(mockAuction);

        scheduleEventsSpy.mockClear();
    });

    it('Планирует эвенты, если есть текущий аукцион', async () => {
        const mockAuction = { id: 2 };
        jest.spyOn(AuctionsDao, 'getScheduledAuctions').mockImplementation(() => Promise.resolve([]));
        jest.spyOn(AuctionsDao, 'getActiveAuctions').mockImplementation(() => Promise.resolve([mockAuction]));
        const scheduleEventsSpy = jest.spyOn(NextScheduler, 'ScheduleAuctionEvents').mockImplementation(() => {});

        await ScheduleAuctionIfNeeded();

        expect(scheduleEventsSpy).toBeCalledTimes(1);
        expect(scheduleEventsSpy).toHaveBeenLastCalledWith(mockAuction);

        scheduleEventsSpy.mockClear();
    });

    it('Планирует аукцион, если не нашлось текущих в очереди', async () => {
        jest.spyOn(AuctionsDao, 'getScheduledAuctions').mockImplementation(() => Promise.resolve([]));
        jest.spyOn(AuctionsDao, 'getActiveAuctions').mockImplementation(() => Promise.resolve([]));
        const scheduleNextSpy = jest.spyOn(NextScheduler, 'ScheduleNextAuction').mockImplementation(() => {});

        await ScheduleAuctionIfNeeded();

        expect(scheduleNextSpy).toBeCalledTimes(1);

        scheduleNextSpy.mockClear();
    });
});