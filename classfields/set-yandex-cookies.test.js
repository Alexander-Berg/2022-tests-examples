const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');

const middleware = require('./set-yandex-cookies');

let validQueryI;
let validQueryM;
let validIV;
let req;
let res;
let next;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    validQueryI = 'ZbPGKiee6LMYW0sf3omxnKE8';
    validQueryM = 'ZbPGKiee6LMYT1M=';
    validIV = 'AAAAAAAAAAA=';
    next = jest.fn();
});

it('редиректит в корень авто.ру, если не передеан retpath', () => {
    req.query = {
        v: validIV,
    };

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
        v: validIV,
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_NO_VALID_RETPATH);
        expect(error.data.location).toEqual('https://auto.ru');
    });

    middleware(req, res, next);

    expect(next).toHaveBeenCalled();
});

it('редиректит в корень авто.ру, если не передеан iv', () => {
    req.query = {
        retpath: 'https://auto.ru',
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
        v: validIV,
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

it('не должен выставить кук, если запрос пустой', () => {
    req.query = {
        retpath: 'https://auto.ru/some/',
        v: validIV,
    };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/');
    });

    middleware(req, res, next);

    expect(res.cookie).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalled();
});

it('не должен выставить кук, если есть параметры i и m, но они пустые', () => {
    req.query = { i: '', m: '', retpath: 'https://auto.ru/some/', v: validIV };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/');
    });

    middleware(req, res, next);

    expect(res.cookie).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalled();
});

it('не должен выставить кук, если есть параметры i и m, но они неверные', () => {
    req.query = { i: 'asdasd', m: 'loloodd', retpath: 'https://auto.ru/some/', v: validIV };

    middleware(req, res, next);

    expect(res.cookie).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalled();
});

it('должен выставить 3 куки, если есть параметры i и m', () => {
    req.query = { i: validQueryI, m: validQueryM, retpath: 'https://auto.ru/some/', v: validIV };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/');
    });

    middleware(req, res, next);

    expect(res.cookie).toHaveBeenCalledWith('yuidlt', '1', { httpOnly: false, maxAge: 604800000, secure: true });
    expect(res.cookie).toHaveBeenCalledWith('yandexuid', 'some yandexuid', { httpOnly: false, maxAge: 2592000000, secure: true });
    expect(res.cookie).toHaveBeenCalledWith('my', 'some my', { httpOnly: false, maxAge: 2592000000, secure: true });
    expect(next).toHaveBeenCalled();
});

it('должен выставить куки с SameSite, если есть поддержка в браузере', () => {
    req.uatraits.SameSiteSupport = true;
    req.query = { i: validQueryI, m: validQueryM, retpath: 'https://auto.ru/some/', v: validIV };

    next.mockImplementationOnce((error) => {
        expect(error instanceof RedirectError).toBe(true);
        expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
        expect(error.data.location).toEqual('https://auto.ru/some/');
    });

    middleware(req, res, next);

    expect(res.cookie).toHaveBeenCalledWith('yuidlt', '1', { httpOnly: false, maxAge: 604800000, secure: true });
    expect(res.cookie).toHaveBeenCalledWith('yandexuid', 'some yandexuid', { httpOnly: false, maxAge: 2592000000, secure: true, sameSite: 'none' });
    expect(res.cookie).toHaveBeenCalledWith('my', 'some my', { httpOnly: false, maxAge: 2592000000, secure: true, sameSite: 'none' });
    expect(next).toHaveBeenCalled();
});

describe('query.i => yandexuid', () => {
    it('должен выставить куку yandexuid, если есть параметр i', () => {
        req.query = { i: validQueryI, retpath: 'https://auto.ru/some/', v: validIV };

        next.mockImplementationOnce((error) => {
            expect(error instanceof RedirectError).toBe(true);
            expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
            expect(error.data.location).toEqual('https://auto.ru/some/');
        });

        middleware(req, res, next);

        expect(res.cookie).toHaveBeenCalledWith('yuidlt', '1', { httpOnly: false, maxAge: 604800000, secure: true });
        expect(res.cookie).toHaveBeenCalledWith('yandexuid', 'some yandexuid', { httpOnly: false, maxAge: 2592000000, secure: true });
        expect(next).toHaveBeenCalled();
    });

    it('не должен выставить куку yandexuid, если есть параметр i, но он неправильный', () => {
        req.query = { i: 'X' + validQueryI, retpath: 'https://auto.ru/some/', v: validIV };

        next.mockImplementationOnce((error) => {
            expect(error instanceof RedirectError).toBe(true);
            expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
            expect(error.data.location).toEqual('https://auto.ru/some/');
        });

        middleware(req, res, next);

        expect(res.cookie).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
    });
});

describe('query.m => my', () => {
    it('должен выставить куку my, если есть параметр m', () => {
        req.query = { m: validQueryM, retpath: 'https://auto.ru/some/', v: validIV };

        next.mockImplementationOnce((error) => {
            expect(error instanceof RedirectError).toBe(true);
            expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
            expect(error.data.location).toEqual('https://auto.ru/some/');
        });

        middleware(req, res, next);

        expect(res.cookie).toHaveBeenCalledWith('yuidlt', '1', { httpOnly: false, maxAge: 604800000, secure: true });
        expect(res.cookie).toHaveBeenCalledWith('my', 'some my', { httpOnly: false, maxAge: 2592000000, secure: true });
        expect(next).toHaveBeenCalled();
    });

    it('не должен выставить куку my, если есть параметр m, но он неправильный', () => {
        req.query = { m: 'X' + validQueryM, retpath: 'https://auto.ru/some/', v: validIV };

        next.mockImplementationOnce((error) => {
            expect(error instanceof RedirectError).toBe(true);
            expect(error.code).toEqual(RedirectError.CODES.COOKIE_SYNC_REDIRECT_TO_AUTORU);
            expect(error.data.location).toEqual('https://auto.ru/some/');
        });

        middleware(req, res, next);

        expect(res.cookie).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
    });
});
