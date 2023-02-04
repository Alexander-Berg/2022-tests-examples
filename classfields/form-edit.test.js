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

const formEdit = require('./form-edit');

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

it('должен запросить id черновика, потом черновик, потом крошки для черновика', () => {
    publicApi
        .post('/1.0/user/offers/trucks/123-abc/edit')
        .reply(200, {
            offer_id: '9040931609094442784-3291eb2b',
        });

    publicApi
        .get('/1.0/user/draft/trucks/9040931609094442784-3291eb2b')
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

    return de.run(formEdit, {
        context,
        params: { parent_category: 'trucks', sale_id: '123-abc' },
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});

it('в карточку сохранит id объявы, а не id драфта', () => {
    publicApi
        .post('/1.0/user/offers/trucks/123-abc/edit')
        .reply(200, {
            offer_id: '9040931609094442784-3291eb2b',
        });

    publicApi
        .get('/1.0/user/draft/trucks/9040931609094442784-3291eb2b')
        .reply(200, {
            offer: {
                additional_info: {
                    original_id: 'offer-id',
                },
                category: 'TRUCKS',
                truck_info: { truck_category: 'TRUCK', mark: 'VOLVO', model: 'FE' },
                id: 'draft-id',
            },
            offer_id: 'draft-id',
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

    return de.run(formEdit, {
        context,
        params: { parent_category: 'trucks', sale_id: '123-abc' },
    })
        .then((result) => {
            expect(result.card.id).toEqual('offer');
            expect(result.card.hash).toEqual('id');
            expect(result.card.saleId).toEqual('offer-id');
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

    return de.run(formEdit, { context, params }).then(
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
            .post('/1.0/user/offers/trucks/123-abc/edit')
            .reply(200, {
                offer_id: '9040931609094442784-3291eb2b',
            });

        publicApi
            .get('/1.0/user/draft/trucks/9040931609094442784-3291eb2b')
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

        return de.run(formEdit, {
            context,
            params: { parent_category: 'trucks', sale_id: '123-abc' },
        })
            .then((result) => {
                expect(result.parsedOptions).toEqual({ foo: 'bar' });
            });
    });

    it('не должен запросить ручку парсинга, если в черновике нет описание', () => {
        publicApi
            .post('/1.0/user/offers/trucks/123-abc/edit')
            .reply(200, {
                offer_id: '9040931609094442784-3291eb2b',
            });

        publicApi
            .get('/1.0/user/draft/trucks/9040931609094442784-3291eb2b')
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

        return de.run(formEdit, {
            context,
            params: { parent_category: 'trucks', sale_id: '123-abc' },
        })
            .then((result) => {
                expect(result.parsedOptions.error.id).toBe('BLOCK_GUARDED');
            });
    });
});
