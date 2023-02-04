const MockDate = require('mockdate');
const getOwningMonths = require('./getOwningMonths');

afterEach(() => {
    MockDate.reset();
});

it('should return 5 for year=2017, date=2017.10.29', () => {
    MockDate.set('2017-10-29');
    expect(getOwningMonths(2017)).toEqual(4);
});

it('should return 1 for year=2017, date=2017.07.15', () => {
    MockDate.set('2017-07-15');
    expect(getOwningMonths(2017)).toEqual(1);
});

it('should return 3 for year=2017, date=2017.03.01', () => {
    MockDate.set('2017-03-01');
    expect(getOwningMonths(2017)).toEqual(3);
});

it('should return 9 for year=2016, date=2017.03.01', () => {
    MockDate.set('2017-03-01');
    expect(getOwningMonths(2016)).toEqual(9);
});

it('should return 3 for year=2017, month=1, date=2017.03.01', () => {
    MockDate.set('2017-03-01');
    expect(getOwningMonths(2017, 1)).toEqual(3);
});

it('should return 3 for year=2016, month=2, date=2017.03.01', () => {
    MockDate.set('2017-03-01');
    expect(getOwningMonths(2016, 2)).toEqual(14);
});
