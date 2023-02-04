/**
 * @jest-environment jsdom
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import configMock from 'auto-core/react/dataDomain/config/mock';
import addDreamcar from 'auto-core/react/dataDomain/garageCard/actions/addDreamcar';
import updateDreamcar from 'auto-core/react/dataDomain/garageCard/actions/updateDreamcar';

import GarageSubmitButtonDreamcar from './GarageSubmitButtonDreamcar';
import type { Props } from './GarageSubmitButtonDreamcar';

jest.mock('auto-core/react/dataDomain/garageCard/actions/addDreamcar', () => jest.fn());
jest.mock('auto-core/react/dataDomain/garageCard/actions/updateDreamcar', () => jest.fn());

interface Fields {
    [key: string]: {
        value: string;
    };
}

const store = {
    config: configMock.value(),
    formFields: { data: {} as Fields },
};

const mockedAddDreamcar = addDreamcar as jest.MockedFunction<typeof addDreamcar>;
mockedAddDreamcar.mockImplementation(() => jest.fn().mockResolvedValue({ card: 1 }));

const mockedUpdateDreamcar = updateDreamcar as jest.MockedFunction<typeof updateDreamcar>;
mockedUpdateDreamcar.mockImplementation(() => jest.fn().mockResolvedValue({ card: 1 }));

const shallowRender = (props?: Partial<Props>) => {
    const context = { ...contextMock, store: mockStore(store) };
    return shallow(
        <GarageSubmitButtonDreamcar
            onSubmit={ jest.fn() }
            { ...props }
        />,
        { context },
    );
};

describe('должен блокировать кнопку сабмита любимой машины', () => {
    it('если не все обязательные поля заполнены', () => {
        const wrapper = shallowRender();

        expect(wrapper.dive().prop('disabled')).toBe(true);
    });

    it('если загружается', () => {
        const wrapper = shallowRender();
        wrapper.dive().simulate('submit');

        expect(wrapper.dive().prop('disabled')).toBe(true);
    });
});

it('после удачного сабмита любимой машины должен вызывать колбек', async() => {
    const callback = jest.fn();
    const wrapper = shallowRender({ onSubmit: callback });

    expect(callback).toHaveBeenCalledTimes(0);
    wrapper.dive().simulate('submit');

    await flushPromises();

    expect(callback).toHaveBeenCalledTimes(1);
});

it('после неудачного сабмита любимой машины должен показать ошибку', async() => {
    const errorText = 'hehehe';
    mockedAddDreamcar.mockImplementationOnce(() => jest.fn().mockRejectedValueOnce({
        validation_results: [ {
            code: '123',
            description: errorText,
        } ],
    }));
    const wrapper = shallowRender();
    const button = wrapper.dive();
    button.simulate('submit');

    await flushPromises();

    expect(button.prop('errorMessage')).toBe(errorText);
});

it('должен дернуть updateDreamcar вместо addDreamcar, если карточка уже создана', async() => {
    const wrapper = shallowRender({ cardId: 'blah-blah-666-some-id' });
    expect(mockedUpdateDreamcar).toHaveBeenCalledTimes(0);
    wrapper.dive().simulate('submit');

    await flushPromises();

    expect(mockedUpdateDreamcar).toHaveBeenCalledTimes(1);
});
