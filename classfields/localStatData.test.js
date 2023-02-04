/**
 * @jest-environment node
 */

const MockDate = require('mockdate');

const getIdHash = require('auto-core/react/lib/offer/getIdHash').default;
const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;
const { MINUTE } = require('auto-core/lib/consts');

const href = 'https://auto.ru/mock_card/';
const referrer = 'https://auto.ru/';
const referrerFromRouter = 'https://auto.ru/cars/all';
const currentTabId = 'currentTabId';
const parentTabId = 'parentTabId';
const anotherTabId = 'anotherTabId';

const mockOfferID = getIdHash(offerMock);

const mockLocalData = {
    logStatQuery: {
        [referrer]: [
            {
                timestamp: 123,
                searchID: 'SEARCH_ID_1',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: currentTabId,
            },
            {
                timestamp: 456,
                searchID: 'SEARCH_ID_2',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: anotherTabId,
            },
            {
                timestamp: 789,
                searchID: 'SEARCH_ID_3',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: parentTabId,
            },
        ],
        [referrerFromRouter]: [
            {
                timestamp: 12,
                searchID: 'SEARCH_ID_4',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: currentTabId,
            },
            {
                timestamp: 345,
                searchID: 'SEARCH_ID_5',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: anotherTabId,
            },
            {
                timestamp: 678,
                searchID: 'SEARCH_ID_6',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: parentTabId,
            },
        ],
    },
};

const mockSessionData = {
    tabId: currentTabId,
    parentTabId: parentTabId,
};

const mockLS = {
    getItem: jest.fn((key) => JSON.stringify(mockLocalData[key])),
    setItem: jest.fn(),
    removeItem: () => {},
};

const mockSS = {
    getItem: jest.fn((key) => mockSessionData[key]),
    setItem: jest.fn(),
    removeItem: () => {},
};

jest.mock('auto-core/lib/util/safeRunningDecorator', () => lib => lib);
jest.mock('./localstorage.js', () => mockLS);
jest.mock('./sessionstorage.js', () => mockSS);

const localStatData = require('./localStatData');

const originLocation = global.location;
const originDocument = global.document;
const originPerformance = global.performance;

beforeEach(() => {
    global.location = { href };
    global.document = { referrer };
    global.performance = { now: () => MINUTE };
    mockLS.getItem.mockClear();
    mockLS.setItem.mockClear();
});

afterEach(() => {
    global.location = originLocation;
    global.document = originDocument;
    global.performance = originPerformance;
});

it('возвращает searchID для оффера', () => {
    expect(localStatData.getSearchIdByOffer(offerMock)).toEqual('SEARCH_ID_1');
});

it('возвращает параметры контекста для оффера', () => {
    expect(localStatData.getContextByOfferID(offerMock)).toEqual({
        timestamp: 123,
        searchID: 'SEARCH_ID_1',
        offerID: mockOfferID,
        block: 'block',
        page: 'page',
        selfType: 'selfType',
        tabId: currentTabId,
    });
});

it('возвращает параметры контекста для оффера и реферрера', () => {
    expect(localStatData.getContextByOfferID(offerMock, referrerFromRouter)).toEqual({
        timestamp: 12,
        searchID: 'SEARCH_ID_4',
        offerID: mockOfferID,
        block: 'block',
        page: 'page',
        selfType: 'selfType',
        tabId: currentTabId,
    });
});

it('возвращает предположительный searchID исходя из времени жизни страницы', () => {
    expect(localStatData.getSearchIdByPageLifeTime()).toEqual('SEARCH_ID_1');
});

it('возвращает предположительные параметры контекста исходя из времени жизни страницы', () => {
    const clickStory = {
        timestamp: 123,
        searchID: 'SEARCH_ID',
        offerID: mockOfferID,
        block: 'block',
        page: 'page',
        selfType: 'selfType',
        tabId: currentTabId,
    };
    const tooOldClickStoryTimestamp = 148000;
    const recentClickStoryTimastamp = 1483228800000;
    const tooNewClickStoryTimestamp = 1483999900000;

    MockDate.set('2017-01-01');
    mockLS.getItem.mockImplementationOnce(() => JSON.stringify({
        [referrer]: [
            {
                ...clickStory,
                timestamp: tooOldClickStoryTimestamp,
            },
            {
                ...clickStory,
                timestamp: recentClickStoryTimastamp,
            },
            {
                ...clickStory,
                timestamp: tooNewClickStoryTimestamp,
            },
        ],
    }));

    expect(localStatData.getContextByPageLifeTime()).toEqual({
        ...clickStory,
        timestamp: 1483228800000,
    });
});

it('сохраняет в localStorage историю кликов', () => {
    global.location = { href: referrer };
    MockDate.set('2017-01-01');

    expect(localStatData.saveSearchClickInfo('SEARCH_ID', offerMock, {
        block: 'block',
        page: 'page',
        self_type: 'selfType',
    })).toBeUndefined();

    expect(mockLS.setItem).toHaveBeenCalledWith('logStatQuery', JSON.stringify({
        ...mockLocalData.logStatQuery,
        [referrer]: [
            ...mockLocalData.logStatQuery[referrer],
            {
                timestamp: 1483228800000,
                searchID: 'SEARCH_ID',
                offerID: mockOfferID,
                block: 'block',
                page: 'page',
                selfType: 'selfType',
                tabId: currentTabId,
            },
        ],
    }));
});

it('перекидывает в текущий таб данные из родителя', () => {
    const dataMock = {
        ...mockLocalData.logStatQuery,
        [referrer]: [
            mockLocalData.logStatQuery[ referrer ][1], // левый таб
            mockLocalData.logStatQuery[ referrer ][2], // истинный родитель
            {
                ...mockLocalData.logStatQuery[ referrer ][2], // родитель с другим searchId
                searchID: 'SEARCH_ID_10',
            },
        ],
    };

    mockLS.getItem.mockImplementationOnce(() => JSON.stringify(dataMock));

    localStatData.transferStoriesFromTabToTab(currentTabId, parentTabId, referrer, 'SEARCH_ID_3');

    expect(mockLS.setItem).toHaveBeenCalledWith('logStatQuery', JSON.stringify({
        ...dataMock,
        [ referrer ]: [
            ...dataMock[ referrer ],
            {
                ...mockLocalData.logStatQuery[ referrer ][2],
                tabId: currentTabId,
            },
        ],
    }));
});

it('удаляет из localStorage истории кликов старше трех дней', () => {
    const oldClickStory = {
        timestamp: 123,
        searchID: 'SEARCH_ID',
        offerID: mockOfferID,
        block: 'block',
        page: 'page',
        selfType: 'selfType',
    };
    const newClickStory = {
        timestamp: 1483228800000,
        searchID: 'SEARCH_ID',
        offerID: mockOfferID,
        block: 'block',
        page: 'page',
        selfType: 'selfType',
    };

    MockDate.set('2017-01-01');
    mockLS.getItem.mockImplementationOnce(() => JSON.stringify({
        [referrer]: [
            oldClickStory,
            newClickStory,
        ],
    }));

    return localStatData.clearOldClickStories()
        .then(() => {
            expect(mockLS.setItem).toHaveBeenCalledWith('logStatQuery', JSON.stringify({
                [referrer]: [
                    newClickStory,
                ],
            }));
        });
});
