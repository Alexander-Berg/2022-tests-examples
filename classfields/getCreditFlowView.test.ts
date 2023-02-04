import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import CreditFlowView from 'auto-core/react/dataDomain/credit/dicts/creditFlowView';

import { CreditApplicationState } from 'auto-core/types/TCreditBroker';

import getCreditFlowView from './getCreditFlowView';

const creditProductCalculator = creditProductMock().value();

const soldOffer = cloneOfferWithHelpers(offerMock)
    .withIsOwner(false)
    .withSaleId('870-678')
    .withStatus(OfferStatus.INACTIVE)
    .value();
const availableOffer = cloneOfferWithHelpers(offerMock)
    .withIsOwner(false)
    .withSaleId('890-111')
    .withCreditPrecondition()
    .value();
const availableOffer2 = cloneOfferWithHelpers(offerMock)
    .withIsOwner(false)
    .withSaleId('890-678')
    .withCreditPrecondition()
    .value();
const unavailableOffer = cloneOfferWithHelpers(offerMock)
    .withSaleId('892-678')
    .withIsOwner(false)
    .value();

const draftCreditApplication = creditApplicationMock()
    .withState(CreditApplicationState.DRAFT)
    .value();
const activeCreditApplication = creditApplicationMock()
    .withState(CreditApplicationState.ACTIVE)
    .value();

it('возвращает ATTACH_CAR, если нет прикрепленной тачки и есть текущая', () => {
    expect(getCreditFlowView({
        creditProductCalculator,
        creditApplication: draftCreditApplication,
        currentOffer: availableOffer,
    }))
        .toEqual(CreditFlowView.ATTACH_CAR);
});

it('возвращает REPLACE_DRAFT_CAR для драфта, если есть текущий и прикрепленный оффер, они разные', () => {
    expect(getCreditFlowView({
        creditProductCalculator,
        creditApplication: draftCreditApplication,
        attachedOffer: availableOffer2,
        currentOffer: availableOffer,
    }))
        .toEqual(CreditFlowView.REPLACE_DRAFT_CAR);
});

it('возвращает REPLACE_ACTIVE_CAR для активной заявки, если есть текущий и прикрепленный оффер, они разные', () => {
    expect(getCreditFlowView({
        creditProductCalculator,
        creditApplication: activeCreditApplication,
        attachedOffer: availableOffer2,
        currentOffer: availableOffer,
    }))
        .toEqual(CreditFlowView.REPLACE_ACTIVE_CAR);
});

it('возвращает NO_CURRENT_CAR_DRAFT для драфта, с прикрепленной, но без текущей тачки', () => {
    expect(getCreditFlowView({
        creditProductCalculator,
        creditApplication: draftCreditApplication,
        attachedOffer: availableOffer,
    }))
        .toEqual(CreditFlowView.NO_CURRENT_CAR_DRAFT);
});

it('возвращает CONTINUE для драфта и если прикрепленная и текущая тачки - одинаковые', () => {
    expect(getCreditFlowView({
        creditProductCalculator,
        creditApplication: draftCreditApplication,
        attachedOffer: availableOffer,
        currentOffer: availableOffer,
    }))
        .toEqual(CreditFlowView.CONTINUE);
});

describe('возвращает EMPTY', () => {
    it('если нет доступного кредитного продукта', () => {
        expect(getCreditFlowView({ attachedOffer: availableOffer }))
            .toEqual(CreditFlowView.EMPTY);
    });

    it('если текущий оффер есть и он недоступен в кредит', () => {
        expect(getCreditFlowView({ currentOffer: unavailableOffer, creditProductCalculator }))
            .toEqual(CreditFlowView.EMPTY);
    });
});

describe('возвращает FORM', () => {
    it('если нет активной заявки', () => {
        expect(getCreditFlowView({ currentOffer: availableOffer, creditProductCalculator }))
            .toEqual(CreditFlowView.FORM);
    });

    it('если заявка в статусе CANCELED', () => {
        const creditApplication = creditApplicationMock()
            .withState(CreditApplicationState.CANCELED)
            .value();

        expect(getCreditFlowView({
            creditApplication,
            creditProductCalculator,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.FORM);
    });
});

describe('возвращает ACTIVE', () => {
    it('если заявка активна, текущей тачки нет, но есть прикрепленная', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: activeCreditApplication,
            attachedOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.ACTIVE);
    });

    it('для активной заявки и если прикрепленная и текущая тачки - одинаковые', () => {
        const creditApplication = creditApplicationMock()
            .withState(CreditApplicationState.ACTIVE)
            .withOffer({ id: '123-456', category: 'cars' })
            .value();

        expect(getCreditFlowView({
            creditApplication,
            creditProductCalculator,
            attachedOffer: availableOffer,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.ACTIVE);
    });
});

describe('возвращает NO_ATTACHED_OR_CURRENT_CARS', () => {
    it('если заявка активна, нет текущей и прикрепленной тачки', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: activeCreditApplication,
        }))
            .toEqual(CreditFlowView.NO_ATTACHED_OR_CURRENT_CARS);
    });

    it('если заявка в драфте, нет текущей и прикрепленной тачки', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: draftCreditApplication,
        }))
            .toEqual(CreditFlowView.NO_ATTACHED_OR_CURRENT_CARS);
    });
});

describe('возвращает REPLACE_SOLD_CAR', () => {
    it('если заявка в драфте', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: draftCreditApplication,
            attachedOffer: soldOffer,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.REPLACE_SOLD_CAR);
    });

    it('если заявка активна', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: activeCreditApplication,
            attachedOffer: soldOffer,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.REPLACE_SOLD_CAR);
    });
});

describe('возвращает REPLACE_UNAVAILABLE_CAR', () => {
    it('если заявка в драфте', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: draftCreditApplication,
            attachedOffer: unavailableOffer,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.REPLACE_UNAVAILABLE_CAR);
    });

    it('если заявка активна', () => {
        expect(getCreditFlowView({
            creditProductCalculator,
            creditApplication: activeCreditApplication,
            attachedOffer: unavailableOffer,
            currentOffer: availableOffer,
        }))
            .toEqual(CreditFlowView.REPLACE_UNAVAILABLE_CAR);
    });
});
