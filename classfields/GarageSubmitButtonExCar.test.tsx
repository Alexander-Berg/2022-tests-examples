import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import configMock from 'auto-core/react/dataDomain/config/mock';
import addOrUpdateFromFormFields from 'auto-core/react/dataDomain/garageCard/actions/addOrUpdateFromFormFields';

import GarageSubmitButtonExCar from './GarageSubmitButtonExCar';

jest.mock('auto-core/react/dataDomain/garageCard/actions/addOrUpdateFromFormFields', () => jest.fn());

const Context = createContextProvider(contextMock);
const store = {
    config: configMock.value(),
    formFields: { data: { vin: { value: '' } } },
};

const mockedAction = addOrUpdateFromFormFields as jest.MockedFunction<typeof addOrUpdateFromFormFields>;
mockedAction.mockImplementation(() => jest.fn().mockResolvedValue(1));

describe('должен блокировать кнопку', () => {
    it('если не все обязательные поля заполнены', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ mockStore(store) }>
                    <GarageSubmitButtonExCar
                        onSubmit={ jest.fn() }
                    />
                </Provider>
            </Context>
            ,
        );

        expect(wrapper.dive().dive().dive().prop('disabled')).toBe(true);
    });

    it('если загружается', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ mockStore(store) }>
                    <GarageSubmitButtonExCar
                        onSubmit={ jest.fn() }
                    />
                </Provider>
            </Context>
            ,
        );

        wrapper.dive().dive().dive().simulate('submit');
        expect(wrapper.dive().dive().dive().prop('disabled')).toBe(true);
    });
});

it('после удачного сабмита машины в гараж должен вызывать колбек', async() => {
    const callback = jest.fn();

    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(store) }>
                <GarageSubmitButtonExCar
                    onSubmit={ callback }
                />
            </Provider>
        </Context>
        ,
    );

    expect(callback).toHaveBeenCalledTimes(0);
    wrapper.dive().dive().dive().simulate('submit');

    await flushPromises();
    expect(callback).toHaveBeenCalledTimes(1);
});

it('после неудачного сабмита машины в гараж должен показать ошибку', async() => {
    const errorText = 'hehehe';
    mockedAction.mockImplementationOnce(() => jest.fn().mockRejectedValueOnce({
        validation_results: [ {
            code: '123',
            description: errorText,
        } ],
    }));

    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(store) }>
                <GarageSubmitButtonExCar
                    onSubmit={ jest.fn() }
                />
            </Provider>
        </Context>
        ,
    );

    const button = wrapper.dive().dive().dive();
    button.simulate('submit');

    await flushPromises();

    expect(button.prop('errorMessage')).toBe(errorText);
});
