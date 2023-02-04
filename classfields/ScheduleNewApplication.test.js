/* eslint-disable max-len */
jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));

jest.mock('@app/libs/scheduler', () => ({
    scheduleTask: () => {},
}));

jest.mock('@app/models');
const { AuctionsDao, ApplicationsDao } = require('@app/models');

jest.mock('./ScheduleNextAuction');
const NextScheduler = require('./ScheduleNextAuction');

const { ScheduleNewApplication } = require('./ScheduleNewApplication');

describe('Планирование новой заявки', () => {
    it('Создает аукцион', async() => {
        const APPLICATION_ID = 111;
        const AUCTION_ID = 2;
        const START_PRICE = 100000;
        const mockApplications = { [APPLICATION_ID]: { id: APPLICATION_ID, predicted_price: START_PRICE } };

        jest.spyOn(ApplicationsDao, 'getApplicationById').mockImplementation((id) => Promise.resolve(mockApplications[id]));

        const createAuctionSpy = jest.spyOn(AuctionsDao, 'createAuction').mockImplementation(() => Promise.resolve(AUCTION_ID));
        jest.spyOn(AuctionsDao, 'getActiveOrScheduledAuctions').mockImplementation(() => Promise.resolve([{}]));

        await ScheduleNewApplication(APPLICATION_ID);

        expect(createAuctionSpy).toBeCalledTimes(1);
        expect(createAuctionSpy).toHaveBeenLastCalledWith({
            application_id: APPLICATION_ID,
            start_price: START_PRICE,
            prolong_counter: 10000,
        });
    });
    it('Создает и планирует аукцион, если нет текущих в очереди', async () => {
        const APPLICATION_ID = 111;
        const AUCTION_ID = 2;
        const START_PRICE = 100000;
        const mockApplications = { [APPLICATION_ID]: { id: APPLICATION_ID, predicted_price: START_PRICE } };
        jest.spyOn(ApplicationsDao, 'getApplicationById').mockImplementation((id) => Promise.resolve(mockApplications[id]));

        jest.spyOn(AuctionsDao, 'createAuction').mockImplementation(() => Promise.resolve(AUCTION_ID));
        jest.spyOn(AuctionsDao, 'getActiveOrScheduledAuctions').mockImplementation(() => Promise.resolve([]));

        const scheduleNextSpy = jest.spyOn(NextScheduler, 'ScheduleNextAuction').mockImplementation(() => {});

        await ScheduleNewApplication(APPLICATION_ID);

        expect(scheduleNextSpy).toBeCalledTimes(1);
        expect(scheduleNextSpy).toHaveBeenLastCalledWith(AUCTION_ID);
    });
});