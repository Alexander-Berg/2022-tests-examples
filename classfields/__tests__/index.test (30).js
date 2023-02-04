import React from 'react';
import { mount } from 'enzyme';

import SiteCardDescription from '..';
import styles from '../styles.module.css';

import card from './mocks';

test('рендерить заголовок h2 с неразрывным пробелом', () => {
    const wrapper = mount(
        <SiteCardDescription
            card={card}
        />
    );

    expect(wrapper.find(`.${styles.title}`).text()).toBe('Описание\u00A0ЖК Небо');
});
