const redirectOldDealers = require('./redirect-old-dealers');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего сделать для обычой ссылки', () => {
    req.url = '/';

    redirectOldDealers(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить /dealers/', () => {
    req.url = '/dealers/';

    redirectOldDealers(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: 'https://autoru_frontend.base_domain/dilery/cars/new/',
                status: 301,
            },
        });
    });
});

it('должен средиректить /dealers/123-456/', () => {
    req.url = '/dealers/';

    redirectOldDealers(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: 'https://autoru_frontend.base_domain/dilery/cars/new/',
                status: 301,
            },
        });
    });
});
