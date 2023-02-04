import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { ClassifiedClearedSecret } from 'www-cabinet/react/dataDomain/settingsMultiposting/types';

import SettingsMultipostingClassified from './SettingsMultipostingClassified';

type AvitoOrDrom = 'avito' | 'drom';

const avitoClearedSecretMock: ClassifiedClearedSecret = {
    classified: 'avito',
    cleared_avito_secret: {
        has_avito_user_id: true,
        has_avito_client_id: true,
        has_avito_client_secret: true,
    },
    has_password: true,
    login: 'БЕЗНОГИМ',
};

const submitMock = jest.fn(() => Promise.resolve('SUCCESS'));

const defaultProps = {
    classified: 'avito' as AvitoOrDrom,
    clearedSecret: avitoClearedSecretMock,
    onSubmit: submitMock,
    onClearData: () => Promise.resolve('SUCCESS'),
};

it('покажет лоадер при загрузке', async() => {
    const tree = shallowRenderComponent();

    tree.setState({ isLoading: true });
    expect(tree.find('Loader')).toExist();
});

it('не покажет часть инпутов, если Дром', async() => {
    const props = { ...defaultProps, classified: 'drom' as AvitoOrDrom };
    const tree = shallowRenderComponent(props);

    expect(tree.find('.SettingsMultipostingClassified__input')).not.toExist();
});

it('покажет инпуты для Авито', async() => {
    const tree = shallowRenderComponent();
    expect(tree.find('.SettingsMultipostingClassified__input')).toExist();
});

it('не даст вручную стереть "спрятанные" данные', async() => {
    const tree = shallowRenderComponent();
    const inputName = 'profileNumber';
    const initialInputState = tree.state(inputName);
    const input = tree.findWhere(node => node.prop('name') === inputName);

    input.simulate('change', 'azaza', { name: inputName });

    expect(tree.state(inputName)).toEqual(initialInputState);
});

it('покажет ошибку, если заполнены не все связанные инпуты', async() => {
    const tree = shallowRenderComponent();
    const inputName = 'profileNumber';
    const input = tree.findWhere(node => node.prop('name') === inputName);

    input.simulate('clearClick', null, { name: inputName });
    input.simulate('focusChange', false, { name: inputName });

    expect(tree.find('.SettingsMultipostingClassified__note_error')).toExist();
});

it('покажет кнопку "отмена", если есть что отменять', async() => {
    const tree = shallowRenderComponent();
    tree.setState({ hasAnythingChanged: true });

    const cancelButton = tree.findWhere(node => node.key() === 'cancelButton');

    expect(cancelButton).toExist();
});

it('покажет кнопку "стереть данные", если есть что стирать', async() => {
    const tree = shallowRenderComponent();
    const cancelButton = tree.findWhere(node => node.key() === 'clearDataButton');

    expect(cancelButton).toExist();
});

it('правильно передаст параметры в колбэк при сохранении', async() => {
    const tree = shallowRenderComponent();
    const inputName = 'profileNumber';
    const input = tree.findWhere(node => node.prop('name') === inputName);
    const submitButton = tree.findWhere(node => node.key() === 'saveButton');

    input.simulate('clearClick', null, { name: inputName });
    input.simulate('change', '555', { name: inputName });
    submitButton.simulate('click');

    expect(submitMock).toHaveBeenCalledWith({
        avito_client_id: undefined,
        avito_client_secret: undefined,
        avito_user_id: '555',
        login: 'БЕЗНОГИМ',
        password: undefined,
    });
});

it('у инпута для пароля будет тип "password"', async() => {
    const tree = shallowRenderComponent();
    const passwordInput = tree.findWhere(node => node.prop('name') === 'password');

    expect(passwordInput.prop('type')).toEqual('password');
});

function shallowRenderComponent(props = defaultProps) {
    const page = shallow(
        <SettingsMultipostingClassified { ...props }/>,
    );
    return page;
}
