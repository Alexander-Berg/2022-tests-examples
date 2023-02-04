import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getTechParam';

const state = {
    catalogTechParam,
};

it('должен правильно выбрать объект с инфой о техпараме', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities[0].tech_param);
});
