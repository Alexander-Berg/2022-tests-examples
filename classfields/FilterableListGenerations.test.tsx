import { shallow } from 'enzyme';
import React from 'react';

import photo from 'autoru-frontend/mockData/images/320';

import type { State } from './FilterableListGenerations';
import FilterableListGenerations from './FilterableListGenerations';

const gen = {
    count: 4,
    id: '12345',
    itemFilterParams: {
        super_gen: '12345',
    },
    name: 'iii',
    photo: photo,
    mobilePhoto: photo,
    yearFrom: 2000,
    yearTo: 3000,
};

const items = [
    {
        generations: [ gen, gen, gen ],
        id: '1',
        name: 'модель номер один',
    },
    {
        generations: [ gen ],
        id: '2',
        name: 'модель номер два',
    },
    {
        generations: [ gen ],
        id: '3',
        name: 'модель номер три',
    },
];

it('должен переключать марки по клику на табы', () => {
    const wrapper = shallow(
        <FilterableListGenerations
            catalogFilter={{ mark: '1', models: [ { id: '1', generations: [], nameplates: [] } ] }}
            items={ items }
            onItemCheck={ jest.fn() }
            onSubmit={ jest.fn() }
        />,
    );
    wrapper.find('Tags').simulate('change', '2');

    expect((wrapper.state() as State).selectedModelIndex).toBe(1);
});
