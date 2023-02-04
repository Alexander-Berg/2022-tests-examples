import { Provider } from 'react-redux';
import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { Props as PopupProps } from 'auto-core/react/components/mobile/FiltersPopup/FiltersPopup';
import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import ListingFiltersPopupGenerations from './ListingFiltersPopupGenerations';

const props = {
    onItemCheck: jest.fn(),
    onRequestHide: () => {},
    onSubmitClick: () => {},
    submitText: 'Показать 1 объявление',
};

describe('Шапка поп-апа выбора поколений', () => {
    it('нет выбранных поколений', () => {
        const store = mockStore({
            breadcrumbsPublicApi: breadcrumbsPublicApi,
        });
        const wrapper = shallow(
            <Provider store={ store }>
                <ListingFiltersPopupGenerations
                    { ... props }
                    catalogFilter={{ mark: 'FORD', models: [ { id: 'ECOSPORT', generations: [], nameplates: [] } ] }}/>
            </Provider>
            ,
        ).dive().dive();

        const popupProps = wrapper.find('FiltersPopup').props() as PopupProps;
        expect(popupProps.title).toBe('Выбрать поколения');
        expect(popupProps.withReset).toBe(false);
    });

    it('есть выбранные поколения', () => {
        const store = mockStore({
            breadcrumbsPublicApi: breadcrumbsPublicApi,
        });
        const wrapper = shallow(
            <Provider store={ store }>
                <ListingFiltersPopupGenerations
                    { ... props }
                    catalogFilter={{ mark: 'FORD', models: [ { id: 'ECOSPORT', generations: [ '1', '2' ], nameplates: [] } ] }}/>
            </Provider>
            ,
        ).dive().dive();

        const popupProps = wrapper.find('FiltersPopup').props() as PopupProps;
        expect(popupProps.title).toBe('Выбрано 2');
        expect(popupProps.withReset).toBe(true);
    });

    it('есть исключенные поколения', () => {
        const store = mockStore({
            breadcrumbsPublicApi: breadcrumbsPublicApi,
        });
        const wrapper = shallow(
            <Provider store={ store }>
                <ListingFiltersPopupGenerations
                    { ... props }
                    catalogFilter={
                        {
                            exclude: true,
                            mark: 'FORD',
                            models: [ { id: 'ECOSPORT', generations: [ '1', '2' ], nameplates: [] } ],
                        }
                    }/>
            </Provider>
            ,
        ).dive().dive();

        const popupProps = wrapper.find('FiltersPopup').props() as PopupProps;
        expect(popupProps.title).toBe('Исключено 2');
        expect(popupProps.withReset).toBe(true);
    });
});

it('сброс поколений', () => {
    const store = mockStore({
        breadcrumbsPublicApi: breadcrumbsPublicApi,
    });
    const wrapper = shallow(
        <Provider store={ store }>
            <ListingFiltersPopupGenerations
                { ... props }
                catalogFilter={
                    {
                        exclude: true,
                        mark: 'FORD',
                        models: [ { id: 'ECOSPORT', generations: [ '1', '2' ], nameplates: [] } ],
                    }
                }/>
        </Provider>
        ,
    ).dive().dive();

    wrapper.find('FiltersPopup').simulate('reset');
    expect(props.onItemCheck).toHaveBeenCalledWith([ { id: 'ECOSPORT', generations: [], nameplates: [] } ]);
});
