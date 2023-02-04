import { shallow } from 'enzyme';
import React from 'react';

import FilterableListMarks from './FilterableListMarks';

const items = [
    {
        cyrillic_name: 'Лада',
        'big-logo': '',
        count: 22,
        reviews_count: 22,
        id: '1',
        logo: '',
        name: 'LADA (ВАЗ)',
        numeric_id: 1,
        popular: true,
        itemFilterParams: {},
    },
    {
        cyrillic_name: 'Ауди',
        'big-logo': '',
        count: 22,
        reviews_count: 22,
        id: '2',
        logo: '',
        name: 'Audi',
        numeric_id: 2,
        popular: true,
        itemFilterParams: {},
    },
];

it('должен фильтровать марки по поиску', () => {
    const onItemClick = jest.fn();
    const wrapper = shallow(
        <FilterableListMarks
            value="1"
            items={ items }
            onMarkClick={ onItemClick }
            onVendorClick={ onItemClick }
            renderVendors={ true }
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', ' ваз ');
    const filteredMarks = wrapper.find('List').dive().map(item => item.children().text());

    expect(filteredMarks).toEqual([ 'LADA (ВАЗ)' ]);
});

it('должен фильтровать марки по поиску по cyrillic_name', () => {
    const onItemClick = jest.fn();
    const wrapper = shallow(
        <FilterableListMarks
            value="1"
            items={ items }
            onMarkClick={ onItemClick }
            onVendorClick={ onItemClick }
            renderVendors={ true }
        />,
    );

    wrapper.find('SearchTextInput').simulate('change', ' ауди ');
    const filteredMarks = wrapper.find('List').dive().map(item => item.children().text());

    expect(filteredMarks).toEqual([ 'Audi' ]);
});
