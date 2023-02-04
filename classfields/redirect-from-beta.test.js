const redirect = require('./redirect-from-beta');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const TESTS = [
    {
        url: '/moskva/beta/motorcycle/all/',
        redirect: true,
        location: '/moskva/motorcycle/all/',
    },
    {
        url: '/beta/motorcycle/all/',
        redirect: true,
        location: '/motorcycle/all/',
    },
    {
        url: '/motorcycle/beta/all/',
        redirect: false,
    },
    {
        url: '/atv/beta/used/',
        redirect: false,
    },
    {
        url: '/moscow/beta/motorcycle/beta/all/',
        redirect: true,
        location: '/moscow/motorcycle/beta/all/',
    },
    {
        url: '/beta/motorcycle/beta/all/',
        redirect: true,
        location: '/motorcycle/beta/all/',
    },
    {
        url: '/motorcycle/new/sale/beta/rr_2t_125_200/3290144-9eed7fd2/',
        routeName: 'card',
        redirect: false,
    },
];

let res;
let req;
beforeEach(() => {
    res = createHttpRes();
    req = createHttpReq();
});

TESTS.forEach((test) => {
    if (test.redirect) {
        it(`Должен редиректить ${ test.url } на ${ test.location }`, () => {
            req.originalUrl = test.url;
            req.router.route.getName.mockImplementation(() => test.routeName || 'some-random-route');

            redirect(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'LISTING_FROM_BETA',
                    data: {
                        location: test.location,
                        status: 301,
                    },
                });
            });
        });
    } else {
        it(`Не должен редиректить ${ test.url }`, () => {
            req.originalUrl = test.url;
            req.router.route.getName.mockImplementation(() => test.routeName || 'some-random-route');

            redirect(req, res, (error) => {
                expect(error).toBeUndefined();
            });
        });
    }
});
