import sortBy from 'lodash/sortBy';

import stubs from 'realty-core/app/test-utils/stubs';

import { getPhoneGoals } from '../createOfferPhoneStats';

describe('getPhoneGoals(offer)', () => {
    stubs.everySearcherOfferMatchSnapshot(offer => sortBy(getPhoneGoals({ offer })), {
        filter: offer => offer.active,
        checkVos: false
    });
});
