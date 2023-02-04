import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';

import getCreditApplicationAttachedOfferID from './getCreditApplicationAttachedOfferID';

describe('getCreditApplicationAttachedOfferID', () => {
    it('возвращает true при наличии оффера', () => {
        const creditApplication = creditApplicationMock()
            .withOffer({ id: '123-456', category: 'cars' })
            .value();

        expect(getCreditApplicationAttachedOfferID(creditApplication))
            .toEqual('123-456');
    });

    it('возвращает undefined при отсутствии оффера', () => {
        const creditApplication = creditApplicationMock().value();

        expect(getCreditApplicationAttachedOfferID(creditApplication))
            .toBeUndefined();
    });
});
