import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ItemAddressWithLinks } from '../index';

import { SITE_ADDRESS_ONE, SITE_ADDRESS_THREE, SITE_ADDRESS_TWO, SITE_ADDRESS_FOUR } from './mocks';

describe('ItemAddressWithLinks', () => {
    [SITE_ADDRESS_ONE, SITE_ADDRESS_TWO, SITE_ADDRESS_THREE, SITE_ADDRESS_FOUR].forEach(({ item }, idx) => {
        it(`Адрес новостройки ${idx}`, async () => {
            const wrapper = mount(
                <AppProvider context={{ link: () => 'testLink' }}>
                    <ItemAddressWithLinks linkPageName="newbuilding-search" item={item} />
                </AppProvider>
            );

            expect(wrapper).toMatchSnapshot();
        });
    });
});
