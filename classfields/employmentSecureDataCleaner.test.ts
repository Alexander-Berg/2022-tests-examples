import employmentSecureDataCleaner from './employmentSecureDataCleaner';

it('если данных нет, возвращает ничего', () => {
    expect(employmentSecureDataCleaner()).toBeUndefined();
});

it('чистит данные самозанятого', () => {
    const result = employmentSecureDataCleaner({
        lol: 1,
        employed: {
            address: 'не дом и не улица',
            self_employed: {
                xxx: 2,
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('чистит данные работящего', () => {
    const result = employmentSecureDataCleaner({
        lol: 1,
        employed: {
            address: 'не дом и не улица',
            employee: {
                xxx: 2,
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('чистит данные иждивенца', () => {
    const result = employmentSecureDataCleaner({
        lol: 1,
        not_employed: {
            address: 'не дом и не улица',
        },
    });

    expect(result).toMatchSnapshot();
});
