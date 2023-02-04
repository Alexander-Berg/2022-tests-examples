const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const MockDate = require('mockdate');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./relatedOffersForCurrentOrPreviousGeneration');

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

it('должен запросить объявления из текущего поколения и вернуть их, если есть объявления', () => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.body_type_group === '' &&
                query.catalog_filter === 'mark=BMW,model=4,generation=10398058' &&
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
        catalog_filter: [ { mark: 'BMW', model: '4', generation: '10398058' } ],
        category: 'cars',
        body_type_group: '',
        mark: 'BMW',
        model: '4',
        super_gen: '10398058',
        geo_id: [ 213 ],
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

it('должен запросить объявления из предыдущего поколения и вернуть их, если в текущем поколении нет объявлений и текущее поколение не закрыто', () => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.body_type_group === '' &&
                query.catalog_filter === 'mark=BMW,model=4,generation=10398058' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query({
            bc_lookup: 'BMW#4',
        })
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        {
                            id: '10398058',
                            super_gen: {
                                year_from: 2017,
                            },
                        },
                        {
                            id: '10398057',
                            super_gen: {
                                year_from: 2016,
                            },
                        },
                    ],
                    meta_level: 'GENERATION_LEVEL',
                },
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.body_type_group === '' &&
                query.catalog_filter === 'mark=BMW,model=4,generation=10398057' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '456',
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        catalog_filter: [ { mark: 'BMW', model: '4', generation: '10398058' } ],
        category: 'cars',
        body_type_group: '',
        mark: 'BMW',
        model: '4',
        super_gen: '10398058',
        geo_id: [ 213 ],
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                offers: [
                    { id: '456' },
                ],
            });
        });
});

it('не должен запросить объявления из предыдущего поколения, если в текущем поколении нет объявлений и текущее поколение закрыто', () => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.body_type_group === '' &&
                query.catalog_filter === 'mark=BMW,model=4,generation=10398058' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query({
            category: 'cars',
            bc_lookup: 'BMW#4',
            rid: '213',
        })
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        {
                            id: '10398058',
                            super_gen: {
                                year_from: 2017,
                                year_to: 2018,
                            },
                        },
                        {
                            id: '10398057',
                            super_gen: {
                                year_from: 2016,
                            },
                        },
                    ],
                    meta_level: 'GENERATION_LEVEL',
                },
            ],
            status: 'SUCCESS',
        });

    const prevGenerationRequest = publicApi
        .get('/1.0/search/cars')
        .query(() => true)
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '456',
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        catalog_filter: [ { mark: 'BMW', model: '4', generation: '10398058' } ],
        category: 'cars',
        body_type_group: '',
        mark: 'BMW',
        model: '4',
        super_gen: '10398058',
        geo_id: [ 213 ],
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(prevGenerationRequest.isDone()).toEqual(false);
            expect(result).toMatchObject({
                offers: [],
            });
        });
});

it('правильно сформирует параметры запрос для ком тс', () => {
    publicApi
        .get('/1.0/search/trucks')
        .query((query) => {
            return (
                query.body_type_group === '' &&
                query.catalog_filter === 'mark=BMW,model=4,generation=10398058' &&
                query.rid === '213'
            );
        })
        .reply(200, {
            status: 'SUCCESS',
        });

    const breadcrumbsRequest = publicApi
        .get('/1.0/search/trucks/breadcrumbs')
        .query({
            bc_lookup: 'bus#BMW#4',
        })
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        {
                            id: '10398058',
                            super_gen: {
                                year_from: 2017,
                            },
                        },
                        {
                            id: '10398057',
                            super_gen: {
                                year_from: 2016,
                            },
                        },
                    ],
                    meta_level: 'GENERATION_LEVEL',
                },
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/trucks')
        .query(() => true)
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '456',
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        catalog_filter: [ { mark: 'BMW', model: '4', generation: '10398058' } ],
        category: 'trucks',
        sub_category: 'bus',
        body_type_group: '',
        mark: 'BMW',
        model: '4',
        super_gen: '10398058',
        geo_id: [ 213 ],
    };

    return de.run(block, { context, params })
        .then(() => {
            expect(breadcrumbsRequest.isDone()).toEqual(true);
        });
});
