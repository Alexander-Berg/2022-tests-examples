import React from 'react';
import { shallow } from 'enzyme';

import SectionTitle from './SectionTitle';

it('должен отрендерить SectionTitle тегом h3', () => {
    const wrapper = shallow(
        <SectionTitle tag="h3">
            Привет!
        </SectionTitle>,
    );

    expect(wrapper.find('h3').exists()).toBe(true);
});

it('должен отрендерить SectionTitle ссылкой в контенте и выставленными атрибутами', () => {
    const wrapper = shallow(
        <SectionTitle
            url="yandex.ru"
            attributes={{
                title: 'Привет!',
            }}
        >
            Привет!
        </SectionTitle>,
    );

    expect(wrapper).toMatchSnapshot();
});
