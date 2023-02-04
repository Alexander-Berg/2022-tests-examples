import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';

import isCarAttachedToCreditApplication from './isCarAttachedToCreditApplication';

describe('isCarAttachedToCreditApplication', () => {
    it('возвращает true при наличии оффера', () => {
        const creditApplication = creditApplicationMock()
            .withOffer({ id: '123-456', category: 'cars' })
            .value();

        expect(isCarAttachedToCreditApplication(creditApplication))
            .toEqual(true);
    });

    it('возвращает false при отсутствии оффера', () => {
        const creditApplication = creditApplicationMock().value();

        expect(isCarAttachedToCreditApplication(creditApplication))
            .toEqual(false);
    });
});
