import { CARD_PLANS_OFFER } from '../../__tests__/mocks';
import { getOfferUpdateTime } from '../getOfferUpdateTime';

describe('getOfferUpdateTime', () => {
    it('Работает корректно', () => {
        expect(getOfferUpdateTime({ updateDate: CARD_PLANS_OFFER.updateDate })).toEqual(1643960793308);
        expect(getOfferUpdateTime({})).toEqual(undefined);
    });
});
