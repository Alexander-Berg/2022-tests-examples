const middleware = require('./redirect-reviews');

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

it(`должен средиректить /reviews/?page=1 в /reviews/`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.router.route.getData.mockImplementation(jest.fn(() => ({
        controller: 'reviews-index',
    })));
    req.query = { page: '1' };
    req.url = '/reviews/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REVIEWS_PAGE1',
            data: {
                location: 'https://autoru_frontend.base_domain/reviews/',
                status: 301,
            },
        });
    });
});

it(`должен средиректить /reviews/?page=1&foo=bar в /reviews/?foo=bar`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.router.route.getData.mockImplementation(jest.fn(() => ({
        controller: 'reviews-index',
    })));
    req.query = { foo: 'bar', page: '1' };
    req.url = '/reviews/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REVIEWS_PAGE1',
            data: {
                location: 'https://autoru_frontend.base_domain/reviews/?foo=bar',
                status: 301,
            },
        });
    });
});

it(`должен средиректить категорию crane_hydraulics в truck`, () => {
    req.headers['x-forwarded-host'] = 'media.autoru_frontend.base_domain';
    req.router.params = { category: 'crane_hydraulics' };
    req.url = '/reviews/trucks/crane_hydraulics/daf/?sort=relevance-exp1-desc';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REVIEWS_CRANE_HYDRAULICS_TO_TRUCK',
            data: {
                location: 'https://autoru_frontend.base_domain/reviews/trucks/truck/daf/?sort=relevance-exp1-desc',
                status: 301,
            },
        });
    });
});
