const securityCheck = require('./security-check');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
let next;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    next = jest.fn();
});

it('должен вернуть статус 401, если юзер незалогин', () => {
    req.session = { auth: false };

    securityCheck(req, res, next);

    expect(res.end).toHaveBeenCalledTimes(1);
    expect(res.end).toHaveBeenCalledWith();
    expect(res.statusCode).toEqual(401);
    expect(next).not.toHaveBeenCalled();
});

it('должен вернуть статус 403, если юзер залогин, но запрос пришел из внешней сети', () => {
    req.session = { auth: true };

    securityCheck(req, res, next);

    expect(res.end).toHaveBeenCalledTimes(1);
    expect(res.end).toHaveBeenCalledWith();
    expect(res.statusCode).toEqual(403);
    expect(next).not.toHaveBeenCalled();
});

it('должен прокинуть запрос дальше, если юзер залогин и он пришел из внутренней сети', () => {
    req.session = { auth: true };
    req.isInternalNetwork = true;

    securityCheck(req, res, next);

    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledWith();
});

it('должен прокинуть запрос дальше, если юзер незалогин и скачивает дефолтный отчет', () => {
    req.session = { auth: false };
    req.path = '/printer/history/Z0NZWE00054341234';

    securityCheck(req, res, next);

    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledWith();
});
