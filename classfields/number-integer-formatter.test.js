const numberIntegerFormatter = require('./number-integer-formatter');

it('should format 10 as "10"', () => {
    expect(numberIntegerFormatter(10)).toEqual('10');
});

it('should format 100 as "100"', () => {
    expect(numberIntegerFormatter(100)).toEqual('100');
});

it('should format 1000 as "1 000"', () => {
    expect(numberIntegerFormatter(1000)).toEqual('1 000');
});

it('should format 10000 as "10 000"', () => {
    expect(numberIntegerFormatter(10000)).toEqual('10 000');
});

it('should format 100000 as "100 000"', () => {
    expect(numberIntegerFormatter(100000)).toEqual('100 000');
});

it('should format 1000000 as "1 000 000"', () => {
    expect(numberIntegerFormatter(1000000)).toEqual('1 000 000');
});

it('should format "1 0000" as "10 000"', () => {
    expect(numberIntegerFormatter('1 0000')).toEqual('10 000');
});
