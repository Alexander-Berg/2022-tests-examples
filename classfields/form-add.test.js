jest.mock('auto-core/lib/luster-bunker', () => {
    return {
        getNode(path) {
            if (path === '/auto_ru/common/vas') {
                return {};
            }
        },
    };
});

const de = require('descript');

const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const userFixtures = require('auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures');

const formAdd = require('./form-add');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.geoIds = [ 213 ];
    req.geoParents = [];
    req.geoIdsInfo = [];
});

it('должен ответить 500, если черновие не ответил', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.no_auth());

    publicApi
        .get('/1.0/user/draft/trucks')
        .times(2)
        .reply(500);

    const params = {
        parent_category: 'trucks',
    };

    return de.run(formAdd, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject(({
                error: {
                    id: 'REQUIRED_BLOCK_FAILED',
                },
            }));
        },
    );
});

it('должен запросить черновик, потом крошки для черновика', () => {
    publicApi
        .get('/1.0/user/draft/trucks')
        .reply(200, {
            offer: {
                category: 'TRUCKS',
                truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                id: '9040931609094442784-3291eb2b',
            },
            offer_id: '9040931609094442784-3291eb2b',
        });

    publicApi
        .get('/1.0/search/trucks/breadcrumbs?bc_lookup=TRUCK%23VOLVO%23FE&rid=225')
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [],
                    meta_level: 'TYPE_LEVEL',
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(formAdd, {
        context,
        params: { parent_category: 'trucks' },
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});

it('должен обогатить данные черновика данными из сессии', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth());

    publicApi
        .get('/1.0/user/draft/trucks')
        .reply(200, {
            offer: {
                category: 'TRUCKS',
                seller: {
                    name: 'Битумощебнераспределитель',
                },
                truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                id: '9040931609094442784-3291eb2b',
            },
            offer_id: '9040931609094442784-3291eb2b',
        });

    publicApi
        .get('/1.0/search/trucks/breadcrumbs?bc_lookup=TRUCK%23VOLVO%23FE&rid=225')
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [],
                    meta_level: 'TYPE_LEVEL',
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(formAdd, {
        context,
        params: { parent_category: 'trucks' },
    })
        .then((result) => {
            expect(result).toMatchObject({
                draft: {
                    offer: {
                        seller: {
                            name: 'Битумощебнераспределитель',
                            unconfirmed_email: 'natix@yandex-team.ru',
                            phones: [
                                {
                                    phone: '71234567890',
                                    added: '2018-02-22T17:21:27Z',
                                    phone_formatted: '+7 123 456-78-90',
                                },
                            ],
                        },
                        truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                        id: '9040931609094442784-3291eb2b',
                    },
                    offer_id: '9040931609094442784-3291eb2b',
                },
            });
        });
});

it('должен ответить 403 для дилера без прав WRITE для OFFER', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth_readonly());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.client_auth_readonly());

    const params = { parent_category: 'trucks' };

    return de.run(formAdd, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'USER_ROLE_HAS_NO_RIGHTS',
                    status_code: 403,
                },
            });
        });
});

describe('парсинг опций', () => {
    it('должен запросить ручку парсинга, если в черновике есть описание', () => {
        publicApi
            .get('/1.0/user/draft/trucks')
            .reply(200, {
                offer: {
                    category: 'TRUCKS',
                    truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                    id: '9040931609094442784-3291eb2b',
                    description: 'foo',
                },
                offer_id: '9040931609094442784-3291eb2b',
            });

        publicApi
            .post('/1.0/reference/catalog/cars/parse-options')
            .reply(200, {
                result: {
                    foo: 'bar',
                },
            });

        return de.run(formAdd, {
            context,
            params: { parent_category: 'trucks' },
        })
            .then((result) => {
                expect(result.parsedOptions).toEqual({ foo: 'bar' });
            });
    });

    it('не должен запросить ручку парсинга, если в черновике нет описание', () => {
        publicApi
            .get('/1.0/user/draft/trucks')
            .reply(200, {
                offer: {
                    category: 'TRUCKS',
                    truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                    id: '9040931609094442784-3291eb2b',
                },
                offer_id: '9040931609094442784-3291eb2b',
            });

        publicApi
            .post('/1.0/reference/catalog/cars/parse-options')
            .reply(200, {
                result: {
                    foo: 'bar',
                },
            });

        return de.run(formAdd, {
            context,
            params: { parent_category: 'trucks' },
        })
            .then((result) => {
                expect(result.parsedOptions.error.id).toBe('BLOCK_GUARDED');
            });
    });
});
