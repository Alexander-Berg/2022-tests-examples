/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import LoginFrame from './LoginFrame';

let initialState = {
    config: configStateMock.value(),
};

let props: Record<string, any> = {};

const eventMap: Record<string, any> = {};
const AUTH_SUCCESS_PAYLOAD = { foo: 'bar' };

beforeEach(() => {
    initialState = {
        config: configStateMock.value(),
    };

    props = {};

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });
});

it('вызывает onAuthSuccess при получении сигнала об авторизации', () => {
    props.onAuthSuccess = jest.fn();
    const wrapper = shallowRenderLoginFrame({ props, initialState });

    eventMap.message({ data: { source: 'auth_form', type: 'auth_success', payload: AUTH_SUCCESS_PAYLOAD, requestId: wrapper.state('requestId') } });

    expect(props.onAuthSuccess).toHaveBeenCalledTimes(1);
});

it('обновляет высоту по сообщению', () => {
    props.authFrameHeight = 500;

    const wrapper = shallowRenderLoginFrame({ props, initialState });

    expect(wrapper.prop('height')).toBe(props.authFrameHeight + 'px');

    eventMap.message({ data: { source: 'auth_form', type: 'resize', payload: 200, requestId: wrapper.state('requestId') } });

    expect(wrapper.prop('height')).toBe('200px');
});

it('всегда добавит к урлу айфрема флаг autoLogin=true', () => {
    const page = shallowRenderLoginFrame({ props, initialState });
    const frame = page.find('.LoginFrame');
    expect(frame.prop('src')).toMatch('autoLogin=true');
});

type TShallowRenderLoginFrame = {
    props: typeof props;
    initialState: typeof initialState;
}

function shallowRenderLoginFrame({ initialState, props }: TShallowRenderLoginFrame) {
    const store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <LoginFrame { ...props }/>
        </Provider>,
    );

    return wrapper.dive().dive();
}
