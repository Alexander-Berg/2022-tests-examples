const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const MockDate = require('mockdate');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./getNewOffersForTradeIn');

let context;
let req;
let res;
beforeEach(() => {
    MockDate.set('2019-09-23');

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

afterEach(() => {
    MockDate.reset();
});

it('должен запросить объявления с указанием диапазона цен, если для переданных марки и модели есть объявления в новых', () => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.catalog_filter === 'mark=BMW,model=4' &&
                query.page_size === '1',
                query.rid === '225'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '123',
                },
            ],
            price_range: {
                min: {
                    price: 1440495,
                },
                max: {
                    price: 3254700,
                },
            },
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.catalog_filter === 'mark=BMW,model=4' &&
                query.page_size === '1',
                query.rid === '225'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '123',
                },
            ],
            price_range: {
                min: {
                    price: 1440495,
                },
                max: {
                    price: 3254700,
                },
            },
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.group_by === 'CONFIGURATION' &&
                query.price_from === '2454276' &&
                query.price_to === '4294983' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '123',
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        mark: 'BMW',
        model: '4',
        geo_id: [ 213 ],
        year: 2017,
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                offers: [
                    { id: '123' },
                ],
            });
        });
});

it('должен запросить объявления с указанием диапазона цен, если для переданной марки есть объявления в новых, а для модели нет', () => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.catalog_filter === 'mark=BMW,model=4' &&
                query.page_size === '1',
                query.rid === '225'
            );
        })
        .reply(200, {
            offers: [],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.catalog_filter === 'mark=BMW' &&
                query.page_size === '1',
                query.rid === '225'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '123',
                },
            ],
            price_range: {
                min: {
                    price: 1440495,
                },
                max: {
                    price: 3254700,
                },
            },
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.group_by === 'CONFIGURATION' &&
                query.price_from === '2454276' &&
                query.price_to === '4294983' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '123',
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        mark: 'BMW',
        model: '4',
        geo_id: [ 213 ],
        year: 2017,
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                offers: [
                    { id: '123' },
                ],
            });
        });
});
