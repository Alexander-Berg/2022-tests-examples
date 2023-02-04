const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const ContextProvider = createContextProvider(contextMock);

const HeaderSearchClient = require('./HeaderSearchClient');

it('должен формировать правильную ссылку в кнопке при вводе id клиента', () => {
    const wrapper = shallow(
        <ContextProvider>
            <HeaderSearchClient/>
        </ContextProvider>,
    ).dive();

    wrapper.find('TextInput').simulate('change', '123');

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
