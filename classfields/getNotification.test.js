const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const MockDate = require('mockdate');

const block = require('./getNotification');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const hydra = require('auto-core/server/resources/hydra/getResource.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
let params;
beforeEach(() => {
    MockDate.set('2019-09-23');

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    req.cookies.autoru_sid = 'testsid';

    params = {
        category: 'cars',
        price: 580000,
        saleIdHash: '123-abc',
        section: 'used',
        year: 2008,
        creationDate: 1568764800000,
    };
});

afterEach(() => {
    MockDate.reset();
});

describe('callsCount', () => {
    it('должен вернуть callsCount, если есть данные про звонки и это сообщение еще не видел пользователь', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 10,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен вернуть callsCount, если нет данных про звонки и это сообщение еще не видел пользователь', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).not.toMatchObject({
                    type: 'callsCount',
                });
            });
    });

    it('не должен вернуть callsCount, если данныt про звонки есть, но пользователь уже видел сообщение', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        params.notificationsSeen = [ 'callsCount' ];

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).not.toMatchObject({
                    type: 'callsCount',
                });
            });
    });
});

describe('noCalls', () => {
    it('должен вернуть noCalls, если звонков меньше одного и это сообщение еще не видел пользователь', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен вернуть noCalls, если по объявлению нельзя позвонить', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats', { hasChatOnlyFlag: true })
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть noCalls, если звонков больше 0 и это сообщение еще не видел пользователь', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 1,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть noCalls, если звонков меньше одного, но пользователь уже видел сообщение', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        params.notificationsSeen = [ 'noCalls' ];

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть noCalls, если звонков меньше одного и это сообщение еще не видел пользователь, но объявление старо', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        params.creationDate = '1568678400000';
        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });
});

describe('lastCall', () => {
    beforeEach(() => {
        // блокируем нотифайки сверху в очереди
        params.notificationsSeen = [ 'callsCount', 'noCalls' ];
    });

    it('должен вернуть lastCall, если последний звонок был меньше 24 назад', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 10,
                last_call_timestamp: Date.now() - 23 * 60 * 60 * 1000,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен вернуть lastCall, если нет данных про последний звонок', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                request: 'offer/calls-stats',
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть lastCall, если данные про последний звонок есть, но пользователь уже видел сообщение', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/calls-stats')
            .reply(200, {
                count: 0,
                last_call_timestamp: Math.floor(Date.now() / 1000) - 23 * 60 * 60,
                request: 'offer/calls-stats',
            });

        params.notificationsSeen = [ 'callsCount', 'noCalls', 'lastCall' ];

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });
});

describe('views', () => {
    beforeEach(() => {
        // блокируем нотифайки сверху в очереди
        params.notificationsSeen = [ 'callsCount', 'noCalls', 'lastCall' ];
    });

    it('должен вернуть views, если показов больше 5', () => {
        hydra
            .get('/api/v2/counter/auto/ru/current_shows/cars-123/testsid')
            .reply(200, 6);

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен вернуть views, если показов меньше 6', () => {
        hydra
            .get('/api/v2/counter/auto/ru/current_shows/cars-123/testsid')
            .reply(200, 5);

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть views, если данные есть, но пользователь уже видел сообщение', () => {
        hydra
            .get('/api/v2/counter/auto/ru/current_shows/cars-123/testsid')
            .reply(200, '10');

        params.notificationsSeen = [ 'callsCount', 'noCalls', 'lastCall', 'views' ];

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });
});

describe('viewsOverall', () => {
    beforeEach(() => {
        // блокируем нотифайки сверху в очереди
        params.notificationsSeen = [ 'callsCount', 'noCalls', 'lastCall', 'views' ];
    });

    it('должен вернуть viewsOverall, если за последние 3 дня было больше 100 просмотров', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/stats')
            .reply(200, {
                items: [
                    {
                        offer_id: '123-abc',
                        counters: [
                            { date: '2019-09-21', views: 50 },
                            { date: '2019-09-22', views: 0 },
                            { date: '2019-09-23', views: 50 },
                        ],
                    },

                ],
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен вернуть viewsOverall, если за последние 3 дня было меньше 100 просмотров', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/stats')
            .reply(200, {
                items: [
                    {
                        offer_id: '123-abc',
                        counters: [
                            { date: '2019-09-20', views: 50 },
                            { date: '2019-09-21', views: 50 },
                            { date: '2019-09-22', views: 0 },
                            { date: '2019-09-23', views: 40 },
                        ],
                    },

                ],
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });

    it('не должен вернуть viewsOverall, если данные есть, но пользователь уже видел сообщение', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc/stats')
            .reply(200, {
                items: [
                    {
                        offer_id: '123-abc',
                        counters: [
                            { date: '2019-09-21', views: 50 },
                            { date: '2019-09-22', views: 0 },
                            { date: '2019-09-23', views: 50 },
                        ],
                    },
                ],
            });

        params.notificationsSeen = [ 'callsCount', 'noCalls', 'lastCall', 'views', 'viewsOverall' ];

        return de.run(block, { context, params })
            .then((result) => {
                expect(result).toBeUndefined();
            });
    });
});
