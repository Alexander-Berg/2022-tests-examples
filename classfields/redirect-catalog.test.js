const redirectsCatalog = require('./redirect-catalog');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего делать для урла /catalog/cars/', () => {
    expect.assertions(1);

    req.url = '/catalog/cars/';
    redirectsCatalog(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен ничего делать для урла /cars/bmw/all/', () => {
    expect.assertions(1);

    req.router.route.getName.mockReturnValue('listing-cars');
    req.url = '/cars/bmw/all/';
    redirectsCatalog(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен ничего делать для урла /catalog/cars/bmw/', () => {
    expect.assertions(1);

    req.url = '/catalog/cars/bmw/';
    redirectsCatalog(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить для урла /cars/catalog/?from=1', () => {
    expect.assertions(1);

    req.url = '/cars/catalog/?from=1';
    redirectsCatalog(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'CATALOG_FIX_URL',
            data: {
                location: '/catalog/cars/?from=1',
                status: 301,
            },
        });
    });
});

it('должен средиректить для урла /catalog/bmw/?from=1', () => {
    expect.assertions(1);

    req.url = '/catalog/bmw/?from=1';
    redirectsCatalog(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'CATALOG_FIX_URL',
            data: {
                location: '/catalog/cars/bmw/?from=1',
                status: 301,
            },
        });
    });
});

it('должен средиректить для урла /omsk/catalog/cars/bmw/?from=1', () => {
    expect.assertions(1);

    req.router.route.getName.mockReturnValue('catalog-listing');
    req.url = '/omsk/catalog/cars/bmw/?from=1';
    req.urlWithoutRegion = '/catalog/cars/bmw/?from=1';
    redirectsCatalog(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'CATALOG_FIX_GEO',
            data: {
                location: '/catalog/cars/bmw/?from=1',
                status: 301,
            },
        });
    });
});
