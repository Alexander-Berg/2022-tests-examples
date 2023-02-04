/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type { TSearchParametersCounters } from 'auto-core/react/dataDomain/listing/selectors/getSearchParametersCounters';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateGeo } from 'auto-core/react/dataDomain/geo/StateGeo';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import ListingHead from './ListingHead';

let store: {
    geo: StateGeo;
    config: StateConfig;
};
let searchParameters: TSearchParameters;
let searchParametersCounters: TSearchParametersCounters;
beforeEach(() => {
    searchParameters = {
        category: 'cars',
        section: 'used',
    };
    searchParametersCounters = {
        main: 0,
        mmm: 0,
        extended: 0,
        panel: 0,
    };
    store = {
        geo: geoMock,
        config: configMock.value(),
    };
});

describe('Кнопки с гео', () => {
    it('должен в обычном листинге нарисовать кнопки в последовательности: Параметры, Гео', () => {
        const wrapper = shallow(
            <ListingHead
                mmmInfo={ [ {} ] }
                onChangeFilter={ noop }
                onChangeSearchParameters={ noop }
                onSetSearchParameters={ noop }
                onSubmit={ noop }
                searchParameters={ searchParameters }
                searchParametersCounters={ searchParametersCounters }
                searchTagsDictionary={ [] }
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();

        const buttons = wrapper.find('.ListingHeadMobile__buttons').children();

        expect(buttons).toHaveLength(3);
        expect(buttons.at(1).dive().text()).toEqual('<IconSvg />Параметры');
        expect(buttons.at(2).dive().text()).toEqual('<FilterGeo />');
    });

    it('должен на странице дилера легковых нарисовать кнопки в последовательности: Параметры', () => {
        searchParameters.dealer_code = 'code';
        const wrapper = shallow(
            <ListingHead
                mmmInfo={ [ {} ] }
                onChangeFilter={ noop }
                onChangeSearchParameters={ noop }
                onSetSearchParameters={ noop }
                onSubmit={ noop }
                searchParameters={ searchParameters }
                searchParametersCounters={ searchParametersCounters }
                searchTagsDictionary={ [] }
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();

        const buttons = wrapper.find('.ListingHeadMobile__buttons').children();

        expect(buttons).toHaveLength(2);
        expect(buttons.at(1).dive().text()).toEqual('<IconSvg />Параметры');
    });
});

it('должен открыть фильтр и убрать параметр, если есть showFilters в урле', () => {
    store.config = configMock.withPageParams({ showFilters: 'true' }).value();
    searchParameters.dealer_code = 'code';
    const wrapper = shallow(
        <ListingHead
            mmmInfo={ [ {} ] }
            onChangeFilter={ noop }
            onChangeSearchParameters={ noop }
            onSetSearchParameters={ noop }
            onSubmit={ noop }
            searchParameters={ searchParameters }
            searchParametersCounters={ searchParametersCounters }
            searchTagsDictionary={ [] }
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    const filterPopup = wrapper.find('Connect(ListingFiltersPopup)');
    expect(filterPopup).not.toBeEmptyRender();
    expect(contextMock.replaceState).toHaveBeenCalledWith('link/listing/?');
});

describe('FAB', () => {
    it('покажется в обычных условиях', () => {
        const wrapper = shallow(
            <ListingHead
                mmmInfo={ [ {} ] }
                onChangeFilter={ noop }
                onChangeSearchParameters={ noop }
                onSetSearchParameters={ noop }
                onSubmit={ noop }
                searchParameters={ searchParameters }
                searchParametersCounters={ searchParametersCounters }
                searchTagsDictionary={ [] }
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const fab = wrapper.find('ListingFab');

        expect(fab.isEmptyRender()).toBe(false);
    });

    it('не покажется для переданного пропа', () => {
        const wrapper = shallow(
            <ListingHead
                mmmInfo={ [ {} ] }
                onChangeFilter={ noop }
                onChangeSearchParameters={ noop }
                onSetSearchParameters={ noop }
                onSubmit={ noop }
                searchParameters={ searchParameters }
                searchParametersCounters={ searchParametersCounters }
                searchTagsDictionary={ [] }
                hideFiltersFab
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const fab = wrapper.find('ListingFab');

        expect(fab.isEmptyRender()).toBe(true);
    });
});
