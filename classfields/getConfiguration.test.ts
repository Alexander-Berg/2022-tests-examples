import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getConfiguration';

const state = {
    catalogTechParam,
};

it('должен правильно выбрать объект с конфигурацией', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities[0].configuration);
});
