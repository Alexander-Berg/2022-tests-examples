const cookies = require('./cookies');

const TESTS = [
    {
        test: { autoru_sid: '12', gids: '1' },
        result: { gids: '1' },
    },
    {
        test: { autoru_sid_key: '12', gids: '1' },
        result: { gids: '1' },
    },
    {
        test: { autoruuid: '12', gids: '1' },
        result: { gids: '1' },
    },
    {
        test: { suid: '12', gids: '1' },
        result: { gids: '1' },
    },
    {
        test: { autoru_sid: '12', autoru_sid_key: '34', gids: '1' },
        result: { gids: '1' },
    },
];

TESTS.forEach(testCase => {
    it(`должен преобразовать куки ${ JSON.stringify(testCase.test) } как ${ JSON.stringify(testCase.result) }`, () => {
        expect(cookies({}, { cookies: testCase.test })).toEqual(testCase.result);
    });
});
