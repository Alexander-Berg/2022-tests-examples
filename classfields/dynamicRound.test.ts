import dynamicRound from './dynamicRound';

const rules = [
    {
        to: 15,
        divider: 1,
    },
    {
        from: 15,
        to: 50,
        divider: 5,
    },
    {
        from: 50,
        to: 300,
        divider: 10,
    },
    {
        from: 300,
        to: 500,
        divider: 30,
    },
    {
        from: 500,
        divider: 50,
    },
];

const rounder = dynamicRound(rules);

const CASES = [
    {
        input: 0,
        output: 0,
    },
    {
        input: 10,
        output: 10,
    },
    {
        input: 15,
        output: 15,
    },
    {
        input: 23,
        output: 20,
    },
    {
        input: 50,
        output: 50,
    },
    {
        input: 154,
        output: 150,
    },
    {
        input: 300,
        output: 300,
    },
    {
        input: 345,
        output: 330,
    },
    {
        input: 499,
        output: 480,
    },
    {
        input: 500,
        output: 500,
    },
    {
        input: 785,
        output: 750,
    },
];

CASES.forEach(testCase => {
    const {
        input,
        output,
    } = testCase;

    it(`при округлении ${ input } должно получиться ${ output }`, () => {
        expect(rounder(input)).toBe(output);
    });
});
