/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { HelmetTitle } from './HelmetTitle';

it('рендерит все теги с содержимым, если передавать только title', () => {
    const title = 'Моника Геллер Рейчел Грин';

    const wrapper = shallow(
        <HelmetTitle title={ title }/>
    );

    expect(wrapper.find('title').text()).toBe(title);
    expect(wrapper.find('meta[property="og:title"]').prop('content')).toBe(title);
});

it('рендерит другое содержимое для meta тегов, если передавать ogTitle', () => {
    const title = 'Моника Геллер Рейчел Грин';
    const ogTitle = 'Привет!';

    const wrapper = shallow(
        <HelmetTitle title={ title } ogTitle={ ogTitle }/>
    );

    expect(wrapper.find('title').text()).toBe(title);
    expect(wrapper.find('meta[property="og:title"]').prop('content')).toBe(ogTitle);
});

