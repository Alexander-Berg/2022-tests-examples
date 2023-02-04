import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getSuperGen';

const state = {
    catalogTechParam,
};

it('должен правильно выбрать объект с инфой о поколении', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities[0].super_gen);
});
