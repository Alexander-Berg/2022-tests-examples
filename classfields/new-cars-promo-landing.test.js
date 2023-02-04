jest.mock('auto-core/lib/util/getBunkerDict', () => jest.fn());

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const controller = require('./new-cars-promo-landing');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const getBunkerDictMock = require('auto-core/lib/util/getBunkerDict');

const { WEEK } = require('auto-core/lib/consts');

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

it('отдаст страницу с лендингом', () => {
    const params = { category: 'cars', mark: 'bmw' };
    getBunkerDictMock.mockImplementation(() => ({
        landing_pages_for_new: [
            {
                mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' },
                timestamps: {
                    start: new Date(Date.now() - WEEK),
                    end: new Date(Date.now() + WEEK),
                },
            },
        ],
    }));

    return de.run(controller, { context, params }).then(
        (result) => expect(result).toBeDefined(),
        () => Promise.reject('UNEXPECTED_REJECT'));
});

describe('должен средиректить на листинг', () => {
    it('если нет данных о компаниях в бункере', () => {
        const params = { category: 'cars', mark: 'bmw' };
        getBunkerDictMock.mockImplementation(() => {});

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_UNKNOWN_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/new/',
                        status_code: 301,
                    },
                });
            });
    });

    it('если нет данных о компаниях для этой марки', () => {
        const params = { category: 'cars', mark: 'audi' };
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                { mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' } },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_UNKNOWN_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/audi/new/',
                        status_code: 301,
                    },
                });
            });
    });

    it('если нет данных о компаниях для этой марки-модели', () => {
        const params = { category: 'cars', mark: 'bmw', model: 'x2' };
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                { mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' } },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_UNKNOWN_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/x2/new/',
                        status_code: 301,
                    },
                });
            });
    });

    it('если есть компания для этой марки, но у неё не выставлены сроки', () => {
        const params = { category: 'cars', mark: 'bmw' };
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                { mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' } },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_UNKNOWN_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/new/',
                        status_code: 301,
                    },
                });
            });
    });

    it('если есть компания для этой марки, но она ещё не стартовала', () => {
        const params = { category: 'cars', mark: 'bmw' };
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                {
                    mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' },
                    timestamps: {
                        start: 'Sat Nov 01 2121 00:00:00 GMT+0300 (Moscow Standard Time)',
                        end: 'Sun Nov 30 2121 00:00:00 GMT+0300 (Moscow Standard Time)',
                    },
                },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_FUTURE_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/new/',
                        status_code: 301,
                    },
                });
            });
    });

    it('если есть компания для этой марки, но она уже прошла', () => {
        const params = { category: 'cars', mark: 'bmw' };
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                {
                    mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' },
                    timestamps: {
                        start: 'Sun Nov 01 2020 00:00:00 GMT+0300 (Moscow Standard Time)',
                        end: 'Mon Nov 30 2020 00:00:00 GMT+0300 (Moscow Standard Time)',
                    },
                },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_PAST_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/new/',
                        status_code: 301,
                    },
                });
            });
    });
});

describe('в режиме дебага', () => {
    const params = { category: 'cars', mark: 'bmw', _debug: 'true' };

    it('отдаст страницу, если есть компания для этой марки, но у неё не выставлены сроки', () => {
        context.req.isInternalNetwork = true;
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                { mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' } },
            ],
        }));

        return de.run(controller, { context, params }).then(
            (result) => expect(result).toBeDefined(),
            () => Promise.reject('UNEXPECTED_REJECT'));
    });

    it('отдаст страницу, если есть компания для этой марки, но она ещё не стартовала', () => {
        context.req.isInternalNetwork = true;
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                {
                    mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' },
                    timestamps: {
                        start: 'Sat Nov 01 2121 00:00:00 GMT+0300 (Moscow Standard Time)',
                        end: 'Sun Nov 30 2121 00:00:00 GMT+0300 (Moscow Standard Time)',
                    },
                },
            ],
        }));

        return de.run(controller, { context, params }).then(
            (result) => expect(result).toBeDefined(),
            () => Promise.reject('UNEXPECTED_REJECT'));
    });

    it('отдаст страницу, если есть компания для этой марки, но она уже прошла', () => {
        context.req.isInternalNetwork = true;
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                {
                    mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' },
                    timestamps: {
                        start: 'Sun Nov 01 2020 00:00:00 GMT+0300 (Moscow Standard Time)',
                        end: 'Mon Nov 30 2020 00:00:00 GMT+0300 (Moscow Standard Time)',
                    },
                },
            ],
        }));

        return de.run(controller, { context, params }).then(
            (result) => expect(result).toBeDefined(),
            () => Promise.reject('UNEXPECTED_REJECT'));
    });

    it('не отдаст страницу, если есть компания для этой марки, но пользователь не из внутренней сети', () => {
        context.req.isInternalNetwork = false;
        getBunkerDictMock.mockImplementation(() => ({
            landing_pages_for_new: [
                { mark: { code: 'BMW', name: 'BMW', ru_name: 'БМВ' } },
            ],
        }));

        return de.run(controller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'PROMO_LANDING_UNKNOWN_COMPANY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/bmw/new/',
                        status_code: 301,
                    },
                });
            });
    });
});
