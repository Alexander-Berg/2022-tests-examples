import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SiteCardMortgages from '..';

import { card, location, adfoxParams, state } from './mocks';

test.skip('рендерится заголовок h2 с неразрывным пробелом', () => {
    const wrapper = mount(
        <AppProvider initialState={state}>
            <SiteCardMortgages
                card={card}
                location={location}
                adfoxParams={adfoxParams}
            />
        </AppProvider>
    );

    expect(wrapper.find('.alfaBankMortgageTitle').text())
        .toEqual('Ипотека на спецусловиях в ЖК Небо\u00A0для пользователей Яндекс.Недвижимости');
});
