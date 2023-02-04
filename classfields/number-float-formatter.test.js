const numberFormatter = require('./number-float-formatter');

it('should format "" as ""', () => {
    expect(numberFormatter('')).toEqual('');
});

it('should format 10 as "10"', () => {
    expect(numberFormatter(10)).toEqual('10');
});

it('should format 1.5 as "1,5"', () => {
    expect(numberFormatter(1.5)).toEqual('1,5');
});

it('should format 100 as "100"', () => {
    expect(numberFormatter(100)).toEqual('100');
});

it('should format 1000 as "1 000"', () => {
    expect(numberFormatter(1000)).toEqual('1 000');
});

it('should format 10000 as "10 000"', () => {
    expect(numberFormatter(10000)).toEqual('10 000');
});

it('should format 100000 as "100 000"', () => {
    expect(numberFormatter(100000)).toEqual('100 000');
});

it('should format 1000000 as "1 000 000"', () => {
    expect(numberFormatter(1000000)).toEqual('1 000 000');
});

it('should format 1000000.5 as "1 000 000,5"', () => {
    expect(numberFormatter(1000000.5)).toEqual('1 000 000,5');
});

it('should format "1 0000" as "10 000"', () => {
    expect(numberFormatter('1 0000')).toEqual('10 000');
});
