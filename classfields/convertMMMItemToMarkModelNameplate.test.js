const convertMMMItemToMarkModelNameplate = require('./convertMMMItemToMarkModelNameplate');

it('должен преобразовать объект в значение', () => {
    expect(convertMMMItemToMarkModelNameplate({
        mark: 'AUDI',
        model: 'A3',
        nameplate: '111',
        generation: '222',
    })).toEqual('AUDI#A3#111#222');
});

it('должен преобразовать объект в значение без нэймплейта', () => {
    expect(convertMMMItemToMarkModelNameplate({
        mark: 'AUDI',
        model: 'A3',
        generation: '222',
    })).toEqual('AUDI#A3##222');
});

it('должен преобразовать объект в значение (вендор)', () => {
    expect(convertMMMItemToMarkModelNameplate({
        vendor: 'VENDOR1',
    })).toEqual('VENDOR1');
});

it('должен вернуть пустую строку', () => {
    expect(convertMMMItemToMarkModelNameplate()).toEqual('');
});
