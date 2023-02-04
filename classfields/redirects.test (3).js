'use strict';

const middleware = require('./redirects');
const { baseDomain } = require('auto-core/appConfig');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    req.method = 'GET';
});

it('не должен ничего сделать для обычой ссылки', () => {
    req.url = '/';

    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен убрать марку из /dilery/cars/audi/used/', () => {
    req.router.route.getName = () => 'dealers-listing';
    req.router.params = Object.freeze({ mark: 'audi', section: 'used' });
    req.url = '/dilery/cars/audi/used/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: '/dilery/cars/used/',
                status: 301,
            },
        });
    });
});

it('не должен ничего сделать для /dilery/cars/audi/new/', () => {
    req.router.route.getName = () => 'dealers-listing';
    req.router.params = Object.freeze({ mark: 'audi', section: 'new' });
    req.url = '/dilery/cars/audi/new/';

    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен сделать редирект с match-application на get-best-price', () => {
    req.router.route.getName = () => 'match-application';
    req.router.params = Object.freeze({ category: 'cars', mark: 'audi', section: 'new' });
    req.url = '/cars/new/match-application/audi/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: '/cars/new/get_best_price/audi/',
                status: 301,
            },
        });
    });
});

it('должен сделать редирект с edit на промку app и не потерять гет-параметры', () => {
    req.router.route.getName = () => 'edit';
    req.router.params = Object.freeze({ utm_source: 'some-source', from: 'some-where' });
    req.url = '/edit/';

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: `https://${ baseDomain }/promo/from-web-to-app/?utm_source=some-source&from=some-where`,
            },
        });
    });
});
