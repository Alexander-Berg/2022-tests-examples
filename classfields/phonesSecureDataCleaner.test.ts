import phonesSecureDataCleaner from './phonesSecureDataCleaner';

it('если данных нет, возвращает ничего', () => {
    expect(phonesSecureDataCleaner()).toBeUndefined();
});

it('чистит данные', () => {
    const result = phonesSecureDataCleaner({
        lol: 1,
        phone_entities: [
            {
                phone_type: 'PERSONAL',
            },
            {
                phone_type: 'LOL',
            },
        ],
    });

    expect(result).toMatchSnapshot();
});
