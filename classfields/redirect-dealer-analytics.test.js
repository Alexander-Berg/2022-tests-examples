const middleware = require('./redirect-dealer-analytics');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it(`не должен ничего сделать для обычой ссылки`, () => {
    req.headers['x-forwarded-host'] = 'auto.ru';
    req.url = '/';

    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

describe('auto.ru/dealer/analytics/* -> mag.auto.ru', () => {
    const TESTS = [
        {
            desktop: '/dealer/analytics/',
            mobile: 'https://mag.autoru_frontend.base_domain/tag/research/',
        },
        {
            desktop: '/dealer/analytics/?from=foo',
            mobile: 'https://mag.autoru_frontend.base_domain/tag/research/?from=foo',
        },
        {
            desktop: '/dealer/analytics/srok_prodaji/',
            mobile: 'https://mag.autoru_frontend.base_domain/article/srokprodaji/',
        },
        {
            desktop: '/dealer/analytics/srok_prodaji/?from=foo',
            mobile: 'https://mag.autoru_frontend.base_domain/article/srokprodaji/?from=foo',
        },
    ];

    TESTS.forEach((testCase) => {
        it(`должен средиректить ${ testCase.desktop } в ${ testCase.mobile }`, () => {
            req.url = testCase.desktop;

            middleware(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'DEALER_ANALYTICS_TO_MAG',
                    data: {
                        location: testCase.mobile,
                        status: 301,
                    },
                });
            });
        });
    });

});
