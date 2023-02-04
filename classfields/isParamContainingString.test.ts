import isParamContainingString from './isParamContainingString';

it('должен вернуть true для строки, если совпадает', () => {
    const param = 'someValue';
    const value = 'someValue';
    expect(isParamContainingString(param, value)).toBe(true);
});

it('должен вернуть false для строки, если не совпадает', () => {
    const param = 'someWeirdValue';
    const value = 'someValue';
    expect(isParamContainingString(param, value)).toBe(false);
});

it('должен вернуть true для массива, если включает', () => {
    const param = [ 'someValue', 'someOtherValue' ];
    const value = 'someValue';
    expect(isParamContainingString(param, value)).toBe(true);
});

it('должен вернуть false для массива, если не включает', () => {
    const param = [ 'someWeirdValue', 'someOtherWeirdValue' ];
    const value = 'someValue';
    expect(isParamContainingString(param, value)).toBe(false);
});
