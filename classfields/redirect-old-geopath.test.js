const { baseDomain: BASE_DOMAIN } = require('auto-core/appConfig');

const redirectMobileOldGeopath = require('./redirect-old-geopath');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    req.method = 'GET';

    res = createHttpRes();
});

it('не должен ничего делать, если геодомен, но метод не GET', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/moscow/`;
        req.method = 'POST';
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен ничего делать, если метод не GET, но геоурл неизвестен', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/moskva/`;
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен ничего делать, если метод GET, но url=/', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/`;
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен ничего делать, если метод GET, но старый и новый алиас совпадают', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/voronezh/lcv/all/`;
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('должен сделать редирект со старого геоурла на новый, если он известен', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/moscow/`;
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_OLD_GEOPATH',
                data: {
                    location: 'https://m.autoru_frontend.base_domain/moskva/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен сделать редирект со старого геоурла на новый, если он известен. С сохранением всего запроса', () => {
    return new Promise((done) => {
        req.fullUrl = `https://m.${ BASE_DOMAIN }/moscow/cars/all/?from=search`;
        redirectMobileOldGeopath(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'MOBILE_OLD_GEOPATH',
                data: {
                    location: 'https://m.autoru_frontend.base_domain/moskva/cars/all/?from=search',
                    status: 301,
                },
            });
            done();
        });
    });
});
