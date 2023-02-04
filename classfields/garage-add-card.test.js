'use strict';

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const controller = require('./garage-add-card');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('авторизация', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?state=NEW&state=USED&rid=225')
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

        publicApi
            .get('/1.0/reference/catalog/cars/suggest')
            .reply(200, {
                car_suggest: {},
                status: 'SUCCESS',
            });
    });

    it('должен сделать редирект на морду гаража, если нет авторизации', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        return de.run(controller, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'GARAGE_ADD_CARD_NOAUTH',
                        id: 'REDIRECTED',
                        location: '/garage/',
                    },
                });
            });
    });

    it('должен отдать данные, если есть авторизации', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        return de.run(controller, { context }).then(
            (result) => {
                expect(result).toMatchObject({
                    breadcrumbsPublicApi: [ { level: 'MARK_LEVEL' } ],
                    carsTechOptions: { year: [] },
                    session: { auth: true },
                });
            });
    });

    it('должен сделать редирект в кабинет дилера, если пришел дилер', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        return de.run(controller, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'GARAGE_TO_CABINET',
                        id: 'REDIRECTED',
                        location: 'https://cabinet.autoru_frontend.base_domain',
                    },
                });
            });
    });
});
