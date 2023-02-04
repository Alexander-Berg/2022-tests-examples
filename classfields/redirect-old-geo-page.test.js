const middleware = require('./redirect-old-geo-page');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    req.method = 'GET';
});

it('не должен ничего сделать для обычой ссылки', () => {
    req.url = '/';

    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить /geo/ на морду', () => {
    req.url = '/geo/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'MOBILE_OLD_GEOPAGE',
            data: {
                location: 'https://autoru_frontend.base_domain/',
                status: 301,
            },
        });
    });
});
