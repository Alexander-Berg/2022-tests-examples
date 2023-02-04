/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

jest.mock('www-poffer/react_bem/actions/form', () => ({
    updateFormByFirstStep: jest.fn(() => ({ type: 'mock_updateFormByFirstStep' })),
    log: jest.fn(() => () => { }),
}));
import { updateFormByFirstStep } from 'www-poffer/react_bem/actions/form';
const updateFormByFirstStepMock = updateFormByFirstStep as jest.MockedFunction<typeof updateFormByFirstStep>;

import FirstStepForm from './FirstStepForm';

it('должен отрисовать только кнопку Пропустить, если не введен VIN', () => {
    const store = mockStore({
        formFields: { data: { vin: { value: '' } } },
        user: { data: {} },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <FirstStepForm/>
        </Provider>,
    ).dive().dive();

    const button = wrapper.find('Button');

    expect(button).toHaveLength(1);
    expect(button.children().text()).toEqual('Пропустить');
});

it('должен отрисовать кнопки Пропустить и Далее, если введен VIN', () => {
    const store = mockStore({
        formFields: { data: { vin: { value: '1234567890' } } },
        user: { data: {} },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <FirstStepForm/>
        </Provider>,
    ).dive().dive();

    const buttons = wrapper.find('Button');

    expect(buttons).toHaveLength(2);
    expect(buttons.at(0).children().text()).toEqual('Пропустить');
    expect(buttons.at(1).children().text()).toEqual('Далее');
});

it('должен отрисовать только кнопку Заполнить вручную, если не получается заполнить по этому VIN', () => {
    const store = mockStore({
        formFields: { data: { vin: { value: '' } } },
        user: { data: {} },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <FirstStepForm/>
        </Provider>,
    ).dive().dive();

    wrapper.setState({ showNoDraft: true });

    const button = wrapper.find('Button');

    wrapper.update();

    expect(button).toHaveLength(1);
    expect(button.children().text()).toEqual('Заполнить вручную');
});

it('должен вызвать экшн обновления черновика при нажатии "далее"', () => {
    const store = mockStore({
        formFields: { data: { vin: { value: 'Z9PFF7550H0554799' } } },
        user: { data: {} },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <FirstStepForm/>
        </Provider>,
    ).dive().dive();

    const buttons = wrapper.find('Button');
    buttons.at(1).simulate('click');

    expect(updateFormByFirstStepMock).toHaveBeenCalledWith({ vin: 'Z9PFF7550H0554799' });
});
