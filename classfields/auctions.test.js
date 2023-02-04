jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));

jest.mock('@app/libs/scheduler', () => ({
    scheduleEvent: () => {},
    scheduleTask: () => {},
}));

jest.mock('@app/models');
const { AuctionsDao } = require('@app/models');

const { CancelAuction } = require('./auctions');


describe('Аукционы', () => {
    describe('Отмена аукциона', () => {
        it('Отменяет аукцион, если он не был запланирован', async () => {
            const mockAuction = {
                id: 1,
                auction_start_time: undefined,
                auction_finish_time: undefined,
            };

            jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));
            const deleteSpy = jest.spyOn(AuctionsDao, 'deleteAuction').mockImplementation(() => Promise.resolve(true));

            const isCancelled = await CancelAuction(1);

            expect(isCancelled).toBe(true);
            expect(deleteSpy).toBeCalledTimes(1);
            expect(deleteSpy).toHaveBeenLastCalledWith(1);

            deleteSpy.mockClear();
        });

        it('Отменяет аукцион, если он был запланирован, но порог отмены не преодолен', async () => {
            jest
                .useFakeTimers()
                .setSystemTime(new Date('2021-12-01 12:00').getTime());

            const mockAuction = {
                id: 1,
                auction_start_time: '2021-12-01 12:15:10',
                auction_finish_time: '2021-12-01 13:15:10',
            };
            jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));
            const deleteSpy = jest.spyOn(AuctionsDao, 'deleteAuction').mockImplementation(() => Promise.resolve(true));

            const isCancelled = await CancelAuction(1);
            expect(isCancelled).toBe(true);
            expect(deleteSpy).toBeCalledTimes(1);

            jest.clearAllTimers();
            deleteSpy.mockClear();
        });

        it('Не отменяет аукцион, если он был запланирован, но порог отмены преодолен', async () => {
            jest
                .useFakeTimers()
                .setSystemTime(new Date('2021-12-01 12:00').getTime());

            const mockAuction = {
                id: 1,
                auction_start_time: '2021-12-01 12:10:00',
                auction_finish_time: '2021-12-01 13:10:00',
            };
            jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));
            const deleteSpy = jest.spyOn(AuctionsDao, 'deleteAuction').mockImplementation(() => Promise.resolve(true));

            const isCancelled = await CancelAuction(1);
            expect(isCancelled).toBe(false);
            expect(deleteSpy).toBeCalledTimes(0);

            jest.clearAllTimers();
            deleteSpy.mockClear();
        });

        it('Не отменяет аукцион, если он не найден по ID', async () => {
            const mockAuction = {
                id: 1,
                auction_start_time: '2021-12-01 12:10:00',
                auction_finish_time: '2021-12-01 13:10:00',
            };
            jest.spyOn(AuctionsDao, 'getAuctionById').mockImplementation(() => Promise.resolve(mockAuction));
            const deleteSpy = jest.spyOn(AuctionsDao, 'deleteAuction').mockImplementation(() => Promise.resolve(true));

            const isCancelled = await CancelAuction(2);
            expect(isCancelled).toBe(false);
            expect(deleteSpy).toBeCalledTimes(0);

            jest.clearAllTimers();
            deleteSpy.mockClear();
        });
    });
});
