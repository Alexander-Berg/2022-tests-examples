import _ from 'lodash';

import cardGroupComplectationsMock from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';

import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import getGroupInfo from './getGroupInfo';

const listing = {
    data: {
        price_range: {
            min: {
                price: 1827000,
                currency: 'RUR',
            },
            max: {
                price: 2027000,
                currency: 'RUR',
            },
        },
        pagination: {
            total_offers_count: 55,
        },
    },
} as TStateListing;

it('должен вернуть общую информацию о группе', () => {
    const state = {
        ...cardGroupComplectationsMock,
        listing,
    };

    expect(getGroupInfo(state)).toMatchSnapshot();
});

it('должен вернуть информацию о конфигурации группы когда есть notice', () => {
    const cardGroupComplectations = _.cloneDeep(cardGroupComplectationsMock.cardGroupComplectations);
    cardGroupComplectations.data.complectations[0].tech_info.configuration.notice = 'notice';
    const state = {
        cardGroupComplectations,
        listing,
    };

    expect(getGroupInfo(state).configuration).toMatchSnapshot();
});
