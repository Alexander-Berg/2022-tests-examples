import { CARD_PLANS_OFFER } from '../../__tests__/mocks';
import { getOfferInfo } from '../getOfferInfo';

describe('getOfferInfo', () => {
    it('Возвращает корректый объект', () => {
        expect(getOfferInfo({ offerId: CARD_PLANS_OFFER.offerId })).toMatchSnapshot();
    });
});
