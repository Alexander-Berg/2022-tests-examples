/**
 * @jest-environment node
 */
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');
const redirectUpperCaseMarkModel = require('./redirect-upper-case-mark-model');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query(true)
        .reply(200, {});
});

it('должен средиректить upperCase марку на lowerCase', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BMW' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/BMW/all/';
        req.path = '/cars/BMW/all/';

        redirectUpperCaseMarkModel(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.UPPERCASE_MARK_MODEL_TO_LOWERCASE,
                data: {
                    location: '/cars/bmw/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить upperCase модель на lowerCase', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BMW', model: '3ER' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/bmw/3ER/all/';
        req.path = '/cars/bmw/3ER/all/';

        redirectUpperCaseMarkModel(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.UPPERCASE_MARK_MODEL_TO_LOWERCASE,
                data: {
                    location: '/cars/bmw/3er/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить upperCase марку и модель на lowerCase', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BMW', model: '3ER' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/BMW/3ER/all/';
        req.path = '/cars/BMW/3ER/all/';

        redirectUpperCaseMarkModel(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.UPPERCASE_MARK_MODEL_TO_LOWERCASE,
                data: {
                    location: '/cars/bmw/3er/all/',
                    status: 301,
                },
            });
            done();
        });

    });
});

it('ничего не должен делать при несуществующей марке или модели', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query(true)
        .reply(404, {});

    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'NOT_EXIST', model: '3ER' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/NOT_EXIST/3ER/all/';
        req.path = '/cars/NOT_EXIST/3ER/all/';

        redirectUpperCaseMarkModel(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
