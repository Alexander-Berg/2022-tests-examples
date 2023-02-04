const middleware = require('./redirect-listing-filters');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    req.method = 'GET';
});

describe('редиректы старых фильтров в листинг', () => {
    it('не должен ничего сделать для обычной ссылки', () => {
        req.urlWithoutRegion = '/';

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('должен средиректить старый фильтр в листинг (/motorcycle/all/filters/?drive_key=BELT&mark-model-nameplate=BMW)', () => {
        req.urlWithoutRegion = '/motorcycle/all/filters/?drive_key=BELT&mark-model-nameplate=BMW';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_LISTING_FILTERS',
                data: {
                    location: 'https://autoru_frontend.base_domain/motorcycle/bmw/all/?drive_key=BELT',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить старый фильтр дилера на страницу дилера (/cars/all/filters/?dealer_code=tambov_avto_siti_vaz)', () => {
        req.urlWithoutRegion = '/cars/all/filters/?dealer_code=tambov_avto_siti_vaz';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_LISTING_FILTERS',
                data: {
                    location: 'https://autoru_frontend.base_domain/diler/cars/all/tambov_avto_siti_vaz/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить старый фильтра марки в листинг (/cars/used/marks/)', () => {
        req.urlWithoutRegion = '/cars/used/marks/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_LISTING_FILTERS',
                data: {
                    location: 'https://autoru_frontend.base_domain/cars/used/',
                    status: 301,
                },
            });
        });
    });

    it('не должен средиректить старый фильтра марки в листинг (/cars/used/marks/), если запрос POST', () => {
        req.method = 'POST';
        req.urlWithoutRegion = '/cars/used/marks/';

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('должен средиректить старый фильтр модели в листинг (/cars/used/models/?catalog_filter=mark%3DAUDI)', () => {
        req.urlWithoutRegion = '/cars/used/models/?catalog_filter=mark%3DAUDI';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_LISTING_FILTERS',
                data: {
                    location: 'https://autoru_frontend.base_domain/cars/audi/used/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить старый фильтр модели в листинг (/cars/used/models/), кейс мультивыбора ммм', () => {
        req.urlWithoutRegion = '/cars/used/models/?catalog_filter=mark%3DAUDI&catalog_filter=mark%3DBMW';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_LISTING_FILTERS',
                data: {
                    location: 'https://autoru_frontend.base_domain/cars/used/?catalog_filter=mark%3DAUDI&catalog_filter=mark%3DBMW',
                    status: 301,
                },
            });
        });
    });
});
