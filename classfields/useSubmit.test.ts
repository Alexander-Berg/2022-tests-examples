jest.mock('www-billing/react/hooks/useCheckPaymentStatus', () => {
    return jest.fn(() => () => Promise.resolve());
});
jest.mock('auto-core/react/dataDomain/billing/actions/autoProlongation', () => ({
    toggleAutoProlongationToTransaction: jest.fn(() => () => Promise.resolve()),
    setAutoBoostSchedule: jest.fn(() => () => Promise.resolve()),
    addAutoProlongationToOffer: jest.fn(() => () => Promise.resolve()),
}));
jest.mock('www-billing/react/hooks/usePostWindowMessage');
jest.mock('www-billing/react/hooks/usePayByWallet');

import { renderHook, act } from '@testing-library/react-hooks';
import MockDate from 'mockdate';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import {
    toggleAutoProlongationToTransaction,
    setAutoBoostSchedule,
    addAutoProlongationToOffer,
} from 'auto-core/react/dataDomain/billing/actions/autoProlongation';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { PaymentFrameMessageTypes, PaymentMethodIds } from 'auto-core/types/TBilling';

import useCheckPaymentStatus from 'www-billing/react/hooks/useCheckPaymentStatus';
import usePostWindowMessage from 'www-billing/react/hooks/usePostWindowMessage';
import usePayByWallet from 'www-billing/react/hooks/usePayByWallet';

import type { AppStateBilling } from '../AppState';

import type { Params } from './useSubmit';
import useSubmit from './useSubmit';

let state: Partial<AppStateBilling>;
let params: Params;

const useCheckPaymentStatusMock = useCheckPaymentStatus as jest.MockedFunction<typeof useCheckPaymentStatus>;
const paymentStatusChecker = jest.fn(() => Promise.resolve());
useCheckPaymentStatusMock.mockReturnValue(paymentStatusChecker);

const usePayByWalletMock = usePayByWallet as jest.MockedFunction<typeof usePayByWallet>;
const walletPayer = jest.fn(() => Promise.resolve());
usePayByWalletMock.mockReturnValue(walletPayer);

const usePostWindowMessageMock = usePostWindowMessage as jest.MockedFunction<typeof usePostWindowMessage>;
const messagePoster = jest.fn(() => {});
usePostWindowMessageMock.mockReturnValue(messagePoster);

beforeEach(() => {
    state = {};
    params = {
        freezeFrameHeight: jest.fn(),
        onSuccess: jest.fn(),
        onError: jest.fn(),
        ticketId: 'ticket_id',
        purchaseToken: 'token',
        recurrentPaymentInfo: undefined,
        isRecurrentPaymentOn: false,
        isPostPaymentPlacementProlong: false,
        dispatchFrameAction: jest.fn(),
        handle3ds: jest.fn(),
    };
    MockDate.set('2022-05-25T13:42:00+03:00');
});

afterEach(() => {
    MockDate.reset();
});

it('при вызове фризит высоту окна', () => {
    setupStore(state);
    const { result } = renderHook(() => useSubmit(params));
    act(() => {
        result.current();
    });

    expect(params.freezeFrameHeight).toHaveBeenCalledTimes(1);
});

describe('проведение платежа', () => {
    it('для обычного платежа запустит статус чеккер', () => {
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        expect(paymentStatusChecker).toHaveBeenCalledTimes(1);
        expect(walletPayer).toHaveBeenCalledTimes(0);
        expect(params.dispatchFrameAction).toHaveBeenCalledTimes(0);
    });

    it('для платежа кошельком вызовет хук и переведет статус формы в PROCESS_PAYMENT', () => {
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current({ method: PaymentMethodIds.WALLET });
        });

        expect(paymentStatusChecker).toHaveBeenCalledTimes(0);
        expect(walletPayer).toHaveBeenCalledTimes(1);
        expect(params.dispatchFrameAction).toHaveBeenCalledTimes(1);
        expect(params.dispatchFrameAction).toHaveBeenCalledWith({ type: 'PROCESS_PAYMENT' });
    });
});

