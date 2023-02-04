const calculateDisplacement = require('./calculateDisplacement');

const testCases = [
    {
        displacement: 1250,
        expected: 1300,
    },
    {
        displacement: 1300,
        expected: 1300,
    },
    {
        displacement: 4100,
        expected: 4500,
    },
    {
        displacement: 6500,
        expected: 7000,
    },
];

testCases.forEach(testCase => {
    it(`Рассчитал правильный объем двигателя для чпу ${ testCase.displacement } -> ${ testCase.expected }`, () => {
        expect(calculateDisplacement(testCase.displacement)).toEqual(testCase.expected);
    });
});

it('Вернул null, если объем двигателя больше 10л', () => {
    expect(calculateDisplacement(14_000)).toBeNull();
});
