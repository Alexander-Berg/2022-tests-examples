/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

jest.mock('auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutApplicationSubmit');

import { useSelector, useDispatch } from 'react-redux';
import { renderHook, act } from '@testing-library/react-hooks';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type { UserPhone } from 'auto-core/react/dataDomain/user/types';
import { nbsp } from 'auto-core/react/lib/html-entities';
import gateApi from 'auto-core/react/lib/gateApi';
import mockUser from 'auto-core/react/dataDomain/user/mocks';
import * as logBuyoutApplicationSubmitModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutApplicationSubmit';
import * as logBuyoutClickModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutClick';
import { DEFAULT_VEHICLE } from 'auto-core/react/dataDomain/c2bAuctionLanding/data/vehicle';
import { C2BAuctionAppraisalStatus } from 'auto-core/react/dataDomain/c2bAuction/types';

import { ApplicationFormStatus } from '../types';
import { METRIKA_PAGE, MetrikaBlock, MetrikaEventType, MetrikaEventTarget } from '../helpers/sendC2BLandingMetrika';

import { useC2BApplyForm } from './useC2BApplyForm';

const getResourceMock = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

jest.useFakeTimers();

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

const defaultState = {
    can_apply: false,
    price_range: {
        from: 100000,
        to: 1000000,
    },
    price_prediction: 1000000,
};

function createStore(
    appraisalState: Record<string, any> = {},
    userPhones: Array<UserPhone> = [],
) {
    const storeData = {
        c2bAuctionLanding: {
            appraisal: {
                ...defaultState,
                ...appraisalState,
            },
            defaultCarInfo: DEFAULT_VEHICLE,
        },
        user: mockUser.withPhones(userPhones).value(),
    };

    const store = mockStore(storeData);

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(storeData),
    );

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );
}

