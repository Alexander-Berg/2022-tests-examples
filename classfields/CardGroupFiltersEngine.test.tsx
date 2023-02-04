import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { TCardGroupFilterItems } from 'auto-core/react/dataDomain/cardGroup/types';

import CardGroupFiltersEngine from './CardGroupFiltersEngine';

const Context = createContextProvider(contextMock);

describe('описание при скрытом фильтре', () => {
    const items = {
        engineFilterItems: [
            {
                id: '1st one',
                title: '1st one',
                value: [ '1' ],
            },
            {
                id: '2nd one',
                title: '2nd one',
                value: [ '2' ],
            },
            {
                id: '3rd one',
                title: '3rd one',
                value: [ '3' ],
            },
        ],
    };

    it('не рендерит описание', () => {
        const wrapper = shallow(
            <Context>
                <CardGroupFiltersEngine
                    value={ [] }
                    items={ items as TCardGroupFilterItems }
                    category="cars"
                    onChange={ _.noop }
                />
            </Context>,
        ).dive().dive();

        const description = wrapper.find('.ListingFiltersItem__titleDescriptionText');

        expect(description).not.toExist();
    });

    it('рендерит число выбранных опций если их больше одной', () => {
        const wrapper = shallow(
            <Context>
                <CardGroupFiltersEngine
                    value={ [ '1', '2' ] }
                    items={ items as TCardGroupFilterItems }
                    category="cars"
                    onChange={ _.noop }
                />
            </Context>,
        ).dive().dive();

        const description = wrapper.find('.ListingFiltersItem__titleDescriptionText');

        expect(description.children().text()).toEqual('2');
    });

    it('рендерит название единственной вызванной опции', () => {
        const wrapper = shallow(
            <Context>
                <CardGroupFiltersEngine
                    value={ [ '1' ] }
                    items={ items as TCardGroupFilterItems }
                    category="cars"
                    onChange={ _.noop }
                />
            </Context>,
        ).dive().dive();

        const description = wrapper.find('.ListingFiltersItem__titleDescriptionText');

        expect(description.children().text()).toEqual('1st one');
    });
});
