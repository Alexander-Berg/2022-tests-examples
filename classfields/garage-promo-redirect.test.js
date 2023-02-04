const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const isMobileApp = require('auto-core/lib/core/isMobileApp');

jest.mock('auto-core/lib/core/isMobileApp');

const controller = require('./garage-promo-redirect');

let context;
let req;
let res;

const CARD = {
    id: '12345',
};

const VIN = 'WVWZZZ1JZYW095538';
const LICENSE_PLATE = 'T850PY177';

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('редирект на карточку гаража, если в гараже есть машина с заданным VIN', () => {
    const card = {
        id: '12345',
        vehicle_info: {
            documents: {
                vin: VIN,
            },
        },
    };
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ card ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice', vin_or_license_plate: VIN };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/12345/?promo=fitservice',
                },
            });
        });
});

it('редирект на карточку гаража, если в гараже есть машина с заданным ГРЗ', () => {
    const card = {
        id: '12345',
        vehicle_info: {
            documents: {
                license_plate: LICENSE_PLATE,
            },
        },
    };
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ card ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice', vin_or_license_plate: LICENSE_PLATE };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/12345/?promo=fitservice',
                },
            });
        });
});

it('редирект на страницу добавления машины в гараж, в параметрах есть vin/грз', () => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ CARD ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice', vin_or_license_plate: LICENSE_PLATE };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_ADD_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/add/?promo=fitservice&vin_or_license_plate=T850PY177',
                },
            });
        });
});

it('редирект на страницу добавления машины в гараж в таче, в параметрах есть vin/грз', () => {
    isMobileApp.mockImplementationOnce(() => true);
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ CARD ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice', vin_or_license_plate: LICENSE_PLATE };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_ADD_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/add/current/?promo=fitservice&vin_or_license_plate=T850PY177',
                },
            });
        });
});

it('редирект на первую карточку гаража, в параметрах нет vin/грз', () => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ CARD, { id: '4567' } ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/12345/?promo=fitservice',
                },
            });
        });
});

it('редирект на лендинг гаража, если нет тачки в гараже', () => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_LANDING',
                    id: 'REDIRECTED',
                    location: '/garage/?promo=fitservice',
                },
            });
        });
});

it('редирект на карточку гаража, если есть тачка в гараже', () => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [ CARD ],
            status: 'SUCCESS',
        });

    const params = { promo: 'fitservice' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_TO_CARD',
                    id: 'REDIRECTED',
                    location: `/garage/${ CARD.id }/?promo=fitservice`,
                },
            });
        });
});

it('отдаст 404, если нет параметра promo', () => {
    return de.run(controller, { context, params: {} }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                },
            });
        });
});
