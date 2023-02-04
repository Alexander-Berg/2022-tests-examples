const middleware = require('./page-for-old-browsers');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const AndroidBrowser = require('autoru-frontend/mocks/uatraits/android_browser_4');
const Chrome74AndroidYandexSearch = require('autoru-frontend/mocks/uatraits/chrome_74_android_5.1_YandexSearch');
const Chrome85Android = require('autoru-frontend/mocks/uatraits/chrome_85_android_5.1');
const Chrome86MacOS = require('autoru-frontend/mocks/uatraits/chrome_86_macos_yandex');
const IE10 = require('autoru-frontend/mocks/uatraits/ie_10');
const IE11 = require('autoru-frontend/mocks/uatraits/ie_11');
const Safari10IPad = require('autoru-frontend/mocks/uatraits/safari_10_ipad');
const Safari10MacOS = require('autoru-frontend/mocks/uatraits/safari_10_macos');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    req.url = '/';
    req = { ...req, ...IE11 };
});

describe('redirect', () => {
    it('не должен ничего делать, если запрос POST', () => {
        expect.assertions(1);
        req.method = 'POST';

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('не должен ничего делать для Chrome 85', () => {
        expect.assertions(1);
        req = { ...req, ...Chrome86MacOS };

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('не должен ничего делать для Chrome 85 Android 5.1', () => {
        expect.assertions(1);
        req = { ...req, ...Chrome85Android };

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('не должен ничего делать для Chrome 74 Android 5.1 YandexSearch', () => {
        expect.assertions(1);
        req = { ...req, ...Chrome74AndroidYandexSearch };

        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
        });
    });

    it('должен средиректить для IE 10', () => {
        req = { ...req, ...IE10 };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/old-browser/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен средиректить для IE 11', () => {
        req = { ...req, ...IE11 };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/old-browser/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен средиректить для Safari 10 macOS', () => {
        req = { ...req, ...Safari10MacOS };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/old-browser/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен средиректить для Safari 10 iPad', () => {
        req = { ...req, ...Safari10IPad };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/old-browser/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен средиректить для AndroidBrowser', () => {
        req = { ...req, ...AndroidBrowser };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/old-browser/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен средиректить нормальный браузер по страницы /old-browser/', () => {
        req = { ...req, ...Chrome86MacOS, url: '/old-browser/' };
        middleware(req, res);

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });
});

describe('/old-browser/', () => {
    beforeEach(() => {
        req.url = '/old-browser/';
    });

    it('должен отдать ссылку для IE 11', () => {
        req = { ...req, ...IE11 };
        middleware(req, res);

        expect(res.statusCode).toEqual(200);
        expect(res.send.mock.calls[0][0]).toMatch('https://www.microsoft.com/ru-ru/edge');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен отдать ссылку для Safari', () => {
        req = { ...req, ...Safari10MacOS };
        middleware(req, res);

        expect(res.statusCode).toEqual(200);
        expect(res.send.mock.calls[0][0]).toMatch('https://www.apple.com/ru/safari/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    it('должен отдать ссылку для AndroidBrowser', () => {
        req = { ...req, ...AndroidBrowser };
        middleware(req, res);

        expect(res.statusCode).toEqual(200);
        expect(res.send.mock.calls[0][0]).toMatch('https://www.google.ru/chrome/');
        expect(res.end).toHaveBeenCalledTimes(1);
    });
});
