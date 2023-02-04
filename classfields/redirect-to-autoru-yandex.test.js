const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');

const middleware = require('./redirect-to-autoru-yandex');

const COOKIE_SYNC_URL_PARAMETER = require('auto-core/models/cookieSync/constants/cookieSyncUrlParameter');

let req;
let res;
let next;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    next = jest.fn();

    req.isRobot = false;

    req.headers = {
        'x-forwarded-host': 'autoru_frontend.base_domain',
    };

    req.method = 'GET';

    req.protocol = 'https';
    req.url = '/currentUrl/';
});

it('редиректит на autoru-yandex и добавляет параметр cookiesync в retpath', () => {
    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_YANDEX);
        // eslint-disable-next-line max-len
        expect(error.data.location).toEqual('https://autoru.yandex.ru/sync/?retpath=https%3A%2F%2Fautoru_frontend.base_domain%2Fautoruyandexcookiesync%2F%3Fretpath%3Dhttps%253A%252F%252Fautoru_frontend.base_domain%252FcurrentUrl%252F%253Fcookiesync%253Dtrue');
    });

    middleware(req, res, next);

    expect(next).toHaveBeenCalled();
});

it('редиректит на autoru-yandex с мобилки', () => {
    req.headers = {
        'x-forwarded-host': 'autoru_frontend.base_domain',
    };
    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_YANDEX);
        // eslint-disable-next-line max-len
        expect(error.data.location).toEqual('https://autoru.yandex.ru/sync/?retpath=https%3A%2F%2Fautoru_frontend.base_domain%2Fautoruyandexcookiesync%2F%3Fretpath%3Dhttps%253A%252F%252Fautoru_frontend.base_domain%252FcurrentUrl%252F%253Fcookiesync%253Dtrue');
    });

    middleware(req, res, next);

    expect(next).toHaveBeenCalled();
});

it('не редиректит на autoru-yandex с promo', () => {
    req.headers = {
        'x-forwarded-host': 'promo.autoru_frontend.base_domain',
    };

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex роботов', () => {
    req.isRobot = true;

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex xhr', () => {
    req.headers['x-requested-with'] = 'XMLHttpRequest';

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex post запросы', () => {
    req.method = 'POST';

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex, если есть кука SYNCTTL', () => {
    req.cookies = {
        yuidlt: '1',
    };

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex, если есть параметр cookiesync в урле', () => {
    req.url = `/?${ COOKIE_SYNC_URL_PARAMETER }=true`;

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});

it('не редиректит на autoru-yandex, если это неведомый роут', () => {
    req.router = null;

    middleware(req, res, next);

    expect(next).toHaveBeenCalledWith();
});
