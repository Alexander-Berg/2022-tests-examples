import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import ListingFiltersCars from './ListingFiltersCars';

const Context = createContextProvider(contextMock);

describe('фильтры used/new', () => {
    const FILTERS = [
        'KmAgeFromToFilter',
        'SteeringWheelFilterTags',
        'SellerGroupFilterTags',
        'OwnersCountGroupFilterTags',
        'OwningTimeGroupFilterTags',
        'DamageGroupFilterTags',
        'CustomsStateGroupFilterTags',
    ];

    const TAG_FILTERS = [
        'PtsStatusOriginalFilter',
        'WithWarrantyFilter',
        'ExchangeGroupFilter',
        'HasManufacturerCertificateFilter',
        'WithDeliveryFilter',
    ];

    FILTERS.forEach((filter) => {
        it(`должен нарисовать фильтр ${ filter } для used`, () => {
            const searchParameters: TSearchParameters = {
                category: 'cars',
                section: 'used',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersCars
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                        searchTagsDictionary={ [] }
                    />
                </Context>,
            ).dive();

            expect(wrapper.find(filter)).toHaveLength(1);
        });

        it(`не должен нарисовать фильтр ${ filter } для new`, () => {
            const searchParameters: TSearchParameters = {
                category: 'cars',
                section: 'new',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersCars
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                        searchTagsDictionary={ [] }
                    />
                </Context>,
            ).dive();

            expect(wrapper.find(filter)).toHaveLength(0);
        });
    });

    TAG_FILTERS.forEach((filter) => {
        it(`должен нарисовать фильтр ${ filter } для used в доп фильтрах`, () => {
            const searchParameters: TSearchParameters = {
                category: 'cars',
                section: 'used',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersCars
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                        searchTagsDictionary={ [] }
                    />
                </Context>,
            ).dive();

            const tagFilters = wrapper.findWhere(n => n.props().title === 'Дополнительные параметры').dive();
            expect(tagFilters.find(filter)).toHaveLength(1);
        });

        it(`не должен нарисовать фильтр ${ filter } для new в доп фильтрах`, () => {
            const searchParameters: TSearchParameters = {
                category: 'cars',
                section: 'new',
            };
            const wrapper = shallow(
                <Context>
                    <ListingFiltersCars
                        offersCount={ 10 }
                        onChange={ noop }
                        searchParameters={ searchParameters }
                        searchTagsDictionary={ [] }
                    />
                </Context>,
            ).dive();

            const tagFilters = wrapper.findWhere(n => n.props().title === 'Дополнительные параметры').dive();
            expect(tagFilters.find(filter)).toHaveLength(0);
        });
    });
});
