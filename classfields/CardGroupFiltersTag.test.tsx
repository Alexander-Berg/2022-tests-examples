import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type {
    TCardGroupFiltersValues,
    TCardGroupFilterItems,
} from 'auto-core/react/dataDomain/cardGroup/types';

import CardGroupFiltersTag from './CardGroupFiltersTag';

const initialValues = {
    catalog_equipment: [],
    color: [],
    gear_type: [],
    search_tag: [],
    tech_param_id: [],
    transmission: [],
} as TCardGroupFiltersValues;

const baseProps = {
    filteredOffersCount: 2,
    items: {} as TCardGroupFilterItems,
    searchParameters: {},
    onChange: _.noop,
    onChangeComplectation: _.noop,
    onChangeOptions: _.noop,
    onReset: _.noop,
    onSubmit: _.noop,
    getMetrikaFilterItem: () => '',
    sendMetrikaEvent: _.noop,
    showPopup: false,
    selectedComplectationsNames: [],
};

it('не должен рендерить каунтер, если никакие фильтры не выбраны', () => {
    const wrapper = shallow(
        <CardGroupFiltersTag
            { ...baseProps }
            filterValues={ initialValues }
        />,
        { context: contextMock },
    );

    const counter = wrapper.find('Tag').dive().find('.CardGroupFiltersTag__buttonFiltersCounter');

    expect(counter).not.toExist();
});

it('должен рендерить каунтер 1, если выбран фильтр', () => {
    const wrapper = shallow(
        <CardGroupFiltersTag
            { ...baseProps }
            filterValues={{
                ...initialValues,
                price_from: 1000000,
            }}
        />,
        { context: contextMock },
    );

    const counter = wrapper.find('Tag').dive().find('.CardGroupFiltersTag__buttonFiltersCounter');

    expect(counter.text()).toBe('1');
});

it('должен рендерить каунтер 3, если выбрано 2 серчтега и еще фильтр', () => {
    const wrapper = shallow(
        <CardGroupFiltersTag
            { ...baseProps }
            filterValues={{
                ...initialValues,
                price_from: 1000000,
                search_tag: [ 'first', 'second' ],
            }}
        />,
        { context: contextMock },
    );

    const counter = wrapper.find('Tag').dive().find('.CardGroupFiltersTag__buttonFiltersCounter');

    expect(counter.text()).toBe('3');
});

it('должен рендерить каунтер 2, если передается число выбранных доп. опций', () => {
    const wrapper = shallow(
        <CardGroupFiltersTag
            { ...baseProps }
            filterValues={ initialValues }
            selectedAdditionalOptionsCount={ 2 }
        />,
        { context: contextMock },
    );

    const counter = wrapper.find('Tag').dive().find('.CardGroupFiltersTag__buttonFiltersCounter');

    expect(counter.text()).toBe('2');
});
