const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const cardOld = require('./card-old');

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

it('должен ответить 301, если карточка ответила 200 и в объявлении есть нужные данные для редиректа', () => {
    publicApi.get('/1.0/offer/cars/200-ok').reply(200, {
        offer: {
            additional_info: {},
            car_info: {
                mark_info: {
                    code: 'MARK_CODE',
                },
                model_info: {
                    code: 'MODEL_CODE',
                },
            },
            category: 'CARS',
            id: '200-nomodel',
            section: 'used',
        },
    });

    return de.run(cardOld, {
        context,
        params: { category: 'cars', sale_id: '200', sale_hash: 'ok' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'CARD_OLD_TO_NEW',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/used/sale/mark_code/model_code/200-ok/',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 301, если карточка ответила 200 и в объявлении есть нужные данные для редиректа, сохранив GET-параметры', () => {
    publicApi.get('/1.0/offer/cars/200-ok').reply(200, {
        offer: {
            additional_info: {},
            car_info: {
                mark_info: {
                    code: 'MARK_CODE',
                },
                model_info: {
                    code: 'MODEL_CODE',
                },
            },
            category: 'CARS',
            id: '200-nomodel',
            section: 'used',
        },
    });

    req.fullUrl = '/cars/used/sale/200-ok/?from=test';
    return de.run(cardOld, {
        context,
        params: { category: 'cars', sale_id: '200', sale_hash: 'ok' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'CARD_OLD_TO_NEW',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/used/sale/mark_code/model_code/200-ok/?from=test',
                    status_code: 301,
                },
            });
        },
    );
});

it('должен ответить 404, если карточка ответила 200, но в объявлении нет нужных данных для редиректа', () => {
    publicApi
        .get('/1.0/offer/cars/200-nomodel')
        .times(Number.POSITIVE_INFINITY)
        .reply(200, {
            offer: {
                additional_info: {},
                car_info: {
                    mark_info: {
                        code: 'MARK_CODE',
                    },
                },
                category: 'CARS',
                id: '200-nomodel',
                section: 'used',
            },
        });

    return de.run(cardOld, {
        context,
        params: { category: 'cars', sale_id: '200', sale_hash: 'nomodel' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_OLD_BAD_OFFER',
                    status_code: 404,
                },
            });
        },
    );
});

it('должен ответить 404, если карточка ответила 404', () => {
    publicApi.get('/1.0/offer/cars/404-notfound').reply(404, { error: 'OFFER_NOT_FOUND' });

    return de.run(cardOld, {
        context,
        params: { category: 'cars', sale_id: '404', sale_hash: 'notfound' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'OFFER_NOT_FOUND',
                    status_code: 404,
                },
            });
        });
});

it('должен ответить 500, если карточка ответила 500', () => {
    publicApi
        .get('/1.0/offer/cars/500-error')
        .times(2)
        .reply(500, { error: 'UNKNOWN_ERROR' });

    return de.run(cardOld, {
        context,
        params: { category: 'cars', sale_id: '500', sale_hash: 'error' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'CARD_OLD_ERROR',
                    status_code: 500,
                },
            });
        },
    );
});
