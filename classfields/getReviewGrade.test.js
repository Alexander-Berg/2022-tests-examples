const getReviewGrade = require('./getReviewGrade');

const GRADES = {
    '1': 'good',
    '2': 'neutral',
    '3': 'bad',
};

const testCases = [
    {
        input: 4,
        output: GRADES['1'],
    },
    {
        input: 3.551,
        output: GRADES['1'],
    },
    {
        input: 3.549,
        output: GRADES['2'],
    },
    {
        input: 3,
        output: GRADES['2'],
    },
    {
        input: 2.551,
        output: GRADES['2'],
    },
    {
        input: 2.549,
        output: GRADES['3'],
    },
    {
        input: 2,
        output: GRADES['3'],
    },
];

testCases.forEach(({ input, output }) => {
    it(`returns correct grade when rating is ${ input }`, () => {
        const result = getReviewGrade(input);
        expect(result).toBe(output);
    });
});
