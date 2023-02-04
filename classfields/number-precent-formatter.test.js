const numberFormatter = require('./number-percent-formatter');

it('должен вернуть "10 123.27%" для 10123.27', () => {
    expect(numberFormatter(10123.27)).toEqual('10 123.27%');
});

it('должен вернуть "0%" для 0', () => {
    expect(numberFormatter(0)).toEqual('0%');
});

it('должен вернуть "" для undefined', () => {
    expect(numberFormatter(undefined)).toEqual('');
});
