/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import CardDetailsPhysical from './CardDetailsPhysical';
import requisites from 'www-cabinet/react/dataDomain/cardDetails/mocks/requisites.mock.js';

it('не должен вызывать editPhysicalForm, если поля формы не прошли валидацию', () => {
    const editPhysicalForm = jest.fn(() => Promise.resolve('SUCCESS'));
    const individualMock = { ...requisites[1].properties.individual, first_name: '' };

    const cardDetailsPhysical = shallow(
        <CardDetailsPhysical
            individual={ individualMock }
            editPhysicalForm={ editPhysicalForm }
            isNewClientWithoutRegistration={ true }
        />,
    );
    cardDetailsPhysical.find('CardDetailsButtons').simulate('submitButtonClick');
    expect(editPhysicalForm).not.toHaveBeenCalled();
});

it('должен провалидировать значения полей и вызвать editPhysicalForm c корректными параметрами', () => {
    const editPhysicalForm = jest.fn(() => Promise.resolve('SUCCESS'));
    const individual = { ...requisites[1].properties.individual };

    const cardDetailsPhysical = shallow(
        <CardDetailsPhysical
            type="manager"
            individual={ individual }
            isPhoneValidationFailed={ false }
            firstNameError=""
            familyNameError=""
            patronymicError=""
            buhEmailError=""
            isLoading={ false }
            editPhysicalForm={ editPhysicalForm }
            isNewClientWithoutRegistration={ true }
        />,
    );
    cardDetailsPhysical.find('CardDetailsButtons').simulate('submitButtonClick');

    expect(editPhysicalForm).toHaveBeenCalledWith({
        firstName: 'Test2',
        lastName: 'test2',
        midName: '123',
        phone: '81111111118888',
        email: 'test2@ya.ru',
        id: undefined,
        clientId: undefined,
        isNew: undefined,
    });
});
