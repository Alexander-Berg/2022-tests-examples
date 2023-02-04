import vinValidator from './is_vin';

it('должен не пропускать VIN неправильной длины', () => {
    expect(vinValidator('ASDASDAS')).toBe(false);
});

it('должен не пропускать VIN неправильной длины в тексте', () => {
    expect(vinValidator('а такой вин ASDASDAS норм?')).toBe(false);
});

it('должен не пропускать VIN c некорректными символами (O, Q)', () => {
    expect(vinValidator('KOQKF4DV5B5309254')).toBe(false);
});

it('должен пропускать корректный VIN', () => {
    expect(vinValidator('JTMKF4DV5B5309254')).toBe(true);
});

it('должен пропускать корректный VIN в тексте', () => {
    expect(vinValidator('вот мой вин JTMKF4DV5B5309254, норм же?')).toBe(true);
});
