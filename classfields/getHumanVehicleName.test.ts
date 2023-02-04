import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import selector from './getHumanVehicleName';

const state = {
    catalogTechParam,
};

it('должен правильно вернуть название тачки на русском', () => {
    expect(selector(state)).toEqual('Audi e-tron 50 I');
});
