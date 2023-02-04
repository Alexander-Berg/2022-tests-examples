const formatReviewRatingValue = require('./formatReviewRatingValue');

const testCases = [
    {
        input: 4,
        output: '4,0',
    },
    {
        input: 3.551,
        output: '3,6',
    },
    {
        input: '2.551',
        output: '2,6',
    },
];

testCases.forEach(({ input, output }) => {
    it(`должен вернуть ${ output } для ${ input }`, () => {
        const result = formatReviewRatingValue(input);
        expect(result).toEqual(output);
    });
});
