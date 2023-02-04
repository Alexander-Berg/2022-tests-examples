const redirectCarsListingUrl = require('./redirect-cars-listing');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить с невалидного урла из списка', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'listing';
        req.url = '/great-wall/';

        redirectCarsListingUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_CARS_URLS_TO_VALID',
                data: {
                    location: '/cars/great_wall/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('не должен сработать редирект', () => {
    return new Promise((done) => {
        req.router.route.getName = () => 'listing';
        req.url = '/acura/';

        redirectCarsListingUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
