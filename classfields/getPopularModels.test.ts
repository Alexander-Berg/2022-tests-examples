import _ from 'lodash';

import modelMock from 'auto-core/models/catalogSuggest/mocks/model.mock';

import allModels from '../mocks/kia_models.mock';

import getPopularModels from './getPopularModels';

it('если моделей меньше 12 вернет их все', () => {
    const models = [
        modelMock.withName('Rio').value(),
        modelMock.withName('Optima').value(),
        modelMock.withName('Soul').value(),
    ];
    const result = getPopularModels(models);
    expect(result).toHaveLength(3);
});

it('если кол-во моделей с 30 и более офферами больше 12, вернет первый 23 из них', () => {
    const models = allModels.map((model) => {
        return modelMock.withName(model).withOfferCount(24 + model.length).value();
    });

    const result = getPopularModels(models);
    expect(result).toHaveLength(20);
    expect(_.first(result)?.name).toBe('Borrego');
    expect(_.last(result)?.name).toBe('Telluride');
});

it('если кол-во моделей с 30 и более офферами меньше 12, то возьмет лимит в 10 офферов и 11 моделей', () => {
    const models = allModels.map((model) => {
        return modelMock.withName(model).withOfferCount(4 + model.length).value();
    });

    const result = getPopularModels(models);
    expect(result).toHaveLength(11);
    expect(_.first(result)?.name).toBe('Carnival');
    expect(_.last(result)?.name).toBe('Stinger');
});

it('в противном случае вернет модели как есть', () => {
    const models = allModels.map((model) => {
        return modelMock.withName(model).withOfferCount(0).value();
    });

    const result = getPopularModels(models);
    expect(result).toHaveLength(models.length);
    expect(_.first(result)?.name).toBe('Avella');
    expect(_.last(result)?.name).toBe('XCeed');
});
