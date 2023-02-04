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

it('должен сделать редирект с edit на промку app и не потерять гет-параметры', () => {
    req.router.route.getName = () => 'form';
    req.router.params = {
        form_type: 'edit',
        section: 'used',
        category: 'cars',
        utm_source: 'some-source',
        from: 'some-where',
    };
    req.isMobilePoffer = true;
    req.uatraits.isMobile = true;

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: `https://${ baseDomain }/promo/from-web-to-app/?utm_source=some-source&from=some-where`,
            },
        });
    });
});

describe('новая форма', () => {
    it('не редиректит с edit на промку app под экспом', () => {
        req.router.route.getName = () => 'form';
        req.router.params = {
            form_type: 'edit',
            section: 'used',
            category: 'cars',
            sale_id: 'sale_id',
            sale_hash: 'sale_hash',
        };
        req.isMobilePoffer = true;
        req.uatraits.isMobile = true;

        req.experimentsData = {
            has: () => true,
        };

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('редиректит в промку, если зашли без экспа на мобилке', () => {
        req.router.route.getName = () => 'form';
        req.router.params = {
            form_type: 'add',
            section: 'used',
            category: 'cars',
        };
        req.experimentsData = {
            has: () => false,
        };

        req.isMobilePoffer = true;
        req.uatraits.isMobile = true;

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'REQUIRED_REDIRECT',
                data: {
                    location: `https://${ baseDomain }/promo/from-web-to-app/`,
                },
            });
        });
    });

    it('редиректит на старую форму для и ком транса, для десктопа, тк нет ню поффер реализации', () => {
        req.router.route.getName = () => 'form';
        req.router.params = {
            form_type: 'add',
            section: 'used',
            category: 'trucks',
        };

        req.experimentsData = {
            has: () => true,
        };

        req.isMobilePoffer = false;
        req.uatraits.isMobile = false;

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'REQUIRED_REDIRECT',
                data: {
                    location: `https://${ baseDomain }/trucks/add/`,
                },
            });
        });
    });

    it('редиректит на старую форму для мото, для десктопа, тк нет ню поффер реализации', () => {
        req.router.route.getName = () => 'form';
        req.router.params = {
            form_type: 'add',
            section: 'used',
            category: 'moto',
        };

        req.experimentsData = {
            has: () => true,
        };

        req.isMobilePoffer = false;
        req.uatraits.isMobile = false;

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'REQUIRED_REDIRECT',
                data: {
                    location: `https://${ baseDomain }/moto/add/`,
                },
            });
        });
    });
});
