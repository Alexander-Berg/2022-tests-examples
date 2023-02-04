const redirectSpecialSubdomains = require('./redirect-to-media');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it(`не должен ничего сделать для обычой ссылки`, () => {
    req.headers['x-forwarded-host'] = 'mag.auto.ru';
    req.url = '/';

    redirectSpecialSubdomains(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it(`должен безусловно средиректить mag.auto.ru/review/* в auto.ru`, () => {
    req.headers['x-forwarded-host'] = 'mag.auto.ru';
    req.url = '/review/cars/toyota/camry/3492871/7551817413853346234/';

    redirectSpecialSubdomains(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'MAG_TO_MEDIA',
            data: {
                location: 'https://autoru_frontend.base_domain/review/cars/toyota/camry/3492871/7551817413853346234/',
                status: 301,
            },
        });
    });
});

it(`должен безусловно средиректить mag.auto.ru/reviews/* в auto.ru`, () => {
    req.headers['x-forwarded-host'] = 'mag.auto.ru';
    req.url = '/reviews/cars/';

    redirectSpecialSubdomains(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'MAG_TO_MEDIA',
            data: {
                location: 'https://autoru_frontend.base_domain/reviews/cars/',
                status: 301,
            },
        });
    });
});
