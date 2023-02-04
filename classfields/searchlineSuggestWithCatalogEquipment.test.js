const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./searchlineSuggestWithCatalogEquipment');

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

it('должен отдать пустой список, если саджеста', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query(() => true)
        .reply(200, {
            status: 'SUCCESS',
        });

    const params = {
        category: 'cars',
        query: 'test',
        section: 'all',
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                suggests: [],
            });
        });
});

it('должен отдать саджест, если он есть', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query({
            query: 'test',
            state_group: 'ALL',
        })
        .reply(200, {
            query: 'test',
            suggests: [
                {
                    params: {
                        catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                    },
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        category: 'cars',
        query: 'test',
        section: 'all',
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                suggests: [
                    {
                        category: 'CARS',
                        params: {
                            catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                        },
                        url: 'https://autoru_frontend.base_domain/cars/opel/astra/all/?query=test&from=searchline',
                        subcategoryHumanName: 'Автомобили',
                    },
                ],
            });
        });
});

it('должен обогатить данные словарем опций', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query({
            query: 'test',
            state_group: 'ALL',
        })
        .reply(200, {
            query: 'test',
            suggests: [
                {
                    params: {
                        catalog_equipment: [ 'wheel-heat' ],
                        catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                    },
                },
            ],
            status: 'SUCCESS',
        });

    const params = {
        category: 'cars',
        query: 'test',
        section: 'all',
    };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                suggests: [
                    {
                        category: 'CARS',
                        params: {
                            catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                        },
                        url: 'https://autoru_frontend.base_domain/cars/opel/astra/all/?catalog_equipment=wheel-heat&query=test&from=searchline',
                        subcategoryHumanName: 'Автомобили',
                    },
                ],
            });
        });
});
