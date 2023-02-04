const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');

const middleware = require('auto-core/models/cookieSync/middleware/get-cookies-from-yandex');

let req;
let res;
let next;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    next = jest.fn();
});

it('редиректит в корень авто.ру, если не передеан retpath', () => {
    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_NO_VALID_RETPATH);
        expect(error.data.location).toEqual('https://auto.ru');
    });

    middleware(req, res, next);

    expect(next).toHaveBeenCalled();
});

it('редиректит в корень авто.ру, если retpath не автору', () => {
    req.query = {
        retpath: 'https://notauto.ru',
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_NO_VALID_RETPATH);
        expect(error.data.location).toEqual('https://auto.ru');
    });

    middleware(req, res, next);

    expect(next).toHaveBeenCalled();
});

it('редиректит на retpath', () => {
    req.query = {
        retpath: 'https://auto.ru/some/',
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/');
    });

    const result = middleware(req, res, next);

    expect(next).toHaveBeenCalled();
    expect(result).toBeUndefined();
});

it('редиректит на retpath и добавляет параметр i и v (iv), если есть кука yandexuid', () => {
    req.query = {
        retpath: 'https://auto.ru/some/?ret=x',
    };

    req.cookies = {
        yandexuid: 'some yandexuid',
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/?ret=x&i=ZbPGKiee6LMYW0sf3omxnKE8&v=AAAAAAAAAAA%3D');
    });

    const result = middleware(req, res, next);

    expect(next).toHaveBeenCalled();
    expect(result).toBeUndefined();
});

it('редиректит на retpath и добавляет параметр m и v (iv), если есть кука my', () => {
    req.query = {
        retpath: 'https://auto.ru/some/?ret=x',
    };

    req.cookies = {
        my: 'some my',
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/?ret=x&m=ZbPGKiee6LMYT1M%3D&v=AAAAAAAAAAA%3D');
    });

    const result = middleware(req, res, next);

    expect(next).toHaveBeenCalled();
    expect(result).toBeUndefined();
});

it('редиректит на retpath и добавляет все три параметра, если есть обе куки', () => {
    req.query = {
        retpath: 'https://auto.ru/some/?ret=x',
    };

    req.cookies = {
        my: 'some my',
        yandexuid: 'some yandexuid',
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/?ret=x&i=ZbPGKiee6LMYW0sf3omxnKE8&m=ZbPGKiee6LMYT1M%3D&v=AAAAAAAAAAA%3D');
    });

    const result = middleware(req, res, next);

    expect(next).toHaveBeenCalled();
    expect(result).toBeUndefined();
});
