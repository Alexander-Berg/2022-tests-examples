import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import ListingFiltersMotoAndTrucks from './ListingFiltersMotoAndTrucks';

const Context = createContextProvider(contextMock);

describe('фильтры used/new', () => {
    const FILTERS = [
        'KmAgeFromToFilter',
        'SellerGroupFilterTags',
        'DamageGroupFilterTags',
        'CustomsStateGroupFilterTags',
    ];

    FILTERS.forEach((filter) => {
        it(`должен нарисовать фильтр ${ filter } для used`, () => {
            const searchParameters: TSearchParameters = {
                category: 'trucks',
                trucks_category: 'LCV',
                section: 'used',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersMotoAndTrucks
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                    />
                </Context>,
            ).dive();

            expect(wrapper.find(filter)).toHaveLength(1);
        });

        it(`не должен нарисовать фильтр ${ filter } для new`, () => {
            const searchParameters: TSearchParameters = {
                category: 'trucks',
                trucks_category: 'LCV',
                section: 'new',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersMotoAndTrucks
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                    />
                </Context>,
            ).dive();

            expect(wrapper.find(filter)).toHaveLength(0);
        });
    });
});

it(`должен нарисовать фильтр ExchangeGroupFilter для used`, () => {
    const searchParameters: TSearchParameters = {
        category: 'trucks',
        trucks_category: 'LCV',
        section: 'used',
    };
    const wrapper = shallow(
        <Context>
            <ListingFiltersMotoAndTrucks
                offersCount={ 10 }
                onChange={ noop }
                searchParameters={ searchParameters }
            />
        </Context>,
    ).dive();

    const tagFilters = wrapper.findWhere(n => n.props().title === 'Дополнительные параметры').dive();
    expect(tagFilters.find('ExchangeGroupFilter')).toHaveLength(1);
});

it(`не должен нарисовать фильтр ExchangeGroupFilter для new`, () => {
    const searchParameters: TSearchParameters = {
        category: 'trucks',
        trucks_category: 'LCV',
        section: 'new',
    };
    const wrapper = shallow(
        <Context>
            <ListingFiltersMotoAndTrucks
                offersCount={ 10 }
                onChange={ noop }
                searchParameters={ searchParameters }
            />
        </Context>,
    ).dive();

    const tagFilters = wrapper.findWhere(n => n.props().title === 'Дополнительные параметры').dive();
    expect(tagFilters.find('ExchangeGroupFilter')).toHaveLength(0);
});
