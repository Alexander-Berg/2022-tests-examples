const typesPreparer = require('./types');

it('Срезаем из выдачи CRANE_HYDRAULICS', () => {
    const IN = [
        {
            id: 'CRANE_HYDRAULICS',
        },
        {
            id: 'TRUCKS',
        },

    ];

    const OUT = typesPreparer(IN);

    expect(OUT.some((item) => item.id === 'CRANE_HYDRAULICS')).toEqual(false);
    expect(OUT.some((item) => item.id === 'TRUCKS')).toEqual(true);
});
