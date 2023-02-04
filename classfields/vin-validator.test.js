const vinValidator = require('./vin-validator');

it('должен не пропускать VIN неправильной длины', () => {
    expect(vinValidator('ASDASDAS')).toBe(false);
});

it('должен не пропускать VIN c некорректными символами (O, Q)', () => {
    expect(vinValidator('KOQKF4DV5B5309254')).toBe(false);
});

it('должен пропускать корректный VIN', () => {
    expect(vinValidator('JTMKF4DV5B5309254')).toBe(true);
});
