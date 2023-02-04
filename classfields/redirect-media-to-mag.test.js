const redirectMediaToMag = require('./redirect-media-to-mag');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.url = '/reviews/';

    res = createHttpRes();
});

it(`не должен ничего сделать для обычой ссылки`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.url = '/reviews/';

    redirectMediaToMag(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it(`должен средиректить / в mag`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.url = '/?foo=bar';

    redirectMediaToMag(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'DOMAIN_MEDIA_TO_MAG',
            data: {
                location: 'https://mag.autoru_frontend.base_domain/?foo=bar',
                status: 301,
            },
        });
    });
});

it(`должен средиректить /article/ в mag`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.url = '/article/?foo=bar';

    redirectMediaToMag(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'DOMAIN_MEDIA_TO_MAG',
            data: {
                location: 'https://mag.autoru_frontend.base_domain/article/?foo=bar',
                status: 301,
            },
        });
    });
});

it(`должен средиректить /theme/ в mag`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.url = '/theme/?foo=bar';

    redirectMediaToMag(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'DOMAIN_MEDIA_TO_MAG',
            data: {
                location: 'https://mag.autoru_frontend.base_domain/theme/?foo=bar',
                status: 301,
            },
        });
    });
});
