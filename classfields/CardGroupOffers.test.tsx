import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockListing from 'autoru-frontend/mockData/state/listing';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import catalogComplectationsMock from 'auto-core/react/dataDomain/catalogComplectations/mocks/mock';
import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';

import CardGroupOffers from './CardGroupOffers';

const state = {
    bunker: getBunkerMock([ 'moderation/proven_owner', 'desktop/state_support' ]),
    cardGroupComplectations: cardGroupComplectationsMock,
    catalogComplectations: catalogComplectationsMock,
    equipmentDictionary: equipmentDictionaryMock,
    geo: geoMock,
    listing: _.cloneDeep(mockListing),
};
const params = {
    catalog_filter: [
        {
            configuration: '21640631',
            generation: '21640574',
            mark: 'CHERY',
            model: 'TIGGO_4',
        },
    ],
    category: 'cars',
    section: 'new',
};

it('должен отрисовать блок бесконечного листинга после списка офферов в карточке группы', () => {
    const tree = shallow(
        <CardGroupOffers
            pageParams={ params }
        />,
        { context: { ...contextMock, store: mockStore(state) } },
    ).dive();
    expect(tree.find('Connect(ListingInfiniteDesktop)')).toExist();
});

it('не должен отрисовать блок расширения радиуса после списка офферов в карточке группы', () => {
    const tree = shallow(
        <CardGroupOffers
            pageParams={ params }
        />,
        { context: { ...contextMock, store: mockStore(state) } },
    ).dive();
    expect(tree.find('Connect(CardGroupOffers)')).not.toExist();
});
