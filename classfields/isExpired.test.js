const isExpired = require('./isExpired');
const MockDate = require('mockdate');

afterEach(() => {
    MockDate.reset();
});

it('должен отдать true, если текущая дата и время после expire date', () => {
    MockDate.set('2019-02-03T13:59:59.999999999Z');

    const result = isExpired({
        expire_date: '2019-02-03T08:59:59.999999999Z',
    });

    expect(result).toBe(true);
});

it('должен отдать false, если текущая дата и время до expire date', () => {
    MockDate.set('2019-02-03T13:59:59.999999999Z');

    const result = isExpired({
        expire_date: '2019-02-04T13:59:59.999999999Z',
    });

    expect(result).toBe(false);
});

it('должен отдать true, если текущая дата и время равны expire date', () => {
    MockDate.set('2019-02-03T13:59:59.999999999Z');

    const result = isExpired({
        expire_date: '2019-02-03T13:59:59.999999999Z',
    });

    expect(result).toBe(true);
});
