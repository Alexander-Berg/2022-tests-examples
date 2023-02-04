import { shallow } from 'enzyme';
import React from 'react';

import photo from 'autoru-frontend/mockData/images/320';

import FilterableListGenerationItem from './FilterableListGenerationItem';

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

it('должен вызвать onCheck по клику в чекбокс', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListGenerationItem
            checked
            generation={ gen }
            onCheck={ onCheck }
            onSubmit={ onSubmit }
            multiSelect
        />,
    );

    wrapper.find('Checkbox').simulate('check', false);

    expect(onCheck).toHaveBeenCalledWith('12345', false);
    expect(onSubmit).not.toHaveBeenCalled();
});

it('должен вызвать onCheck и onSubmit по клику вне чекбокса', () => {
    const onCheck = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = shallow(
        <FilterableListGenerationItem
            checked
            generation={ gen }
            onCheck={ onCheck }
            onSubmit={ onSubmit }
            multiSelect
        />,
    );

    wrapper.find('.FilterableListGenerationItem').simulate('click');

    expect(onCheck).toHaveBeenCalledWith('12345', false);
    expect(onSubmit).toHaveBeenCalled();
});
