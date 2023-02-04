import { getSiteShownInfo } from '../getSiteShownInfo';

import { LOCATION, SITE_PRICE, ALL_SPECIAL_PROPOSALS, SITE_PLAN } from '../../__tests__/mocks';

describe('getSiteShownInfo', () => {
    it('Возвращает ничего', () => {
        expect(getSiteShownInfo({ location: {} })).toMatchSnapshot();
    });

    it('Возвращает полный объект', () => {
        expect(
            getSiteShownInfo({
                location: LOCATION,
                specialProposals: [ALL_SPECIAL_PROPOSALS[1]],
                price: SITE_PRICE,
                plan: SITE_PLAN,
            })
        ).toMatchSnapshot();
    });
});
