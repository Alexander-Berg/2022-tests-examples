const redirectOldLikePages = require('./redirect-old-like-pages');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего сделать для обычной ссылки', () => {
    req.router.route.getName = () => 'like';

    redirectOldLikePages(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить роута like-by-category', () => {
    req.router.route.getName = () => 'like-by-category';

    redirectOldLikePages(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: 'https://autoru_frontend.base_domain/like/',
                status: 301,
            },
        });
    });
});

it('должен средиректить роута searches-old и сохранить гет-параметры', () => {
    req.router.route.getName = () => 'searches-old';
    req.router.params = { foo: 'bar' };

    redirectOldLikePages(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: 'https://autoru_frontend.base_domain/like/searches/?foo=bar',
                status: 301,
            },
        });
    });
});
