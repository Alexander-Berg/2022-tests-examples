const middleware = require('./redirect-my.auto.ru');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

const TESTS = [
    {
        req: {
            host: 'my.autoru_frontend.base_domain',
            url: '/scooters/suzuki/address',
        },
        result: 'https://autoru_frontend.base_domain/reviews/moto/scooters/suzuki/address_110/',
        code: 'REVIEWS_OLD_URL',
    },
    {
        req: {
            host: 'my.autoru_frontend.base_domain',
            url: '/scooters/suzuki/address/?foo=bar',
        },
        result: 'https://autoru_frontend.base_domain/reviews/moto/scooters/suzuki/address_110/?foo=bar',
        code: 'REVIEWS_OLD_URL',
    },
    // @see AUTORUFRONT-15846
    // {
    //     req: {
    //         host: 'autoru_frontend.base_domain',
    //         url: '/reviews/',
    //     },
    //     result: 'https://media.autoru_frontend.base_domain/reviews/',
    //     code: 'REVIEWS_DOMAIN',
    // },
    // {
    //     req: {
    //         host: 'autoru_frontend.base_domain',
    //         url: '/review/cars/ford/focus/20243246/4017004/?from=wizard.model',
    //     },
    //     result: 'https://media.autoru_frontend.base_domain/review/cars/ford/focus/20243246/4017004/?from=wizard.model',
    //     code: 'REVIEWS_DOMAIN',
    // },
];

TESTS.forEach((testCase) => {
    it(`должен средиректить ${ testCase.req.host }${ testCase.req.url } в ${ testCase.result }`, () => {
        req.headers['x-forwarded-host'] = testCase.req.host;
        req.url = testCase.req.url;

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: testCase.code,
                data: {
                    location: testCase.result,
                    status: 301,
                },
            });
        });
    });
});
