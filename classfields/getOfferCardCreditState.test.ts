import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import getOfferID from 'auto-core/react/lib/offer/getIdHash';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import OfferCardCreditState from 'auto-core/react/dataDomain/credit/dicts/offerCardCreditState';

import { CreditApplicationState } from 'auto-core/types/TCreditBroker';

import getOfferCardCreditState from './getOfferCardCreditState';

const creditProduct = creditProductMock().value();
const offer = cloneOfferWithHelpers(offerMock)
    .withCreditPrecondition()
    .value();

describe('getOfferCardCreditState', () => {
    it('возвращает EMPTY, если нет доступного кредитного продукта', () => {
        expect(getOfferCardCreditState({ offer }))
            .toEqual(OfferCardCreditState.EMPTY);
    });

    it('возвращает EMPTY, если у оффера нет стоимости кредита в месяц', () => {
        expect(getOfferCardCreditState({ offer: offerMock, creditProduct }))
            .toEqual(OfferCardCreditState.EMPTY);
    });

    it('возвращает FORM, если нет активной заявки', () => {
        expect(getOfferCardCreditState({ offer, creditProduct }))
            .toEqual(OfferCardCreditState.FORM);
    });

    it('возвращает FORM, если заявка в статусе CANCELED', () => {
        const creditApplication = creditApplicationMock()
            .withState(CreditApplicationState.CANCELED)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.FORM);
    });

    it('возвращает ACTIVE, если заявка в статусе ACTIVE при наличии оффера', () => {
        const creditApplication = creditApplicationMock()
            .withOffer({ id: '123-456', category: 'cars' })
            .withState(CreditApplicationState.ACTIVE)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.ACTIVE);
    });

    it('возвращает ACTIVE, если заявка в статусе ACTIVE при отсутствии оффера', () => {
        const creditApplication = creditApplicationMock()
            .withState(CreditApplicationState.ACTIVE)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.ATTACH_CAR);
    });

    it('возвращает ATTACH_CAR, если заявка в статусе DRAFT и не прикреплена машина', () => {
        const creditApplication = creditApplicationMock()
            .withState(CreditApplicationState.DRAFT)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.ATTACH_CAR);
    });

    it('возвращает REPLACE_CAR, если заявка в статусе DRAFT и прикреплена другая машина', () => {
        const creditApplication = creditApplicationMock()
            .withOffer({ id: '123-456', category: 'cars' })
            .withState(CreditApplicationState.DRAFT)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.REPLACE_CAR);
    });

    it('возвращает CONTINUE, если заявка в статусе DRAFT и прикреплена текущая машина', () => {
        const creditApplication = creditApplicationMock()
            .withOffer({ id: getOfferID(offer), category: 'cars' })
            .withState(CreditApplicationState.DRAFT)
            .value();

        expect(getOfferCardCreditState({ creditApplication, offer, creditProduct }))
            .toEqual(OfferCardCreditState.CONTINUE);
    });
});
