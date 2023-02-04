import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import _ from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import userMockWithAuth from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';

import CreditUserForm from './CreditUserForm2';

it('должен заполнить инпуты информацией про юзера', () => {
    const user = changeNameAndEmail(userMockWithAuth, { name: 'Иван Иванов', email: 'valid@email.ru' });
    const store = mockStore({ user });
    const tree = shallow(
        <Provider store={ store }>
            <CreditUserForm
                onSubmit={ _.noop }
                fullName={ user.data.profile?.autoru?.full_name }
                email={ String(user.data.emails?.[0].email) }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    expect(tree.find('.CreditUserForm2__nameField').prop('value')).toEqual('Иван Иванов');
    expect(tree.find('.CreditUserForm2__emailField').prop('value')).toEqual('valid@email.ru');
});

it('не должен автозаполнить инпуты информацией из профиля юзера, если там невалидные данные', () => {
    const user = changeNameAndEmail(userMockWithAuth, { name: 'Phil Spencer', email: 'notvalidemail' });
    const store = mockStore({ user });
    const tree = shallow(
        <Provider store={ store }>
            <CreditUserForm
                onSubmit={ _.noop }
                fullName={ user.data.profile?.autoru?.full_name }
                email={ String(user.data.emails?.[0].email) }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    expect(tree.find('.CreditUserForm2__nameField').prop('value')).toEqual('');
    expect(tree.find('.CreditUserForm2__emailField').prop('value')).toEqual('');
});

it('должен вызвать коллбек при сабмите с валидными данными', () => {
    const name = 'Иван Иванович';
    const email = 'email@test.ru';
    const phone = '79998887766';
    const user = changeNameAndEmail(userMockWithAuth, { name, email });
    const store = mockStore({ user });
    const callback = jest.fn();

    const tree = shallow(
        <Provider store={ store }>
            <CreditUserForm
                onSubmit={ callback }
                fullName={ name }
                email={ email }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    tree.find('.CreditUserForm2__nameField').simulate('change', name);
    tree.find('.CreditUserForm2__emailField').simulate('change', email);
    tree.find('.CreditUserForm2__phoneAuth').dive().simulate('authSuccess', phone);

    expect(callback).toHaveBeenCalledWith({ name, email, phone });
});

it('должен показать ошибки валидации при сабмите с невалидными данными и поменять состояние', () => {
    const store = mockStore({ user: userMockWithAuth });
    const callback = jest.fn();
    const name = 'Name';
    const email = 'email';
    const phone = '79998887766';

    const tree = shallow(
        <Provider store={ store }>
            <CreditUserForm
                onSubmit={ callback }
                fullName={ name }
                email={ email }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    tree.find('.CreditUserForm2__nameField').simulate('change', name);
    tree.find('.CreditUserForm2__emailField').simulate('change', email);
    tree.find('.CreditUserForm2__phoneAuth').dive().simulate('authSuccess', phone);

    expect(callback).not.toHaveBeenCalled();
    expect(tree.find('.CreditUserForm2__phoneAuth')).not.toExist(); // скрылась ленивая авторизация
    expect(tree.find('.CreditUserForm2__actionButton')).toExist(); // появилась простая кнопка сабмита формы
    expect(tree.find('.CreditUserForm2__nameField').prop('error')).not.toBeUndefined();
    expect(tree.find('.CreditUserForm2__emailField').prop('error')).not.toBeUndefined();
});

function changeNameAndEmail(user: StateUser, { email, name }: { name: string; email: string}): StateUser {
    const newUser = _.cloneDeep(user);
    if (newUser.data.profile?.autoru?.full_name && newUser.data?.emails?.[0]) {
        newUser.data.profile.autoru.full_name = name;
        newUser.data.emails[0] = { email, confirmed: true };
    }

    return newUser;
}
