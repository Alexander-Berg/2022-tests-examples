const generateDeepTree = require('./generateDeepTree');

const TESTS = [
    {
        args: [ {}, [ 'foo', 'bar' ] ],
        result: { foo: { bar: {} } },
    },
    {
        args: [ {}, [ 'foo', [ 'bar', 'baz' ] ] ],
        result: { foo: { bar: {}, baz: {} } },
    },
    {
        args: [ { baz: {} }, [ 'foo', 'bar' ] ],
        result: { foo: { bar: { baz: {} } } },
    },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ JSON.stringify(testCase.result) } для ${ JSON.stringify(testCase.args) }`, () => {
        expect(generateDeepTree(...testCase.args)).toEqual(testCase.result);
    });
});
