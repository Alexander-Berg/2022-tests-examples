import { shallow } from 'enzyme';
import React from 'react';

import FilterableListModels from './FilterableListModels';

const items = [
    {
        count: 12,
        reviews_count: 22,
        cyrillic_name: 'Калина',
        id: '1',
        name: 'Kalina',
        itemFilterParams: {
            model: 'KALINA',
        },
        nameplates: [],
        popular: true,
    },
    {
        count: 12,
        reviews_count: 22,
        cyrillic_name: 'Нива Легенд',
        id: '2',
        name: 'Niva Legend',
        itemFilterParams: {
            model: 'NIVA_LEGEND',
        },
        nameplates: [],
        popular: true,
    },
    {
        count: 12,
        reviews_count: 22,
        cyrillic_name: 'Гранта',
        id: '3',
        itemFilterParams: {
            model: 'GRANTA',
        },
        name: 'Granta',
        nameplates: [ { id: 'cross', name: 'Cross', no_model: false, semantic_url: 'granta-cross' } ],
        popular: true,
    },
    {
        count: 12,
        reviews_count: 22,
        cyrillic_name: 'Лайт Эйс',
        id: '9',
        itemFilterParams: {
            model: 'LITE_ACE',
        },
        name: 'Lite Ace',
        nameplates: [ { id: 'noah', name: 'LiteAce Noah', no_model: false, semantic_url: 'lite_ace-noah' } ],
        popular: true,
    },
    {
        count: 12,
        reviews_count: 22,
        cyrillic_name: 'Noah',
        id: '10',
        name: 'Noah',
        itemFilterParams: {
            model: 'NOAH',
        },
        nameplates: [],
        popular: true,
    },
];

it('должен фильтровать модели по поиску', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '1', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', 'legend');
    const filteredModels = wrapper.find('List').dive().map(item => item.children().text());

    expect(filteredModels).toEqual([ 'Niva Legend' ]);
});

it('должен фильтровать шильды по поиску', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '1', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', 'Cross');

    const child = wrapper.find('List').dive();

    expect(child.prop('openedPersistent')).toEqual(true);
    expect(child.children().map(child => child.children().text())).toEqual([ 'Granta', 'Granta Cross' ]);
});

it('если в поиске есть и шильды, и модели, то модели должны бтыь выше', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '1', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', 'noah');

    const list = wrapper.find('List').dive();

    expect(list.at(0).prop('openedPersistent')).toBeUndefined();
    expect(list.at(0).children().text()).toEqual('Noah');

    expect(list.at(1).prop('openedPersistent')).toEqual(true);
    expect(list.at(1).children().map(child => child.children().text())).toEqual([ 'Lite Ace', 'Lite Ace LiteAce Noah' ]);
});

it('должен фильтровать модели по поиску по cyrillic_name', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '1', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', 'калина');
    const filteredModels = wrapper.find('List').dive().map(item => item.children().text());

    expect(filteredModels).toEqual([ 'Kalina' ]);
});

it('модель и все ее шильды выбраны, если поиск не по шильду', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '3', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
            withNameplates
        />,
    );

    expect(wrapper.find('ItemGroupRoot').at(0).props().checked).toBe(true);
    expect(wrapper.find('ItemGroup Item').at(0).props().checked).toBe(true);
});

it('выбран только шильд, если поиск по шильду', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListModels
            catalogFilter={{ mark: '1', models: [ { id: '3', generations: [], nameplates: [ 'granta-cross' ] } ] }}
            items={ items }
            onItemCheck={ onCheck }
            onSubmit={ onSubmit }
            type="check"
            withNameplates
        />,
    );

    expect(wrapper.find('ItemGroupRoot').at(0).props().checked).toBe(false);
    expect(wrapper.find('ItemGroup Item').at(0).props().checked).toBe(true);
});
