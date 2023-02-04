import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getMarkInfo';

const state = {
    catalogTechParam,
};

it('должен правильно выбрать объект с инфой о марке', () => {
    expect(selector(state)).toEqual(catalogTechParam.data?.entities[0].mark_info);
});
