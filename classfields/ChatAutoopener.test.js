/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const ChatAutoopener = require('./ChatAutoopener');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

beforeEach(() => {
    global.window = Object.create(window);
    window.vertis_chat = {
        show: jest.fn(),
        onready: (f) => {
            return f();
        },
        on: () => {},
    };
    Object.defineProperty(window, 'location', {
        value: {
            pathname: '',
        },
        writable: true,
    });
});

it('должен открывать чат если в pathname содержится /chat/', () => {
    const state = {
        user: {
            data: { auth: true },
        },
        config: {
            data: {
                pageParams: {},
            },
        },
    };

    window.location.pathname = '/chat/';

    shallow(
        <ChatAutoopener/>,
        { context: { ...contextMock, store: mockStore(state) } },
    ).dive();

    expect(window.vertis_chat.show).toHaveBeenCalled();
});

it('должен открывать чат если в query есть параметр chat', () => {
    const stateWithChat = {
        user: {
            data: { auth: true },
        },
        config: {
            data: {
                pageParams: {
                    chat: '',
                },
            },
        },
    };

    shallow(
        <ChatAutoopener/>,
        {
            context: {
                ...contextMock,
                store: mockStore(stateWithChat),
            },
        },
    ).dive();
    expect(window.vertis_chat.show).toHaveBeenCalled();
});
