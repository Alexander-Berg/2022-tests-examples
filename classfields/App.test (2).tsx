import React from 'react';
import { shallow } from 'enzyme';
import { useSelector } from 'react-redux';

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

function shallowRenderComponent(state: Record<string, any>) {
    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    return shallow(<App route={{}}>Привет!</App>);
}

it('рендерит c базовыми элементами', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-index').value(),
    });

    expect(wrapper).toMatchSnapshot();
});

it('передаёт пропс isRobot=true в AdblockAnalyzer, если запрос от робота', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-index').withIsRobot(true).value(),
    });

    expect(wrapper.find('AdblockAnalyzer').prop('isRobot')).toBe(true);
});

describe('рендерит без', () => {
    it('шапки Авто.ру на странице поста', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-article').value(),
        });

        expect(wrapper.find('Connect(Header)').exists()).toBe(false);
    });

    it('шапки и футера Авто.ру, если зашли с webview', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-index').withWebview(true).value(),
        });

        expect(wrapper.find('Connect(Header)').exists()).toBe(false);
        expect(wrapper.find('Footer').exists()).toBe(false);
    });

    it('marketingIframe, если запрос от робота', () => {
        const wrapper = shallowRenderComponent({
            config: configMock.withPageType('mag-index').withIsRobot(true).value(),
        });

        expect(wrapper.find('MarketingIframe').exists()).toBe(false);
    });
});
