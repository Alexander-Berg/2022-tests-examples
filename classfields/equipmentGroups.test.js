const equipmentGroups = require('./equipmentGroups');

it('должен вернуть названия из одной группы', () => {
    const result = equipmentGroups(
        [ 'code1', 'code2' ],
        {
            code1: { code: 'code1', name: 'code1-name', group: 'group1' },
            code2: { code: 'code2', name: 'code2-name', group: 'group1' },
        },
    );

    expect(result).toEqual([
        { name: 'group1', values: [ 'code1-name', 'code2-name' ] },
    ]);
});

it('должен вернуть названия из разных групп группы', () => {
    const result = equipmentGroups(
        [ 'code1', 'code2', 'code3', 'code4' ],
        {
            code1: { code: 'code1', name: 'code1-name', group: 'group1' },
            code2: { code: 'code2', name: 'code2-name', group: 'group2' },
            code3: { code: 'code3', name: 'code3-name', group: 'group1' },
            code4: { code: 'code4', name: 'code4-name', group: 'group2' },
        },
    );

    expect(result).toEqual([
        { name: 'group1', values: [ 'code1-name', 'code3-name' ] },
        { name: 'group2', values: [ 'code2-name', 'code4-name' ] },
    ]);
});

it('должен игнорировать названия, которых нет в словаре', () => {
    const result = equipmentGroups(
        [ 'code1', 'code2', 'code21', 'code3', 'code4' ],
        {
            code1: { code: 'code1', name: 'code1-name', group: 'group1' },
            code2: { code: 'code2', name: 'code2-name', group: 'group2' },
            code3: { code: 'code3', name: 'code3-name', group: 'group1' },
            code4: { code: 'code4', name: 'code4-name', group: 'group2' },
        },
    );

    expect(result).toEqual([
        { name: 'group1', values: [ 'code1-name', 'code3-name' ] },
        { name: 'group2', values: [ 'code2-name', 'code4-name' ] },
    ]);
});
