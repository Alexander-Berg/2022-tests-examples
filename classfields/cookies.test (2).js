const de = require('descript');

const cookies = require('./cookies');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.cookies.foo = 'bar';
});

it('должен вернуть пустой объект, если не передали массив кук', () => {
    return de.run(cookies, {
        context,
    }).then((result) => {
        expect(result).toEqual({});
    });
});

it('должен вернуть объект с кукой, если она есть в переданном массиве', () => {
    req.cookies.test = 'foo';
    return de.run(cookies, {
        context,
        params: { cookies: [ 'test' ] },
    }).then((result) => {
        expect(result).toEqual({ test: 'foo' });
    });
});

it('должен вернуть объект с кукой, если ее префикс есть в переданном массиве', () => {
    req.cookies.test = 'foo1';
    req.cookies.test_foo = 'foo2';
    return de.run(cookies, {
        context,
        params: { cookies: [ 'test' ] },
    }).then((result) => {
        expect(result).toEqual({ test: 'foo1', test_foo: 'foo2' });
    });
});
