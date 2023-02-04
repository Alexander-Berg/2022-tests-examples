const redirectOldPromo = require('./redirect-old-promo');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен сделать редирект для /promo/', () => {
    return new Promise((done) => {
        req.url = '/promo/';
        redirectOldPromo(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('должен сохранить query-параметры', () => {
    return new Promise((done) => {
        req.url = '/promo/history/?foo=bar';
        redirectOldPromo(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_PROMO',
                data: {
                    location: '/history/?foo=bar',
                    status: 301,
                },
            });
            done();
        });
    });
});

it.each([
    '/be-first/',
    '/be-first/?foo=bar',
    '/mkad54/',
    '/mkad54/?foo=bar',
    '/promo/new-prices/',
    '/promo/new-prices/?from=foo',
    '/promo/safe-number/',
    '/promo/safe-number/?from=foo',
    '/progorod10/',
    '/progorod10/?from=foo',
])('должен сделать редирект для %s', (url) => {
    expect.assertions(1);
    req.url = url;

    redirectOldPromo(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'OLD_PROMO',
            data: {
                location: '/',
                status: 301,
            },
        });
    });
});
