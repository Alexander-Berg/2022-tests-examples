/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { ReduxState } from './Notifier';
import Notifier from './Notifier';

declare var global: { location: Record<string, any> };
const { location } = global;

let store: ThunkMockStore<ReduxState>;
beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = new URL('https://auto.ru');
    history.replaceState = jest.fn();
});

afterEach(() => {
    global.location = location;
});

it('должен отрисоваться в невидимом состоянии', () => {
    store = mockStore({
        notifier: {
            message: 'Привет!',
            visible: false,
        },
    });

    const tree = shallow(
        <Notifier/>,
        { context: { ...contextMock, store } },
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
    expect(store.getActions()).toHaveLength(0);
});

it('в мобилке при клике на крестик скроет нотифайку', () => {
    store = mockStore({
        notifier: {
            message: 'Привет!',
            visible: true,
        },
    });

    const tree = shallow(
        <Notifier isMobile={ true }/>,
        { context: { ...contextMock, store } },
    ).dive();

    const closer = tree.find('.Notifier__closer');
    closer.simulate('click');

    expect(store.getActions()[0]).toEqual({
        type: 'NOTIFIER_HIDE_MESSAGE',
    });
});

it('отправит метрику по клику на крестие, если она передана', () => {
    store = mockStore({
        notifier: {
            message: 'Привет!',
            visible: true,
            closeMetrika: 'test,metrika,params',
        },
    });

    const Context = createContextProvider(contextMock);

    const { container } = render(
        <Provider store={ store }>
            <Context>
                <Notifier isMobile={ true }/>
            </Context>
        </Provider>,
    );

    const closer = container.getElementsByClassName('Notifier__closer')[0];
    userEvent.click(closer);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'test', 'metrika', 'params' ]);
});
