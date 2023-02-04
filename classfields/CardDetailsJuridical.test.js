/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import CardDetailsJuridical from './CardDetailsJuridical';

import requisites from 'www-cabinet/react/dataDomain/cardDetails/mocks/requisites.mock.js';

it('не должен вызывать editJuridicalForm, если поля формы не прошли валидацию', () => {
    const editJuridicalForm = jest.fn(() => Promise.resolve('SUCCESS'));
    const juridical = { ...requisites[0].properties.juridical, name: '' };

    const cardDetailsJuridical = shallow(
        <CardDetailsJuridical
            juridical={ juridical }
            editJuridicalForm={ editJuridicalForm }
        />,
    );
    cardDetailsJuridical.find('CardDetailsButtons').simulate('submitButtonClick');
    expect(editJuridicalForm).not.toHaveBeenCalled();
});

// eslint-disable-next-line jest/no-disabled-tests
it.skip('должен провалидировать значения полей и вызвать editJuridicalForm c корректными параметрами', () => {
    const editJuridicalForm = jest.fn(() => Promise.resolve('SUCCESS'));
    const juridical = { ...requisites[0].properties.juridical };

    const cardDetailsJuridical = shallow(
        <CardDetailsJuridical
            type="client"
            juridical={ juridical }
            isClient={ true }
            editJuridicalForm={ editJuridicalForm }
            isNewClientWithoutRegistration={ false }
        />,
    );
    cardDetailsJuridical.find('CardDetailsButtons').simulate('submitButtonClick');

    expect(editJuridicalForm).toHaveBeenCalledWith({
        name: 'ООО "МЭЙДЖОР ХОЛДИНГ" ( c 13.02.20 ООО "М Адвайс")',
        long_name: 'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "М АДВАЙС"',
        phone: '8(495)229-20-80',
        email: 'aristos@ma.ru;artur.kolesnikov@ma.ru;anna.krol@ma.ru',
        legal_address: '143420, Московская область, г. Красногорск, д. Михалково, дорога Балтия, владение 24, литера 1Б, этаж 3, пом1, комн 41',
        inn: '5024154432',
        kpp: '502401001',
        ogrn: '1155024003617',
        id: undefined,
        isNew: undefined,
        post_address: '143420',
        type: 'client',
        account: '40702810500000001167',
        bik: '044525097',
        postcode: '143420',
    }, false);
});

it('onInputChange: должен установить верное значение buhEmail', () => {
    const editJuridicalForm = jest.fn(() => Promise.resolve('SUCCESS'));
    const juridical = { ...requisites[0].properties.juridical };

    const cardDetailsJuridical = shallow(
        <CardDetailsJuridical
            editJuridicalForm={ editJuridicalForm }
            juridical={ juridical }
        />,
    );
    cardDetailsJuridical.find('CardDetailsRequisites').simulate('inputChange', 'petr@yandex.ru', { name: 'buhEmail' });
    expect(cardDetailsJuridical.instance().state.buhEmailField.value).toBe('petr@yandex.ru');
});
