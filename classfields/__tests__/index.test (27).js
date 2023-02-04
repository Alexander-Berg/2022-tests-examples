import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import CardSection from '../index';

test('рендерится заголовок h2 с неразрывным пробелом', () => {
    const wrapper = mount(
        <AppProvider>
            <CardSection
                title='Расположение'
                locativeFullName='ЖК Небо'
            >
                <h1>test</h1>
            </CardSection>
        </AppProvider>
    );

    expect(wrapper.find('.CardSection__title').text()).toBe('Расположение\u00A0ЖК Небо');
});
