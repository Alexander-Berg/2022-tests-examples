'use strict';

const MockDate = require('mockdate');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const appPromoMiddleware = require('./app-promo');

let req;
let res;
beforeEach(() => {
    MockDate.set('2020-05-20');
    req = createHttpReq();
    res = createHttpRes();

    req.uatraits = {
        OSFamily: 'iOS',
        OSVersion: '15',
        isMobile: true,
    };

    req.router.route.getData = () => ({
        method: 'GET',
        action: 'build',
    });
    req.router.route.getName = () => 'listing';
});

it(`Должен поставить куки при первом заходе на сервис`, () => {
    return new Promise((resolve) => {
        appPromoMiddleware(req, res, () => {
            expect(res.cookie).toHaveBeenCalledWith('first-visit', 1, { maxAge: 86400000, path: '/' });
            expect(res.cookie).toHaveBeenCalledWith('first-visit-date', Date.now(), { maxAge: 86400000000, path: '/' });
            expect(res.cookie).toHaveBeenCalledWith('second-visit', 1, { maxAge: 172800000, path: '/' });
            resolve();
        });
    });
});

describe('не должен ничего делать', () => {
    it(`если запрос уже завершен`, () => {
        res.headersSent = true;

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если нет данных роутера`, () => {
        req.router = undefined;

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если это не GET запрос`, () => {
        req.router.route.getData = () => ({
            method: 'POST',
            action: 'build',
        });

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если это action не build`, () => {
        req.router.route.getData = () => ({
            method: 'GET',
            action: 'some-action',
        });

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если это XMLHttpRequest`, () => {
        req.router.route.getData = () => ({
            method: 'GET',
            action: 'build',
        });
        req.headers['x-requested-with'] = 'XMLHttpRequest';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если это робот`, () => {
        req.isRobot = true;

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если зашли не в мобильную версию`, () => {
        req.uatraits.isMobile = false;

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it(`если сплешы принудительно выключены`, () => {
        req.router.params = Object.freeze({ nosplash: '1' });

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    [
        { param: 'from', value: 'auto_app' },
        { param: 'from', value: 'autoru-app' },
        { param: 'from', value: 'navigator' },
        { param: 'only-content', value: 'true' },
    ]
        .forEach(({ param, value }) => {
            it(`если пришли из аппов с параметром ` + value, () => {
                req.router.params = Object.freeze({ [param]: value });

                return new Promise((resolve) => {
                    appPromoMiddleware(req, res, () => {
                        expect(res.cookie).not.toHaveBeenCalled();
                        expect(req.promoHeaderState).toBeUndefined();

                        resolve();
                    });
                });
            });
        });
});

describe('для старых телефонов', () => {
    it('должен показывать сплеш для Android <= 6 при вызове с параметром type', () => {
        req.uatraits = {
            OSFamily: 'Android',
            OSVersion: '6.4.2',
            isMobile: true,
        };
        req.router.params = Object.freeze({ bannerType: 'some-banner' });
        // eslint-disable-next-line max-len
        req.headers['user-agent'] = 'Mozilla/5.0 (Linux; Android 6.0.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/4.1 Chrome/63.0.3239.111 Mobile Safari/537.36';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(req.promoHeaderState).toEqual('show');

                resolve();
            });
        });
    });

    it('должен показывать сплеш для iOS <= 13 при вызове с параметром type', () => {
        req.uatraits = {
            OSFamily: 'iOS',
            OSVersion: '13',
            isMobile: true,
        };
        req.router.params = Object.freeze({ bannerType: 'some-banner' });
        // eslint-disable-next-line max-len
        req.headers['user-agent'] = 'Mozilla/5.0 (iPhone; CPU iPhone OS 13_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.4 Mobile/15E148 Safari/604.1';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(req.promoHeaderState).toEqual('show');

                resolve();
            });
        });
    });

    it('не должен показывать сплеш для Android <= 6.0', () => {
        req.uatraits = {
            OSFamily: 'Android',
            OSVersion: '6.4.2',
            isMobile: true,
        };
        // eslint-disable-next-line max-len
        req.headers['user-agent'] = 'Mozilla/5.0 (Linux; Android 6.0.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/4.1 Chrome/63.0.3239.111 Mobile Safari/537.36';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });

    it('не должен показывать сплеш для iOS <= 13', () => {
        req.uatraits = {
            OSFamily: 'iOS',
            OSVersion: '13',
        };
        // eslint-disable-next-line max-len
        req.headers['user-agent'] = 'Mozilla/5.0 (iPhone; CPU iPhone OS 13_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.4 Mobile/15E148 Safari/604.1';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).not.toHaveBeenCalled();
                expect(req.promoHeaderState).toBeUndefined();

                resolve();
            });
        });
    });
});

