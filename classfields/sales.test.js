/**
 * @jest-environment node
 */
const sales = require('./sales');

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createDeps = require('autoru-frontend/mocks/createDeps');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const salesWithDeps = (navigation) => de.func({
    block: (args) => {
        const { blocks, ids } = createDeps({ navigation: navigation || [] }, args);

        return de.pipe({
            block: [
                blocks.navigation,
                sales(ids),
            ],
        });
    },
});

it('должен блокировать вызов getTruckCategories и getMotoCategories страницы на странице /sales/cars', () => {
    setRequiredNocks();

    publicApi
        .matchHeader('x-dealer-id', val => val === 20101)
        .get('/1.0/user/offers/cars/used')
        .query({})
        .reply(200, { categories: 'cars' });

    return de.run(salesWithDeps(), { context, params: { client_id: 20101, category: 'cars' } })
        .then((result) => {
            expect(result.motoCategories.error.id).toBe('BLOCK_GUARDED');
            expect(result.truckCategories.error.id).toBe('BLOCK_GUARDED');
        });
});

it('должен блокировать вызов onboardingOffer, если не пришел флаг эксперимента', () => {
    setRequiredNocks();

    publicApi
        .matchHeader('x-dealer-id', val => val === 20101)
        .get('/1.0/user/offers/cars/used')
        .query({})
        .reply(200, { categories: 'cars' });
    context.req.experimentsData.has = () => false;
    context.req.headers.cookie = '';

    return de.run(salesWithDeps(), { context, params: { client_id: 20101, category: 'cars' } })
        .then((result) => {
            expect(result.onboardingOffer.error.id).toBe('BLOCK_GUARDED');
        });
});

it('должен вызывать getMotoCategories на странице /sales/moto (c dealer_id)', () => {
    setRequiredNocks();

    publicApi
        .matchHeader('x-dealer-id', val => val === 20101)
        .get('/1.0/user/offers/moto/moto-categories')
        .query({})
        .reply(200, { categories: 'moto' });

    return de.run(salesWithDeps(), { context, params: { client_id: 20101, category: 'moto' } })
        .then((result) => {
            expect(result.motoCategories).toEqual({ categories: 'moto' });
        });
});

it('должен вызывать getTruckCategories на странице /sales/trucks (c dealer_id)', () => {
    setRequiredNocks();

    publicApi
        .matchHeader('x-dealer-id', val => val === 20101)
        .get('/1.0/user/offers/trucks/truck-categories')
        .query({})
        .reply(200, { categories: 'trucks' });

    return de.run(salesWithDeps(), { context, params: { client_id: 20101, category: 'trucks' } })
        .then((result) => {
            expect(result.truckCategories).toEqual({ categories: 'trucks' });
        });
});

it('должен сделать редирект, если пользователю доступен один тариф и он попал на url /sales/', () => {
    setRequiredNocks();

    return de.run(salesWithDeps([ {
        name: 'Объявления',
        alias: 'sales',
        host: 'manager',
        url: '/sales/cars/used/',
        cnt: 0,
        amount: null,
        unlim: false,
    } ]), { context, params: { client_id: 20101, category: '' } })
        .catch(error => error)
        .then((error) => {
            expect(error.error.code).toEqual('CABINET_TO_SALES_WITH_CATEGORY');
        });
});

function setRequiredNocks() {
    publicApi
        .get('/1.0/dealer/info')
        .query(true)
        .reply(200, {});
    publicApi
        .get('/1.0/user/offers/cars')
        .query(true)
        .reply(200, {});
    publicApi
        .get('/1.0/user/offers/moto')
        .query(true)
        .reply(200, {});
    publicApi
        .get('/1.0/user/offers/trucks')
        .query(true)
        .reply(200, {});
}