describe('useC2BApplyForm', () => {

    afterEach(() => {
        getResourceMock.mockClear();
    });

    it('значения по умолчанию', () => {
        createStore();

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        expect(result.current.isAgreed).toBe(false);
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);
        expect(result.current.isFormValid).toBe(false);
        expect(result.current.vehicle).toBe(DEFAULT_VEHICLE);
        expect(result.current.vehicleId).toBe('');
        expect(result.current.mileage).toBe(0);
        expect(result.current.initialPhoneNumber).toBe('');
    });

    it('возвращает правильный телефон из стора с оценкой', () => {
        createStore({
            phoneNumber: '79991231234',
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        expect(result.current.initialPhoneNumber).toBe('79991231234');
    });

    it('возвращает телефон юзера из лк, если его нет в оценке', () => {
        createStore({}, [
            { phone_formatted: '+7(999)1231231', phone: '79991231231' },
            { phone_formatted: '+7(999)1231232', phone: '79991231232' },
        ]);

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        expect(result.current.initialPhoneNumber).toBe('79991231231');
    });

    it('при маунте устанавливает правильный статус, если тачка подходит в аукциону', () => {
        createStore({
            can_apply: true,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        expect(result.current.status).toBe(ApplicationFormStatus.ELIGIBLE);
        //форма по умолчанию невалидна, если тачка подходит
        expect(result.current.isFormValid).toBe(false);
    });

    it('если введены идентификатор тачки и пробег, то сообщает что форма валидна', () => {
        createStore();

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        expect(result.current.isFormValid).toBe(true);
    });

    it('если идентификатор тачки не валидный, то показывает ошибку', () => {
        createStore();

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN1234567890123');
        });

        expect(result.current.isFormValid).toBe(false);
        expect(result.current.vehicleIdError).toBe('Введите правильный VIN/госномер');

        act(() => {
            result.current.onChangeVehicleId('A345AA3');
        });

        expect(result.current.isFormValid).toBe(false);
        expect(result.current.vehicleIdError).toBe('Введите правильный VIN/госномер');
    });

    it('если идентификатор тачки валидный, то скрывает ошибку', () => {
        createStore();

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN1234567890123');
        });

        expect(result.current.isFormValid).toBe(false);
        expect(result.current.vehicleIdError).toBe('Введите правильный VIN/госномер');

        act(() => {
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        expect(result.current.isFormValid).toBe(true);
        expect(result.current.vehicleIdError).toBe('');
    });

    it('если тачка подходит и введены номер телефона и согласие, то форма валидна', () => {
        createStore({
            can_apply: true,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onAgreementChange(true);
            result.current.onPhoneChange('79653332211');
        });

        expect(result.current.status).toBe(ApplicationFormStatus.ELIGIBLE);
        expect(result.current.isFormValid).toBe(true);
    });

    it('устанавливает статус о том что не удалось оценить тачку при сабмите', async() => {
        createStore();
        getResourceMock.mockResolvedValue({
            car_info: {},
            can_apply: false,
            status: C2BAuctionAppraisalStatus.NOT_FOUND,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        act(() => {
            result.current.onSubmit();
        });
        //Пока запрос не выполнился стоит статус загрузки
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        // Ждем выполнения запросов
        await act(async() => {
            await jest.runAllTicks();
        });

        expect(result.current.status).toBe(ApplicationFormStatus.NOT_ELIGIBLE);
    });

    it('если запрос на оценку ответил успехом то проставляем соотвествующий статус и данные тачки', async() => {
        createStore();
        getResourceMock.mockResolvedValue({
            car_info: {
                mileage: 78,
                mark: 'Audi',
                model: 'Q3',
                year: 2017,
                generation: '(B8) Рестайлинг',
            },
            can_apply: true,
            status: C2BAuctionAppraisalStatus.SUCCESS,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        act(() => {
            result.current.onSubmit();
        });
        //Пока запрос не выполнился стоит статус загрузки
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        // Ждем выполнения запросов
        await act(async() => {
            await jest.runAllTicks();
        });

        expect(result.current.status).toBe(ApplicationFormStatus.ELIGIBLE);

        expect(result.current.vehicle).toMatchObject({
            mileage: '78\u00a0км',
            name: `Audi${ nbsp }Q3`,
            generationName: '(B8) Рестайлинг',
            year: 2017,
        });
    });

    it('если запрос на оценку ответил успехом, но нужных данных нет – считаем неуспехом', async() => {
        createStore();
        getResourceMock.mockResolvedValue({
            can_apply: true,
            status: C2BAuctionAppraisalStatus.SUCCESS,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        act(() => {
            result.current.onSubmit();
        });
        //Пока запрос не выполнился стоит статус загрузки
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        // Ждем выполнения запросов
        await act(async() => {
            await jest.runAllTicks();
        });

        expect(result.current.status).toBe(ApplicationFormStatus.NOT_ELIGIBLE);
    });

    it('Для статуса WAITING делаем ретраи в течение 10 секунд и если не получаем ответа, считаем неудачей', async() => {
        const realDateNow = global.Date.now;

        createStore();
        const requestMock = getResourceMock.mockResolvedValue({
            can_apply: true,
            status: C2BAuctionAppraisalStatus.WAITING,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        act(() => {
            global.Date.now = () => 1;
            result.current.onSubmit();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        await act(async() => {
            await jest.runOnlyPendingTimers();
            global.Date.now = () => 5001;
            await jest.runAllTicks();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        await act(async() => {
            await jest.runOnlyPendingTimers();
            global.Date.now = () => 11002;
            await jest.runAllTicks();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.NOT_ELIGIBLE);
        expect(requestMock).toHaveBeenCalledTimes(3);

        global.Date.now = realDateNow;
    });

    it('Для статуса WAITING делаем ретраи в течение 10 секунд и отдаем ответ, если получили раньше', async() => {
        const realDateNow = global.Date.now;

        createStore();
        const requestMock = getResourceMock.mockResolvedValue({
            can_apply: true,
            status: C2BAuctionAppraisalStatus.WAITING,
        });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));
        expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

        act(() => {
            result.current.onChangeMileage(10000);
            result.current.onChangeVehicleId('VIN12345678901234');
        });

        act(() => {
            global.Date.now = () => 1;
            result.current.onSubmit();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        await act(async() => {
            await jest.runOnlyPendingTimers();
            global.Date.now = () => 5001;
            await jest.runAllTicks();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.LOADING);

        requestMock.mockResolvedValue({
            car_info: {
                mileage: 78,
                mark: 'Audi',
                model: 'Q3',
                year: 2017,
                generation: '(B8) Рестайлинг',
            },
            can_apply: true,
            status: C2BAuctionAppraisalStatus.SUCCESS,
        });

        await act(async() => {
            await jest.runOnlyPendingTimers();
            global.Date.now = () => 6002;
            await jest.runAllTicks();
        });
        expect(result.current.status).toBe(ApplicationFormStatus.ELIGIBLE);
        expect(result.current.vehicle).toMatchObject({
            mileage: '78\u00a0км',
            name: `Audi${ nbsp }Q3`,
            generationName: '(B8) Рестайлинг',
            year: 2017,
        });
        expect(requestMock).toHaveBeenCalledTimes(3);

        global.Date.now = realDateNow;
    });

    it('при успешном создании заявки на оцененную тачку проставляем нужный статус', async() => {
        createStore({
            can_apply: true,
        });

        getResourceMock.mockResolvedValue({ application_id: 1 });

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onAgreementChange(true);
            result.current.onPhoneChange('79653332211');
        });

        act(() => {
            result.current.onSubmit();
        });

        await act(async() => {
            await jest.runAllTicks();
        });

        expect(result.current.status).toBe(ApplicationFormStatus.SUCCESS);
    });

    it('при неуспешном создании заявки на оцененную тачку возвращаем ошибку', async() => {
        createStore({
            can_apply: true,
        });

        getResourceMock.mockResolvedValue(undefined);

        const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

        act(() => {
            result.current.onAgreementChange(true);
            result.current.onPhoneChange('79653332211');
        });

        act(() => {
            result.current.onSubmit();
        });

        await act(async() => {
            await jest.runAllTicks();
        });

        expect(result.current.error).toBe('Не удалось создать заявку, попробуйте снова');
    });

    describe('Метрика и фронтлог - отпрвляет нужные события', () => {
        it('при вводе пробега и вина, сабмите на начальном шаге и возврате назад', async() => {
            const { logClickMock } = mockFrontLog();

            createStore();

            getResourceMock.mockResolvedValue({
                car_info: {},
                can_apply: false,
            });

            const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

            expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

            act(() => {
                result.current.onChangeMileage(10000);

                expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                    METRIKA_PAGE,
                    MetrikaBlock.FORM_EVALUATION,
                    MetrikaEventType.ADD,
                    MetrikaEventTarget.MILEAGE,
                ]);

                result.current.onChangeVehicleId('VIN12345678901234');

                expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                    METRIKA_PAGE,
                    MetrikaBlock.FORM_EVALUATION,
                    MetrikaEventType.ADD,
                    MetrikaEventTarget.VIN,
                ]);
            });

            act(() => {
                result.current.onSubmit();
            });

            expect(logClickMock).toHaveBeenCalledTimes(1);
            expect(logClickMock).toHaveBeenLastCalledWith({
                category: 'cars',
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });

            expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                METRIKA_PAGE,
                MetrikaBlock.FORM_EVALUATION,
                MetrikaEventType.CLICK,
                MetrikaEventTarget.CHECK,
            ]);

            await act(async() => {
                await jest.runAllTicks();
            });

            act(() => {
                result.current.erase(MetrikaBlock.FORM_EVALUATION);

                expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                    METRIKA_PAGE,
                    MetrikaBlock.FORM_EVALUATION,
                    MetrikaEventType.CLICK,
                    MetrikaEventTarget.NEW_EVALUATION,
                ]);
            });
        });

        it('при показе шага с ошибкой и возврате на начальный шаг', async() => {
            createStore();

            getResourceMock.mockResolvedValue(undefined);

            const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

            expect(result.current.status).toBe(ApplicationFormStatus.IDLE);

            act(() => {
                result.current.onChangeMileage(10000);
                result.current.onChangeVehicleId('VIN12345678901234');
            });

            act(() => {
                result.current.onSubmit();
            });

            await act(async() => {
                await jest.runAllTicks();
            });

            act(() => {
                result.current.erase(MetrikaBlock.FORM_EVALUATION);

                expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                    METRIKA_PAGE,
                    MetrikaBlock.FORM_EVALUATION,
                    MetrikaEventType.CLICK,
                    MetrikaEventTarget.NEW_EVALUATION,
                ]);
            });
        });

        it('при создании заявки', async() => {
            const { logSubmitMock } = mockFrontLog();

            createStore({
                can_apply: true,
            });

            getResourceMock.mockResolvedValue({ application_id: 1 });

            const { result } = renderHook(() => useC2BApplyForm(contextMock.metrika));

            act(() => {
                result.current.onAgreementChange(true);
                result.current.onPhoneChange('79653332211');
            });

            act(() => {
                result.current.onSubmit();

                expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([
                    METRIKA_PAGE,
                    MetrikaBlock.FORM_INSPECTION,
                    MetrikaEventType.CLICK,
                    MetrikaEventTarget.APPLICATION_CREATE,
                ]);
            });

            await act(async() => {
                await jest.runAllTicks();
            });

            expect(logSubmitMock).toHaveBeenCalledTimes(1);
            expect(logSubmitMock).toHaveBeenLastCalledWith({
                category: 'cars',
                applicationId: 1,
                contextPage: ContextPage.PAGE_LANDING_BUYOUT,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });
    });
});

function mockFrontLog() {
    const logClickMock = jest.fn();
    const logSubmitMock = jest.fn();

    jest.spyOn(logBuyoutClickModule, 'logBuyoutClick').mockImplementation(logClickMock);
    jest.spyOn(logBuyoutApplicationSubmitModule, 'logBuyoutApplicationSubmit').mockImplementation(logSubmitMock);

    return {
        logClickMock,
        logSubmitMock,
    };
}
