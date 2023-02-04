import React from 'react';
import { shallow } from 'enzyme';

import { PostHelmet } from 'core/client/features/Post/PostHelmet/PostHelmet';

import PostMain from './PostMain';

it('в разметке присутствует разметка для helmet', () => {
    const wrapper = shallow(<PostMain/>);

    expect(wrapper.find(PostHelmet).exists()).toBe(true);
});
