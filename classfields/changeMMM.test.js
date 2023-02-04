const changeMMM = require('./changeMMM');

it('должен добавить первый МММ', () => {
    const result = changeMMM([ 'AUDI' ], { name: 'mark' }, 0, []);
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});

it('должен добавить первый МММ вендор', () => {
    const result = changeMMM([ 'VENDOR1' ], { name: 'mark' }, 0, []);
    expect(result).toEqual([ { vendor: 'VENDOR1' } ]);
});

it('должен добавить исключение', () => {
    const result = changeMMM(true, { name: 'exclude' }, 0);
    expect(result).toEqual([ { exclude: true } ]);
});

it('должен поменять вендора на марку', () => {
    const mmm = [ { vendor: 'VENDOR1' } ];
    const result = changeMMM([ 'AUDI' ], { name: 'mark' }, 0, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});

it('должен поменять марку на вендора', () => {
    const mmm = [ { mark: 'AUDI', models: [ { id: '100' } ] } ];
    const result = changeMMM([ 'VENDOR1' ], { name: 'mark' }, 0, mmm);
    expect(result).toEqual([ { vendor: 'VENDOR1' } ]);
});

it('должен добавить второй МММ', () => {
    const mmm = [ { mark: 'AUDI', models: [ { id: '100' } ] } ];
    const result = changeMMM([ 'BMW' ], { name: 'mark' }, 1, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [ { id: '100' } ] }, { mark: 'BMW', models: [] } ]);
});

it('должен добавить второй МММ с исключением', () => {
    const mmm = [ { mark: 'AUDI', models: [], exclude: true }, { exclude: true } ];
    const result = changeMMM([ 'BMW' ], { name: 'mark' }, 1, mmm);
    expect(result).toEqual([ { exclude: true, mark: 'AUDI', models: [] }, { exclude: true, mark: 'BMW', models: [] } ]);
});

it('должен сбросить марку', () => {
    const mmm = [ { mark: 'AUDI', models: [ { id: '100' } ] } ];
    const result = changeMMM([], { name: 'mark' }, 0, mmm);
    expect(result).toEqual([ { mark: undefined, models: [] } ]);
});

it('должен добавить модель', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ];
    const result = changeMMM([ '100', 'A4' ], { name: 'model', mode: 'add', value: [ 'A4' ] }, 0, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] }, { id: 'A4', nameplates: [], generations: [] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ]);
});

it('должен добавить модель c шильдом', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ];

    const result = changeMMM([ '100', 'A4' ], { name: 'model', mode: 'add', value: [ 'A4#123' ] }, 0, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] }, { id: 'A4', nameplates: [ '123' ], generations: [] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ]);
});

it('должен добавить второй шильд к модели c шильдом', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ];
    const result = changeMMM([ '100' ], { name: 'model', mode: 'add', value: [ '100#123' ] }, 0, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111', '123' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ]);
});

it('должен добавить модель ко второму ММН', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5' } ] },
    ];
    const result = changeMMM([ '100', 'X5' ], { name: 'model', mode: 'add', value: [ '100' ] }, 1, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ], generations: [ '222' ] } ] },
        { mark: 'BMW', models: [ { id: '100', nameplates: [], generations: [] }, { id: 'X5' } ] },
    ]);
});

it('должен удалить модель, когда выбрана одна модель', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', generations: [ '222', '333' ] } ] },
    ];
    const result = changeMMM([], { name: 'model', mode: 'delete', value: [ '100' ] }, 0, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});

it('должен удалить модель, когда выбрано несколько моделей', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', generations: [ '222', '333' ] }, { id: '80', nameplates: [ '88' ], generations: [ '99' ] } ] },
    ];
    const result = changeMMM([], { name: 'model', mode: 'delete', value: [ '100' ] }, 0, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [ { id: '80', nameplates: [ '88' ], generations: [ '99' ] } ] } ]);
});

it('должен удалить шильд и модель', () => {
    const mmm = [ { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111' ] } ] } ];
    const result = changeMMM([], { name: 'model', mode: 'delete', value: [ '100#111' ] }, 0, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});

it('должен удалить только один из шильдов', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '111', '222' ], generations: [ '333', '444' ] } ] },
    ];

    const result = changeMMM([], { name: 'model', mode: 'delete', value: [ '100#111' ] }, 0, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', nameplates: [ '222' ], generations: [ '333', '444' ] } ] },
    ]);
});

it('должен очистить модели', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100' }, { id: '80', nameplates: [ '111' ], generations: [ '222' ] } ] },
    ];
    const result = changeMMM([], { name: 'model', mode: 'clear', value: [] }, 0, mmm);
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});

it('должен правильно обрабатывать изменение поколений', () => {
    const mmm = [
        { mark: 'AUDI', models: [ { id: '100', generations: [ '123' ] } ] },
        { mark: 'BMW', models: [ { id: 'X5', generations: [ '100500' ] } ] },
    ];
    const result = changeMMM([ { id: '100', generations: [ '123', '456' ], nameplates: [] } ], { name: 'generation' }, 0, mmm);
    expect(result).toEqual([
        { mark: 'AUDI', models: [ { id: '100', generations: [ '123', '456' ], nameplates: [] } ] },
        { mark: 'BMW', models: [ { id: 'X5', generations: [ '100500' ] } ] },
    ]);
});
