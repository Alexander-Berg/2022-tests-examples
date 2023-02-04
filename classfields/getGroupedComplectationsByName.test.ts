import listingBlockComplectations from 'auto-core/react/dataDomain/listingBlockComplectations/mocks/listingBlockComplectationsMock';

import groupComplectationsByNameMock from '../mocks/groupComplectationsByNameMock';

import { getGroupedComplectationsByName } from './getGroupedComplectationsByName';

it('Должен сгруппировать комплектации по имени', () => {
    const { complectations } = listingBlockComplectations.data;

    expect(getGroupedComplectationsByName(complectations)).toEqual(groupComplectationsByNameMock);
});
