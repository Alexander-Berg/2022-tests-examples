const formatPrice = require('auto-core/lib/util/formatPrice');

const defaultParams = {
    value: undefined,
    currency: 'RUR',
    showNull: undefined,
};

const TESTS = [
    { result: '0 ₽', params: { ...defaultParams, value: 0, showNull: true } },
    { result: '', params: { ...defaultParams, value: 0 } },
    { result: '100 500 ₽', params: { ...defaultParams, value: 100500 } },
];

TESTS.forEach(({ params, result }) => {
    it(
        `should return ${ result } for ${ params.value } and params: ${ JSON.stringify(params) }`,
        () => {
            expect(formatPrice(
                params.value,
                params.currency,
                params.showNull,
            )).toEqual(result);
        },
    );
});
