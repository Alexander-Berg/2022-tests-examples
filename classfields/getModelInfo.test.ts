import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getModelInfo';

const state = {
    catalogTechParam,
};

it('должен правильно выбрать объект с инфой о модели', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities[0].model_info);
});
