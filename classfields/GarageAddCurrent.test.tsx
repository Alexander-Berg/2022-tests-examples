/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn().mockResolvedValue({}),
    };
});

import type { ShallowWrapper } from 'enzyme';
import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { act } from 'react-dom/test-utils';
import { shallow, mount } from 'enzyme';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { StateVinCheckInput } from 'auto-core/react/dataDomain/vinCheckInput/types';
import gateApi from 'auto-core/react/lib/gateApi';

import fixturesGetCard from 'auto-core/server/resources/publicApiGarage/methods/getCard.fixtures';
import fixturesGetVehicleInfo from 'auto-core/server/resources/publicApiGarage/methods/getVehicleInfo.fixtures.js';
import fixturesAddCard from 'auto-core/server/resources/publicApiGarage/methods/addCard.fixtures';

import GarageAddCurrent from './GarageAddCurrent';

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;
const Context = createContextProvider(contextMock);

const TEST_VIN = 'XTA211440B5032032';

interface State {
    vinCheckInput: StateVinCheckInput;
}

let originalWindowLocation: Location;
let state: State;
let store: ThunkMockStore<State>;

beforeEach(() => {
    state = {
        vinCheckInput: { value: 'JF1GT7LL5JG008055' },
    };
    store = mockStore(state);

    originalWindowLocation = global.window.location;
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.window.location;

    global.window.location = { ...originalWindowLocation };

    getResourcePublicApi.mockClear();
});

afterEach(() => {
    global.window.location = originalWindowLocation;
    getResourcePublicApi.mockClear();
});

describe('поиск по VIN', () => {
    it('должен нарисовать превью, если нашли VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValue(fixturesGetVehicleInfo.responseReport());

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                const step = wrapper.find('GarageAddCardMobileStep1').dive();
                expect(step.find('GarageAddCardPreview')).toHaveLength(1);
                expect(step.find('GarageAddCardPreview')).toHaveProp({
                    vehicleInfo: fixturesGetVehicleInfo.responseReport(),
                });
                expect(step.find('.GarageAddCardMobileStep1__preview Button').dive()).toHaveText('Добавить в гараж');
            });
    });

    it('должен нарисовать карточку, если нашли авто в гараже по VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValue(fixturesGetCard.response200());

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                const step = wrapper.find('GarageAddCardMobileStep1').dive();
                expect(step.find('GarageAddCardPreview')).toHaveLength(1);
                expect(step.find('GarageAddCardPreview')).toHaveProp({
                    vehicleInfo: fixturesGetCard.response200(),
                });
                expect(step.find('.GarageAddCardMobileStep1__preview Button').dive()).toHaveText('Перейти в гараж');
                expect(step.find('.GarageAddCardMobileStep1__preview Button')).toHaveProp('url', 'link/garage-card/?card_id=58133405');
            });
    });

    it('должен залипнуть лоадер и перезапросить данные, если ручка ответила IN_PROGRESS', async() => {
        const wrapper = mount(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent retryTimeout={ 0 } params={{ vin_or_license_plate: TEST_VIN }}/>
                </Provider>
            </Context>,
        );

        getResourcePublicApi.mockRejectedValueOnce({ error: 'IN_PROGRESS', status: 'ERROR' });

        await act(async() => {
            wrapper.find('GarageAddCardMobileStep1').find('Button').simulate('click');
            await flushPromises();
            await flushPromises();
        });

        const step = wrapper.find('GarageAddCardMobileStep1');
        expect(step.find('Loader')).toHaveLength(1);
        expect(getResourcePublicApi).toHaveBeenCalledTimes(2);
    });

    it('должен нарисовать ошибку, если ручка не ответила', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'UNKNOWN_ERROR', status: 'ERROR' });

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                const step = wrapper.find('GarageAddCardMobileStep1').dive();
                expect(step.find('GarageAddCardError')).toHaveLength(1);
            });
    });

    it('должен нарисовать ошибку, если ручка стаймаутилась', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'REQUEST_TIMEOUT', status: 'ERROR' });

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                const step = wrapper.find('GarageAddCardMobileStep1').dive();
                expect(step.find('GarageAddCardError')).toHaveLength(1);
            });
    });

    it('должен открыть форму ручного добавления, если VIN не найден', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'VIN_CODE_NOT_FOUND', status: 'ERROR' });

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'add', 'not_find', 'VIN_CODE_NOT_FOUND' ]);
                wrapper.find('GarageAddCardMobileStep1').dive().find('.GarageAddCardMobileStep1__submit Button').simulate('click');
                expect(wrapper.find('GarageFormFieldsSelectorCurrent')).toHaveLength(1);
            });
    });

    it('должен сбросить состояние после фокуса в поле ввода', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'VIN_CODE_NOT_FOUND', status: 'ERROR' });

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                wrapper.find('GarageAddCardMobileStep1').dive().find('TopSearchField').simulate('focusChange', true);
                expect(wrapper.find('GarageAddCardMobileStep1').dive().find('GarageAddCardSpoiler')).toHaveLength(1);
            });
    });

    it('должен нарисовать GarageAddCardNotFound, если ручка прислала ошибку с невалидным VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'VIN_CODE_INVALID', status: 'ERROR' });

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                const step = wrapper.find('GarageAddCardMobileStep1').dive();
                expect(step.find('GarageAddCardNotFound')).toHaveLength(1);
            });
    });
});

describe('добавление по VIN', () => {
    let wrapper: ShallowWrapper;
    beforeEach(async() => {
        wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <GarageAddCurrent/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValue(fixturesGetVehicleInfo.responseReport());

        wrapper.find('GarageAddCardMobileStep1').dive().find('Button').simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));
    });

    it('должен перейти на карточку, если она добавилась', async() => {
        getResourcePublicApi.mockResolvedValueOnce(fixturesGetCard.response200());
        wrapper.find('GarageAddCardMobileStep1').dive().find('.GarageAddCardMobileStep1__preview Button').simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(location.href).toEqual('link/garage-card/?card_id=58133405');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'add', 'add' ]);
    });

    it('должен перейти на карточку и отправить цели, если она добавилась', async() => {
        getResourcePublicApi.mockResolvedValueOnce(fixturesGetCard.response200());
        wrapper.find('GarageAddCardMobileStep1').dive().find('.GarageAddCardMobileStep1__preview Button').simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('add-car-landing_garage_success');
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('add-car-all-page_garage_success');
    });

    it('должен показать общую ошибку, если ручка 500ит', async() => {
        getResourcePublicApi.mockRejectedValueOnce({ error: 'UNKNOWN_ERROR', status: 'ERROR' });
        wrapper.find('GarageAddCardMobileStep1').dive().find('.GarageAddCardMobileStep1__preview Button').simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(store.getActions()).toEqual([
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Ошибка. Попробуйте снова', view: 'error' },
            },
        ]);
    });

    it('должен показать ошибку, если ручка ответила типизированной ошибкой', async() => {
        getResourcePublicApi.mockRejectedValueOnce(fixturesAddCard.response400TooManyCards());
        wrapper.find('GarageAddCardMobileStep1').dive().find('.GarageAddCardMobileStep1__preview Button').simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(store.getActions()).toEqual([
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Превышен лимит созданных карт', view: 'error' },
            },
        ]);
    });
});
