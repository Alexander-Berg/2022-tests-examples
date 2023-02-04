const sortNewMmmAsPrev = require('./sortNewMmmAsPrev');

it('Должен отсортировать новые МММ', () => {
    const prevMmmInfo = [
        { mark: 'AUDI', models: [] },
        { mark: 'AUDI', models: [ { id: 'A3' } ], exclude: true },
        {},
    ];
    const newMmmInfo = [
        {},
        { mark: 'BMW', models: [] },
        { mark: 'AUDI', models: [ { id: 'A4' } ], exclude: true },
        { mark: 'AUDI', models: [] },
    ];
    const sorted = [
        { mark: 'AUDI', models: [] },
        { mark: 'AUDI', models: [ { id: 'A4' } ], exclude: true },
        {},
        { mark: 'BMW', models: [] },
    ];
    expect(sortNewMmmAsPrev(prevMmmInfo, newMmmInfo)).toEqual(sorted);
});
