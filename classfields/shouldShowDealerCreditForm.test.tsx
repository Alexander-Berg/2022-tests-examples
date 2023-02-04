import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import shouldShowDealerCreditForm from './shouldShowDealerCreditForm';

describe('должен вернуть false, если', () => {

    it('нет конфигурации дилерского кредита', () => {
        expect(shouldShowDealerCreditForm(cloneOfferWithHelpers({}).value())).toEqual(false);
    });
});

describe('должен вернуть true, если есть конфигурация дилерского кредита и', () => {

    it('оффер по б/у машине', () => {
        const offer = cloneOfferWithHelpers({})
            .withCategory('cars')
            .withSection('used')
            .withDealerCredit()
            .value();
        expect(shouldShowDealerCreditForm(offer)).toEqual(true);
    });
});
