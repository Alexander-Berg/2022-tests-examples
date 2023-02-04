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

    return shallow(<App>Привет!</App>);
}

it('рендерит c базовыми элементами', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-index').value(),
    });

    expect(wrapper).toMatchSnapshot();
});

it('первый пункт навигации "Главная", если тип роута не "mag-index"', () => {
    const wrapper = shallowRenderComponent({
        config: configMock.withPageType('mag-theme-amp').value(),
    });
    const navigationItems = wrapper.find('Navigation').prop('items');

    expect(navigationItems[0]).toEqual({ id: 'index', name: 'Главная', pageType: 'mag-index' });
});
