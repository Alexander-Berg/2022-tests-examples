jest.mock('auto-core/react/dataDomain/cardGroup/actions/changeFilters', () => {
    return jest.fn(() => ({ type: 'CHANGE_FILTERS' }));
});
jest.mock('auto-core/react/dataDomain/config/actions/updateData', () => {
    return jest.fn(() => ({ type: 'CONFIG_UPDATE' }));
});

import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import cardGroupState from 'auto-core/react/dataDomain/cardGroup/mocks/state.mock';
import changeFilters from 'auto-core/react/dataDomain/cardGroup/actions/changeFilters';
import updateData from 'auto-core/react/dataDomain/config/actions/updateData';
import getCardGroupFilterValues from 'auto-core/react/dataDomain/cardGroup/selectors/getCardGroupFilterValues';

import CardGroupFilters from './CardGroupFilters';

let storeMock: any;
let values: any;

beforeEach(() => {
    storeMock = cardGroupState;
    values = getCardGroupFilterValues(cardGroupState);
});

it('должен правильно вызвать changeFilters при изменении комплектации', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFilterComplectation').simulate('changeComplectation', 'some_complectation');
    expect(changeFilters).toHaveBeenCalledWith(
        { ...values, complectation_name: 'some_complectation', catalog_equipment: undefined },
        { complectationFilter: true },
    );
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении опций', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFilterComplectation').simulate('changeOptions', [ 'some_option' ]);
    expect(changeFilters).toHaveBeenCalledWith(
        { ...values, catalog_equipment: [ 'some_option' ] },
        undefined,
    );
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении двигателя', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFilter').at(0).simulate('change');

    // тут не проверяем аргументы, потому что они вычисляются и проверяются в CardGroupFilter
    expect(changeFilters).toHaveBeenCalled();
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении наличия', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFilterStock').simulate('change', 'IN_STOCK');

    expect(changeFilters).toHaveBeenCalledWith(
        { ...values, in_stock: 'IN_STOCK' },
        undefined,
    );
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении внутри CardGroupFiltersTag', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFiltersTag').simulate('change', 'new_value');

    expect(changeFilters).toHaveBeenCalledWith('new_value', undefined);
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении комплектации внутри CardGroupFiltersTag', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFiltersTag').simulate('changeComplectation', 'some_complectation');

    expect(changeFilters).toHaveBeenCalledWith(
        { ...values, complectation_name: 'some_complectation', catalog_equipment: undefined },
        { complectationFilter: true },
    );
    expect(updateData).toHaveBeenCalledTimes(1);
});

it('должен правильно вызвать changeFilters при изменении опций внутри CardGroupFiltersTag', () => {
    const wrapper = shallow(
        <CardGroupFilters filterValues={ values }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();
    wrapper.find('CardGroupFiltersTag').simulate('changeOptions', [ 'some_option' ]);
    expect(changeFilters).toHaveBeenCalledWith(
        { ...values, catalog_equipment: [ 'some_option' ] },
        undefined,
    );
    expect(updateData).toHaveBeenCalledTimes(1);
});
