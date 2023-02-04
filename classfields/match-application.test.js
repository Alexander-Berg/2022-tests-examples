const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./match-application');

let context;
let params;
let res;
let req;

beforeEach(() => {
    params = Object.freeze({
        category: 'cars',
        section: 'new',
        mark: 'audi',
    });
    res = createHttpRes();
    req = createHttpReq();
    context = createContext({ req, res });

    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query(true)
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        { id: 'AUDI', mark: {}, name: 'Audi', numeric_id: 3139 },
                        { id: 'BMW', mark: {}, name: 'BMW', numeric_id: 3141 },
                        { id: 'SKODA', mark: {}, name: 'Skoda', numeric_id: 3189 },
                    ],
                    levelFilterParams: {},
                    level: 'MARK_LEVEL',
                    meta_level: 'MARK_LEVEL',
                },
                {
                    entities: [
                        { id: 'A5', model: {}, name: 'A5' },
                        { id: 'A6', model: {}, name: 'A6' },
                        { id: 'Q5', model: {}, name: 'Q5' },
                    ],
                    levelFilterParams: {
                        mark: 'AUDI',
                    },
                    level: 'MODEL_LEVEL',
                    meta_level: 'MODEL_LEVEL',
                    mark: {
                        id: 'AUDI',
                        name: 'Audi',
                    },
                },
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/mark-model-filters')
        .query(true)
        .reply(200, {
            mark_entries: [
                { mark_code: 'AUDI', models: [ { model_code: 'A5' }, { model_code: 'Q5' } ] },
                { mark_code: 'BMW', models: [ { model_code: 'X3' }, { model_code: 'X6' } ] },
            ],
        });
});

describe('проверка shouldRenderPage', () => {
    it('вернет true, если сервис доступен для региона', () => {
        req.geoFromUrl = {
            geoAlias: 'moskva',
            ids: [ 213 ],
        };

        publicApi
            .get('/1.0/search/cars')
            .query(true)
            .reply(200, {
                response_flags: {
                    show_match_application_form: true,
                },
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result.matchApplication.shouldRenderPage).toEqual(true);
            });
    });

    it('вернет false, если сервис не доступен для региона', () => {
        req.geoFromUrl = {
            geoAlias: 'moskva',
            ids: [ 213 ],
        };

        publicApi
            .get('/1.0/search/cars')
            .query(true)
            .reply(200, {
                response_flags: {
                    show_match_application_form: false,
                },
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result.matchApplication.shouldRenderPage).toEqual(false);
            });
    });

    it('вернет false, если в ссылке не указан регион', () => {
        req.geoFromUrl = null;

        publicApi
            .get('/1.0/search/cars')
            .query(true)
            .reply(200, {
                response_flags: {
                    show_match_application_form: true,
                },
            });

        return de.run(block, { context, params })
            .then((result) => {
                expect(result.matchApplication.shouldRenderPage).toEqual(false);
            });
    });
});

it('возвращает marks и models', () => {
    req.geoFromUrl = {
        geoAlias: 'moskva',
        ids: [ 213 ],
    };

    publicApi
        .get('/1.0/search/cars')
        .query(true)
        .reply(200, {
            response_flags: {
                show_match_application_form: true,
            },
        });

    return de.run(block, { context, params })
        .then((result) => {
            const { matchApplication } = result;
            expect({ marks: matchApplication.marks, models: matchApplication.models }).toMatchSnapshot();
        });
});
