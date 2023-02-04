import React from 'react';
import { mount } from 'enzyme';

import i18n from 'realty-core/view/react/libs/i18n';

import { withContext } from 'view/lib/test-helpers';

import FormTextInput from '../';

describe.skip('Price', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    it('renders price component without period', () => {
        const state = {
            offerForm: {
                type: 'HOUSE',
                period: 'PER_MONTH',
                price: 123
            }
        };

        const component = (
            <FormTextInput
                name='price'
                controlType='price'
                controlProps={{}}
            />
        );

        const wrapper = withContext(mount, component, state);

        expect(wrapper.find('.offer-form__price-label-wrapper').text()).toEqual('123 ₽');
    });

    it('renders price component with month period', () => {
        const state = {
            offerForm: {
                type: 'RENT',
                period: 'PER_MONTH',
                price: 123
            }
        };

        const component = (
            <FormTextInput
                name='price'
                controlType='price'
                controlProps={{}}
            />
        );

        const wrapper = withContext(mount, component, state);

        expect(wrapper.find('.offer-form__price-label-wrapper').text()).toEqual('123 ₽ / месяц');
    });

    it('renders price component with day period', () => {
        const state = {
            offerForm: {
                type: 'RENT',
                period: 'PER_DAY',
                price: 123
            }
        };

        const component = (
            <FormTextInput
                name='price'
                controlType='price'
                controlProps={{}}
            />
        );

        const wrapper = withContext(mount, component, state);

        expect(wrapper.find('.offer-form__price-label-wrapper').text()).toEqual('123 ₽ / сутки');
    });
});