describe('автопродление обычного васа', () => {
    it('подключит к транзакции если есть инфа и галочка взведена', () => {
        params.recurrentPaymentInfo = {
            service: TOfferVas.COLOR, name: 'Цвет', effective_price: 179,
        };
        params.isRecurrentPaymentOn = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(1);
        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledWith(true);
    });

    it('не подключит к транзакции если есть инфа но галочка не взведена', () => {
        params.recurrentPaymentInfo = {
            service: TOfferVas.COLOR, name: 'Цвет', effective_price: 179,
        };
        params.isRecurrentPaymentOn = false;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
    });

    it('не подключит к транзакции если нет инфы', () => {
        params.isRecurrentPaymentOn = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
    });

    it('не подключит к транзакции если это поднятие в поиске', () => {
        params.recurrentPaymentInfo = {
            service: TOfferVas.FRESH, name: 'Поднятие', effective_price: 179,
        };
        params.isRecurrentPaymentOn = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        expect(toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
    });
});

describe('автопродление размещения', () => {
    it('подключит если есть флаг', async() => {
        params.isPostPaymentPlacementProlong = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();

        expect(addAutoProlongationToOffer).toHaveBeenCalledTimes(1);
        expect(addAutoProlongationToOffer).toHaveBeenCalledWith(TOfferVas.PLACEMENT);
    });

    it('не подключит если нет флага', async() => {
        params.isPostPaymentPlacementProlong = false;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();

        expect(addAutoProlongationToOffer).toHaveBeenCalledTimes(0);
    });
});

describe('автоподнияте', () => {
    it('подключит если есть флаг', async() => {
        params.recurrentPaymentInfo = {
            service: TOfferVas.FRESH, name: 'Поднятие', effective_price: 179,
        };
        params.isRecurrentPaymentOn = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();

        expect(setAutoBoostSchedule).toHaveBeenCalledTimes(1);
        expect(setAutoBoostSchedule).toHaveBeenCalledWith('13:00');
    });

    it('не подключит если нет флага', async() => {
        params.recurrentPaymentInfo = {
            service: TOfferVas.FRESH, name: 'Поднятие', effective_price: 179,
        };
        params.isRecurrentPaymentOn = false;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();

        expect(setAutoBoostSchedule).toHaveBeenCalledTimes(0);
    });

    it('не подключит если нет инфы', async() => {
        params.recurrentPaymentInfo = undefined;
        params.isRecurrentPaymentOn = true;
        setupStore(state);
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();

        expect(setAutoBoostSchedule).toHaveBeenCalledTimes(0);
    });
});

describe('при успешном платеже', () => {
    beforeEach(async() => {
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();
    });

    it('отправит сообщение родителю', () => {
        expect(messagePoster).toHaveBeenCalledTimes(1);
        expect(messagePoster).toHaveBeenCalledWith({ type: PaymentFrameMessageTypes.PAYMENT_SUCCESS });
    });

    it('поменяет состояние фрейма', () => {
        expect(params.dispatchFrameAction).toHaveBeenCalledTimes(1);
        expect(params.dispatchFrameAction).toHaveBeenCalledWith({ type: 'COMPLETE_PAYMENT' });
    });

    it('вызовет коллбэк', () => {
        expect(params.onSuccess).toHaveBeenCalledTimes(1);
    });
});

describe('при ошибке', () => {
    beforeEach(async() => {
        paymentStatusChecker.mockReturnValueOnce(Promise.reject());
        const { result } = renderHook(() => useSubmit(params));
        act(() => {
            result.current();
        });

        await flushPromises();
    });

    it('поменяет состояние фрейма', () => {
        expect(params.dispatchFrameAction).toHaveBeenCalledTimes(1);
        expect(params.dispatchFrameAction).toHaveBeenCalledWith({ type: 'FAIL_PAYMENT' });
    });

    it('вызовет коллбэк', () => {
        expect(params.onError).toHaveBeenCalledTimes(1);
    });
});

function setupStore(state?: Partial<AppStateBilling>) {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state || {});
    mockUseDispatch(mockStore(state));
}
