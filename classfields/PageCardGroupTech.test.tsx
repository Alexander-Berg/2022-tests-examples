/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import _ from 'lodash';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { cardGroupComplectations } from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';

import changeFilters from 'auto-core/react/dataDomain/cardGroup/actions/changeFilters';

import PageCardGroupTech from './PageCardGroupTech';

jest.mock('auto-core/react/dataDomain/cardGroup/actions/changeFilters', () => {
    return jest.fn(() => ({ type: 'blahblahblah,tech_param=21221553' }));
});

const CATALOG_FILTER = [
    {
        mark: 'TOYOTA',
        model: 'CAMRY',
        generation: '22462291',
        configuration: '22462326',
        tech_param: '22462414',
    },
];

const initialState = {
    config: {
        data: {
            pageParams: {
                category: 'cars',
                section: 'new',
                catalog_filter: CATALOG_FILTER,
            },
        },
    },
    listing: {
        filteredOffersCount: 0,
        data: {
            pagination: {},
            search_parameters: {
                catalog_filter: CATALOG_FILTER,
            },
        },
    },
    cardGroupComplectations: _.cloneDeep(cardGroupComplectations),
};

initialState.cardGroupComplectations.data.search_parameters.catalog_filter = CATALOG_FILTER;

const store = mockStore(initialState);
const Context = createContextProvider(contextMock);

it('проставляет в адресе первую опцию, если она не указана явно', () => {
    renderWrapper();

    expect(changeFilters).toHaveBeenCalledTimes(1);
    expect((changeFilters as jest.MockedFunction<typeof changeFilters>).mock.calls[0][0].tech_param_id).toEqual([ '22462414' ]);

    expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
    // Вот здесь должен прийти catalog_filter но мы замокали иначе,
    // поэтому это не важно. Важно, что ключ - значение совпадают c моком
    expect(contextMock.replaceState.mock.calls[0][0]).toEqual('link/card-group-tech/?type=blahblahblah%2Ctech_param%3D21221553');
});

describe('при выборе модификации', () => {
    it('просит обновить счетчики, передает правильный tech_param_id', () => {
        const wrapper = renderWrapper();
        const list = wrapper.find('CardGroupModificationsList');

        list.simulate('change', [ '21221553' ]);

        expect(changeFilters).toHaveBeenCalledTimes(2);
        expect((changeFilters as jest.MockedFunction<typeof changeFilters>).mock.calls[1][0].tech_param_id).toEqual([ '21221553' ]);
    });

    it('меняет адрес при выборе модификации', () => {
        const wrapper = renderWrapper();
        const list = wrapper.find('CardGroupModificationsList');

        list.simulate('change', [ '21221553' ]);

        expect(contextMock.replaceState).toHaveBeenCalledTimes(2);
        // Вот здесь должен прийти catalog_filter но мы замокали иначе,
        // поэтому это не важно. Важно, что ключ - значение совпадают c моком
        expect(contextMock.replaceState.mock.calls[1][0]).toEqual('link/card-group-tech/?type=blahblahblah%2Ctech_param%3D21221553');
    });
});

function renderWrapper() {
    const page = shallow(
        <Context>
            <Provider store={ store }>
                <PageCardGroupTech/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    return page;
}
