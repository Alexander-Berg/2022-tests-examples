const de = require('descript');

const versus = require('./versus');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const ERROR_ID = require('auto-core/server/descript/error-id').default;

const versusMock = require('auto-core/react/dataDomain/versus/mock').default.value().map(({ model }) => model);

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

it('должен ответить 200, если пришли с корректными МММ и крошки и сравнение ответили', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs?bc_lookup=FORD%23ECOSPORT%2320104320&bc_lookup=KIA%23RIO%2321738448&rid=213')
        .reply(200, {});

    publicApi
        .post('/1.0/user/compare/cars/models/show-without-save?geo_radius=200')
        .reply(200, { models: versusMock });

    const params = {
        first_mark: 'frod',
        first_model: 'ecosport',
        second_mark: 'kia',
        second_model: 'rio',
    };

    return de.run(versus, { context, params }).then(
        (result) => {
            expect(Array.isArray(result.versus)).toEqual(true);
        },
        () => Promise.reject('UNEXPECTED_REJECT'));
});

it('должен ответить 500, если пришли с корректными МММ и сравнение и крошки ответили, но ответ некоректный', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs?bc_lookup=FORD%23ECOSPORT%2320104320&bc_lookup=KIA%23RIO%2321738448&rid=213')
        .reply(200, {});

    publicApi
        .post('/1.0/user/compare/cars/models/show-without-save?geo_radius=200')
        .reply(200, { models: [] });

    const params = {
        first_mark: 'frod',
        first_model: 'ecosport',
        second_mark: 'kia',
        second_model: 'rio',
    };

    return de.run(versus, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: ERROR_ID.REQUIRED_BLOCK_FAILED,
                },
            });

            expect(result.error.reason).toMatchObject({
                error: {
                    id: ERROR_ID.VERSUS_UNEXPECTED_RESULT,
                    status_code: 500,
                },
            });
        });
});

it('должен ответить 404, если пришли с несуществующими МММ', () => {
    publicApi
        .post('/1.0/user/compare/cars/models/show-without-save?geo_radius=200')
        .reply(404, {
            error: 'NOT_FOUND',
            status: 'ERROR',
            detailed_error: 'Can\'t find id 123 on SUPER_GEN level',
        });

    const params = {
        first_mark: 'aaa',
        first_model: 'bbb',
        second_mark: 'ccc',
        second_model: 'ddd',
    };

    return de.run(versus, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: ERROR_ID.MODELS_NOT_FOUND,
                    status_code: 404,
                },
            });
        });
});
