const { ndash } = require('auto-core/react/lib/html-entities');

const getString = require('./getStringForElectricRange');

it('вернет первое значение, если оно одно', () => {
    const items = [ '1' ];
    expect(getString(items)).toBe('1');
});

it('вернет диапазон значений', () => {
    const items = [ '1', '2', '3', '4', '5' ];
    expect(getString(items)).toBe(`1 ${ ndash } 5`);
});

it('вернет undefined для пустого аргумента', () => {
    const items = undefined;
    expect(getString(items)).toBeUndefined();
});
