const prepareUrlForLog = require('./prepareUrlForLog');

const TESTS = [
    {
        url: '',
        result: '',
    },
    {
        url: '/test',
        result: 'http://basedomain/test',
    },
    {
        url: 'http://backend/test?foo=bar&token=123456789012345678901234567890',
        result: 'http://backend/test?foo=bar&token=123456789012345***',
    },
    {
        url: 'http://backend/test?foo=bar&token=123456789012345678901234567890&bar=baz',
        result: 'http://backend/test?foo=bar&token=123456789012345***&bar=baz',
    },
    {
        url: 'http://backend/test?token=123456789012345678901234567890',
        result: 'http://backend/test?token=123456789012345***',
    },
    {
        url: 'http://backend/test?sid=123456789012345678901234567890',
        result: 'http://backend/test?sid=123456789012345***',
    },
    {
        url: 'http://backend/test?session_id=123456789012345678901234567890',
        result: 'http://backend/test?session_id=123456789012345***',
    },
];

TESTS.forEach((testCase) => {
    it(`должен преобразовать "${ testCase.url }" в "${ testCase.result }"`, () => {
        expect(prepareUrlForLog(testCase.url)).toEqual(testCase.result);
    });
});