describe('должен не показывать сплеш', () => {
    it(`и если кука-счётчик стоит уже месяц, задать ей максимальное значение и завести новую куку на показ`, () => {
        req.cookies['promo-header-counter'] = '5';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).toHaveBeenNthCalledWith(4, 'promo-header-shown', 1, { maxAge: 1209600000, path: '/' });
                expect(res.cookie).toHaveBeenNthCalledWith(5, 'promo-header-counter', '99', { maxAge: 86400000000, path: '/' });
                expect(req.promoHeaderState).toEqual('notshow');

                resolve();
            });
        });
    });
});

describe('должен показать сплеш', () => {
    it(`если в параметрах указади тип баннера, но не должен ставить куку`, () => {
        req.router.params = Object.freeze({ bannerType: 'some-banner' });

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).toHaveBeenCalledTimes(4);
                expect(req.promoHeaderState).toEqual('show');

                resolve();
            });
        });
    });

    it(`и поставить недельную куку-счётчик`, () => {
        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).toHaveBeenCalledWith('promo-header-counter', 1, { maxAge: 1209600000, path: '/' });
                expect(req.promoHeaderState).toEqual('show');

                resolve();
            });
        });
    });

    it(`и если стоит максимальная кука-счётчик и нет второй куки на показ, поставить её`, () => {
        req.cookies['promo-header-counter'] = '99';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).toHaveBeenCalledWith('promo-header-shown', 1, { maxAge: 1209600000, path: '/' });
                expect(req.promoHeaderState).toEqual('show');

                resolve();
            });
        });
    });

    it(`если первый заход на листинг`, () => {
        req.router.route.getName = () => 'listing';

        return new Promise((resolve) => {
            appPromoMiddleware(req, res, () => {
                expect(res.cookie).toHaveBeenNthCalledWith(4, 'promo-header-counter', 1, { maxAge: 1209600000, path: '/' });

                resolve();
            });
        });
    });
});

[ 'moto-listing', 'commercial-listing', 'listing' ]
    .forEach((page) => {
        describe('для страницы ' + page, () => {
            it(`должен показать сплеш аппа, поставить общую куку-счётчик и куку для этого сплеша`, () => {
                req.router.route.getName = () => page;

                return new Promise((resolve) => {
                    appPromoMiddleware(req, res, () => {
                        expect(res.cookie).toHaveBeenNthCalledWith(4, 'promo-header-counter', 1, { maxAge: 1209600000, path: '/' });
                        expect(res.cookie).toHaveBeenNthCalledWith(5, 'promo-bottom-banner', 1, { maxAge: 259200000, path: '/' });
                        expect(req.promoHeaderState).toEqual('show');

                        resolve();
                    });
                });
            });

            it(`должен показать сплеш аппа и поставить куку для этого сплеша`, () => {
                req.router.route.getName = () => page;
                req.cookies['promo-header-counter'] = '2';

                return new Promise((resolve) => {
                    appPromoMiddleware(req, res, () => {
                        expect(res.cookie).toHaveBeenNthCalledWith(4, 'promo-bottom-banner', 1, { maxAge: 259200000, path: '/' });
                        expect(req.promoAppBannerState).toEqual('show');

                        resolve();
                    });
                });
            });

            it(`с экспериментом VTF-878_disable_splash_touch_auto и переходом из яндекса, не должны показываться сплеши`, () => {
                req.trafficFrom = 'yandex';
                req.experimentsData = { has: () => true };

                return new Promise((resolve) => {
                    appPromoMiddleware(req, res, () => {
                        expect(req.promoHeaderState).toBeUndefined();

                        resolve();
                    });
                });
            });
        });
    });
