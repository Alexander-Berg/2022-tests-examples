const { baseDomain: BASE_DOMAIN } = require('auto-core/appConfig');
const redirectRemoveGeoFromUrl = require('./redirect-remove-geo');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

jest.mock('auto-core/lib/core.js', () => {
    return {
        susanin: {
            findFirst: jest.fn(),
        },
    };
});

let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    req.router.params = {
        category: 'cars',
        mark: 'audi',
    };
    req.geoIds = [ 213 ];
});

it('должен средиректить с валидной страницы на ту же страницу без гео', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'stats';
        req.path = '/moskva/stats/cars/audi/';
        req.fullUrl = `https://${ BASE_DOMAIN }/moskva/stats/cars/audi/`;

        redirectRemoveGeoFromUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'REMOVE_GEO',
                data: {
                    location: 'https://autoru_frontend.base_domain/stats/cars/audi/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('ничего не должен делать, если страница не для редиректа', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'listing';
        req.url = '/moskva/cars/all/';
        req.fullUrl = `https://${ BASE_DOMAIN }/moskva/cars/all/`;

        redirectRemoveGeoFromUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать, если страница уже без гео', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'stats';
        req.path = '/stats/cars/audi/';
        req.fullUrl = `https://${ BASE_DOMAIN }/stats/cars/audi/`;

        redirectRemoveGeoFromUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
