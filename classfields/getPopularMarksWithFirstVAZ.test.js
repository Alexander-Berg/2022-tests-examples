const getPopularMarksWithFirstVAZ = require('./getPopularMarksWithFirstVAZ');

it('Должен поднимать LADA наверх', () => {
    const items = [
        { id: 'BMW', name: 'BMW', count: 1 },
        { id: 'VAZ', name: 'LADA (ВАЗ)', count: 1 },
    ];

    const result = [
        { id: 'VAZ', name: 'LADA (ВАЗ)', count: 1 },
        { id: 'BMW', name: 'BMW', count: 1 },
    ];

    expect(getPopularMarksWithFirstVAZ(items, 100)).toEqual(result);
});

it('Не поднимаем пустую LADA наверх', () => {
    const items = [
        { id: 'BMW', name: 'BMW', count: 1 },
        { id: 'VAZ', name: 'LADA (ВАЗ)', count: 0 },
    ];

    const result = [
        { id: 'BMW', name: 'BMW', count: 1 },
    ];

    expect(getPopularMarksWithFirstVAZ(items, 100)).toEqual(result);
});

it('Должен поднимать пустую LADA наверх при передаче опции withEmpty', () => {
    const items = [
        { id: 'BMW', name: 'BMW', count: 1 },
        { id: 'VAZ', name: 'LADA (ВАЗ)', count: 0 },
    ];

    const result = [
        { id: 'VAZ', name: 'LADA (ВАЗ)', count: 0 },
        { id: 'BMW', name: 'BMW', count: 1 },
    ];

    expect(getPopularMarksWithFirstVAZ(items, 100, { withEmpty: true })).toEqual(result);
});
