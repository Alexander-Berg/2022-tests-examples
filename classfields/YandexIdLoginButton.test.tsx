import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import YandexIdLoginButton from './YandexIdLoginButton';

it('должен правильно сформировать ссылку для паспорта с uuid и retpath', () => {
    const tree = shallow(
        <YandexIdLoginButton
            retpath="/moskva/cars/all/"
            uuid="768dcbdb-deda-4e2b-bfa9-15db26af9290"
        />,
        { context: { ...contextMock } },
    );

    expect(tree.find('Button').prop('url')).toBe(
        'https://passport.yandex.ru/auth?origin=autoru&retpath=https%3A%2F%2Fsso.passport.yandex.ru%2Fpush%3Fretpath%3D' +
        'https%253A%252F%252Fautoru_frontend.auth_domain%252Fyandex-sync%252F%253Fr%253D%25252Fmoskva%25252Fcars%25252Fall%25252F%26' +
        'uuid%3D768dcbdb-deda-4e2b-bfa9-15db26af9290',
    );
});
