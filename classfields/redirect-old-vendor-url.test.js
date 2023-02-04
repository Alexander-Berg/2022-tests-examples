const redirectOldVendorUrl = require('./redirect-old-vendor-url');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить со старого вендора на новый', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { vendor: 'VENDOR1' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/vendor1/all/';

        redirectOldVendorUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'VENDOR_OLD_TO_NEW',
                data: {
                    location: '/cars/vendor-domestic/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('ничего не должен делать, если вендоров несколько', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { vendor: 'VENDOR1' }, { vendor: 'VENDOR2' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/all/?catalog_filter=vendor%3DVENDOR3&catalog_filter=vendor%3DVENDOR10';

        redirectOldVendorUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать с новым вендором', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { vendor: 'VENDOR-DOMESTIC' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/vendor-domestic/all/';

        redirectOldVendorUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать с несуществующим вендором', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { vendor: 'VENDOR99' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/vendor99/all/';

        redirectOldVendorUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
