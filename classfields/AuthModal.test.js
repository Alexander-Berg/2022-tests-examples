/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/state/actions/authModalChangeScreen');
jest.mock('auto-core/react/dataDomain/user/actions/renew');
jest.mock('auto-core/react/dataDomain/state/actions/authModalClose');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

const _ = require('lodash');
const React = require('react');
const AuthModal = require('./AuthModal');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userStateMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const { authModal: authModalStateMock } = require('autoru-frontend/mockData/state/state.mock');

const authModalChangeScreen = require('auto-core/react/dataDomain/state/actions/authModalChangeScreen');
const authRenew = require('auto-core/react/dataDomain/user/actions/renew').default;
const { showAutoclosableMessage } = require('auto-core/react/dataDomain/notifier/actions/notifier');
const authModalClose = require('auto-core/react/dataDomain/state/actions/authModalClose').default;

let initialState;
let props;
let store;
const eventMap = {};
const AUTH_SUCCESS_PAYLOAD = { foo: 'bar' };

showAutoclosableMessage.mockImplementation(() => () => {});
authRenew.mockImplementation(() => () => {});
authModalChangeScreen.mockImplementation(() => () => {});
authModalClose.mockImplementation(() => () => {});

beforeEach(() => {
    initialState = {
        user: _.cloneDeep(userStateMock),
        config: configStateMock.value(),
        state: {
            authModal: _.cloneDeep(authModalStateMock),
        },
    };

    initialState.state.authModal.isOpened = true;

    props = {};

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('при получении сообщения об успешной авторизации пользователя', () => {
    it('если передан проп shouldCloseAfterAuth закроет модал', () => {
        props.shouldCloseAfterAuth = true;
        shallowRenderAuthModal({ props, initialState });

        eventMap.message({ data: { source: 'auth_form', type: 'auth_success', payload: AUTH_SUCCESS_PAYLOAD } });

        expect(authModalClose).toHaveBeenCalledTimes(1);
    });

    it('если не передан проп shouldCloseAfterAuth покажет лоадер', () => {
        props.shouldCloseAfterAuth = false;
        shallowRenderAuthModal({ props, initialState });

        eventMap.message({ data: { source: 'auth_form', type: 'auth_success', payload: AUTH_SUCCESS_PAYLOAD } });

        expect(authModalChangeScreen).toHaveBeenCalledTimes(1);
        expect(authModalChangeScreen).toHaveBeenCalledWith('loader');
    });

    it('подновит данные о пользователе', () => {
        shallowRenderAuthModal({ props, initialState });

        eventMap.message({ data: { source: 'auth_form', type: 'auth_success', payload: AUTH_SUCCESS_PAYLOAD } });

        expect(authRenew).toHaveBeenCalledTimes(1);
        expect(authRenew).toHaveBeenCalledWith({ ...AUTH_SUCCESS_PAYLOAD, auth: true });
    });

    it('если был автологин яндексом, покажет нотификацию', () => {
        shallowRenderAuthModal({ props, initialState });

        eventMap.message({ data: {
            source: 'auth_form',
            type: 'auth_success',
            payload: AUTH_SUCCESS_PAYLOAD,
            meta: { source: 'yandex', autologin: true },
        } });

        expect(showAutoclosableMessage).toHaveBeenCalledTimes(1);
        expect(showAutoclosableMessage).toHaveBeenCalledWith({ message: 'Вы вошли на Авто.ру' });
    });
});

function shallowRenderAuthModal({ initialState, props }) {
    store = mockStore(initialState);

    const wrapper = shallow(
        <AuthModal store={ store } { ...props }/>,
    );

    return wrapper.dive().dive();
}
