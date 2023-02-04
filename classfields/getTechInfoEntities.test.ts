import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getTechInfoEntities';

const state = {
    catalogTechParam,
};

it('должен правильно массив с энтитями по tech_param_id', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities);
});
