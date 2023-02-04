const redirectInvalidPagesToMain = require('./redirect-invalid-pages-to-main');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить с невалидного урла - nikolay', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'index';
        req.url = '/home/nikolay/GV/';

        redirectInvalidPagesToMain(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'INVALID_PAGES_TO_MAIN',
                data: {
                    location: '/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить с невалидного урла - wwwboards', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'index';
        req.url = '/wwwboards/test-test/';

        redirectInvalidPagesToMain(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'INVALID_PAGES_TO_MAIN',
                data: {
                    location: '/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('не должен сработать редирект', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'index';
        req.url = '/home/nikolay/';

        redirectInvalidPagesToMain(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
