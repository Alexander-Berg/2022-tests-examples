/* eslint-disable max-len */
import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OfferCardHeader from '../';

import { i18n, delimiter, className, item, state } from './mocks';

test('render correct seo h1', () => {
    const wrapper = mount(
        <AppProvider initialState={state}>
            <OfferCardHeader
                item={item}
                className={className}
                delimiter={delimiter}
                i18n={i18n}
            />
        </AppProvider>
    );

    expect(wrapper.find('.OfferCardHeader__title').text()).toBe('35\u00A0м², 1-комн. квартира, 3/9\u00A0этаж\u00A013\u00A0000\u00A0000 ₽\u00A0371\u00A0429 ₽ за\u00A0м²');
});
