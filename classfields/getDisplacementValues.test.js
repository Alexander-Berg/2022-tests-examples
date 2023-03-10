const getDisplacementValues = require('./getDisplacementValues');
const RESULT = [
    200,
    400,
    600,
    800,
    1000,
    1200,
    1400,
    1600,
    1800,
    2000,
    2200,
    2400,
    2600,
    2800,
    3000,
    3500,
    4000,
    4500,
    5000,
    5500,
    6000,
    7000,
    8000,
    9000,
    10000,
];

const RESULT_CARS = [
    200,
    300,
    400,
    500,
    600,
    700,
    800,
    900,
    1000,
    1100,
    1200,
    1300,
    1400,
    1500,
    1600,
    1700,
    1800,
    1900,
    2000,
    2100,
    2200,
    2300,
    2400,
    2500,
    2600,
    2700,
    2800,
    2900,
    3000,
    3500,
    4000,
    4500,
    5000,
    5500,
    6000,
    7000,
    8000,
    9000,
    10000,
];

it('getDisplacementValues возвращает правильные значения для легковых', () => {
    expect(getDisplacementValues('cars')).toEqual(RESULT_CARS);
});

it('getDisplacementValues возвращает правильные значения для остальных', () => {
    expect(getDisplacementValues()).toEqual(RESULT);
});
