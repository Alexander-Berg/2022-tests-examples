import React from 'react';
import { shallow } from 'enzyme';

import HighlightedText from './HighlightedText';

it('рендерит HighlightedText переданным тегом', () => {
    const wrapper = shallow(
        <HighlightedText tag="h3">Привет</HighlightedText>,
    );

    expect(wrapper.find('h3').exists()).toBe(true);
});

it('рендерит HighlightedText с переданным классом', () => {
    const wrapper = shallow(
        <HighlightedText className="Test">Привет!</HighlightedText>,
    );

    expect(wrapper.find('.Test').exists()).toBe(true);
});
