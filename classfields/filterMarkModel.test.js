const filterMarkModel = require('./filterMarkModel');

let list;
beforeEach(() => {
    list = [
        { name: 'Audi', cyrillic_name: 'Ауди' },
        { name: 'BMW', cyrillic_name: 'БМВ' },
        { name: 'Mercedes-Benz', cyrillic_name: 'Мерседес-Бенц' },
        { name: '400', cyrillic_name: '400' },
        { name: 'without_cyrillic_name' },
    ];
});

it('должен вернуть тот же массив, если строка поиска пустая', () => {
    const result = filterMarkModel(list, '');
    expect(result).toEqual(list);
});

it('должен найти 400 для "4"', () => {
    const result = filterMarkModel(list, '4');
    expect(result).toEqual([ { name: '400', cyrillic_name: '400' } ]);
});

it('должен найти Audi для "au"', () => {
    const result = filterMarkModel(list, 'au');
    expect(result).toEqual([ { name: 'Audi', cyrillic_name: 'Ауди' } ]);
});

it('должен найти Audi для "Au"', () => {
    const result = filterMarkModel(list, 'Au');
    expect(result).toEqual([ { name: 'Audi', cyrillic_name: 'Ауди' } ]);
});

it('должен найти Audi для "ау"', () => {
    const result = filterMarkModel(list, 'ау');
    expect(result).toEqual([ { name: 'Audi', cyrillic_name: 'Ауди' } ]);
});

it('должен найти Audi для "фг" (английское "au" в русской раскладке)', () => {
    const result = filterMarkModel(list, 'фг');
    expect(result).toEqual([ { name: 'Audi', cyrillic_name: 'Ауди' } ]);
});

it('должен найти Audi для "fe" (русское "ау" в английской раскладке)', () => {
    const result = filterMarkModel(list, 'fe');
    expect(result).toEqual([ { name: 'Audi', cyrillic_name: 'Ауди' } ]);
});

it('не должен ничего найти для "adiu"', () => {
    const result = filterMarkModel(list, 'adiu');
    expect(result).toEqual([]);
});
