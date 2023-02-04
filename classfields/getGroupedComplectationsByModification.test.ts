import listingBlockComplectations from 'auto-core/react/dataDomain/listingBlockComplectations/mocks/listingBlockComplectationsMock';

import groupComplectationsByModification from '../mocks/groupComplectationsByModification';

import { getGroupedComplectationsByModification } from './getGroupedComplectationsByModification';
import { getGroupedComplectationsByName } from './getGroupedComplectationsByName';

it('Должен сгруппировать по имени модификации', () => {
    const { complectations } = listingBlockComplectations.data;
    const groupComplectationsByName = getGroupedComplectationsByName(complectations);

    expect(getGroupedComplectationsByModification(groupComplectationsByName)).toEqual(groupComplectationsByModification);
});
