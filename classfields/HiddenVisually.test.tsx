import React from 'react';
import { shallow } from 'enzyme';

import HiddenVisually from './HiddenVisually';

it('рендерит элемент в разметке', () => {
    const wrapper = shallow(<HiddenVisually>Привет!</HiddenVisually>);

    expect(wrapper.find('div').text()).toContain('Привет!');
});
