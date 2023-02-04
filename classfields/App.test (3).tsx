import React from 'react';
import { shallow } from 'enzyme';
import { useSelector } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import configMock from 'auto-core/react/dataDomain/config/mock';

import App from './App';

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

const Context = createContextProvider(contextMock);

function shallowRenderComponent(state: Record<string, any>) {
    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    return shallow(
        <Context>
            <App route={{}}>Привет!</App>
        </Context>);
}

it('рендерит c базовыми элементами', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-index').value(),
    });
    expect(wrapper.dive()).toMatchSnapshot();
});

it('передаёт пропс isRobot=true в AdblockAnalyzer, если запрос от робота', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-index').withIsRobot(true).value(),
    });

    expect(wrapper.dive().find('AdblockAnalyzer').prop('isRobot')).toBe(true);
});

it('первый пункт навигации "Главная", если тип роута не "mag-index"', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-theme').value(),
    });

    const navigationItems = wrapper.dive().find('Navigation').prop('items');
    expect(navigationItems[0]).toEqual({ id: 'index', name: 'Главная', pageType: 'mag-index' });
});

describe('рендерит без', () => {
    it('шапки Авто.ру на странице поста', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-article').value(),
        });

        expect(wrapper.dive().find('Connect(Header)').exists()).toBe(false);
    });

    it('шапки и футера Авто.ру, если зашли с webview', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-index').withWebview(true).value(),
        });

        expect(wrapper.dive().find('Connect(Header)').exists()).toBe(false);
        expect(wrapper.dive().find('Footer').exists()).toBe(false);
    });

    it('marketingIframe, если запрос от робота', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-index').withIsRobot(true).value(),
        });

        expect(wrapper.dive().find('MarketingIframe').exists()).toBe(false);
    });
});
