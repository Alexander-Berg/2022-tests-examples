import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import getComplectationId from './getComplectationId';

it('должен ID комплектации', () => {
    expect(getComplectationId(
        cloneOfferWithHelpers({}).withComplectation({ id: '123' }).value(),
    )).toEqual('123');
});

it('должен вернуть "0" строкой, если нет информации', () => {
    expect(getComplectationId(
        cloneOfferWithHelpers({}).value(),
    )).toEqual('0');
});
