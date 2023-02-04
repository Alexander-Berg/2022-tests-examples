const de = require('descript');

const contoller = require('./dealer-page');

const createContext = require('auto-core/server/descript/createContext');
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

describe('обработка ответа /1.0/salon/<salon>', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/search/moto')
            .query(true)
            .reply(200, {
                offers: [],
            });

        publicApi
            .get('/1.0/search/moto/breadcrumbs')
            .query(true)
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });
    });

    it('должен ответить 200, если ручка ответила 200', () => {
        publicApi
            .get('/1.0/salon/motopole_sankt_peterburg')
            .reply(200, {
                salon: { dealer_id: '123' },
                status: 'SUCCESS',
            });

        const params = {
            category: 'moto',
            section: 'all',
            dealer_code: 'motopole_sankt_peterburg',
            moto_category: 'MOTORCYCLE',
        };

        return de.run(contoller, { context, params }).then(
            (result) => {
                expect(result).toMatchObject({
                    salonInfo: {
                        dealer_id: '123',
                    },
                });
            });
    });

    it('должен ответить 404, если ручка ответила 404', () => {
        publicApi
            .get('/1.0/salon/motopole_sankt_peterburg')
            .reply(404);

        const params = {
            category: 'moto',
            section: 'all',
            dealer_code: 'motopole_sankt_peterburg',
            moto_category: 'MOTORCYCLE',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'SALON_NOT_FOUND',
                        status_code: 404,
                    },
                });
            });
    });

    it('должен ответить 500, если ручка ответила 500', () => {
        publicApi
            .get('/1.0/salon/motopole_sankt_peterburg')
            .times(2)
            .reply(500);

        const params = {
            category: 'moto',
            section: 'all',
            dealer_code: 'motopole_sankt_peterburg',
            moto_category: 'MOTORCYCLE',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'REQUIRED_BLOCK_FAILED',
                        path: '.salonInfo',
                    },
                });
            });
    });

    it('должен ответить 500, если ручка не ответила', () => {
        publicApi
            .get('/1.0/salon/motopole_sankt_peterburg')
            .delay(1000)
            .times(2)
            .reply(200, {
                salon: { dealer_id: '123' },
                status: 'SUCCESS',
            });

        const params = {
            category: 'moto',
            section: 'all',
            dealer_code: 'motopole_sankt_peterburg',
            moto_category: 'MOTORCYCLE',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'REQUIRED_BLOCK_FAILED',
                        path: '.salonInfo',
                    },
                });
            });
    });
});
