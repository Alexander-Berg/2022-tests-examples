const middleware = require('./redirects');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить /my/reports/ в /history/?action=scroll-to-reports', () => {
    expect.assertions(1);

    req.url = '/my/reports/';
    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'LK_MY_REPORTS',
            data: {
                location: 'https://autoru_frontend.base_domain/history/?action=scroll-to-reports',
                status: 302,
            },
        });
    });
});

it('должен средиректить /my/reports/?foo=bar в /history/?foo=bar&action=scroll-to-reports', () => {
    expect.assertions(1);

    req.url = '/my/reports/?foo=bar';
    req.query = { foo: 'bar' };
    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'LK_MY_REPORTS',
            data: {
                location: 'https://autoru_frontend.base_domain/history/?foo=bar&action=scroll-to-reports',
                status: 302,
            },
        });
    });
});

describe('страница промокодов', () => {
    it('для десктопов редиректит на страницу кошелька', () => {
        req.url = '/my/promo-codes/?foo=bar';
        req.query = { foo: 'bar' };

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LK_PROMO_CODE_TO_WALLET',
                data: {
                    location: 'https://autoru_frontend.base_domain/my/wallet/?foo=bar',
                    status: 302,
                },
            });
        });
    });

    it('для мобилок с редиректит на тач версию', () => {
        req.url = '/my/promo-codes/?foo=bar';
        req.query = { foo: 'bar' };
        req.uatraits.isMobile = true;
        // так как такого урла нет в десктопе, то роутера тоже нет
        req.router = null;

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'DESKTOP_TO_MOBILE',
                data: {
                    location: 'https://autoru_frontend.base_domain/my/promo-codes/?foo=bar',
                    status: 302,
                },
            });
        });
    });
});
