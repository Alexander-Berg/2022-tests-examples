import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SiteCardLocation from '..';

import card from './mocks';

test('рендерится заголовок h2 с неразрывным пробелом', () => {
    const wrapper = mount(
        <AppProvider>
            <SiteCardLocation
                card={card}
            />
        </AppProvider>
    );

    expect(wrapper.find('.SiteCardLocation__title').text()).toEqual('Расположение\u00A0ЖК Небо');
});
