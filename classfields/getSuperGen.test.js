const getSuperGen = require('./getSuperGen');

it('должен вернуть название', () => {
    const name = getSuperGen({
        vehicle_info: {
            super_gen: { id: '1', name: 'VI (F3x)' },
        },
    });

    expect(name).toEqual('VI (F3x)');
});

it('не должен вернуть название, если нет поколения', () => {
    const name = getSuperGen({
        vehicle_info: {},
    });

    expect(name).toEqual('');
});

it('должен вернуть пустую строку, если нет название и нет fallback', () => {
    const name = getSuperGen({
        vehicle_info: {
            super_gen: { id: '1', year_from: 1970, year_to: 1988 },
        },
    }, false);

    expect(name).toEqual('');
});

it('должен вернуть годы выпуска, если нет название и есть fallback', () => {
    const name = getSuperGen({
        vehicle_info: {
            super_gen: { id: '1', year_from: 1970, year_to: 1988 },
        },
    }, true);

    expect(name).toEqual('1970-1988');
});

it('должен вернуть год выпуска-н.в., если нет название и есть fallback', () => {
    const name = getSuperGen({
        vehicle_info: {
            super_gen: { id: '1', year_from: 2011 },
        },
    }, true);

    expect(name).toEqual('2011-н.в.');
});
