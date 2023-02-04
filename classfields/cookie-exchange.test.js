const MockDate = require('mockdate');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const middleware = require('./cookie-exchange');

const COOKIE = '_cookie_exchange';

let req;
let res;
beforeEach(() => {
    MockDate.set('2020-05-20');
    req = createHttpReq();
    res = createHttpRes();
});

it(`не выставляет куки, если нет куки ${ COOKIE }`, () => {
    return new Promise((resolve) => {
        middleware(req, res, () => {
            expect(res.cookie).not.toHaveBeenCalled();
            expect(res.clearCookie).not.toHaveBeenCalled();

            resolve();
        });
    });
});

it(`не выставляет куки, если кука ${ COOKIE } пустая`, () => {
    return new Promise((resolve) => {
        req.cookies[COOKIE] = '';
        middleware(req, res, () => {
            expect(res.cookie).not.toHaveBeenCalled();
            expect(res.clearCookie).not.toHaveBeenCalled();

            resolve();
        });
    });
});

it.each([
    [ '|' ],
    [ 'a|' ],
    [ 'a=|' ],
    [ 'a=0|' ],
    [ 'a=1589058000000|' ],
])(`должен стереть куку ${ COOKIE } и ничего не выставить, если у куки плохие значения: %s`, (value) => {
    return new Promise((resolve) => {
        req.cookies.a = 'foo';
        req.cookies[COOKIE] = value;
        middleware(req, res, () => {
            expect(res.cookie).not.toHaveBeenCalled();
            expect(res.clearCookie).toHaveBeenCalledWith(COOKIE);

            resolve();
        });
    });
});

it(`должен стереть куку ${ COOKIE } и перевыставить: a=1591736400000`, () => {
    return new Promise((resolve) => {
        req.cookies.a = 'foo';
        req.cookies[COOKIE] = 'a=1591736400000';
        middleware(req, res, () => {
            expect(res.cookie).toHaveBeenCalledWith('a', 'foo', { expires: new Date(1591736400000) });
            expect(res.clearCookie).toHaveBeenCalledWith(COOKIE);

            resolve();
        });
    });
});

it(`должен стереть куку ${ COOKIE } и перевыставить: a=1591736400000|b=1591736400000`, () => {
    return new Promise((resolve) => {
        req.cookies.a = 'foo';
        req.cookies.b = 'bar';
        req.cookies[COOKIE] = 'a=1591736400000|b=1591736400000';
        middleware(req, res, () => {
            expect(res.cookie).toHaveBeenCalledWith('a', 'foo', { expires: new Date(1591736400000) });
            expect(res.cookie).toHaveBeenCalledWith('b', 'bar', { expires: new Date(1591736400000) });
            expect(res.clearCookie).toHaveBeenCalledWith(COOKIE);

            resolve();
        });
    });
});
