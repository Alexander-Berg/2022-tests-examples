const prepareHeadersForLog = require('./prepareHeadersForLog');

const TESTS = [
    {
        headers: {
            cookie: 'foo=bar',
            host: 'auto.ru',
        },
        result: {
            cookie: 'foo=bar',
            host: 'auto.ru',
        },
    },
    {
        headers: {
            host: 'auto.ru',
        },
        result: {
            host: 'auto.ru',
        },
    },
    {
        headers: {
            cookie: 'autoru_sid=36201940%7C1563384608735.7776000',
            host: 'auto.ru',
        },
        result: {
            cookie: 'autoru_sid=36201940%7C156338***',
            host: 'auto.ru',
        },
    },
    {
        headers: {
            cookie: 'foo=bar; autoru_sid=36201940%7C1563384608735.7776000',
            host: 'auto.ru',
        },
        result: {
            cookie: 'foo=bar; autoru_sid=36201940%7C156338***',
            host: 'auto.ru',
        },
    },
    {
        headers: {
            cookie: 'autoru_sid=36201940%7C1563384608735.7776000; foo=bar',
            host: 'auto.ru',
        },
        result: {
            cookie: 'autoru_sid=36201940%7C156338***; foo=bar',
            host: 'auto.ru',
        },
    },
    {
        headers: {
            cookie: 'bar=baz; autoru_sid=36201940%7C1563384608735.7776000; foo=bar',
            host: 'auto.ru',
        },
        result: {
            cookie: 'bar=baz; autoru_sid=36201940%7C156338***; foo=bar',
            host: 'auto.ru',
        },
    },
    {
        // тест на заголовок пабликапи
        headers: {
            host: 'auto.ru',
            'x-session-id': '36201940%7C1563384608735.7776000',
        },
        result: {
            host: 'auto.ru',
            'x-session-id': '36201940%7C15633***',
        },
    },
    {
        // тест на странную куку
        headers: {
            cookie: 'autoru_sid=36201',
            host: 'auto.ru',
        },
        result: {
            cookie: 'autoru_sid=36***',
            host: 'auto.ru',
        },
    },
];

it('не должен обработать куки, если они очень большие', () => {
    const headers = {
        cookie: '0'.repeat(8 * 1024 + 1),
        host: 'auto.ru',
    };

    expect(prepareHeadersForLog(headers)).toEqual({
        cookie: '[cookie too long]',
        host: 'auto.ru',
    });
});

it('не должен ничего делать, если нет заголовков', () => {
    expect(prepareHeadersForLog()).toEqual({});
});

TESTS.forEach((testCase) => {
    it(`должен преобразовать ${ JSON.stringify(testCase.headers) } в ${ JSON.stringify(testCase.result) }`, () => {
        expect(prepareHeadersForLog(testCase.headers)).toEqual(testCase.result);
    });
});
