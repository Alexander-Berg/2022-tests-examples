const numberFormatter = require('./number-integer-refiner');

it('должен вернуть 10 для "10"', () => {
    expect(numberFormatter('10')).toEqual(10);
});

it('должен вернуть 100 для "100"', () => {
    expect(numberFormatter('10')).toEqual(10);
});

it('должен вернуть 1000 для "1 000"', () => {
    expect(numberFormatter('1 000')).toEqual(1000);
});

it('должен вернуть 1000000 для "1 000 000"', () => {
    expect(numberFormatter('1 000 000')).toEqual(1000000);
});
