import markMock from 'auto-core/models/catalogSuggest/mocks/mark.mock';

import getFilteredMarkList from './getFilteredMarkList';

it('фильтрует марки по латинскому названию', () => {
    const marks = [
        markMock.withName('Audi').value(),
        markMock.withName('BMW').value(),
        markMock.withName('Kia').value(),
    ];
    const result = getFilteredMarkList(marks, 'bmw').map(({ name }) => name);
    expect(result).toEqual([ 'BMW' ]);
});

it('фильтрует марки по латинскому названию, если пользователь перепутал раскладку', () => {
    const marks = [
        markMock.withName('Audi').value(),
        markMock.withName('BMW').value(),
        markMock.withName('Kia').value(),
    ];
    const result = getFilteredMarkList(marks, 'фгв').map(({ name }) => name);
    expect(result).toEqual([ 'Audi' ]);
});

it('фильтрует марки по кириллическому названию', () => {
    const marks = [
        markMock.withName('Audi').withCyrillicName('Ауди').value(),
        markMock.withName('BMW').withCyrillicName('БМВ').value(),
        markMock.withName('Kia').withCyrillicName('Киа').value(),
    ];
    const result = getFilteredMarkList(marks, 'бм').map(({ name }) => name);
    expect(result).toEqual([ 'BMW' ]);
});

it('фильтрует марки по кириллическому названию, если пользователь перепутал раскладку', () => {
    const marks = [
        markMock.withName('Audi').withCyrillicName('Ауди').value(),
        markMock.withName('BMW').withCyrillicName('БМВ').value(),
        markMock.withName('Kia').withCyrillicName('Киа').value(),
    ];
    const result = getFilteredMarkList(marks, 'fel').map(({ name }) => name);
    expect(result).toEqual([ 'Audi' ]);
});
