const _ = require('lodash');
const breadcrumbs = require('./breadcrumbs');

let data;
beforeEach(() => {
    data = {
        AUDI: {
            name: 'Audi',
            'cyrillic-name': 'Ауди',
            models: {
                A3: { name: 'a3name', 'cyrillic-name': 'а3имя' },
                A4: { name: 'a4name', 'cyrillic-name': 'а4имя' },
            },
        },
    };
});

it('getMarks должен вернуть марки для "cars"', () => {
    expect(breadcrumbs.getMarks({}, data)).toEqual(_.cloneDeep(data));
});

it('getMarkName должен вернуть название марки', () => {
    expect(breadcrumbs.getMarkName({ mark: 'audi' }, data)).toEqual('Audi');
});

it('getMarkNameRus должен вернуть название марки', () => {
    expect(breadcrumbs.getMarkNameRus({ mark: 'audi' }, data)).toEqual('Ауди');
});

it('getModelName должен вернуть название марки', () => {
    expect(breadcrumbs.getModelName({ mark: 'audi', model: 'a4' }, data)).toEqual('a4name');
});

it('getModelNameRus должен вернуть название марки', () => {
    expect(breadcrumbs.getModelNameRus({ mark: 'audi', model: 'a4' }, data)).toEqual('а4имя');
});
