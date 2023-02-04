jest.mock('auto-core/react/dataDomain/billing/actions/autoProlongation', () => ({
    toggleAutoProlongationToTransaction: jest.fn(() => () => Promise.resolve()),
}));

import { renderHook, act } from '@testing-library/react-hooks';

import { ScheduleType } from '@vertis/schema-registry/ts-types-snake/auto/api/billing/schedules/schedule_model';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import appStateMock from 'auto-core/react/AppState.mock';
import paymentInfoMock, { placementService, freshService, turboService } from 'auto-core/react/dataDomain/billing/mocks/paymentInfo';
import configMock from 'auto-core/react/dataDomain/config/mock';
import { toggleAutoProlongationToTransaction } from 'auto-core/react/dataDomain/billing/actions/autoProlongation';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { AppStateBilling } from '../AppState';

import useRecurrentPayment from './useRecurrentPayment';

let state: AppStateBilling;

beforeEach(() => {
    state = {
        ...appStateMock,
        billing: {
            paymentInfo: [ paymentInfoMock.value() ],
            selectedTicketId: 'TEST_TICKET_ID',
            reportsBundles: [],
        },
    };
});

describe('флаг isRecurrentPaymentOn', () => {
    it('начальное значение будет true для поднятия в поиске', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([ freshService ]).value() ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isRecurrentPaymentOn).toBe(true);
    });

    it('начальное значение будет true если попросить бэк', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([ { ...turboService, prolongation_forced: true } ]).value() ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isRecurrentPaymentOn).toBe(true);
    });

    it('начальное значение будет false если не попросить бэк', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([ { ...turboService, prolongation_forced: false } ]).value() ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isRecurrentPaymentOn).toBe(false);
    });

    it('будет переключаться при вызове метода set', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([ turboService ]).value() ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());

        expect(result.current.isRecurrentPaymentOn).toBe(false);

        act(() => {
            result.current.setRecurrentPayment(true);
        });

        expect(result.current.isRecurrentPaymentOn).toBe(true);
    });
});

describe('при  маунте', () => {
    it('подключит автопродление к 7дневному размещению если услуга одна в транзакции', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([ { ...placementService, prolongation_forced_not_togglable: true } ]).value() ];
        setupStore(state);
        renderHook(() => useRecurrentPayment());

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(1);
        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledWith(true);
    });

    it('не подключит автопродление к 7дневному размещению если услуга не одна в транзакции', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([
            { ...placementService, prolongation_forced_not_togglable: true },
            turboService ]).value(),
        ];
        setupStore(state);
        renderHook(() => useRecurrentPayment());

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
    });

    it('не подключит автопродление не к 7дневному', () => {
        state.billing.paymentInfo = [ paymentInfoMock.withProducts([
            { ...placementService, prolongation_forced_not_togglable: false },
        ]).value() ];
        setupStore(state);
        renderHook(() => useRecurrentPayment());

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
    });
});

describe('инфа про автопродляемый сервис', () => {
    describe('размещение', () => {
        it('будет даже если нет привязанных карт', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ placementService ]).withPaymentMethods([]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo?.service).toBe(TOfferVas.PLACEMENT);
        });

        it('будет даже если есть другой вас в транзакции', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ placementService, turboService ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo?.service).toBe(TOfferVas.PLACEMENT);
        });

        it('не будет для 7дневного размещения', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ { ...placementService, prolongation_forced_not_togglable: true } ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });
    });

    describe('обычный вас', () => {
        it('не будет если нет привязанных карт', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ turboService ]).withPaymentMethods([]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });

        it('не будет если это форма добавления', () => {
            state.config = configMock.withPageParams({ from: 'add_form' }).value();
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ turboService ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });

        it('не будет если несколько сервисов в транзакции', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ turboService, freshService ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });

        it('не будет если сервис не автопродляемый', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ { ...turboService, prolongation_allowed: false } ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });

        it('будет если есть привязанных карт', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ turboService ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo?.service).toBe(TOfferVas.TURBO);
        });
    });

    describe('поднятие в поиске', () => {
        it('будет если не установлено расписание', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ freshService ]).value(),
            ];
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo?.service).toBe(TOfferVas.FRESH);
        });

        it('не будет если установлено расписание', () => {
            state.billing.paymentInfo = [
                paymentInfoMock.withProducts([ freshService ]).value(),
            ];
            state.billing.boostSchedules = {
                schedule_type: ScheduleType.ONCE_AT_TIME,
                timezone: '',
            };
            setupStore(state);
            const { result } = renderHook(() => useRecurrentPayment());

            expect(result.current.recurrentPaymentInfo).toBe(undefined);
        });
    });
});

describe('флаг автопродления размещения после оплаты', () => {
    it('будет true для 7дневного размещения с васом но без випа', () => {
        state.billing.paymentInfo = [
            paymentInfoMock.withProducts([
                { ...placementService, prolongation_forced_not_togglable: true },
                turboService,
            ]).value(),
        ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isPostPaymentPlacementProlong).toBe(true);
    });

    it('будет false для 7дневного размещения с випом', () => {
        state.billing.paymentInfo = [
            paymentInfoMock.withProducts([
                { ...placementService, prolongation_forced_not_togglable: true },
                { ...turboService, service: TOfferVas.VIP },
            ]).value(),
        ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isPostPaymentPlacementProlong).toBe(false);
    });

    it('будет false для не 7дневного размещения с васом без випа', () => {
        state.billing.paymentInfo = [
            paymentInfoMock.withProducts([
                { ...placementService, prolongation_forced_not_togglable: false },
                turboService,
            ]).value(),
        ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isPostPaymentPlacementProlong).toBe(false);
    });

    it('будет false для 7дневного размещения без васа', () => {
        state.billing.paymentInfo = [
            paymentInfoMock.withProducts([
                { ...placementService, prolongation_forced_not_togglable: true },
            ]).value(),
        ];
        setupStore(state);
        const { result } = renderHook(() => useRecurrentPayment());
        expect(result.current.isPostPaymentPlacementProlong).toBe(false);
    });
});

function setupStore(state?: Partial<AppStateBilling>) {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state || {});
    mockUseDispatch(mockStore(state));
}
