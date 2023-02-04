/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { SocialProvider } from '@vertis/schema-registry/ts-types-snake/vertis/common';

import React from 'react';
import { PropTypes } from 'prop-types';
import { shallow } from 'enzyme';

jest.mock('auto-core/react/lib/openSocialPopup');

import ConnectSocialAccountHoc from './ConnectSocialAccountHoc';
import openSocialPopup from 'auto-core/react/lib/openSocialPopup';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

const eventMap = {};
const postMessageMock = jest.fn();
let originalWindowAddEventListener;
let originalWindowPostMessage;

const successCallbackMock1 = jest.fn();
const successCallbackMock2 = jest.fn();

class ComponentMock extends React.PureComponent {
    componentDidMount() {
        if (this.props.registerSocialAccountSuccessCallback) {
            this.props.registerSocialAccountSuccessCallback(successCallbackMock1);
            this.props.registerSocialAccountSuccessCallback(successCallbackMock2);
        }
    }

    render() {
        return <div onClick={ this.handleClick }>i am mock</div>;
    }

    handleClick = () => {
        this.props.connectSocialAccount && this.props.connectSocialAccount(SocialProvider.MOSRU);
    }
}

ComponentMock.propTypes = {
    registerSocialAccountSuccessCallback: PropTypes.func,
    connectSocialAccount: PropTypes.func,
};

beforeEach(() => {
    originalWindowPostMessage = global.postMessage;

    global.addEventListener = jest.fn((event, cb) => {
        eventMap[event] = cb;
    });
    global.postMessage = postMessageMock;
});

afterEach(() => {
    global.addEventListener = originalWindowAddEventListener;
    global.postMessage = originalWindowPostMessage;
});

describe('при открытии', () => {
    beforeEach(() => {
        const page = shallowRenderComponent();
        page.find('div').simulate('click');
    });

    it('вызовет всплывашку с правильными параметрами', () => {
        expect(openSocialPopup).toHaveBeenCalledTimes(1);
        expect(openSocialPopup).toHaveBeenCalledWith(SocialProvider.MOSRU, 'https://autoru_frontend.auth_domain/social/mosru/?r=http%3A%2F%2Flocalhost%2F');
    });

    it('отправит метрику', () => {
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'add-social-mosru' ]);
    });
});

it('при получении пинга от всплывашки отправит ей понг', () => {
    shallowRenderComponent();
    eventMap.message({ data: { type: 'auth-ping', rnd: 0.42 }, source: { postMessage: postMessageMock }, origin: 'foo' });

    expect(postMessageMock).toHaveBeenCalledTimes(1);
    expect(postMessageMock).toHaveBeenCalledWith({ type: 'auth-pong', rnd: 0.42 }, 'foo');
});

describe('при успехе', () => {
    const userResponseMock = { user: 'john doe' };

    beforeEach(() => {
        const page = shallowRenderComponent();
        page.find('div').simulate('click');
        eventMap.message({ data: { type: 'auth-result', result: { success: true, user: userResponseMock } } });
    });

    it('при успехе вызовет все зареганные коллбэки и прокинет в них результат', () => {
        expect(successCallbackMock1).toHaveBeenCalledTimes(1);
        expect(successCallbackMock1).toHaveBeenCalledWith(userResponseMock, SocialProvider.MOSRU);

        expect(successCallbackMock2).toHaveBeenCalledTimes(1);
        expect(successCallbackMock2).toHaveBeenCalledWith(userResponseMock, SocialProvider.MOSRU);
    });

    it('отправит метрику', () => {
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'add-social-mosru', 'success' ]);
    });
});

function shallowRenderComponent() {
    const ContextProvider = createContextProvider(contextMock);
    const EnhancedComponentMock = ConnectSocialAccountHoc(ComponentMock);

    const page = shallow(
        <ContextProvider>
            <EnhancedComponentMock/>
        </ContextProvider>,
    );

    return page.dive().dive();
}
