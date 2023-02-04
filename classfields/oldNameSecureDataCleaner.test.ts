import oldNameSecureDataCleaner from './oldNameSecureDataCleaner';

it('если данных нет, возвращает ничего', () => {
    expect(oldNameSecureDataCleaner()).toBeUndefined();
});

it('чистит данные НЕ переименованного', () => {
    const result = oldNameSecureDataCleaner({
        lol: 1,
        no: {
            name: 'агада',
        },
    });

    expect(result).toMatchSnapshot();
});

it('чистит данные переименованного', () => {
    const result = oldNameSecureDataCleaner({
        lol: 1,
        name: 'собака сутулая',
    });

    expect(result).toMatchSnapshot();
});
