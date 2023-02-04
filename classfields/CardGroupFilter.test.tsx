import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import type { TCardGroupFiltersValues } from 'auto-core/react/dataDomain/cardGroup/types';
import gateApi from 'auto-core/react/lib/gateApi';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import type { State } from './CardGroupFilter';
import CardGroupFilter from './CardGroupFilter';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve()),
    };
});

type ItemProps = {
    checked: boolean;
}

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;
let filterValues: TCardGroupFiltersValues;
let searchParameters: TSearchParameters;

beforeEach(() => {
    getResource.mockClear();
    filterValues = {
        catalog_equipment: [],
        color: [],
        gear_type: [],
        tech_param_id: [ '2' ],
        transmission: [ '1' ],
        search_tag: [],
    };
    searchParameters = {
        catalog_filter: [ {} ],
    };
});

it('должен открыть модал с фильтром по клику на тег', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={ filterValues }
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    expect(wrapper.find('Modal')).not.toExist();
    wrapper.find('Tag').simulate('click');
    expect(wrapper.find('Modal')).toExist();
});

it('не должен открыть модал с фильтром по клику на тег, если items пусто', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={ filterValues }
            getMetrikaFilterItem={ () => '' }
            items={ [] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    const tag = wrapper.find('Tag');
    expect(tag.props().disabled).toBe(true);
    tag.simulate('click');
    expect(wrapper.find('Modal')).not.toExist();
});

it('должен обновить выбранные параметры в стейте', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={ filterValues }
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.state() as State).values).toEqual([ '1' ]);
    wrapper.find('List').simulate('itemClick', '1', false);
    expect((wrapper.state() as State).values).toEqual([ ]);
});

it('должен правильно поставить check для частичного совпадения value', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={{ ...filterValues, transmission: [ '1' ] }}
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1,2' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.find('Item').props() as ItemProps).checked).toBe(true);
});

it('должен правильно поставить check для частичного совпадения value 2', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={{ ...filterValues, transmission: [ '1', '2' ] }}
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.find('Item').props() as ItemProps).checked).toBe(true);
});

it('должен правильно поставить check для полного совпадения value', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={{ ...filterValues, transmission: [ '1,2' ] }}
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1,2' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.find('Item').props() as ItemProps).checked).toBe(true);
});

it('не должен поставить check при несовпадении value', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={{ ...filterValues, transmission: [ '3' ] }}
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1,2' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.find('Item').props() as ItemProps).checked).toBe(false);
});

it('должен запросить счетчик объяв при выборе параметров', () => {
    const mockRequest = Promise.resolve({ pagination: { total_offers_count: 20 } });
    getResource.mockImplementation(() => mockRequest);

    const wrapper = shallow(
        <CardGroupFilter
            filterValues={ filterValues }
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    expect((wrapper.state() as State).offersCount).toEqual(10);
    wrapper.find('List').simulate('itemClick', '1', false);

    return mockRequest.finally(() => {
        expect((wrapper.state() as State).offersCount).toEqual(20);
    });
});

it('должен сбросить изменения при закрытии модала', () => {
    const wrapper = shallow(
        <CardGroupFilter
            filterValues={ filterValues }
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('List').simulate('itemClick', '1', false);
    wrapper.find('Modal').simulate('requestHide');
    expect((wrapper.state() as State).values).toEqual([ '1' ]);
});

it('должен вызвать onChange и метрику при тапе на кнопку "показать"', () => {
    const onChange = jest.fn();
    const sendMetrikaEvent = jest.fn();
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => 'metrikaItem' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ onChange }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ sendMetrikaEvent }
            filterValues={ filterValues }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('List').simulate('itemClick', '1', false);
    wrapper.find('Modal').dive().find('Button').simulate('click');
    expect(onChange).toHaveBeenCalledWith(
        { ...filterValues, transmission: [] },
        { shouldResetEngineParams: false },
    );
    expect(sendMetrikaEvent).toHaveBeenCalledWith('transmission', 'metrikaItem');
    expect(wrapper.find('Modal')).not.toExist();
});

it('должен вызвать onChange с множественными значениями', () => {
    const onChange = jest.fn();
    const sendMetrikaEvent = jest.fn();
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => 'metrikaItem' }
            items={ [ { title: 'Параметр 1', value: '1' }, { title: 'Параметр 2', value: '2,3' } ] }
            offersCount={ 10 }
            onChange={ onChange }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ sendMetrikaEvent }
            filterValues={ filterValues }
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('List').simulate('itemClick', '2,3', true);
    wrapper.find('Modal').dive().find('Button').simulate('click');
    expect(onChange).toHaveBeenCalledWith(
        { ...filterValues, transmission: [ '1', '2', '3' ] },
        { shouldResetEngineParams: false },
    );
});

it('должен вызвать onChange с параметром shouldResetEngineParams если меняется tech_param_id', () => {
    const onChange = jest.fn();
    const sendMetrikaEvent = jest.fn();
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => 'metrikaItem' }
            items={ [ { title: 'Параметр', value: '2,3' } ] }
            offersCount={ 10 }
            onChange={ onChange }
            paramName="tech_param_id"
            placeholder="Фильтр"
            sendMetrikaEvent={ sendMetrikaEvent }
            filterValues={{ ...filterValues, tech_param_id: [ '1' ] }}
            searchParameters={ searchParameters }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('List').simulate('itemClick', '2,3', true);
    wrapper.find('Modal').dive().find('Button').simulate('click');
    expect(onChange).toHaveBeenCalledWith(
        { ...filterValues, tech_param_id: [ '2', '3' ] },
        { shouldResetEngineParams: true },
    );
});

it('должен вычислить текст в теге, когда нет значения', () => {
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            filterValues={{ ...filterValues, transmission: [] }}
            searchParameters={ searchParameters }
        />,
    );
    expect(wrapper.find('Tag').childAt(0).text()).toBe('Фильтр');
});

it('должен вычислить текст в теге, когда одно значение', () => {
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' } ] }
            offersCount={ 10 }
            onChange={ () => {} }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            filterValues={ filterValues }
            searchParameters={ searchParameters }
        />,
    );
    expect(wrapper.find('Tag').childAt(0).text()).toBe('Параметр');
});

it('должен вычислить текст в теге, когда много значений', () => {
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '1' }, { title: 'Параметр2', value: '2' } ] }
            offersCount={ 10 }
            onChange={ () => [] }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            filterValues={{ ...filterValues, transmission: [ '1', '2' ] }}
            searchParameters={ searchParameters }
        />,
    );
    expect(wrapper.find('Tag').childAt(0).text()).toBe('Параметр +1');
});

it('должен вычислить текст в теге, когда значения не совпадают с переданными в items', () => {
    const wrapper = shallow(
        <CardGroupFilter
            getMetrikaFilterItem={ () => '' }
            items={ [ { title: 'Параметр', value: '10' }, { title: 'Параметр2', value: '20' } ] }
            offersCount={ 10 }
            onChange={ () => [] }
            paramName="transmission"
            placeholder="Фильтр"
            sendMetrikaEvent={ () => {} }
            filterValues={{ ...filterValues, transmission: [ '1', '2' ] }}
            searchParameters={ searchParameters }
        />,
    );
    expect(wrapper.find('Tag').childAt(0).text()).toBe('Фильтр');
});
