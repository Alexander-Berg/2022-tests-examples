const React = require('react');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const VinCheckInput = require('./VinCheckInput');

jest.mock('auto-core/react/dataDomain/vinCheckInput/actions/setVinCheckInputValue', () => {
    const mock = jest.fn(() => ({ type: '__MOCK_set_input_value' }));
    mock['default'] = mock;
    return mock;
});
const setVinCheckInputValue = require('auto-core/react/dataDomain/vinCheckInput/actions/setVinCheckInputValue').default;

const domEventMock = {
    preventDefault: () => {},
};

it('должен вызвать onError при поиске по некорректному vin', () => {
    const store = mockStore({ vinCheckInput: { value: 'ПРЕВЕД МЕДВЕД' } });
    const onSubmit = jest.fn();
    const onInputError = jest.fn();
    const page = shallow(
        <VinCheckInput
            onSubmit={ onSubmit }
            onInputChange={ () => {} }
            onInputError={ onInputError }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('Button').simulate('click', domEventMock);

    expect(onInputError).toHaveBeenCalled();
    expect(onSubmit).not.toHaveBeenCalled();
});

it('должен вызвать onSubmit при поиске по корректному vin', () => {
    const store = mockStore({ vinCheckInput: { value: 'у111уу199' } });

    const onSubmit = jest.fn();
    const onInputError = jest.fn();
    const page = shallow(
        <VinCheckInput
            pageParams={{}}
            onSubmit={ onSubmit }
            onInputChange={ () => {} }
            onInputError={ onInputError }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('Button').simulate('click', domEventMock);

    expect(onInputError).not.toHaveBeenCalled();
    expect(onSubmit).toHaveBeenCalled();
});

it('должен правильно форматировать госномер', () => {
    const store = mockStore({ vinCheckInput: { value: 'у 111 уу 11' } });

    const onSubmit = jest.fn();
    const page = shallow(
        <VinCheckInput
            pageParams={{}}
            onSubmit={ onSubmit }
            onInputChange={ () => {} }
            onInputError={ () => {} }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('Button').simulate('click', domEventMock);

    expect(onSubmit).toHaveBeenCalledWith('Y111YY11');
});

it('должен правильно форматировать вин', () => {
    // тут русские буквы
    const store = mockStore({ vinCheckInput: { value: 'ХТА 210510Р1394951' } });

    const onSubmit = jest.fn();
    const page = shallow(
        <VinCheckInput
            pageParams={{}}
            onSubmit={ onSubmit }
            onInputChange={ () => {} }
            onInputError={ () => {} }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('Button').simulate('click', domEventMock);
    // должен подставить в урл без пробелов и русских букв
    expect(onSubmit).toHaveBeenCalledWith('XTA210510P1394951');
});

it('должен вызвать экшн при изменении инпута', () => {
    const store = mockStore({ vinCheckInput: { value: '' } });
    const onInputChange = jest.fn();
    const page = shallow(
        <VinCheckInput
            onSubmit={ () => {} }
            onInputChange={ onInputChange }
            onInputError={ () => {} }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('TextInput').simulate('change', '1');
    expect(setVinCheckInputValue).toHaveBeenCalledWith('1');
    expect(onInputChange).toHaveBeenCalled();

});

it('должен вызвать экшн при очистке инпута', () => {
    const store = mockStore({ vinCheckInput: { value: 'ПРЕВЕД МЕДВЕД' } });
    const onInputChange = jest.fn();
    const page = shallow(
        <VinCheckInput
            onSubmit={ () => {} }
            onInputChange={ onInputChange }
            onInputError={ () => {} }
            isMobile={ false }
            size={ VinCheckInput.SIZE.H64 }
            onHelpClick={ () => {} }
            withHelp
        />, { context: { store } },
    ).dive();

    page.find('TextInput').simulate('clearClick');

    expect(setVinCheckInputValue).toHaveBeenCalledWith('');
    expect(onInputChange).toHaveBeenCalled();
});
