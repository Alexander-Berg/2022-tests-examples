import React from 'react';
import 'jest-enzyme';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';

import PhoneLazyAuth from './PhoneLazyAuth';

it('должен поставить фокус в инпут телефона с пропом focusIfNoValidPhone и невалидным телефоном', () => {
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <PhoneLazyAuth
                onConfirm={ () => {} }
                onChangePhone={ () => {} }
                initialPhone=""
                focusIfNoValidPhone
            />
        </Provider>,
    );

    expect(component.dive().dive().find('TextInput').props()).toHaveProperty('focused', true);
});

it('не должен поставить фокус в инпут телефона без пропа focusIfNoValidPhone и невалидным телефоном', () => {
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <PhoneLazyAuth
                onConfirm={ () => {} }
                onChangePhone={ () => {} }
                initialPhone=""
            />
        </Provider>,
    );

    expect(component.dive().dive().find('TextInput').props()).toHaveProperty('focused', undefined);
});

it('не должен поставить фокус в инпут телефона с пропом focusIfNoValidPhone и валидным телефоном', () => {
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <PhoneLazyAuth
                onConfirm={ () => {} }
                onChangePhone={ () => {} }
                initialPhone="+7 999 999-99-99"
                focusIfNoValidPhone
            />
        </Provider>,
    );

    expect(component.dive().dive().find('TextInput').props()).not.toHaveProperty('focused');
});
