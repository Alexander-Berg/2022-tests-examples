const getAxisXValues = require('./getAxisXValues');

it('должен вернуть годы, если промежуток >= 4 лет', () => {
    expect(getAxisXValues('2015-01-01', '2019-01-01')).toEqual({
        count: 4,
        format: 'YYYY',
    });
});

it('должен вернуть годы, если промежуток >= 4 лет (годы округляем до целого числа)', () => {
    expect(getAxisXValues('2015-12-01', '2019-04-01')).toEqual({
        count: 4,
        format: 'YYYY',
    });
});

it('должен вернуть месяцы, если промежуток < 4 лет', () => {
    expect(getAxisXValues('2015-01-01', '2018-01-01')).toEqual({
        count: 8,
        format: 'YYYY.MM',
    });
});

it('должен вернуть дни (не более 6) если промежуток < 4 месяцев', () => {
    expect(getAxisXValues('2015-01-01', '2015-04-30')).toEqual({
        count: 6,
        format: 'YYYY.MM.DD',
    });
});

it('должен вернуть дни (но не более 3), если промежуток < 4 месяцев для мобилы', () => {
    expect(getAxisXValues('2015-01-01', '2015-04-30', true)).toEqual({
        count: 3,
        format: 'YYYY.MM.DD',
    });
});
