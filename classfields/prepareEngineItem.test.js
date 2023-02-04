const prepareEngineItem = require('./prepareEngineItem');

it('должен вернуть исходное значение, если id не в списке нестандартных', () => {
    const item = {
        id: 'displacement',
        name: 'Объем двигателя',
        value: '2488',
        units: 'см³',
    };

    expect(
        prepareEngineItem(item),
    ).toStrictEqual(item);
});

it('должен вернуть преобразованное значение, если id в списке нестандартных', () => {
    const item = {
        id: 'max_power',
        name: 'Максимальная мощность',
        value: '150/110.0 при 6000',
        units: 'л.с./кВт при об/мин',
    };

    expect(
        prepareEngineItem(item),
    ).toStrictEqual({
        id: 'max_power',
        name: 'Максимальная мощность',
        value: '150/110.0 л.с./кВт при 6000 об/мин',
        units: null,
    });
});
