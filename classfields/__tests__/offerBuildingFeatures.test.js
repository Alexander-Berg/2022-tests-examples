import stubs from 'realty-core/app/test-utils/stubs';
import { selectBuildingFeatures } from '..';
import { removeEnriched } from 'realty-core/view/react/modules/offers/libs/get-source';

describe('selectBuildingFeatures(offer)', () => {
    stubs.everySearcherOfferMatchSnapshot(
        selectBuildingFeatures, {
            checkVos: true,
            filter: offer => [ 'APARTMENT', 'ROOMS' ].includes(offer.offerCategory),
            compareWithVos: (searcherData, vosData) => {
                expect(searcherData.filter(removeEnriched)).toEqual(vosData);
            }
        });
});
