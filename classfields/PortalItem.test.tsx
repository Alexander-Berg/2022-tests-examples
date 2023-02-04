import { shallow } from 'enzyme';
import React from 'react';

import photo from 'autoru-frontend/mockData/images/320';

import PortalItem from './PortalItem';

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

it('должен вызвать onCheck и onSubmit по клику вне чекбокса', () => {
    const onCheck = jest.fn();
    const wrapper = shallow(
        <PortalItem
            checked
            generation={ gen }
            onCheck={ onCheck }
        />,
    );

    wrapper.find('.PortalItem').simulate('click');

    expect(onCheck).toHaveBeenCalledWith('12345', false);
});
