import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import ListingSortTabs from './ListingSortTabs';

const ContextProvider = createContextProvider(contextMock);

describe('сортировка "По оценке стоимости"', () => {
    it('должен отобразить в cars+all', () => {
        const searchParameters: TSearchParameters = {
            category: 'cars',
            section: 'all',
        };
        const wrapper = shallow(
            <ContextProvider>
                <ListingSortTabs
                    filteredOffersCount={ 100 }
                    onChange={ () => {} }
                    searchParameters={ searchParameters }
                />
            </ContextProvider>,
        ).dive();

        wrapper.find('.ListingSortTabs__sort').simulate('click');

        expect(wrapper.find('Item[value="price_profitability-desc"]')).toHaveLength(1);
    });

    it('не должен отобразить в cars+new', () => {
        const searchParameters: TSearchParameters = {
            category: 'cars',
            section: 'new',
        };
        const wrapper = shallow(
            <ContextProvider>
                <ListingSortTabs
                    filteredOffersCount={ 100 }
                    onChange={ () => {} }
                    searchParameters={ searchParameters }
                />
            </ContextProvider>,
        ).dive();

        wrapper.find('.ListingSortTabs__sort').simulate('click');

        expect(wrapper.find('Item[value="price_profitability-desc"]')).toHaveLength(0);
    });
});

it('должен отрендерить баннер с электромобилями, если выбран нужный двигатель и баннер включён', () => {
    const searchParameters: TSearchParameters = {
        category: 'cars',
        section: 'all',
        engine_group: [ 'ELECTRO' ],
    };

    const wrapper = shallow(
        <ContextProvider>
            <ListingSortTabs
                filteredOffersCount={ 100 }
                onChange={ () => {} }
                searchParameters={ searchParameters }
                isElectroBannerEnabled
            />
        </ContextProvider>,
    ).dive();

    expect(wrapper.find('Memo(SmallElectroBanner)')).toExist();
});
