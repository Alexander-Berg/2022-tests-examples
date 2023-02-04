import formatter from './snils-formatter';

const CASES = [
    {
        value: '',
        result: '',
    },
    {
        value: '324225',
        result: '324-225',
    },
    {
        value: '3242254',
        result: '324-225-4',
    },
    {
        value: '52353566323',
        result: '523-535-663 23',
    },
];

CASES.forEach(({ result, value }) => {
    it(`should format '${ value }' as "${ result }"`, () => {
        expect(formatter(value)).toEqual(result);
    });
});
