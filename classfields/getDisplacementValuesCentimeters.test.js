const getDisplacementValues = require('./getDisplacementValuesCentimeters');
const RESULT = [
    50,
    100,
    150,
    200,
    250,
    300,
    350,
    400,
    450,
    600,
    700,
    800,
    900,
    1200,
    1400,
    1600,
    1800,
];

it('getDisplacementValues возвращает правильные значения', () => {
    expect(getDisplacementValues()).toEqual(RESULT);
});
