const webview = require('./webview');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
let routeData;
beforeEach(() => {
    req = createHttpReq();
    req.router.route.getData = () => routeData;
    res = createHttpRes();
    routeData = {};
});

const COOKIE_OPTIONS = { domain: undefined, httpOnly: false, maxAge: 600000, path: '/' };

it('не должен ничего делать, если для нормального запроса', () => {
    return new Promise((done) => {
        webview(req, res, () => {
            expect(req.webview).toEqual(false);
            expect(res.cookie).not.toHaveBeenCalled();
            done();
        });
    });
});

it('не должен ничего делать, если пришел in-app браузер', () => {
    return new Promise((done) => {
        req.uatraits = { inAppBrowser: true };
        webview(req, res, () => {
            expect(req.webview).toEqual(false);
            expect(res.cookie).not.toHaveBeenCalled();
            done();
        });
    });
});

it('должен поставить флаг и куку, если пришли на роут для webview', () => {
    return new Promise((done) => {
        routeData = { onlyContent: true };
        webview(req, res, () => {
            expect(req.webview).toEqual(true);
            expect(res.cookie).toHaveBeenCalledWith('webview', '1', COOKIE_OPTIONS);
            done();
        });
    });
});

it('не должен ничего делать, если пришли на роут для webview, но уже есть кука webview', () => {
    return new Promise((done) => {
        routeData = { onlyContent: true };
        req.cookies.webview = '1';
        webview(req, res, () => {
            expect(req.webview).toEqual(true);
            expect(res.cookie).not.toHaveBeenCalled();
            done();
        });
    });
});

it('должен поставить флаг и куку, если пришли на роут с ?only-content=true', () => {
    return new Promise((done) => {
        req.router.params = { 'only-content': 'true' };
        webview(req, res, () => {
            expect(req.webview).toEqual(true);
            expect(res.cookie).toHaveBeenCalledWith('webview', '1', COOKIE_OPTIONS);
            done();
        });
    });
});

it('должен поставить флаг и куку, если открыли webview из андроид приложения', () => {
    return new Promise((done) => {
        req.headers = { 'x-requested-with': 'ru.auto.ara' };
        webview(req, res, () => {
            expect(req.webview).toEqual(true);
            expect(res.cookie).toHaveBeenCalledWith('webview', '1', COOKIE_OPTIONS);
            done();
        });
    });
});

it('должен поставить флаг и куку, если передали гет параметр only-content', () => {
    return new Promise((done) => {
        req.query['only-content'] = 1;
        webview(req, res, () => {
            expect(req.webview).toEqual(true);
            expect(res.cookie).toHaveBeenCalledWith('webview', '1', COOKIE_OPTIONS);
            done();
        });
    });
});
