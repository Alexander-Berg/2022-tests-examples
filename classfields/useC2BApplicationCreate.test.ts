/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
jest.mock('auto-core/react/components/common/C2BAuctions/helpers/createC2BApplication');
jest.mock('auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutApplicationSubmit');

import type { FormEvent } from 'react';
import { renderHook, act } from '@testing-library/react-hooks';
import { noop } from 'lodash';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import * as logBuyoutApplicationSubmitModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutApplicationSubmit';
import createC2BApplication from 'auto-core/react/components/common/C2BAuctions/helpers/createC2BApplication';
import { C2BCreateApplicationSource } from 'auto-core/react/components/common/C2BAuctions/types';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import { useC2BApplicationCreate } from './useC2BApplicationCreate';

const MOCK_OFFER = { category: 'cars', saleId: '111-111', seller: { phones: [ { original: '+7 999 999-99-99' } ] } } as Offer;
const METRIKA_MOCK = { reachGoal: noop };

describe('Создание заявки на Аукцион', () => {
    it('Управляет чекбоксом согласия на обработку данных', () => {
        mockFrontLog();
        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: METRIKA_MOCK,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        expect(result.current.approveIsChecked).toBe(false);

        act(() => result.current.setApproveChecked(true));
        expect(result.current.approveIsChecked).toBe(true);
    });

    it('Управляет сабмитом формы и отправляет событие метрики при успешной отправке для черновика', async() => {
        jest.useFakeTimers('modern');
        jest.setSystemTime(new Date(2022, 0, 1, 0, 0));

        const createApplicationMock = createApplicationActionMock();

        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: ({ ...MOCK_OFFER, status: 'DRAFT' }) as Offer,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        act(() => result.current.setApproveChecked(true));
        await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));

        expect(createApplicationMock).toHaveBeenCalledTimes(1);
        expect(createApplicationMock).toHaveBeenLastCalledWith({
            offerId: undefined,
            draftId: '000-000',
            phoneNumber: '+7 999 999-99-99',
            source: C2BCreateApplicationSource.OfferCard,
        });

        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_READY', {
            offer_id: '000-000',
        });
        jest.useRealTimers();
    });

    it('Управляет сабмитом формы и отправляет событие метрики при успешной отправке для оффера', async() => {
        jest.useFakeTimers('modern');
        jest.setSystemTime(new Date(2022, 0, 1, 0, 0));

        const createApplicationMock = createApplicationActionMock();

        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        act(() => result.current.setApproveChecked(true));
        await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));

        expect(createApplicationMock).toHaveBeenCalledTimes(1);
        expect(createApplicationMock).toHaveBeenLastCalledWith({
            offerId: '111-111',
            draftId: '000-000',
            phoneNumber: '+7 999 999-99-99',
            source: C2BCreateApplicationSource.OfferCard,
        });

        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_READY', {
            offer_id: MOCK_OFFER.saleId,
        });
        jest.useRealTimers();
    });

    it('Не сабмитит форму, если нет номера телефона', async() => {
        const createApplicationMock = createApplicationActionMock();
        const metrikaMock = { reachGoal: jest.fn() };

        mockFrontLog();

        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: { ...MOCK_OFFER, seller: { phones: [] } } as unknown as Offer,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        act(() => result.current.setApproveChecked(true));
        await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));

        expect(createApplicationMock).toHaveBeenCalledTimes(0);
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(0);
    });

    it('Не сабмитит форму, если нет согласия с условиями', async() => {
        const createApplicationMock = createApplicationActionMock();
        const metrikaMock = { reachGoal: jest.fn() };

        mockFrontLog();

        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        expect(result.current.approveIsChecked).toBe(false);
        await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));

        expect(createApplicationMock).toHaveBeenCalledTimes(0);
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(0);
    });

    it('Возвращает флаги статуса запроса', async() => {
        const metrikaMock = { reachGoal: jest.fn() };
        mockFrontLog();

        const { result } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
        }));

        expect(result.current.isLoading).toBe(false);
        expect(result.current.isSuccess).toBe(false);
        expect(result.current.isError).toBe(false);

        act(() => result.current.setApproveChecked(true));

        // успех
        {
            createApplicationActionMock({ application_id: '123' });
            const requestPromise = act(() => result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(result.current.isLoading).toBe(true);
            expect(result.current.isSuccess).toBe(false);
            expect(result.current.isError).toBe(false);

            await requestPromise;
            expect(result.current.isLoading).toBe(false);
            expect(result.current.isSuccess).toBe(true);
            expect(result.current.isError).toBe(false);
        }

        // неуспех из-за плохих данных
        {
            createApplicationActionMock({});
            const requestPromise = act(() => result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(result.current.isLoading).toBe(true);
            expect(result.current.isSuccess).toBe(false);
            expect(result.current.isError).toBe(false);

            await requestPromise;
            expect(result.current.isLoading).toBe(false);
            expect(result.current.isSuccess).toBe(false);
            expect(result.current.isError).toBe(true);
        }

        // неуспех из-за падения запроса
        {
            createApplicationActionMock({ application_id: '123' }, true);
            const requestPromise = act(() => result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(result.current.isLoading).toBe(true);
            expect(result.current.isSuccess).toBe(false);
            expect(result.current.isError).toBe(false);

            await requestPromise;
            expect(result.current.isLoading).toBe(false);
            expect(result.current.isSuccess).toBe(false);
            expect(result.current.isError).toBe(true);
        }
    });

    it('Дергает коллбэк onAuctionCreate на анмаунте и только если был успешный запрос на создание заявки', async() => {
        const metrikaMock = { reachGoal: jest.fn() };
        mockFrontLog();
        createApplicationActionMock({ application_id: '123' });
        const onAuctionCreateMock = jest.fn();

        const { result, unmount } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
            onAuctionCreate: onAuctionCreateMock,
        }));

        expect(result.current.isSuccess).toBe(false);

        act(() => result.current.setApproveChecked(true));
        await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));

        expect(result.current.isSuccess).toBe(true);
        unmount();
        expect(onAuctionCreateMock).toHaveBeenCalledTimes(1);
    });

    it('Не дергает коллбэк onAuctionCreate на анмаунте если не было успешной заявки', async() => {
        const metrikaMock = { reachGoal: jest.fn() };
        mockFrontLog();
        createApplicationActionMock({ application_id: '123' });
        const onAuctionCreateMock = jest.fn();

        const { result, unmount } = renderHook(() => useC2BApplicationCreate({
            offer: MOCK_OFFER,
            draftId: '000-000',
            metrika: metrikaMock,
            source: C2BCreateApplicationSource.OfferCard,
            contextBlock: ContextBlock.BLOCK_CARD,
            onAuctionCreate: onAuctionCreateMock,
        }));

        expect(result.current.isSuccess).toBe(false);
        unmount();
        expect(onAuctionCreateMock).toHaveBeenCalledTimes(0);
    });

    describe('Фронтлог', () => {
        it('Отправляет фронтлог при успешной подаче заявки и наличия id заявки', async() => {
            const logSubmitMock = mockFrontLog();
            createApplicationActionMock({ application_id: '333' });

            const { result } = renderHook(() => useC2BApplicationCreate({
                offer: MOCK_OFFER,
                draftId: '000-000',
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
                contextBlock: ContextBlock.BLOCK_CARD,
            }));

            expect(logSubmitMock).toHaveBeenCalledTimes(0);

            act(() => result.current.setApproveChecked(true));
            await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(logSubmitMock).toHaveBeenCalledTimes(1);
        });

        it('Отправляет фронтлог для черновика', async() => {
            const logSubmitMock = mockFrontLog();
            createApplicationActionMock({ application_id: '333' });

            const { result } = renderHook(() => useC2BApplicationCreate({
                offer: ({ ...MOCK_OFFER, status: 'DRAFT' }) as Offer,
                draftId: '000-000',
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
                contextBlock: ContextBlock.BLOCK_CARD,
            }));

            act(() => result.current.setApproveChecked(true));
            await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(logSubmitMock).toHaveBeenCalledTimes(1);
            expect(logSubmitMock).toHaveBeenLastCalledWith({
                category: MOCK_OFFER.category,
                applicationId: '333',
                draftId: '000-000',
                contextPage: ContextPage.PAGE_BUYOUT_CREATE,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('Отправляет фронтлог для оффера', async() => {
            const logSubmitMock = mockFrontLog();
            createApplicationActionMock({ application_id: '333' });

            const { result } = renderHook(() => useC2BApplicationCreate({
                offer: ({ ...MOCK_OFFER, status: 'ACTIVE' }) as Offer,
                draftId: '000-000',
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
                contextBlock: ContextBlock.BLOCK_CARD,
            }));

            act(() => result.current.setApproveChecked(true));
            await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(logSubmitMock).toHaveBeenCalledTimes(1);
            expect(logSubmitMock).toHaveBeenLastCalledWith({
                category: MOCK_OFFER.category,
                applicationId: '333',
                draftId: '000-000',
                offerId: MOCK_OFFER.saleId,
                contextPage: ContextPage.PAGE_BUYOUT_CREATE,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('Отправляет во фронтлог переданный contextBlock', async() => {
            const logSubmitMock = mockFrontLog();
            createApplicationActionMock({ application_id: '333' });

            const { result } = renderHook(() => useC2BApplicationCreate({
                offer: ({ ...MOCK_OFFER, status: 'DRAFT' }) as Offer,
                draftId: '000-000',
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
                contextBlock: ContextBlock.BLOCK_POPUP,
            }));

            act(() => result.current.setApproveChecked(true));
            await act(async() => await result.current.handleSubmit({ preventDefault: noop } as FormEvent<HTMLFormElement>));
            expect(logSubmitMock).toHaveBeenCalledTimes(1);
            expect(logSubmitMock).toHaveBeenLastCalledWith({
                category: MOCK_OFFER.category,
                applicationId: '333',
                draftId: '000-000',
                contextPage: ContextPage.PAGE_BUYOUT_CREATE,
                contextBlock: ContextBlock.BLOCK_POPUP,
            });
        });
    });

});

function createApplicationActionMock(data: any = {}, reject = false) {
    const createApplicationMock = jest.fn(() => (reject ? Promise.reject() : Promise.resolve(data)));

    (createC2BApplication as jest.MockedFunction<typeof createC2BApplication>)
        .mockImplementation(createApplicationMock);

    return createApplicationMock;
}

function mockFrontLog() {
    const logSubmitMock = jest.fn();

    jest.spyOn(logBuyoutApplicationSubmitModule, 'logBuyoutApplicationSubmit').mockImplementation(logSubmitMock);

    return logSubmitMock;
}
