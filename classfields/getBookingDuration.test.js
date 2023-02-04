const getBookingDuration = require('./getBookingDuration');
const MockDate = require('mockdate');
const { nbsp } = require('auto-core/react/lib/html-entities');

beforeEach(() => {
    MockDate.set('2020-06-16T20:59:59Z');
});

afterEach(() => {
    MockDate.reset();
});

it('должен вернуть количество дней бронирования', () => {
    const terms = { deadline: '2020-06-21T20:59:59Z' };

    expect(getBookingDuration(terms)).toEqual(`5${ nbsp }дней`);
});

it('должен верно округлять количество дней', () => {
    const termsLess = { deadline: '2020-06-21T16:59:59Z' };
    const termsMore = { deadline: '2020-06-21T23:59:59Z' };

    expect(getBookingDuration(termsLess)).toEqual(`5${ nbsp }дней`);
    expect(getBookingDuration(termsMore)).toEqual(`5${ nbsp }дней`);
});
