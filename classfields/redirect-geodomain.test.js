const { baseDomain: BASE_DOMAIN } = require('auto-core/appConfig');

const geodomainRedirects = require('./redirect-geodomain');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    req.headers['x-forwarded-host'] = BASE_DOMAIN;

    res = createHttpRes();
});

it('не должен ничего делать, если не геодомен', () => {
    return new Promise((done) => {
        req.method = 'GET';
        geodomainRedirects(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен ничего делать, если геодомен, но метод не GET', () => {
    return new Promise((done) => {
        req.headers['x-forwarded-host'] = 'moscow.' + BASE_DOMAIN;
        req.method = 'POST';
        geodomainRedirects(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен ничего делать, если метод GET, но неизвестный поддомен', () => {
    return new Promise((done) => {
        req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
        req.method = 'GET';
        geodomainRedirects(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

describe('геодомен, метод GET', () => {
    beforeEach(() => {
        req.headers['x-forwarded-host'] = 'moscow.' + BASE_DOMAIN;
        req.method = 'GET';
    });

    it('должен сделать редирект с гео, если он должен быть в урле', () => {
        return new Promise((done) => {
            req.url = '/cars/all/';
            geodomainRedirects(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'GEODOMAIN',
                    data: {
                        location: 'https://autoru_frontend.base_domain/moskva/cars/all/',
                        status: 301,
                    },
                });
                done();
            });
        });
    });

    it('должен сделать редирект без гео, если он не должен быть в урле', () => {
        return new Promise((done) => {
            req.url = '/catalog/cars/';
            geodomainRedirects(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'GEODOMAIN',
                    data: {
                        location: 'https://autoru_frontend.base_domain/catalog/cars/?geo_id=213',
                        status: 301,
                    },
                });
                done();
            });
        });
    });

    it('должен сделать редирект без гео, если урл неизвестен', () => {
        return new Promise((done) => {
            req.url = '/404/';
            geodomainRedirects(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'GEODOMAIN',
                    data: {
                        location: 'https://autoru_frontend.base_domain/404/?geo_id=213',
                        status: 301,
                    },
                });
                done();
            });
        });
    });
});
