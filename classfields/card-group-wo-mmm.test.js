const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const cardGroupWoMmm = require('./card-group-wo-mmm');

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

it('должен ответить 301, если поиск ответил 200 и в ответе есть нужные данные для редиректа', () => {
    publicApi
        .get('/1.0/search/cars/new-card')
        .query(true)
        .reply(200, {
            current_complectation: {
                tech_info: {
                    mark_info: {
                        code: 'AUDI',
                    },
                    model_info: {
                        code: 'Q7',
                    },
                    configuration: {
                        id: '111',
                    },
                    super_gen: {
                        id: '222',
                    },
                },
            },
        });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '21631663',
        tech_param_id: '20657612',
    };

    return de.run(cardGroupWoMmm, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/new/group/audi/q7/222-111/',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 301, если поиск ответил 200 и в ответе есть нужные данные для редиректа, сохранив GET-параметры', () => {
    publicApi
        .get('/1.0/search/cars/new-card')
        .query(true)
        .reply(200, {
            current_complectation: {
                tech_info: {
                    mark_info: {
                        code: 'AUDI',
                    },
                    model_info: {
                        code: 'Q7',
                    },
                    configuration: {
                        id: '111',
                    },
                    super_gen: {
                        id: '222',
                    },
                },
            },
        });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '21631663',
        tech_param_id: '20657612',
    };

    req.fullUrl = '/cars/new/group/20657612/21631663?from=test';
    return de.run(cardGroupWoMmm, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/new/group/audi/q7/222-111/?from=test',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 404, если поиск ответил 200, но в ответе нет нужных данных для редиректа', () => {
    publicApi
        .get('/1.0/search/cars/new-card')
        .query(true)
        .reply(200, {
            current_complectation: {
                tech_info: {
                    model_info: {
                        code: 'Q7',
                    },
                },
            },
        });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '21631663',
        tech_param_id: '20657612',
    };

    return de.run(cardGroupWoMmm, { context, params }).then(
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
        .get('/1.0/search/cars/new-card')
        .query(true)
        .reply(400, { error: 'BAD_REQUEST' });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: 'some-undefined-id',
        tech_param_id: 'another-undefuned-id',
    };

    return de.run(cardGroupWoMmm, { context, params }).then(
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
        .get('/1.0/search/cars/new-card')
        .query(true)
        .reply(500, { error: 'UNKNOWN_ERROR' });

    const params = {
        category: 'cars',
        section: 'new',
        complectation_id: '500-error-id',
        tech_param_id: '500-error-id-too',
    };

    return de.run(cardGroupWoMmm, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_GROUP_WO_MMM_ERROR',
                    status_code: 500,
                },
            });
        },
    );
});
