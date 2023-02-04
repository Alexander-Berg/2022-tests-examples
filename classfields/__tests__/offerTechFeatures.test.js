import stubs from 'realty-core/app/test-utils/stubs';
import getOfferTechFeatures from '..';
import { removeEnriched } from 'realty-core/view/react/modules/offers/libs/get-source';

describe('getOfferTechFeatures(offer)', () => {
    stubs.everySearcherOfferMatchSnapshot(getOfferTechFeatures, {
        checkVos: true,
        compareWithVos: (searcherData, vosData) => expect(searcherData.filter(removeEnriched)).toEqual(vosData)
    });
});
