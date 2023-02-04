const getConversion = require('./getConversion');
it('должен вернуть конверсию', () => {
    expect(getConversion(3, 1)).toBe(33.4);
});

it('должен вернуть undefined, если from === 0', () => {
    expect(getConversion(0, 1)).toBe(0);
});

it('должен вернуть undefined, если from === undefined', () => {
    expect(getConversion(undefined, 1)).toBe(0);
});

it('должен вернуть undefined, если to === undefined', () => {
    expect(getConversion(10, undefined)).toBe(0);
});
