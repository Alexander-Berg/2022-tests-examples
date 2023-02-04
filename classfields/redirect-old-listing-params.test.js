jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());
const isMobileApp = require('auto-core/lib/core/isMobileApp');
const redirectOldListingParams = require('./redirect-old-listing-params');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    jest.resetModules();
});

it('должен убрать поколение из урла для новых если это не маркетплейс', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'MITSUBISHI', model: 'OUTLANDER', generation: '123' } ],
            section: 'new',
            from: 'marketplace',
        };
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_REMOVE_BAD_PARAMS_FOR_NEW_SECTION',
                data: {
                    location: '/cars/mitsubishi/outlander/new/?from=marketplace',
                },
            });
            done();
        });
    });
});

it('не должен убрать поколение и конфигурацию из урла для новых если это маркетплейс', () => {
    return new Promise((done) => {
        req.experimentsData.has = jest.fn().mockReturnValue(true);
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'MITSUBISHI', model: 'OUTLANDER', generation: '123', configuration: '123' } ],
            section: 'new',
        };
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toBe(undefined);
            done();
        });
    });
});

it('должен убрать все поколения из параметров поиска', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [
                { mark: 'MITSUBISHI', model: 'OUTLANDER', generation: '123' },
                { mark: 'BMW', model: '3ER', generation: '456' },
            ],
            section: 'new',
        };
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_REMOVE_BAD_PARAMS_FOR_NEW_SECTION',
                data: {
                    location: '/cars/new/?catalog_filter=mark%3DMITSUBISHI%2Cmodel%3DOUTLANDER&catalog_filter=mark%3DBMW%2Cmodel%3D3ER',
                },
            });
            done();
        });
    });
});

it('должен убрать поколение из параметров поиска и оставить цену в чпу', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [
                { mark: 'MITSUBISHI', model: 'OUTLANDER', generation: '123' },
            ],
            price_to: '1900000',
            section: 'new',
        };
        req.geoIds = [];
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_REMOVE_BAD_PARAMS_FOR_NEW_SECTION',
                data: {
                    location: '/cars/mitsubishi/outlander/new/do-1900000/',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для moto-listing в www-mobile', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => true);
        req.router.params = {
            category: 'moto',
            moto_category: 'atv',
            page_num_offers: '2',
        };
        req.router.route.getName = () => 'moto-listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/atv/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для commercial-listing в www-mobile', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => true);
        req.router.params = {
            category: 'trucks',
            trucks_category: 'lcv',
            page_num_offers: '2',
        };
        req.router.route.getName = () => 'commercial-listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/lcv/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для listing в www-mobile', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => true);
        req.router.params = {
            page_num_offers: '2',
        };
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/cars/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для listing в www-desktop', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => false);
        req.router.params = {
            page_num_offers: '2',
            category: 'cars',
        };
        req.router.route.getName = () => 'listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/cars/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для moto-listing в www-desktop', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => false);
        req.router.params = {
            page_num_offers: '2',
            category: 'moto',
            moto_category: 'motorcycle',
        };
        req.router.route.getName = () => 'moto-listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/motorcycle/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для commercial-listing в www-desktop', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => false);
        req.router.params = {
            page_num_offers: '2',
            category: 'trucks',
            trucks_category: 'lcv',
        };
        req.router.route.getName = () => 'commercial-listing';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/lcv/all/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для dealer-page в www-desktop', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => false);
        req.router.params = {
            page_num_offers: '2',
            category: 'cars',
            dealer_code: 'pipos',
        };
        req.router.route.getName = () => 'dealer-page';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/diler/cars/all/pipos/?page=2',
                },
            });
            done();
        });
    });
});

it('должен конвертировать параметры для dealer-page-official в www-desktop', () => {
    return new Promise((done) => {
        isMobileApp.mockImplementation(() => false);
        req.router.params = {
            page_num_offers: '2',
            category: 'cars',
            dealer_code: 'pipos',
        };
        req.router.route.getName = () => 'dealer-page-official';

        redirectOldListingParams(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'LISTING_SEARCHER_TO_PUBLICAPI',
                data: {
                    location: '/diler-oficialniy/cars/all/pipos/?page=2',
                },
            });
            done();
        });
    });
});
