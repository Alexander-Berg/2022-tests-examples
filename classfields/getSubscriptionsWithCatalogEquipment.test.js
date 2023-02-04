const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getSubscriptionsWithCatalogEquipment');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    publicApi
        .get('/1.0/reference/catalog/cars/dictionaries/v1/equipment')
        .reply(200, {
            dictionary_v1: {
                values: [
                    { code: 'automatic-lighting-control', name: 'название automatic-lighting-control' },
                ],
            },
            status: 'SUCCESS',
        });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать пустой список, если нет подписок', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .reply(200, {
            status: 'SUCCESS',
        });

    req.cookies.autoru_sid = 'user_without_subscriptions';
    return de.run(block, { context })
        .then((result) => {
            expect(result).toEqual([]);
        });
});

it('должен отдать пустой список, если я робот и подписки заблокированы', () => {
    req.isRobot = true;

    return de.run(block, { context })
        .then((result) => {
            expect(result).toEqual([]);
        });
});

it('должен пробросить ошибку, если подписки 500ят', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .times(2)
        .reply(500);

    return de.run(block, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'REQUIRED_BLOCK_FAILED',
                },
            });
        });
});

it('должен отдать подписки, если они есть', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .reply(200, {
            saved_searches: [
                {
                    category: 'CARS',
                    deliveries: {},
                    id: 'subscription-id',
                    params: {
                        cars_params: {
                            catalog_filter: [ { mark: 'BMW', model: 'X6' } ],
                        },
                        rid: '2',
                        state_group: 'NEW',
                        price_to: 1000000,
                    },
                    title: 'subscription-title',
                },
            ],
            status: 'SUCCESS',
        });

    req.cookies.autoru_sid = 'user_with_subscriptions';
    return de.run(block, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен обогатить данные словарем опций', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .reply(200, {
            saved_searches: [
                {
                    category: 'CARS',
                    deliveries: {},
                    id: 'subscription-id',
                    params: {
                        cars_params: {
                            catalog_filter: [ { mark: 'BMW', model: 'X6' } ],
                        },
                        catalog_equipment: [ 'automatic-lighting-control' ],
                        rid: '2',
                        state_group: 'NEW',
                    },
                    title: 'subscription-title',
                },
            ],
            status: 'SUCCESS',
        });

    req.cookies.autoru_sid = 'user_with_subscriptions_by_catalog_equipment';
    return de.run(block, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});
