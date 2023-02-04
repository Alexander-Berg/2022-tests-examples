import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import reducer from './reducer';
import { DRAFTS_DELETE_ITEM } from './types';

describe('DRAFTS_DELETE_ITEM:', () => {
    it('удаляет драфт из списка', () => {
        const state = {
            data: [
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').value(), offer_id: '111-aaa', can_create_redirect: true },
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(), offer_id: '222-bbb', can_create_redirect: true },
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(), offer_id: '333-ccc', can_create_redirect: true },
            ],
        };
        const action = {
            type: DRAFTS_DELETE_ITEM as typeof DRAFTS_DELETE_ITEM,
            payload: {
                draft_id: '222-bbb',
            },
        };

        const nextState = reducer(state, action);
        expect(nextState.data).toHaveLength(2);
    });

    it('ничего не делает если такого драфта нет', () => {
        const state = {
            data: [
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').value(), offer_id: '111-aaa', can_create_redirect: true },
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(), offer_id: '222-bbb', can_create_redirect: true },
                { offer: cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(), offer_id: '333-ccc', can_create_redirect: true },
            ],
        };
        const action = {
            type: DRAFTS_DELETE_ITEM as typeof DRAFTS_DELETE_ITEM,
            payload: {
                draft_id: '444-ddd',
            },
        };

        const nextState = reducer(state, action);
        expect(nextState.data).toHaveLength(3);
    });
});
