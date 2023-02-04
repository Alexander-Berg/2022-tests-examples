/* eslint-disable max-len */
import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SiteCardInfra from '..';

import { card, housing, crc, layersFromShortcuts, state } from './mocks';

test('рендерить заголовок h2 с неразрывным пробелом', () => {
    const wrapper = mount(
        <AppProvider initialState={state}>
            <SiteCardInfra
                card={card}
                specialProject={{}}
                housing={housing}
                crc={crc}
                layersFromShortcuts={layersFromShortcuts}
            />
        </AppProvider>
    );

    expect(wrapper.find('.SiteCardInfra__title').text()).toBe('Инфраструктура\u00A0ЖК Небо');
});
