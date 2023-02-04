/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn().mockResolvedValue({}),
    };
});

jest.mock('react-redux', () => {
    return {
        ...jest.requireActual('react-redux'),
        useDispatch: jest.fn(),
    };
});

import type { ShallowWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';
import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
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

import PageGarageAddCard from './PageGarageAddCard';

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;

const Context = createContextProvider(contextMock);

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
});

afterEach(() => {
    global.window.location = originalWindowLocation;
});

describe('поиск по VIN', () => {
    it('должен нарисовать превью, если нашли VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValue(fixturesGetVehicleInfo.responseReport());

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(wrapper.find('GarageAddCardPreview')).toHaveLength(1);
                expect(wrapper.find('GarageAddCardPreview')).toHaveProp({
                    vehicleInfo: fixturesGetVehicleInfo.responseReport(),
                });
                expect(wrapper.find('Button').first().dive()).toHaveText('Добавить в гараж');
            });
    });

    it('должен нарисовать карточку, если нашли авто в гараже по VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValue(fixturesGetCard.response200());

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(wrapper.find('GarageAddCardPreview')).toHaveLength(1);
                expect(wrapper.find('GarageAddCardPreview')).toHaveProp({
                    vehicleInfo: fixturesGetCard.response200(),
                });
                expect(wrapper.find('Button').first().dive()).toHaveText('Перейти в гараж');
                expect(wrapper.find('Button').first()).toHaveProp('url', 'link/garage-card/?card_id=58133405');
            });
    });

    it('должен нарисовать ошибку, если ручка не ответила', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'UNKNOWN_ERROR', status: 'ERROR' });

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(wrapper.find('GarageAddCardError')).toHaveLength(1);
            });
    });

    it('должен нарисовать ошибку, если ручка стаймаутилась', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'REQUEST_TIMEOUT', status: 'ERROR' });

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(wrapper.find('GarageAddCardError')).toHaveLength(1);
            });
    });

    it('должен сохранить лоадер и перезапросить данные, если ручка IN_PROGRESS', async() => {
        // здесь принципиально протестировать всю цепочку обновлений стейта и таймеров,
        // поэтому mount
        const wrapper = mount(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard retryTimeout={ 0 }/>
                </Provider>
            </Context>,
        );

        getResourcePublicApi.mockRejectedValueOnce({ error: 'IN_PROGRESS', status: 'ERROR' });

        await act(async() => {
            wrapper.find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');
            await flushPromises();
            await flushPromises();
        });

        expect(wrapper.find('Loader')).toHaveLength(1);
        expect(getResourcePublicApi).toHaveBeenCalledTimes(2);
    });

    it('должен нарисовать ошибку, если ручка прислала ошибку с невалидным VIN', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'VIN_CODE_INVALID', status: 'ERROR' });

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(wrapper.find('GarageAddCardError')).toHaveLength(1);
                // так же передает правильный урл для ссылки
                expect(wrapper.find('GarageAddCardError').prop('url')).toEqual('link/garage-add-manual/?type=current&vin=JF1GT7LL5JG008055');
            });
    });

    it('должен редиректнуть на форму ручного добавления, если VIN не найден', () => {
        const vin = 'JF1GT7LL5JG008055';
        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockRejectedValue({ error: 'VIN_CODE_NOT_FOUND', status: 'ERROR' });

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', vin);

        return new Promise((resolve) => setTimeout(resolve, 50))
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'add', 'not_find', 'VIN_CODE_NOT_FOUND' ]);
                expect(contextMock.pushState).toHaveBeenLastCalledWith(`link/garage-add-manual/?type=current&vin=${ vin }`);
            });
    });
});

describe('добавление по VIN', () => {
    let wrapper: ShallowWrapper;
    beforeEach(async() => {
        wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PageGarageAddCard/>
                </Provider>
            </Context>,
        ).dive().dive().dive();

        getResourcePublicApi.mockResolvedValueOnce(fixturesGetVehicleInfo.responseReport());

        wrapper.find('GarageAddCardInputDesktop').dive().find('GarageVinOrLicensePlateInput').simulate('submit', 'JF1GT7LL5JG008055');

        await new Promise((resolve) => setTimeout(resolve, 50));
    });

    it('должен перейти на карточку и отправить метрику, если она добавилась', async() => {
        getResourcePublicApi.mockResolvedValueOnce(fixturesGetCard.response200());
        wrapper.find('Button').first().simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(location.href).toEqual('link/garage-card/?card_id=58133405');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'add', 'add' ]);
    });

    it('должен перейти на карточку и отправить цели, если она добавилась', async() => {
        getResourcePublicApi.mockResolvedValueOnce(fixturesGetCard.response200());
        wrapper.find('Button').first().simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(location.href).toEqual('link/garage-card/?card_id=58133405');
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('add-car-landing_garage_success');
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('add-car-all-page_garage_success');
    });

    it('должен показать общую ошибку, если ручка 500ит', async() => {
        getResourcePublicApi.mockRejectedValueOnce({ error: 'UNKNOWN_ERROR', status: 'ERROR' });
        wrapper.find('Button').first().simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(store.getActions()).toEqual([
            {
                type: 'CLEAR_FORM_FIELDS',
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Ошибка. Попробуйте снова', view: 'error' },
            },
        ]);
    });

    it('должен показать ошибку, если ручка ответила типизированной ошибкой', async() => {
        getResourcePublicApi.mockRejectedValueOnce(fixturesAddCard.response400TooManyCards());
        wrapper.find('Button').first().simulate('click');

        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(store.getActions()).toEqual([
            {
                type: 'CLEAR_FORM_FIELDS',
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Превышен лимит созданных карт', view: 'error' },
            },
        ]);
    });
});
