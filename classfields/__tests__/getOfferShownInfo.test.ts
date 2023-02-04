import { ILocation } from 'realty-core/types/location';

import { CARD_PLANS_OFFER } from '../../__tests__/mocks';

import { getOfferShownInfo } from '../getOfferShownInfo';

describe('getOfferShownInfo', () => {
    it('Возвращает ничего', () => {
        expect(getOfferShownInfo({ offer: { location: {} as ILocation } })).toMatchSnapshot();
    });

    it('Возвращает полностью заполненный объект', () => {
        expect(
            getOfferShownInfo({
                offer: CARD_PLANS_OFFER,
            })
        ).toMatchSnapshot();
    });
});
