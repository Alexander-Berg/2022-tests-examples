const formatNumValue = require('auto-core/lib/util/formatNumValue');

const defaultParams = {
    value: undefined,
    dimension: '',
    separator: '\u00a0',
    fractionalDigits: 0,
    showNull: undefined,
};

const TESTS = [
    { result: '0', params: { ...defaultParams, value: 0, showNull: true } },
    { result: '', params: { ...defaultParams, value: 0 } },
    { result: '100 500', params: { ...defaultParams, value: 100500 } },
];

TESTS.forEach(({ params, result }) => {
    it(
        `should return ${ result } for ${ params.value } and params: ${ JSON.stringify(params) }`,
        () => {
            expect(formatNumValue(
                params.value,
                params.dimension,
                params.separator,
                params.fractionalDigits,
                params.showNull,
            )).toEqual(result);
        },
    );
});
