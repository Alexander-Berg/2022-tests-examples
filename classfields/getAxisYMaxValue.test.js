const getAxisYMaxValue = require('./getAxisYMaxValue');

const TEST_CASES = [
    {
        mileage: 1,
        result: 3,
    },
    {
        mileage: 9,
        result: 9,
    },
    {
        mileage: 150345,
        result: 180000,
    },
    {
        mileage: 20000,
        result: 30000,
    },
    {
        mileage: 300000,
        result: 300000,
    },
    {
        mileage: 99000,
        result: 120000,
    },
];

TEST_CASES.forEach(testCase => {
    it(`должен вернуть максимум оси Y для значения ${ testCase.mileage }`, () => {
        expect(getAxisYMaxValue(testCase.mileage)).toEqual(testCase.result);
    });
});
