const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const cardGroupOld = require('./card-group-old');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const techparamResponse = {
    entities: [
        {
            mark_info: {
                code: 'BMW',
            },
            model_info: {
                code: '8ER',
            },
            super_gen: {
                id: '21315985',
            },
            configuration: {
                id: '21595650',
            },
            tech_param: {
                id: '21595652',
            },
        },
    ],
};

const complectationResponse = {
    entities: [
        {
            complectation: {
                id: '21658810',
                name: '840d xDrive',
            },
        },
        {
            complectation: {
                id: '21658812',
                name: '840d xDrive 2',
            },
        },
    ],
};

it('должен ответить 301, если поиск ответил 200 и в ответе есть нужные данные для редиректа (техпарам)', () => {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, techparamResponse);

    const params = {
        category: 'cars',
        section: 'new',
        tech_param_id: '20657612',
    };

    return de.run(cardGroupOld, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/new/group/bmw/8er/21315985-21595650/?tech_param_id=20657612',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 301, если поиск ответил 200 и в ответе есть нужные данные для редиректа (техпарам и комплектация)', () => {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, techparamResponse);

    publicApi
        .get('/1.0/reference/catalog/cars/complectations')
        .query(true)
        .reply(200, complectationResponse);

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '21658812',
        tech_param_id: '20657612',
    };

    return de.run(cardGroupOld, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'REDIRECTED',
                    // eslint-disable-next-line max-len
                    location: 'https://autoru_frontend.base_domain/cars/new/group/bmw/8er/21315985-21595650/?tech_param_id=20657612&complectation_name=840d%20xDrive%202',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 404, если поиск ответил 200, но в ответе нет нужных данных для редиректа', () => {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, { entities: [] });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '21631663',
        tech_param_id: '20657612',
    };

    return de.run(cardGroupOld, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_GROUP_INVALID_TECH_PARAM_OR_COMPLECTATION',
                    status_code: 404,
                },
            });
        },
    );
});

it('должен ответить 404, если поиск ответил 400', () => {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(400, { error: 'BAD_REQUEST' });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: 'some-undefined-id',
        tech_param_id: 'another-undefuned-id',
    };

    return de.run(cardGroupOld, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_GROUP_INVALID_TECH_PARAM_OR_COMPLECTATION',
                    status_code: 404,
                },
            });
        },
    );
});

it('должен ответить 500, если поиск ответил 500', () => {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(500, { error: 'UNKNOWN_ERROR' });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '500-error-id',
        tech_param_id: '500-error-id-too',
    };

    return de.run(cardGroupOld, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_GROUP_OLD_ERROR',
                    status_code: 500,
                },
            });
        },
    );
});
