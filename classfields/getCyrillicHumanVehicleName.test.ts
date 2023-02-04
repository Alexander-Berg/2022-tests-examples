import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getCyrillicHumanVehicleName';

const state = {
    catalogTechParam,
};

it('должен правильно вернуть название тачки на русском', () => {
    expect(selector(state)).toEqual('Ауди И-трон 50 1');
});
