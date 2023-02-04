import modelMock from 'auto-core/models/catalogSuggest/mocks/model.mock';

import getFilteredModelList from './getFilteredModelList';

it('фильтрует марки по латинскому названию', () => {
    const models = [
        modelMock.withName('Rio').value(),
        modelMock.withName('Optima').value(),
        modelMock.withName('Soul').value(),
    ];
    const result = getFilteredModelList(models, 'rio').map(({ name }) => name);
    expect(result).toEqual([ 'Rio' ]);
});

it('фильтрует марки по латинскому названию, если пользователь перепутал раскладку', () => {
    const models = [
        modelMock.withName('Rio').value(),
        modelMock.withName('Optima').value(),
        modelMock.withName('Soul').value(),
    ];
    const result = getFilteredModelList(models, 'кшщ').map(({ name }) => name);
    expect(result).toEqual([ 'Rio' ]);
});

it('фильтрует марки по кириллическому названию', () => {
    const models = [
        modelMock.withName('Rio').withCyrillicName('Рио').value(),
        modelMock.withName('Optima').withCyrillicName('Оптима').value(),
        modelMock.withName('Soul').withCyrillicName('Соул').value(),
    ];
    const result = getFilteredModelList(models, 'ри').map(({ name }) => name);
    expect(result).toEqual([ 'Rio' ]);
});

it('фильтрует марки по кириллическому названию, если пользователь перепутал раскладку', () => {
    const models = [
        modelMock.withName('Rio').withCyrillicName('Рио').value(),
        modelMock.withName('Optima').withCyrillicName('Оптима').value(),
        modelMock.withName('Soul').withCyrillicName('Соул').value(),
    ];
    const result = getFilteredModelList(models, 'hb').map(({ name }) => name);
    expect(result).toEqual([ 'Rio' ]);
});
