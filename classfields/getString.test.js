const getString = require('./getString');

it('вернет первое значение, если оно одно', () => {
    const items = [ '1' ];
    expect(getString(items)).toBe('1');
});

it('вернет 2 значения через запятую', () => {
    const items = [ '1', '2' ];
    expect(getString(items)).toBe('1, 2');
});

it('вернет диапазон значений', () => {
    const items = [ '1', '2', '3', '4', '5' ];
    expect(getString(items)).toBe('1-5');
});

it('вернет undefined для пустого аргумента', () => {
    const items = undefined;
    expect(getString(items)).toBeUndefined();
});
